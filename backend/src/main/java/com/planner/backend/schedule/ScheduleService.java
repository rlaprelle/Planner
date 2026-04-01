package com.planner.backend.schedule;

import com.planner.backend.auth.AppUser;
import com.planner.backend.schedule.dto.SavePlanRequest;
import com.planner.backend.schedule.dto.TimeBlockResponse;
import com.planner.backend.task.Task;
import com.planner.backend.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    static class ScheduleValidationException extends RuntimeException {
        ScheduleValidationException(String message) { super(message); }
    }
}
