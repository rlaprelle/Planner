package com.echel.planner.backend.stats;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.stats.dto.DashboardResponse;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private DailyReflectionRepository reflectionRepository;

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DeferredItemRepository deferredItemRepository;

    @InjectMocks
    private StatsService statsService;

    private AppUser user;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        today = LocalDate.now(ZoneId.of("UTC"));
    }

    // --- Streak tests ---

    @Test
    void getStreak_returnsZero_whenNoReflections() {
        when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of());

        int streak = statsService.getStreak(user);

        assertThat(streak).isZero();
    }

    @Test
    void getStreak_returnsOne_whenOnlyTodayHasReflection() {
        when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of(today));

        int streak = statsService.getStreak(user);

        assertThat(streak).isEqualTo(1);
    }

    @Test
    void getStreak_returnsThree_whenThreeConsecutiveDaysEndingToday() {
        List<LocalDate> dates = List.of(today, today.minusDays(1), today.minusDays(2));
        when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(dates);

        int streak = statsService.getStreak(user);

        assertThat(streak).isEqualTo(3);
    }

    @Test
    void getStreak_returnsOne_whenGapInConsecutiveDays() {
        // Today and today-2 (gap at today-1 breaks the streak)
        List<LocalDate> dates = List.of(today, today.minusDays(2));
        when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(dates);

        int streak = statsService.getStreak(user);

        assertThat(streak).isEqualTo(1);
    }

    // --- Dashboard test ---

    @Test
    void getDashboard_aggregatesAllStats() {
        // Time blocks
        when(timeBlockRepository.countByUserIdAndBlockDate(user.getId(), today)).thenReturn(4L);
        when(timeBlockRepository.countCompletedByUserIdAndBlockDate(user.getId(), today)).thenReturn(2L);

        // Streak — dashboard uses allowYesterdayFallback=true; today has a reflection
        when(reflectionRepository.findFinalizedDatesDesc(user))
                .thenReturn(List.of(today, today.minusDays(1), today.minusDays(2)));

        // Upcoming deadlines — return empty for simplicity
        when(taskRepository.findUpcomingDeadlines(eq(user.getId()), any(PageRequest.class)))
                .thenReturn(List.of());

        // Deferred items
        when(deferredItemRepository.countPendingForUser(user.getId(), today)).thenReturn(3L);

        // Completed today — none qualifying for celebrations
        when(taskRepository.findCompletedTodayForUser(eq(user.getId()), any(Instant.class)))
                .thenReturn(List.of());

        DashboardResponse response = statsService.getDashboard(user);

        assertThat(response.todayBlockCount()).isEqualTo(4);
        assertThat(response.todayCompletedCount()).isEqualTo(2);
        assertThat(response.streakDays()).isEqualTo(3);
        assertThat(response.deferredItemCount()).isEqualTo(3);
        assertThat(response.upcomingDeadlines()).isEmpty();
        assertThat(response.celebrationTasks()).isEmpty();
    }

    @Test
    void getDashboard_mapsUpcomingDeadlines_whenTasksAreReturned() {
        when(timeBlockRepository.countByUserIdAndBlockDate(user.getId(), today)).thenReturn(0L);
        when(timeBlockRepository.countCompletedByUserIdAndBlockDate(user.getId(), today)).thenReturn(0L);
        when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of());
        when(deferredItemRepository.countPendingForUser(user.getId(), today)).thenReturn(0L);

        Project project = new Project(user, "Work");
        project.setColor("#6366f1");

        Task task = new Task(user, project, "Fix login bug");
        task.setDueDate(today);

        when(taskRepository.findUpcomingDeadlines(eq(user.getId()), any(PageRequest.class)))
                .thenReturn(List.of(task));

        // Completed today — none qualifying for celebrations
        when(taskRepository.findCompletedTodayForUser(eq(user.getId()), any(Instant.class)))
                .thenReturn(List.of());

        DashboardResponse response = statsService.getDashboard(user);

        assertThat(response.upcomingDeadlines()).hasSize(1);
        DashboardResponse.DeadlineSummary summary = response.upcomingDeadlines().get(0);
        assertThat(summary.taskTitle()).isEqualTo("Fix login bug");
        assertThat(summary.projectName()).isEqualTo("Work");
        assertThat(summary.projectColor()).isEqualTo("#6366f1");
        assertThat(summary.deadlineGroup()).isEqualTo("TODAY");
    }
}
