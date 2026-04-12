package com.echel.planner.backend.event;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.common.ValidationException;
import com.echel.planner.backend.event.dto.EventCreateRequest;
import com.echel.planner.backend.event.dto.EventResponse;
import com.echel.planner.backend.event.dto.EventUpdateRequest;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.EnergyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private EventService eventService;

    private AppUser user;
    private Project project;
    private UUID userId;
    private UUID projectId;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        user = new AppUser("test@example.com", "hash", "Test User", "UTC");
        setField(user, "id", userId);

        project = new Project(user, "Test Project");
        setField(project, "id", projectId);
        project.setColor("#aabbcc");
    }

    // --- create ---

    @Test
    void create_withAllFields_savesAndReturnsResponse() {
        EventCreateRequest request = new EventCreateRequest(
                "Standup", "Daily standup", EnergyLevel.LOW,
                LocalDate.of(2026, 4, 5), LocalTime.of(9, 0), LocalTime.of(9, 30));

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        when(eventRepository.save(captor.capture())).thenAnswer(inv -> {
            Event saved = inv.getArgument(0);
            setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        EventResponse response = eventService.create(user, projectId, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Standup");
        assertThat(response.description()).isEqualTo("Daily standup");
        assertThat(response.projectId()).isEqualTo(projectId);
        assertThat(response.projectName()).isEqualTo("Test Project");
        assertThat(response.projectColor()).isEqualTo("#aabbcc");
        assertThat(response.energyLevel()).isEqualTo(EnergyLevel.LOW);
        assertThat(response.blockDate()).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(9, 30));

        Event captured = captor.getValue();
        assertThat(captured.getTitle()).isEqualTo("Standup");
        assertThat(captured.getDescription()).isEqualTo("Daily standup");
        assertThat(captured.getEnergyLevel()).isEqualTo(EnergyLevel.LOW);
    }

    @Test
    void create_withRequiredFieldsOnly_savesSuccessfully() {
        EventCreateRequest request = new EventCreateRequest(
                "Meeting", null, null,
                LocalDate.of(2026, 4, 5), LocalTime.of(10, 0), LocalTime.of(11, 0));

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event saved = inv.getArgument(0);
            setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        EventResponse response = eventService.create(user, projectId, request);

        assertThat(response.title()).isEqualTo("Meeting");
        assertThat(response.description()).isNull();
        assertThat(response.energyLevel()).isNull();
    }

    @Test
    void create_throwsWhenProjectNotFound() {
        EventCreateRequest request = new EventCreateRequest(
                "Meeting", null, null,
                LocalDate.of(2026, 4, 5), LocalTime.of(10, 0), LocalTime.of(11, 0));

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.create(user, projectId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(projectId.toString());
    }

    @Test
    void create_throwsWhenEndTimeNotAfterStartTime() {
        EventCreateRequest request = new EventCreateRequest(
                "Meeting", null, null,
                LocalDate.of(2026, 4, 5), LocalTime.of(11, 0), LocalTime.of(10, 0));

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));

        assertThatThrownBy(() -> eventService.create(user, projectId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void create_throwsWhenStartTimeEqualsEndTime() {
        EventCreateRequest request = new EventCreateRequest(
                "Meeting", null, null,
                LocalDate.of(2026, 4, 5), LocalTime.of(10, 0), LocalTime.of(10, 0));

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));

        assertThatThrownBy(() -> eventService.create(user, projectId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("End time must be after start time");
    }

    // --- get ---

    @Test
    void get_returnsEventResponse() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = buildEvent(eventId, "Standup");

        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.of(event));

        EventResponse response = eventService.get(user, eventId);

        assertThat(response.id()).isEqualTo(eventId);
        assertThat(response.title()).isEqualTo("Standup");
        assertThat(response.projectId()).isEqualTo(projectId);
    }

    @Test
    void get_throwsWhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.get(user, eventId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }

    // --- listByProject ---

    @Test
    void listByProject_returnsEventResponses() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = buildEvent(eventId, "Standup");

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));
        when(eventRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(List.of(event));

        List<EventResponse> responses = eventService.listByProject(user, projectId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("Standup");
    }

    @Test
    void listByProject_throwsWhenProjectNotFound() {
        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.listByProject(user, projectId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(projectId.toString());
    }

    // --- findForDate ---

    @Test
    void findForDate_returnsEventEntities() throws Exception {
        LocalDate date = LocalDate.of(2026, 4, 5);
        UUID eventId = UUID.randomUUID();
        Event event = buildEvent(eventId, "Standup");

        when(eventRepository.findForDate(userId, date))
                .thenReturn(List.of(event));

        List<Event> events = eventService.findForDate(user, date);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getTitle()).isEqualTo("Standup");
    }

    // --- update ---

    @Test
    void update_appliesOnlyNonNullFields() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = buildEvent(eventId, "Original Title");
        event.setDescription("Original description");

        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.of(event));

        EventUpdateRequest request = new EventUpdateRequest(
                null, "Updated Title", null, EnergyLevel.HIGH, null, null, null);

        EventResponse response = eventService.update(user, eventId, request);

        assertThat(response.title()).isEqualTo("Updated Title");
        assertThat(response.description()).isEqualTo("Original description");
        assertThat(response.energyLevel()).isEqualTo(EnergyLevel.HIGH);
        assertThat(response.blockDate()).isEqualTo(LocalDate.of(2026, 4, 5));
    }

    @Test
    void update_changesProject() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = buildEvent(eventId, "Meeting");

        UUID newProjectId = UUID.randomUUID();
        Project newProject = new Project(user, "New Project");
        setField(newProject, "id", newProjectId);
        newProject.setColor("#112233");

        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.of(event));
        when(projectRepository.findByIdAndUserId(newProjectId, userId))
                .thenReturn(Optional.of(newProject));

        EventUpdateRequest request = new EventUpdateRequest(
                newProjectId, null, null, null, null, null, null);

        EventResponse response = eventService.update(user, eventId, request);

        assertThat(response.projectId()).isEqualTo(newProjectId);
        assertThat(response.projectName()).isEqualTo("New Project");
    }

    @Test
    void update_throwsWhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.empty());

        EventUpdateRequest request = new EventUpdateRequest(
                null, "Title", null, null, null, null, null);

        assertThatThrownBy(() -> eventService.update(user, eventId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }

    @Test
    void update_throwsWhenNewProjectNotFound() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = buildEvent(eventId, "Meeting");
        UUID badProjectId = UUID.randomUUID();

        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.of(event));
        when(projectRepository.findByIdAndUserId(badProjectId, userId))
                .thenReturn(Optional.empty());

        EventUpdateRequest request = new EventUpdateRequest(
                badProjectId, null, null, null, null, null, null);

        assertThatThrownBy(() -> eventService.update(user, eventId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(badProjectId.toString());
    }

    @Test
    void update_validatesTimeOrderAfterPartialUpdate() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = buildEvent(eventId, "Meeting");
        // event has start_time 09:00, end_time 09:30

        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.of(event));

        // Change only start_time to after current end_time
        EventUpdateRequest request = new EventUpdateRequest(
                null, null, null, null, null, LocalTime.of(10, 0), null);

        assertThatThrownBy(() -> eventService.update(user, eventId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void update_withSameProject_doesNotReassign() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = buildEvent(eventId, "Meeting");

        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.of(event));

        // Pass the same project ID — should not trigger a project lookup
        EventUpdateRequest request = new EventUpdateRequest(
                projectId, null, null, null, null, null, null);

        EventResponse response = eventService.update(user, eventId, request);

        assertThat(response.projectId()).isEqualTo(projectId);
        // projectRepository.findByIdAndUserId should NOT have been called
        verify(projectRepository, org.mockito.Mockito.never())
                .findByIdAndUserId(any(), any());
    }

    // --- archive ---

    @Test
    void archive_setsArchivedAtTimestamp() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = buildEvent(eventId, "Meeting");

        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.of(event));

        EventResponse response = eventService.archive(user, eventId);

        assertThat(event.getArchivedAt()).isNotNull();
        assertThat(response.archivedAt()).isNotNull();
    }

    @Test
    void archive_throwsWhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.archive(user, eventId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }

    // --- Utility ---

    private Event buildEvent(UUID eventId, String title) throws Exception {
        Event event = new Event(user, project, title,
                LocalDate.of(2026, 4, 5), LocalTime.of(9, 0), LocalTime.of(9, 30));
        setField(event, "id", eventId);
        return event;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " not found in " + target.getClass());
    }
}
