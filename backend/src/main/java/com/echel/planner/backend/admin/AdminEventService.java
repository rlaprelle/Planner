package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminEventRequest;
import com.echel.planner.backend.admin.dto.AdminEventResponse;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.event.Event;
import com.echel.planner.backend.event.EventRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.EnergyLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminEventService {

    private final EventRepository eventRepository;
    private final AppUserRepository userRepository;
    private final ProjectRepository projectRepository;

    public AdminEventService(EventRepository eventRepository,
                             AppUserRepository userRepository,
                             ProjectRepository projectRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminEventResponse> listAll() {
        return eventRepository.findAll().stream()
                .map(AdminEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminEventResponse get(UUID id) {
        return AdminEventResponse.from(findEvent(id));
    }

    public AdminEventResponse create(AdminEventRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.userId()));
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.projectId()));

        Event event = new Event(
                user,
                project,
                request.title(),
                request.blockDate(),
                request.startTime(),
                request.endTime()
        );
        event.setDescription(request.description());
        event.setEnergyLevel(parseEnergyLevel(request.energyLevel()));
        return AdminEventResponse.from(eventRepository.save(event));
    }

    public AdminEventResponse update(UUID id, AdminEventRequest request) {
        Event event = findEvent(id);
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.projectId()));

        event.setProject(project);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEnergyLevel(parseEnergyLevel(request.energyLevel()));
        event.setBlockDate(request.blockDate());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        return AdminEventResponse.from(event);
    }

    public void delete(UUID id) {
        findEvent(id);
        eventRepository.deleteById(id);
    }

    private Event findEvent(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));
    }

    private EnergyLevel parseEnergyLevel(String energyLevel) {
        if (energyLevel == null || energyLevel.isBlank()) {
            return null;
        }
        return EnergyLevel.valueOf(energyLevel.toUpperCase());
    }
}
