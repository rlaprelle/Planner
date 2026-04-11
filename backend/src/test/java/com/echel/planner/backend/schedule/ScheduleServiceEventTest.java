package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.event.Event;
import com.echel.planner.backend.event.EventService;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.schedule.ScheduleService.ScheduleValidationException;
import com.echel.planner.backend.schedule.dto.SavePlanRequest;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for event-related behavior in ScheduleService: event block materialization,
 * overlap detection, and session method rejection for event blocks.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceEventTest {

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private ScheduleService scheduleService;

    private AppUser user;
    private UUID userId;
    private UUID taskId;
    private UUID blockId;
    private Task task;
    private Project project;
    private Event event;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        blockId = UUID.randomUUID();

        user = new AppUser("user@example.com", "hash", "Test User", "UTC");
        setId(user, AppUser.class, userId);

        project = new Project(user, "Test Project");
        task = new Task(user, project, "Test Task");
        setId(task, Task.class, taskId);

        event = new Event(user, project, "Standup Meeting",
                LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(10, 30));
        setId(event, Event.class, UUID.randomUUID());
    }

    // -------------------------------------------------------------------------
    // savePlan: event block materialization
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    void savePlan_materializesEventBlocks() {
        var entry = new SavePlanRequest.BlockEntry(
                taskId, LocalTime.of(9, 0), LocalTime.of(9, 30));
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry), null, null);

        when(eventService.findForDate(eq(user), any(LocalDate.class)))
                .thenReturn(List.of(event));
        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(task));
        when(timeBlockRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<TimeBlockResponse> result = scheduleService.savePlan(user, request);

        // Should have 2 blocks: event + task
        assertThat(result).hasSize(2);

        // Verify the saved blocks include both event and task
        ArgumentCaptor<List<TimeBlock>> captor = ArgumentCaptor.forClass(List.class);
        verify(timeBlockRepository).saveAll(captor.capture());
        List<TimeBlock> saved = captor.getValue();

        assertThat(saved).hasSize(2);

        // Sorted by startTime: task at 9:00, event at 10:00
        TimeBlock firstBlock = saved.get(0);
        TimeBlock secondBlock = saved.get(1);
        assertThat(firstBlock.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(firstBlock.getTask()).isEqualTo(task);
        assertThat(firstBlock.getEvent()).isNull();
        assertThat(firstBlock.getSortOrder()).isEqualTo(0);

        assertThat(secondBlock.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(secondBlock.getEvent()).isEqualTo(event);
        assertThat(secondBlock.getTask()).isNull();
        assertThat(secondBlock.getSortOrder()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void savePlan_eventBlockResponseIncludesEventSummary() {
        // No task blocks, just events
        var request = new SavePlanRequest(LocalDate.now(), List.of(), null, null);

        when(eventService.findForDate(eq(user), any(LocalDate.class)))
                .thenReturn(List.of(event));
        when(timeBlockRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<TimeBlockResponse> result = scheduleService.savePlan(user, request);

        assertThat(result).hasSize(1);
        TimeBlockResponse response = result.get(0);
        assertThat(response.task()).isNull();
        assertThat(response.event()).isNotNull();
        assertThat(response.event().title()).isEqualTo("Standup Meeting");
    }

    // -------------------------------------------------------------------------
    // savePlan: overlap detection
    // -------------------------------------------------------------------------

    @Test
    void savePlan_rejectsTaskBlockOverlappingEvent() {
        // Event is 10:00–10:30, task block overlaps at 10:00–10:30
        var entry = new SavePlanRequest.BlockEntry(
                taskId, LocalTime.of(10, 0), LocalTime.of(10, 30));
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry), null, null);

        when(eventService.findForDate(eq(user), any(LocalDate.class)))
                .thenReturn(List.of(event));

        assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("overlaps with event")
                .hasMessageContaining("Standup Meeting");
    }

    @Test
    void savePlan_rejectsPartialOverlapWithEvent() {
        // Event is 10:00–10:30, task block partially overlaps at 9:45–10:15
        var entry = new SavePlanRequest.BlockEntry(
                taskId, LocalTime.of(9, 45), LocalTime.of(10, 15));
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry), null, null);

        when(eventService.findForDate(eq(user), any(LocalDate.class)))
                .thenReturn(List.of(event));

        assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("overlaps with event");
    }

    @SuppressWarnings("unchecked")
    @Test
    void savePlan_allowsAdjacentTaskBlockAndEvent() {
        // Event is 10:00–10:30, task block at 10:30–11:00 (adjacent, no overlap)
        var entry = new SavePlanRequest.BlockEntry(
                taskId, LocalTime.of(10, 30), LocalTime.of(11, 0));
        var request = new SavePlanRequest(LocalDate.now(), List.of(entry), null, null);

        when(eventService.findForDate(eq(user), any(LocalDate.class)))
                .thenReturn(List.of(event));
        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(task));
        when(timeBlockRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<TimeBlockResponse> result = scheduleService.savePlan(user, request);
        assertThat(result).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Session methods: rejection for event blocks
    // -------------------------------------------------------------------------

    @Test
    void startBlock_rejectsEventBlock() {
        TimeBlock block = new TimeBlock(user, LocalDate.now(), event,
                LocalTime.of(10, 0), LocalTime.of(10, 30), 0);
        when(timeBlockRepository.findByIdAndUserId(blockId, userId))
                .thenReturn(Optional.of(block));

        assertThatThrownBy(() -> scheduleService.startBlock(user, blockId))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("Event blocks do not support session tracking");
    }

    @Test
    void completeBlock_rejectsEventBlock() {
        TimeBlock block = new TimeBlock(user, LocalDate.now(), event,
                LocalTime.of(10, 0), LocalTime.of(10, 30), 0);
        when(timeBlockRepository.findByIdAndUserId(blockId, userId))
                .thenReturn(Optional.of(block));

        assertThatThrownBy(() -> scheduleService.completeBlock(user, blockId))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("Event blocks do not support session tracking");
    }

    @Test
    void doneForNow_rejectsEventBlock() {
        TimeBlock block = new TimeBlock(user, LocalDate.now(), event,
                LocalTime.of(10, 0), LocalTime.of(10, 30), 0);
        when(timeBlockRepository.findByIdAndUserId(blockId, userId))
                .thenReturn(Optional.of(block));

        assertThatThrownBy(() -> scheduleService.doneForNow(user, blockId))
                .isInstanceOf(ScheduleValidationException.class)
                .hasMessageContaining("Event blocks do not support session tracking");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static <T> void setId(T entity, Class<T> clazz, UUID id) {
        try {
            var field = clazz.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
