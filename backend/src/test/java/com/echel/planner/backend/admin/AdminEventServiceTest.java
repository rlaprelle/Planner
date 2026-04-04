package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminEventRequest;
import com.echel.planner.backend.admin.dto.AdminEventResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.event.Event;
import com.echel.planner.backend.event.EventRepository;
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
class AdminEventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private AdminEventService service;

    private AppUser user;
    private Project project;
    private UUID userId;
    private UUID projectId;
    private UUID eventId;

    private static final LocalDate BLOCK_DATE = LocalDate.of(2026, 4, 1);
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(10, 0);

    @BeforeEach
    void setUp() {
        user = new AppUser("admin@example.com", "hash", "Admin User", "UTC");
        project = mock(Project.class);
        lenient().when(project.getId()).thenReturn(UUID.randomUUID());
        lenient().when(project.getName()).thenReturn("Test Project");
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    // --- listAll ---

    @Test
    void listAll_returnsMappedResponses() {
        Event event1 = new Event(user, project, "Event 1", BLOCK_DATE, START_TIME, END_TIME);
        Event event2 = new Event(user, project, "Event 2", BLOCK_DATE.plusDays(1),
                LocalTime.of(14, 0), LocalTime.of(15, 0));
        when(eventRepository.findAll()).thenReturn(List.of(event1, event2));

        List<AdminEventResponse> responses = service.listAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).title()).isEqualTo("Event 1");
        assertThat(responses.get(1).title()).isEqualTo("Event 2");
    }

    // --- get ---

    @Test
    void get_throwsAdminNotFoundExceptionWhenEventAbsent() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(eventId))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }

    // --- create ---

    @Test
    void create_looksUpUserAndProjectAndCreatesEventWithAllFields() {
        AdminEventRequest request = new AdminEventRequest(
                userId, projectId, "Team Standup", "Daily standup", "HIGH",
                BLOCK_DATE, START_TIME, END_TIME);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        Event saved = new Event(user, project, "Team Standup", BLOCK_DATE, START_TIME, END_TIME);
        saved.setDescription("Daily standup");
        saved.setEnergyLevel(EnergyLevel.HIGH);
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        AdminEventResponse response = service.create(request);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event captured = captor.getValue();
        assertThat(captured.getUser()).isSameAs(user);
        assertThat(captured.getProject()).isSameAs(project);
        assertThat(captured.getTitle()).isEqualTo("Team Standup");
        assertThat(captured.getDescription()).isEqualTo("Daily standup");
        assertThat(captured.getEnergyLevel()).isEqualTo(EnergyLevel.HIGH);
        assertThat(captured.getBlockDate()).isEqualTo(BLOCK_DATE);
        assertThat(captured.getStartTime()).isEqualTo(START_TIME);
        assertThat(captured.getEndTime()).isEqualTo(END_TIME);
        assertThat(response.title()).isEqualTo("Team Standup");
    }

    @Test
    void create_withNullEnergyLevel_createsEventWithoutEnergyLevel() {
        AdminEventRequest request = new AdminEventRequest(
                userId, projectId, "Event", null, null,
                BLOCK_DATE, START_TIME, END_TIME);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        Event saved = new Event(user, project, "Event", BLOCK_DATE, START_TIME, END_TIME);
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        service.create(request);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getEnergyLevel()).isNull();
    }

    @Test
    void create_throwsAdminNotFoundExceptionWhenUserAbsent() {
        AdminEventRequest request = new AdminEventRequest(
                userId, projectId, "Event", null, null,
                BLOCK_DATE, START_TIME, END_TIME);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void create_throwsAdminNotFoundExceptionWhenProjectAbsent() {
        AdminEventRequest request = new AdminEventRequest(
                userId, projectId, "Event", null, null,
                BLOCK_DATE, START_TIME, END_TIME);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(projectId.toString());
    }

    // --- update ---

    @Test
    void update_setsAllFieldsIncludingProject() {
        Project newProject = mock(Project.class);
        when(newProject.getId()).thenReturn(UUID.randomUUID());
        when(newProject.getName()).thenReturn("New Project");

        Event existing = new Event(user, project, "Old Title", BLOCK_DATE, START_TIME, END_TIME);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(newProject));

        LocalDate newDate = BLOCK_DATE.plusDays(1);
        LocalTime newStart = LocalTime.of(11, 0);
        LocalTime newEnd = LocalTime.of(12, 0);
        AdminEventRequest request = new AdminEventRequest(
                userId, projectId, "New Title", "New description", "LOW",
                newDate, newStart, newEnd);

        service.update(eventId, request);

        assertThat(existing.getProject()).isSameAs(newProject);
        assertThat(existing.getTitle()).isEqualTo("New Title");
        assertThat(existing.getDescription()).isEqualTo("New description");
        assertThat(existing.getEnergyLevel()).isEqualTo(EnergyLevel.LOW);
        assertThat(existing.getBlockDate()).isEqualTo(newDate);
        assertThat(existing.getStartTime()).isEqualTo(newStart);
        assertThat(existing.getEndTime()).isEqualTo(newEnd);
    }

    @Test
    void update_throwsAdminNotFoundExceptionWhenEventAbsent() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());
        AdminEventRequest request = new AdminEventRequest(
                userId, projectId, "Title", null, null,
                BLOCK_DATE, START_TIME, END_TIME);

        assertThatThrownBy(() -> service.update(eventId, request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }

    @Test
    void update_throwsAdminNotFoundExceptionWhenProjectAbsent() {
        Event existing = new Event(user, project, "Title", BLOCK_DATE, START_TIME, END_TIME);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        AdminEventRequest request = new AdminEventRequest(
                userId, projectId, "Title", null, null,
                BLOCK_DATE, START_TIME, END_TIME);

        assertThatThrownBy(() -> service.update(eventId, request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(projectId.toString());
    }

    // --- delete ---

    @Test
    void delete_removesEventById() {
        Event existing = new Event(user, project, "Event", BLOCK_DATE, START_TIME, END_TIME);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existing));

        service.delete(eventId);

        verify(eventRepository).deleteById(eventId);
    }

    @Test
    void delete_throwsAdminNotFoundExceptionWhenEventAbsent() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(eventId))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }
}
