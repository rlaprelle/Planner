package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.schedule.ScheduleService.BlockAlreadyStartedException;
import com.echel.planner.backend.schedule.ScheduleService.BlockNotFoundException;
import com.echel.planner.backend.schedule.ScheduleService.ScheduleValidationException;
import com.echel.planner.backend.schedule.dto.SavePlanRequest;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private AppUser user;
    private UUID userId;
    private UUID taskId;
    private UUID blockId;
    private Task task;
    private Project project;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        blockId = UUID.randomUUID();

        user = new AppUser("user@example.com", "hash", "Test User", "UTC");
        // Reflectively set the id field since there's no setter
        try {
            var field = AppUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        project = new Project(user, "Test Project");
        task = new Task(user, project, "Test Task");
        try {
            var field = Task.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(task, taskId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // savePlan validation
    // -------------------------------------------------------------------------

    @Test
    void savePlan_rejectsNon15MinuteStartTime() {
        var entry = new SavePlanRequest.BlockEntry(
                taskId,
                LocalTime.of(9, 7),  // not a 15-min boundary
                LocalTime.of(9, 30)
        );
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry));

        assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("startTime must be a 15-minute increment");
    }

    @Test
    void savePlan_rejectsNon15MinuteEndTime() {
        var entry = new SavePlanRequest.BlockEntry(
                taskId,
                LocalTime.of(9, 0),
                LocalTime.of(9, 22)  // not a 15-min boundary
        );
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry));

        assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("endTime must be a 15-minute increment");
    }

    @Test
    void savePlan_rejectsEndTimeBeforeStartTime() {
        var entry = new SavePlanRequest.BlockEntry(
                taskId,
                LocalTime.of(10, 0),
                LocalTime.of(9, 0)  // end before start
        );
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry));

        assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    void savePlan_rejectsBlockBeforeDayStart() {
        var entry = new SavePlanRequest.BlockEntry(
                taskId,
                LocalTime.of(7, 0),  // before 08:00
                LocalTime.of(7, 30)
        );
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry));

        assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("within 08:00");
    }

    @Test
    void savePlan_rejectsBlockAfterDayEnd() {
        var entry = new SavePlanRequest.BlockEntry(
                taskId,
                LocalTime.of(16, 45),
                LocalTime.of(17, 15)  // ends after 17:00
        );
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry));

        assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("within 08:00");
    }

    @Test
    void savePlan_rejectsOverlappingBlocks() {
        var entry1 = new SavePlanRequest.BlockEntry(
                taskId,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );
        var entry2 = new SavePlanRequest.BlockEntry(
                taskId,
                LocalTime.of(9, 30),  // overlaps with entry1
                LocalTime.of(10, 30)
        );
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry1, entry2));

        assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void savePlan_acceptsValidAdjacentBlocks() {
        var taskId2 = UUID.randomUUID();
        var task2 = new Task(user, project, "Second Task");
        try {
            var field = Task.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(task2, taskId2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var entry1 = new SavePlanRequest.BlockEntry(taskId,  LocalTime.of(9, 0),  LocalTime.of(9, 30));
        var entry2 = new SavePlanRequest.BlockEntry(taskId2, LocalTime.of(9, 30), LocalTime.of(10, 0));
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry1, entry2));

        when(taskRepository.findByIdAndUserId(taskId,  userId)).thenReturn(Optional.of(task));
        when(taskRepository.findByIdAndUserId(taskId2, userId)).thenReturn(Optional.of(task2));

        TimeBlock savedBlock1 = new TimeBlock(user, LocalDate.now(), task,  LocalTime.of(9, 0),  LocalTime.of(9, 30),  0);
        TimeBlock savedBlock2 = new TimeBlock(user, LocalDate.now(), task2, LocalTime.of(9, 30), LocalTime.of(10, 0), 1);
        when(timeBlockRepository.saveAll(any())).thenReturn(List.of(savedBlock1, savedBlock2));

        List<TimeBlockResponse> result = scheduleService.savePlan(user, request);

        assertThat(result).hasSize(2);
        verify(timeBlockRepository).deleteByUserIdAndBlockDate(eq(userId), any(LocalDate.class));
        verify(timeBlockRepository).saveAll(any());
    }

    // -------------------------------------------------------------------------
    // startBlock
    // -------------------------------------------------------------------------

    @Test
    void startBlock_setsActualStart() {
        TimeBlock block = new TimeBlock(user, LocalDate.now(), task, LocalTime.of(9, 0), LocalTime.of(9, 30), 0);
        when(timeBlockRepository.findByIdAndUserId(blockId, userId)).thenReturn(Optional.of(block));
        when(timeBlockRepository.save(any())).thenReturn(block);

        Instant before = Instant.now();
        scheduleService.startBlock(user, blockId);
        Instant after = Instant.now();

        assertThat(block.getActualStart()).isNotNull();
        assertThat(block.getActualStart()).isBetween(before, after);
        verify(timeBlockRepository).save(block);
    }

    @Test
    void startBlock_throwsWhenAlreadyStarted() {
        TimeBlock block = new TimeBlock(user, LocalDate.now(), task, LocalTime.of(9, 0), LocalTime.of(9, 30), 0);
        block.setActualStart(Instant.now().minusSeconds(300));
        when(timeBlockRepository.findByIdAndUserId(blockId, userId)).thenReturn(Optional.of(block));

        assertThatThrownBy(() -> scheduleService.startBlock(user, blockId))
                .isInstanceOf(BlockAlreadyStartedException.class)
                .hasMessageContaining("already started");
    }

    @Test
    void startBlock_throwsWhenNotFound() {
        when(timeBlockRepository.findByIdAndUserId(blockId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.startBlock(user, blockId))
                .isInstanceOf(BlockNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // -------------------------------------------------------------------------
    // completeBlock
    // -------------------------------------------------------------------------

    @Test
    void completeBlock_setsActualEndAndCompletedAndUpdatesTask() {
        TimeBlock block = new TimeBlock(user, LocalDate.now(), task, LocalTime.of(9, 0), LocalTime.of(9, 30), 0);
        block.setActualStart(Instant.now().minusSeconds(60));
        when(timeBlockRepository.findByIdAndUserId(blockId, userId)).thenReturn(Optional.of(block));
        when(timeBlockRepository.save(any())).thenReturn(block);

        Instant before = Instant.now();
        scheduleService.completeBlock(user, blockId);
        Instant after = Instant.now();

        assertThat(block.getActualEnd()).isNotNull();
        assertThat(block.getActualEnd()).isBetween(before, after);
        assertThat(block.isWasCompleted()).isTrue();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getCompletedAt()).isNotNull();
        assertThat(task.getActualMinutes()).isGreaterThanOrEqualTo(1);

        verify(taskRepository).save(task);
    }

    @Test
    void completeBlock_throwsWhenNotStarted() {
        TimeBlock block = new TimeBlock(user, LocalDate.now(), task, LocalTime.of(9, 0), LocalTime.of(9, 30), 0);
        // actualStart is null — not started
        when(timeBlockRepository.findByIdAndUserId(blockId, userId)).thenReturn(Optional.of(block));

        assertThatThrownBy(() -> scheduleService.completeBlock(user, blockId))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("not been started");
    }

    // -------------------------------------------------------------------------
    // doneForNow
    // -------------------------------------------------------------------------

    @Test
    void doneForNow_setsActualEndAndDoesNotChangeTaskStatus() {
        TimeBlock block = new TimeBlock(user, LocalDate.now(), task, LocalTime.of(9, 0), LocalTime.of(9, 30), 0);
        block.setActualStart(Instant.now().minusSeconds(60));
        when(timeBlockRepository.findByIdAndUserId(blockId, userId)).thenReturn(Optional.of(block));
        when(timeBlockRepository.save(any())).thenReturn(block);

        TaskStatus statusBefore = task.getStatus();

        Instant before = Instant.now();
        scheduleService.doneForNow(user, blockId);
        Instant after = Instant.now();

        assertThat(block.getActualEnd()).isNotNull();
        assertThat(block.getActualEnd()).isBetween(before, after);
        assertThat(block.isWasCompleted()).isFalse();

        // Task status must NOT change
        assertThat(task.getStatus()).isEqualTo(statusBefore);
        assertThat(task.getCompletedAt()).isNull();

        // actualMinutes should be updated on the task
        assertThat(task.getActualMinutes()).isGreaterThanOrEqualTo(1);

        verify(taskRepository).save(task);
        // completedAt setter should NOT have been called — status stays TODO
    }
}
