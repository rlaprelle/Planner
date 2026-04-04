package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.schedule.dto.SavePlanRequest;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ScheduleService {

    private static final LocalTime DAY_START = LocalTime.of(8, 0);
    private static final LocalTime DAY_END = LocalTime.of(17, 0);

    private final TimeBlockRepository timeBlockRepository;
    private final TaskRepository taskRepository;

    public ScheduleService(TimeBlockRepository timeBlockRepository, TaskRepository taskRepository) {
        this.timeBlockRepository = timeBlockRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<TimeBlockResponse> getToday(AppUser user) {
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));
        return timeBlockRepository.findByUserIdAndBlockDateWithTask(user.getId(), today)
                .stream()
                .map(TimeBlockResponse::from)
                .toList();
    }

    public List<TimeBlockResponse> savePlan(AppUser user, SavePlanRequest request) {
        validateBlocks(request.blocks());

        // Use the same server-computed date as getToday() and the dashboard,
        // so save and load always agree on which date "today" is.
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));

        timeBlockRepository.deleteByUserIdAndBlockDate(user.getId(), today);

        List<TimeBlock> toSave = new ArrayList<>();
        for (int i = 0; i < request.blocks().size(); i++) {
            SavePlanRequest.BlockEntry entry = request.blocks().get(i);
            Task task = taskRepository.findByIdAndUserId(entry.taskId(), user.getId())
                    .orElseThrow(() -> new ScheduleValidationException(
                            "Task not found: " + entry.taskId()));
            toSave.add(new TimeBlock(user, today, task, entry.startTime(), entry.endTime(), i));
        }

        return timeBlockRepository.saveAll(toSave)
                .stream()
                .map(TimeBlockResponse::from)
                .toList();
    }

    private void validateBlocks(List<SavePlanRequest.BlockEntry> blocks) {
        for (int i = 0; i < blocks.size(); i++) {
            SavePlanRequest.BlockEntry b = blocks.get(i);

            if (b.startTime().getMinute() % 15 != 0 || b.startTime().getSecond() != 0) {
                throw new ScheduleValidationException(
                        "Block " + i + ": startTime must be a 15-minute increment (e.g. 09:00, 09:15)");
            }
            if (b.endTime().getMinute() % 15 != 0 || b.endTime().getSecond() != 0) {
                throw new ScheduleValidationException(
                        "Block " + i + ": endTime must be a 15-minute increment");
            }
            if (!b.endTime().isAfter(b.startTime())) {
                throw new ScheduleValidationException(
                        "Block " + i + ": endTime must be after startTime");
            }
            if (b.startTime().isBefore(DAY_START) || b.endTime().isAfter(DAY_END)) {
                throw new ScheduleValidationException(
                        "Block " + i + ": times must be within 08:00–17:00");
            }
        }

        // No overlaps (check sorted order)
        List<SavePlanRequest.BlockEntry> sorted = blocks.stream()
                .sorted(Comparator.comparing(SavePlanRequest.BlockEntry::startTime))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
                throw new ScheduleValidationException("Blocks must not overlap");
            }
        }
    }

    public TimeBlockResponse startBlock(AppUser user, UUID blockId) {
        TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, user.getId())
            .orElseThrow(() -> new BlockNotFoundException("Time block not found"));
        if (block.getActualStart() != null) {
            throw new BlockAlreadyStartedException("Time block already started");
        }
        block.setActualStart(Instant.now());
        timeBlockRepository.save(block);
        return TimeBlockResponse.from(block);
    }

    public TimeBlockResponse completeBlock(AppUser user, UUID blockId) {
        TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, user.getId())
            .orElseThrow(() -> new BlockNotFoundException("Time block not found"));
        if (block.getActualStart() == null) {
            throw new ScheduleValidationException("Time block has not been started");
        }

        Instant now = Instant.now();
        block.setActualEnd(now);
        block.setWasCompleted(true);
        timeBlockRepository.save(block);

        if (block.getTask() != null) {
            long elapsedMinutes = java.time.Duration.between(block.getActualStart(), now).toMinutes();
            var task = block.getTask();
            int current = task.getActualMinutes() != null ? task.getActualMinutes() : 0;
            task.setActualMinutes(current + (int) elapsedMinutes);
            task.setStatus(com.echel.planner.backend.task.TaskStatus.DONE);
            task.setCompletedAt(now);
            taskRepository.save(task);
        }

        return TimeBlockResponse.from(block);
    }

    public TimeBlockResponse doneForNow(AppUser user, UUID blockId) {
        TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, user.getId())
            .orElseThrow(() -> new BlockNotFoundException("Time block not found"));
        if (block.getActualStart() == null) {
            throw new ScheduleValidationException("Time block has not been started");
        }

        Instant now = Instant.now();
        block.setActualEnd(now);
        block.setWasCompleted(false);
        timeBlockRepository.save(block);

        if (block.getTask() != null) {
            long elapsedMinutes = java.time.Duration.between(block.getActualStart(), now).toMinutes();
            var task = block.getTask();
            int current = task.getActualMinutes() != null ? task.getActualMinutes() : 0;
            task.setActualMinutes(current + (int) elapsedMinutes);
            taskRepository.save(task);
        }

        return TimeBlockResponse.from(block);
    }

    public TimeBlockResponse extendBlock(AppUser user, UUID blockId, int durationMinutes) {
        TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, user.getId())
            .orElseThrow(() -> new BlockNotFoundException("Time block not found"));

        LocalTime newStart = block.getEndTime();
        LocalTime newEnd = newStart.plusMinutes(durationMinutes);

        TimeBlock extension = new TimeBlock(
            block.getUser(), block.getBlockDate(), block.getTask(),
            newStart, newEnd, block.getSortOrder() + 1
        );
        timeBlockRepository.save(extension);
        return TimeBlockResponse.from(extension);
    }

    static class ScheduleValidationException extends RuntimeException {
        ScheduleValidationException(String message) { super(message); }
    }

    static class BlockNotFoundException extends RuntimeException {
        BlockNotFoundException(String message) { super(message); }
    }

    static class BlockAlreadyStartedException extends RuntimeException {
        BlockAlreadyStartedException(String message) { super(message); }
    }
}
