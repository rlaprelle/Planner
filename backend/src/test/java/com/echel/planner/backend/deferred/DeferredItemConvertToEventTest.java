package com.echel.planner.backend.deferred;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.deferred.dto.ConvertToEventRequest;
import com.echel.planner.backend.deferred.dto.DeferredItemResponse;
import com.echel.planner.backend.event.Event;
import com.echel.planner.backend.event.EventRepository;
import com.echel.planner.backend.event.EventService;
import com.echel.planner.backend.event.dto.EventCreateRequest;
import com.echel.planner.backend.event.dto.EventResponse;
import com.echel.planner.backend.task.EnergyLevel;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskService;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeferredItemConvertToEventTest {

    @Mock
    private DeferredItemRepository repository;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private DeferredItemService service;

    private AppUser user;
    private UUID itemId;
    private UUID projectId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        user = new AppUser("test@example.com", "hash", "Test User", "UTC");
        itemId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    @Test
    void convertToEvent_createsEventAndMarksItemProcessed() {
        DeferredItem item = new DeferredItem(user, "Team standup meeting");
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.of(item));

        EventResponse eventResponse = new EventResponse(
                eventId, projectId, "Work", "#6366f1", user.getId(),
                "Team Standup", "Daily standup", EnergyLevel.LOW,
                LocalDate.of(2026, 4, 5), LocalTime.of(9, 0), LocalTime.of(9, 30),
                null, Instant.now(), Instant.now());

        when(eventService.create(eq(user), eq(projectId), any(EventCreateRequest.class)))
                .thenReturn(eventResponse);

        Event event = mock(Event.class);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        ConvertToEventRequest request = new ConvertToEventRequest(
                projectId, "Team Standup", "Daily standup", EnergyLevel.LOW,
                LocalDate.of(2026, 4, 5), LocalTime.of(9, 0), LocalTime.of(9, 30));

        EventResponse result = service.convertToEvent(user, itemId, request);

        assertThat(result.id()).isEqualTo(eventId);
        assertThat(result.title()).isEqualTo("Team Standup");
        assertThat(item.isProcessed()).isTrue();
        assertThat(item.getProcessedAt()).isNotNull();
        assertThat(item.getResolvedEvent()).isEqualTo(event);
    }

    @Test
    void convertToEvent_passesCorrectFieldsToEventService() {
        DeferredItem item = new DeferredItem(user, "Dentist appointment");
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.of(item));

        EventResponse eventResponse = new EventResponse(
                eventId, projectId, "Personal", "#f59e0b", user.getId(),
                "Dentist", null, null,
                LocalDate.of(2026, 4, 10), LocalTime.of(14, 0), LocalTime.of(15, 0),
                null, Instant.now(), Instant.now());

        ArgumentCaptor<EventCreateRequest> captor = ArgumentCaptor.forClass(EventCreateRequest.class);
        when(eventService.create(eq(user), eq(projectId), captor.capture()))
                .thenReturn(eventResponse);

        Event event = mock(Event.class);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        ConvertToEventRequest request = new ConvertToEventRequest(
                projectId, "Dentist", null, null,
                LocalDate.of(2026, 4, 10), LocalTime.of(14, 0), LocalTime.of(15, 0));

        service.convertToEvent(user, itemId, request);

        EventCreateRequest captured = captor.getValue();
        assertThat(captured.title()).isEqualTo("Dentist");
        assertThat(captured.description()).isNull();
        assertThat(captured.energyLevel()).isNull();
        assertThat(captured.blockDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(captured.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(captured.endTime()).isEqualTo(LocalTime.of(15, 0));
    }

    @Test
    void convertToEvent_throwsWhenItemNotFound() {
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.empty());

        ConvertToEventRequest request = new ConvertToEventRequest(
                projectId, "Meeting", null, null,
                LocalDate.of(2026, 4, 5), LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertThatThrownBy(() -> service.convertToEvent(user, itemId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(itemId.toString());
    }

    @Test
    void convertToEvent_responseIncludesResolvedEventId() {
        DeferredItem item = new DeferredItem(user, "Lunch with client");
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.of(item));

        EventResponse eventResponse = new EventResponse(
                eventId, projectId, "Work", "#6366f1", user.getId(),
                "Lunch Meeting", "With client", null,
                LocalDate.of(2026, 4, 7), LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, Instant.now(), Instant.now());

        when(eventService.create(eq(user), eq(projectId), any(EventCreateRequest.class)))
                .thenReturn(eventResponse);

        Event event = mock(Event.class);
        when(event.getId()).thenReturn(eventId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        ConvertToEventRequest request = new ConvertToEventRequest(
                projectId, "Lunch Meeting", "With client", null,
                LocalDate.of(2026, 4, 7), LocalTime.of(12, 0), LocalTime.of(13, 0));

        service.convertToEvent(user, itemId, request);

        // Verify the item's resolvedEvent is set so DeferredItemResponse.from() will include it
        assertThat(item.getResolvedEvent()).isNotNull();
        assertThat(item.getResolvedEvent().getId()).isEqualTo(eventId);

        DeferredItemResponse itemResponse = DeferredItemResponse.from(item);
        assertThat(itemResponse.resolvedEventId()).isEqualTo(eventId);
    }
}
