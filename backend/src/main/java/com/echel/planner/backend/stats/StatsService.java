package com.echel.planner.backend.stats;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.reflection.DailyReflection;
import com.echel.planner.backend.stats.dto.DashboardResponse;
import com.echel.planner.backend.stats.dto.WeeklySummaryResponse;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.ToIntFunction;

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
     * Computes rolling 7-day stats: tasks completed, points, focus minutes,
     * streak, and mood/energy trends.
     */
    public WeeklySummaryResponse getWeeklySummary(AppUser user) {
        ZoneId zone = ZoneId.of(user.getTimezone());
        LocalDate today = LocalDate.now(zone);
        LocalDate weekStart = today.minusDays(6);

        Instant rangeStart = weekStart.atStartOfDay(zone).toInstant();
        Instant rangeEnd = today.plusDays(1).atStartOfDay(zone).toInstant();

        List<Task> completed = taskRepository.findCompletedInRange(user.getId(), rangeStart, rangeEnd);

        int tasksCompleted = completed.size();
        int totalPoints = completed.stream()
                .mapToInt(t -> t.getPointsEstimate() != null ? t.getPointsEstimate() : 0)
                .sum();
        int totalFocusMinutes = completed.stream()
                .mapToInt(t -> t.getActualMinutes() != null ? t.getActualMinutes() : 0)
                .sum();

        int streak = computeStreak(user, true);

        List<DailyReflection> reflections = reflectionRepository.findFinalizedInDateRange(
                user, weekStart, today);

        String energyTrend = computeTrend(reflections, DailyReflection::getEnergyRating);
        String moodTrend = computeTrend(reflections, DailyReflection::getMoodRating);

        boolean hasActivity = tasksCompleted > 0 || totalFocusMinutes > 0;

        return new WeeklySummaryResponse(
                tasksCompleted, totalPoints, totalFocusMinutes,
                streak, energyTrend, moodTrend, hasActivity);
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

    /**
     * Splits reflections into an earlier and later half and compares average
     * ratings. Returns "improving", "declining", "steady", or null if fewer
     * than two data points.
     */
    private String computeTrend(List<DailyReflection> reflections,
                                 ToIntFunction<DailyReflection> ratingExtractor) {
        if (reflections.size() < 2) {
            return null;
        }
        int mid = reflections.size() / 2;
        double earlier = reflections.subList(0, mid).stream()
                .mapToInt(ratingExtractor)
                .average()
                .orElse(0);
        double later = reflections.subList(mid, reflections.size()).stream()
                .mapToInt(ratingExtractor)
                .average()
                .orElse(0);

        double diff = later - earlier;
        if (diff > 0.5) return "improving";
        if (diff < -0.5) return "declining";
        return "steady";
    }
}
