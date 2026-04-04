package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminProjectRequest;
import com.echel.planner.backend.admin.dto.AdminProjectResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminProjectService {

    private final ProjectRepository projectRepository;
    private final AppUserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DeferredItemRepository deferredItemRepository;

    public AdminProjectService(ProjectRepository projectRepository,
                               AppUserRepository userRepository,
                               TaskRepository taskRepository,
                               DeferredItemRepository deferredItemRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.deferredItemRepository = deferredItemRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminProjectResponse> listAll() {
        return projectRepository.findAll().stream()
                .map(AdminProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminProjectResponse get(UUID id) {
        return AdminProjectResponse.from(findProject(id));
    }

    public AdminProjectResponse create(AdminProjectRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
        Project project = new Project(user, request.name());
        project.setDescription(request.description());
        project.setColor(request.color());
        project.setIcon(request.icon());
        if (request.isActive() != null) {
            project.setActive(request.isActive());
        }
        if (request.sortOrder() != null) {
            project.setSortOrder(request.sortOrder());
        }
        return AdminProjectResponse.from(projectRepository.save(project));
    }

    public AdminProjectResponse update(UUID id, AdminProjectRequest request) {
        Project project = findProject(id);
        if (request.userId() != null) {
            AppUser user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
            project.setUser(user);
        }
        project.setName(request.name());
        project.setDescription(request.description());
        project.setColor(request.color());
        project.setIcon(request.icon());
        if (request.isActive() != null) {
            project.setActive(request.isActive());
            if (!request.isActive()) {
                project.setArchivedAt(Instant.now());
            }
        }
        if (request.sortOrder() != null) {
            project.setSortOrder(request.sortOrder());
        }
        return AdminProjectResponse.from(project);
    }

    public void delete(UUID id) {
        Project project = findProject(id);
        deferredItemRepository.deleteByResolvedProjectId(id);
        taskRepository.deleteByProjectId(id);
        projectRepository.delete(project);
    }

    private Project findProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Project not found: " + id));
    }
}
