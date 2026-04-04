package com.echel.planner.backend.event;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.event.dto.EventCreateRequest;
import com.echel.planner.backend.event.dto.EventResponse;
import com.echel.planner.backend.event.dto.EventUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final ProjectRepository projectRepository;

    public EventService(EventRepository eventRepository, ProjectRepository projectRepository) {
        this.eventRepository = eventRepository;
        this.projectRepository = projectRepository;
    }

    /** Creates a new event, validating ownership and time ordering. */
    public EventResponse create(AppUser user, EventCreateRequest request) {
        Project project = findOwnedProject(user, request.projectId());
        validateTimeOrder(request.startTime(), request.endTime());

        Event event = new Event(user, project, request.title(),
                request.blockDate(), request.startTime(), request.endTime());
        event.setDescription(request.description());
        event.setEnergyLevel(request.energyLevel());

        Event saved = eventRepository.save(event);
        return EventResponse.from(saved);
    }

    /** Returns a single event by id, verifying the user owns it. */
    @Transactional(readOnly = true)
    public EventResponse get(AppUser user, UUID eventId) {
        Event event = findOwnedEvent(user, eventId);
        return EventResponse.from(event);
    }

    /** Lists non-archived events for a project, verifying the user owns the project. */
    @Transactional(readOnly = true)
    public List<EventResponse> listByProject(AppUser user, UUID projectId) {
        findOwnedProject(user, projectId);
        return eventRepository.findByProjectIdAndUserId(projectId, user.getId()).stream()
                .map(EventResponse::from)
                .toList();
    }

    /**
     * Returns non-archived events for a given date with the project eagerly fetched.
     * Used internally by ScheduleService to materialize event TimeBlocks.
     */
    @Transactional(readOnly = true)
    public List<Event> findForDate(AppUser user, LocalDate date) {
        return eventRepository.findForDate(user.getId(), date);
    }

    /** Partially updates an event. Only non-null fields in the request are applied. */
    public EventResponse update(AppUser user, UUID eventId, EventUpdateRequest request) {
        Event event = findOwnedEvent(user, eventId);

        if (request.projectId() != null && !request.projectId().equals(event.getProject().getId())) {
            Project newProject = findOwnedProject(user, request.projectId());
            event.setProject(newProject);
        }
        if (request.title() != null) {
            event.setTitle(request.title());
        }
        if (request.description() != null) {
            event.setDescription(request.description());
        }
        if (request.energyLevel() != null) {
            event.setEnergyLevel(request.energyLevel());
        }
        if (request.blockDate() != null) {
            event.setBlockDate(request.blockDate());
        }
        if (request.startTime() != null) {
            event.setStartTime(request.startTime());
        }
        if (request.endTime() != null) {
            event.setEndTime(request.endTime());
        }

        validateTimeOrder(event.getStartTime(), event.getEndTime());

        return EventResponse.from(event);
    }

    /** Archives an event by setting its archived_at timestamp. */
    public EventResponse archive(AppUser user, UUID eventId) {
        Event event = findOwnedEvent(user, eventId);
        event.setArchivedAt(Instant.now());
        return EventResponse.from(event);
    }

    // --- Private helpers ---

    private Event findOwnedEvent(AppUser user, UUID eventId) {
        return eventRepository.findByIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));
    }

    private Project findOwnedProject(AppUser user, UUID projectId) {
        return projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new EventValidationException(
                        "Project not found or not accessible: " + projectId));
    }

    private void validateTimeOrder(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new EventValidationException(
                    "End time must be after start time: " + startTime + " - " + endTime);
        }
    }

    public static class EventNotFoundException extends RuntimeException {
        public EventNotFoundException(String message) {
            super(message);
        }
    }

    public static class EventValidationException extends RuntimeException {
        public EventValidationException(String message) {
            super(message);
        }
    }
}
