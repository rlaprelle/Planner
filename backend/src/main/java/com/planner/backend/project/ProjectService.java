package com.planner.backend.project;

import com.planner.backend.auth.AppUser;
import com.planner.backend.project.dto.ProjectCreateRequest;
import com.planner.backend.project.dto.ProjectResponse;
import com.planner.backend.project.dto.ProjectUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public ProjectResponse create(AppUser user, ProjectCreateRequest request) {
        Project project = new Project(user, request.name());
        project.setDescription(request.description());
        project.setColor(request.color());
        project.setIcon(request.icon());
        if (request.sortOrder() != null) {
            project.setSortOrder(request.sortOrder());
        }
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listActive(AppUser user) {
        return projectRepository
                .findByUserIdAndIsActiveTrueOrderBySortOrderAsc(user.getId())
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(AppUser user, UUID id) {
        Project project = findOwnedProject(user, id);
        return ProjectResponse.from(project);
    }

    public ProjectResponse update(AppUser user, UUID id, ProjectUpdateRequest request) {
        Project project = findOwnedProject(user, id);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setColor(request.color());
        project.setIcon(request.icon());
        if (request.sortOrder() != null) {
            project.setSortOrder(request.sortOrder());
        }
        return ProjectResponse.from(project);
    }

    public ProjectResponse archive(AppUser user, UUID id) {
        Project project = findOwnedProject(user, id);
        project.setActive(false);
        project.setArchivedAt(Instant.now());
        return ProjectResponse.from(project);
    }

    private Project findOwnedProject(AppUser user, UUID id) {
        return projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + id));
    }

    public static class ProjectNotFoundException extends RuntimeException {
        public ProjectNotFoundException(String message) {
            super(message);
        }
    }
}
