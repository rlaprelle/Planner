package com.echel.planner.backend.stats;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.reflection.DailyReflection;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.stats.dto.DashboardResponse;
import com.echel.planner.backend.stats.dto.DashboardResponse.CelebrationTask;
import com.echel.planner.backend.stats.dto.WeeklySummaryResponse;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    // --- Helpers ---

    /** Builds a Task with given title, points, minutes, and a createdAt timestamp. */
    private Task buildTask(String title, Short points, Integer minutes, Instant createdAt) {
        Project project = new Project(user, "Test Project");
        project.setColor("#6366f1");
        Task t = new Task(user, project, title);
        t.setPointsEstimate(points);
        t.setActualMinutes(minutes);
        ReflectionTestUtils.setField(t, "id", java.util.UUID.randomUUID());
        ReflectionTestUtils.setField(t, "createdAt", createdAt);
        t.setCompletedAt(Instant.now());
        return t;
    }

    private DailyReflection buildReflection(LocalDate date, short energy, short mood) {
        DailyReflection r = new DailyReflection(user, date);
        r.setEnergyRating(energy);
        r.setMoodRating(mood);
        r.setFinalized(true);
        return r;
    }

    /** Stubs all dashboard dependencies to benign defaults (zero counts, empty lists). */
    private void stubDashboardDefaults() {
        when(timeBlockRepository.countByUserIdAndBlockDate(any(), any())).thenReturn(0L);
        when(timeBlockRepository.countCompletedByUserIdAndBlockDate(any(), any())).thenReturn(0L);
        when(taskRepository.findUpcomingDeadlines(any(), any())).thenReturn(List.of());
        when(deferredItemRepository.countPendingForUser(any(), any())).thenReturn(0L);
        when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of());
    }

    // --- Celebration tests ---

    @Nested
    class CelebrationTests {

        @Test
        void returnsEmptyCelebrationsWhenNoTasksQualify() {
            stubDashboardDefaults();
            Task lowPoints = buildTask("Simple task", (short) 2, null, Instant.now());
            when(taskRepository.findCompletedTodayForUser(any(), any())).thenReturn(List.of(lowPoints));

            DashboardResponse response = statsService.getDashboard(user);

            assertThat(response.celebrationTasks()).isEmpty();
        }

        @Test
        void celebratesHighPointsTask() {
            stubDashboardDefaults();
            Task bigTask = buildTask("Big task", (short) 5, null, Instant.now());
            when(taskRepository.findCompletedTodayForUser(any(), any())).thenReturn(List.of(bigTask));

            DashboardResponse response = statsService.getDashboard(user);

            assertThat(response.celebrationTasks()).hasSize(1);
            CelebrationTask celebration = response.celebrationTasks().get(0);
            assertThat(celebration.taskTitle()).isEqualTo("Big task");
            assertThat(celebration.reason()).isEqualTo("High complexity task");
        }

        @Test
        void celebratesHighTimeTask() {
            stubDashboardDefaults();
            Task longTask = buildTask("Long session", null, 150, Instant.now());
            when(taskRepository.findCompletedTodayForUser(any(), any())).thenReturn(List.of(longTask));

            DashboardResponse response = statsService.getDashboard(user);

            assertThat(response.celebrationTasks()).hasSize(1);
            CelebrationTask celebration = response.celebrationTasks().get(0);
            assertThat(celebration.taskTitle()).isEqualTo("Long session");
            assertThat(celebration.reason()).isEqualTo("2h 30m of focused work");
        }

        @Test
        void celebratesExactTwoHoursWithoutMinutes() {
            stubDashboardDefaults();
            Task twoHours = buildTask("Two hour task", null, 120, Instant.now());
            when(taskRepository.findCompletedTodayForUser(any(), any())).thenReturn(List.of(twoHours));

            DashboardResponse response = statsService.getDashboard(user);

            assertThat(response.celebrationTasks()).hasSize(1);
            assertThat(response.celebrationTasks().get(0).reason()).isEqualTo("2 hours of focused work");
        }

        @Test
        void celebratesLongRunningTask() {
            stubDashboardDefaults();
            Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
            Task oldTask = buildTask("Old task", (short) 1, null, tenDaysAgo);
            when(taskRepository.findCompletedTodayForUser(any(), any())).thenReturn(List.of(oldTask));

            DashboardResponse response = statsService.getDashboard(user);

            assertThat(response.celebrationTasks()).hasSize(1);
            assertThat(response.celebrationTasks().get(0).reason()).startsWith("On your list for ");
        }

        @Test
        void capsAtThreeCelebrations() {
            stubDashboardDefaults();
            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                tasks.add(buildTask("Task " + i, (short) 5, null, Instant.now()));
            }
            when(taskRepository.findCompletedTodayForUser(any(), any())).thenReturn(tasks);

            DashboardResponse response = statsService.getDashboard(user);

            assertThat(response.celebrationTasks()).hasSize(3);
        }

        @Test
        void sortsFocusedWorkBeforeComplexityBeforeAge() {
            stubDashboardDefaults();
            Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
            Task ageTask = buildTask("Old task", (short) 1, null, tenDaysAgo);
            Task complexTask = buildTask("Complex task", (short) 8, null, Instant.now());
            Task focusTask = buildTask("Focus task", null, 180, Instant.now());

            when(taskRepository.findCompletedTodayForUser(any(), any()))
                    .thenReturn(List.of(ageTask, complexTask, focusTask));

            DashboardResponse response = statsService.getDashboard(user);

            assertThat(response.celebrationTasks()).hasSize(3);
            assertThat(response.celebrationTasks().get(0).reason()).contains("focused work");
            assertThat(response.celebrationTasks().get(1).reason()).contains("complexity");
            assertThat(response.celebrationTasks().get(2).reason()).startsWith("On your list for");
        }

        @Test
        void highTimeTakesPriorityOverHighPoints() {
            stubDashboardDefaults();
            Task both = buildTask("Both", (short) 8, 150, Instant.now());
            when(taskRepository.findCompletedTodayForUser(any(), any())).thenReturn(List.of(both));

            DashboardResponse response = statsService.getDashboard(user);

            assertThat(response.celebrationTasks()).hasSize(1);
            assertThat(response.celebrationTasks().get(0).reason()).contains("focused work");
        }
    }

    // --- Weekly summary tests ---

    @Nested
    class WeeklySummaryTests {

        @Test
        void returnsEmptyStateWhenNoActivity() {
            when(taskRepository.findCompletedInRange(any(), any(), any())).thenReturn(List.of());
            when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of());
            when(reflectionRepository.findFinalizedInDateRange(eq(user), any(), any())).thenReturn(List.of());

            WeeklySummaryResponse response = statsService.getWeeklySummary(user);

            assertThat(response.tasksCompleted()).isZero();
            assertThat(response.totalPoints()).isZero();
            assertThat(response.totalFocusMinutes()).isZero();
            assertThat(response.hasActivity()).isFalse();
        }

        @Test
        void aggregatesCompletedTaskStats() {
            Task t1 = buildTask("Task 1", (short) 5, 90, Instant.now());
            Task t2 = buildTask("Task 2", (short) 3, 60, Instant.now());
            when(taskRepository.findCompletedInRange(any(), any(), any())).thenReturn(List.of(t1, t2));
            when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of());
            when(reflectionRepository.findFinalizedInDateRange(eq(user), any(), any())).thenReturn(List.of());

            WeeklySummaryResponse response = statsService.getWeeklySummary(user);

            assertThat(response.tasksCompleted()).isEqualTo(2);
            assertThat(response.totalPoints()).isEqualTo(8);
            assertThat(response.totalFocusMinutes()).isEqualTo(150);
            assertThat(response.hasActivity()).isTrue();
        }

        @Test
        void handlesNullPointsAndMinutes() {
            Task noEstimates = buildTask("No estimates", null, null, Instant.now());
            when(taskRepository.findCompletedInRange(any(), any(), any())).thenReturn(List.of(noEstimates));
            when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of());
            when(reflectionRepository.findFinalizedInDateRange(eq(user), any(), any())).thenReturn(List.of());

            WeeklySummaryResponse response = statsService.getWeeklySummary(user);

            assertThat(response.tasksCompleted()).isEqualTo(1);
            assertThat(response.totalPoints()).isZero();
            assertThat(response.totalFocusMinutes()).isZero();
        }

        @Test
        void computesImprovingTrend() {
            List<DailyReflection> reflections = List.of(
                    buildReflection(today.minusDays(5), (short) 2, (short) 2),
                    buildReflection(today.minusDays(4), (short) 2, (short) 1),
                    buildReflection(today.minusDays(1), (short) 4, (short) 5),
                    buildReflection(today, (short) 5, (short) 4)
            );
            when(taskRepository.findCompletedInRange(any(), any(), any())).thenReturn(List.of());
            when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of());
            when(reflectionRepository.findFinalizedInDateRange(eq(user), any(), any())).thenReturn(reflections);

            WeeklySummaryResponse response = statsService.getWeeklySummary(user);

            assertThat(response.energyTrend()).isEqualTo("improving");
            assertThat(response.moodTrend()).isEqualTo("improving");
        }

        @Test
        void returnsNullTrendsWithInsufficientData() {
            List<DailyReflection> single = List.of(buildReflection(today, (short) 3, (short) 3));
            when(taskRepository.findCompletedInRange(any(), any(), any())).thenReturn(List.of());
            when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of());
            when(reflectionRepository.findFinalizedInDateRange(eq(user), any(), any())).thenReturn(single);

            WeeklySummaryResponse response = statsService.getWeeklySummary(user);

            assertThat(response.energyTrend()).isNull();
            assertThat(response.moodTrend()).isNull();
        }

        @Test
        void computesDecliningTrend() {
            List<DailyReflection> reflections = List.of(
                    buildReflection(today.minusDays(3), (short) 5, (short) 5),
                    buildReflection(today.minusDays(2), (short) 4, (short) 4),
                    buildReflection(today.minusDays(1), (short) 2, (short) 2),
                    buildReflection(today, (short) 1, (short) 1)
            );
            when(taskRepository.findCompletedInRange(any(), any(), any())).thenReturn(List.of());
            when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of());
            when(reflectionRepository.findFinalizedInDateRange(eq(user), any(), any())).thenReturn(reflections);

            WeeklySummaryResponse response = statsService.getWeeklySummary(user);

            assertThat(response.energyTrend()).isEqualTo("declining");
            assertThat(response.moodTrend()).isEqualTo("declining");
        }

        @Test
        void computesSteadyTrend() {
            List<DailyReflection> reflections = List.of(
                    buildReflection(today.minusDays(1), (short) 3, (short) 3),
                    buildReflection(today, (short) 3, (short) 3)
            );
            when(taskRepository.findCompletedInRange(any(), any(), any())).thenReturn(List.of());
            when(reflectionRepository.findFinalizedDatesDesc(user)).thenReturn(List.of());
            when(reflectionRepository.findFinalizedInDateRange(eq(user), any(), any())).thenReturn(reflections);

            WeeklySummaryResponse response = statsService.getWeeklySummary(user);

            assertThat(response.energyTrend()).isEqualTo("steady");
            assertThat(response.moodTrend()).isEqualTo("steady");
        }
    }
}
