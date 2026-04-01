package com.planner.backend.stats;

import com.planner.backend.auth.AppUser;
import com.planner.backend.deferred.DeferredItemRepository;
import com.planner.backend.reflection.DailyReflectionRepository;
import com.planner.backend.schedule.TimeBlockRepository;
import com.planner.backend.stats.dto.DashboardResponse;
import com.planner.backend.task.Task;
import com.planner.backend.task.TaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final DailyReflectionRepository reflectionRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final TaskRepository taskRepository;
    private final DeferredItemRepository deferredItemRepository;

    public StatsService(DailyReflectionRepository reflectionRepository,
                        TimeBlockRepository timeBlockRepository,
                        TaskRepository taskRepository,
                        DeferredItemRepository deferredItemRepository) {
        this.reflectionRepository = reflectionRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.taskRepository = taskRepository;
        this.deferredItemRepository = deferredItemRepository;
    }

    public int getStreak(AppUser user) {
        return computeStreak(user, false);
    }

    public DashboardResponse getDashboard(AppUser user) {
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));

        long blockCount = timeBlockRepository.countByUserIdAndBlockDate(user.getId(), today);
        long completedCount = timeBlockRepository.countCompletedByUserIdAndBlockDate(user.getId(), today);

        int streak = computeStreak(user, true);

        List<Task> upcoming = taskRepository.findUpcomingDeadlines(user.getId(), PageRequest.of(0, 5));
        LocalDate endOfWeek = today.plusDays(7);
        List<DashboardResponse.DeadlineSummary> deadlines = upcoming.stream()
                .map(t -> new DashboardResponse.DeadlineSummary(
                        t.getId(),
                        t.getTitle(),
                        t.getProject().getName(),
                        t.getProject().getColor(),
                        t.getDueDate(),
                        computeDeadlineGroup(t.getDueDate(), today, endOfWeek)))
                .toList();

        long deferredCount = deferredItemRepository.countPendingForUser(user.getId(), today);

        return new DashboardResponse(
                (int) blockCount,
                (int) completedCount,
                streak,
                deadlines,
                (int) deferredCount);
    }

    /**
     * Counts consecutive finalized-reflection days ending today.
     * If allowYesterdayFallback is true and today has no reflection,
     * counts from yesterday — useful for dashboard display during the day.
     */
    private int computeStreak(AppUser user, boolean allowYesterdayFallback) {
        List<LocalDate> dates = reflectionRepository.findFinalizedDatesDesc(user);
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));

        LocalDate expected = today;
        if (allowYesterdayFallback && (dates.isEmpty() || !dates.get(0).equals(today))) {
            expected = today.minusDays(1);
        }

        int streak = 0;
        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    private String computeDeadlineGroup(LocalDate dueDate, LocalDate today, LocalDate endOfWeek) {
        if (dueDate == null || dueDate.isAfter(endOfWeek)) return "NO_DEADLINE";
        if (!dueDate.isAfter(today)) return "TODAY";
        return "THIS_WEEK";
    }
}
