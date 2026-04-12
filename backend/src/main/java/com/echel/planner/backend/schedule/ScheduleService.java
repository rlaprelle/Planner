package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.event.Event;
import com.echel.planner.backend.event.EventService;
import com.echel.planner.backend.schedule.dto.SavePlanRequest;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.common.StateConflictException;
import com.echel.planner.backend.common.ValidationException;
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

    private final TimeBlockRepository timeBlockRepository;
    private final TaskRepository taskRepository;
    private final EventService eventService;

    public ScheduleService(TimeBlockRepository timeBlockRepository, TaskRepository taskRepository,
                           EventService eventService) {
        this.timeBlockRepository = timeBlockRepository;
        this.taskRepository = taskRepository;
        this.eventService = eventService;
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
        WorkingHours hours = resolveWorkingHours(user, request);
        validateBlocks(request.blocks(), hours.start(), hours.end());

        // Use the same server-computed date as getToday() and the dashboard,
        // so save and load always agree on which date "today" is.
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));
        List<Event> events = eventService.findForDate(user, today);
        validateNoEventOverlaps(request.blocks(), events);

        timeBlockRepository.deleteByUserIdAndBlockDate(user.getId(), today);
        List<TimeBlock> allBlocks = buildAllBlocks(user, today, request.blocks(), events);
        return saveAndMap(allBlocks);
    }

    private record WorkingHours(LocalTime start, LocalTime end) {}

    private WorkingHours resolveWorkingHours(AppUser user, SavePlanRequest request) {
        int startHour = request.startHour() != null
                ? request.startHour() : user.getDefaultStartTime().getHour();
        int endHour = request.endHour() != null
                ? request.endHour() : user.getDefaultEndTime().getHour();

        if (startHour < 0 || startHour > 23) {
            throw new ValidationException("startHour must be between 0 and 23");
        }
        if (endHour < 1 || endHour > 24) {
            throw new ValidationException("endHour must be between 1 and 24");
        }
        if (startHour >= endHour) {
            throw new ValidationException("startHour must be less than endHour");
        }

        LocalTime dayStart = LocalTime.of(startHour, 0);
        LocalTime dayEnd = endHour == 24 ? LocalTime.MAX : LocalTime.of(endHour, 0);
        return new WorkingHours(dayStart, dayEnd);
    }

    private void validateNoEventOverlaps(List<SavePlanRequest.BlockEntry> blocks, List<Event> events) {
        for (SavePlanRequest.BlockEntry block : blocks) {
            for (Event event : events) {
                if (block.startTime().isBefore(event.getEndTime())
                        && block.endTime().isAfter(event.getStartTime())) {
                    throw new ValidationException(
                            "Task block overlaps with event '" + event.getTitle() + "'");
                }
            }
        }
    }

    private List<TimeBlock> buildAllBlocks(AppUser user, LocalDate today,
                                            List<SavePlanRequest.BlockEntry> taskBlocks,
                                            List<Event> events) {
        List<TimeBlock> allBlocks = new ArrayList<>();

        for (Event event : events) {
            allBlocks.add(new TimeBlock(user, today, event,
                    event.getStartTime(), event.getEndTime(), 0));
        }

        for (SavePlanRequest.BlockEntry entry : taskBlocks) {
            Task task = taskRepository.findByIdAndUserId(entry.taskId(), user.getId())
                    .orElseThrow(() -> new ValidationException(
                            "Task not found: " + entry.taskId()));
            allBlocks.add(new TimeBlock(user, today, task,
                    entry.startTime(), entry.endTime(), 0));
        }

        allBlocks.sort(Comparator.comparing(TimeBlock::getStartTime));
        for (int i = 0; i < allBlocks.size(); i++) {
            allBlocks.get(i).setSortOrder(i);
        }
        return allBlocks;
    }

    private List<TimeBlockResponse> saveAndMap(List<TimeBlock> blocks) {
        return timeBlockRepository.saveAll(blocks)
                .stream()
                .map(TimeBlockResponse::from)
                .toList();
    }

    private void validateBlocks(List<SavePlanRequest.BlockEntry> blocks, LocalTime dayStart, LocalTime dayEnd) {
        for (int i = 0; i < blocks.size(); i++) {
            SavePlanRequest.BlockEntry b = blocks.get(i);

            if (b.startTime().getMinute() % 15 != 0 || b.startTime().getSecond() != 0) {
                throw new ValidationException(
                        "Block " + i + ": startTime must be a 15-minute increment (e.g. 09:00, 09:15)");
            }
            if (b.endTime().getMinute() % 15 != 0 || b.endTime().getSecond() != 0) {
                throw new ValidationException(
                        "Block " + i + ": endTime must be a 15-minute increment");
            }
            if (!b.endTime().isAfter(b.startTime())) {
                throw new ValidationException(
                        "Block " + i + ": endTime must be after startTime");
            }
            if (b.startTime().isBefore(dayStart) || b.endTime().isAfter(dayEnd)) {
                throw new ValidationException(
                        "Block " + i + ": times must be within "
                        + dayStart.toString().substring(0, 5) + "\u2013" + (dayEnd.equals(LocalTime.MAX) ? "24:00" : dayEnd.toString().substring(0, 5)));
            }
        }

        // No overlaps (check sorted order)
        List<SavePlanRequest.BlockEntry> sorted = blocks.stream()
                .sorted(Comparator.comparing(SavePlanRequest.BlockEntry::startTime))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
                throw new ValidationException("Blocks must not overlap");
            }
        }
    }

    public TimeBlockResponse startBlock(AppUser user, UUID blockId) {
        TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Time block not found"));
        if (block.getEvent() != null) {
            throw new ValidationException("Event blocks do not support session tracking");
        }
        if (block.getActualStart() != null) {
            throw new StateConflictException("Time block already started");
        }
        block.setActualStart(Instant.now());
        timeBlockRepository.save(block);
        return TimeBlockResponse.from(block);
    }

    public TimeBlockResponse completeBlock(AppUser user, UUID blockId) {
        TimeBlock block = findAndValidateBlockForClose(blockId, user.getId());

        Instant now = Instant.now();
        block.setActualEnd(now);
        block.setWasCompleted(true);
        timeBlockRepository.save(block);

        if (block.getTask() != null) {
            block.getTask().incrementActualMinutes(block.calculateElapsedMinutes());
            block.getTask().setStatus(com.echel.planner.backend.task.TaskStatus.COMPLETED);
            block.getTask().setCompletedAt(now);
            taskRepository.save(block.getTask());
        }

        return TimeBlockResponse.from(block);
    }

    public TimeBlockResponse doneForNow(AppUser user, UUID blockId) {
        TimeBlock block = findAndValidateBlockForClose(blockId, user.getId());

        Instant now = Instant.now();
        block.setActualEnd(now);
        block.setWasCompleted(false);
        timeBlockRepository.save(block);

        if (block.getTask() != null) {
            block.getTask().incrementActualMinutes(block.calculateElapsedMinutes());
            taskRepository.save(block.getTask());
        }

        return TimeBlockResponse.from(block);
    }

    public TimeBlockResponse extendBlock(AppUser user, UUID blockId, int durationMinutes) {
        TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Time block not found"));

        LocalTime newStart = block.getEndTime();
        LocalTime newEnd = newStart.plusMinutes(durationMinutes);

        TimeBlock extension = new TimeBlock(
            block.getUser(), block.getBlockDate(), block.getTask(),
            newStart, newEnd, block.getSortOrder() + 1
        );
        timeBlockRepository.save(extension);
        return TimeBlockResponse.from(extension);
    }

    /**
     * Finds a time block and validates it is ready to be closed (completed or done-for-now).
     * Not suitable for startBlock() — that method needs the opposite actualStart check.
     */
    private TimeBlock findAndValidateBlockForClose(UUID blockId, UUID userId) {
        TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Time block not found"));
        if (block.getEvent() != null) {
            throw new ValidationException("Event blocks do not support session tracking");
        }
        if (block.getActualStart() == null) {
            throw new ValidationException("Time block has not been started");
        }
        return block;
    }

}
