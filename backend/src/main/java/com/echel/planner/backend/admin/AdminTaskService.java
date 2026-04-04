package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminTaskRequest;
import com.echel.planner.backend.admin.dto.AdminTaskResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.task.EnergyLevel;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminTaskService {

    private final TaskRepository taskRepository;
    private final AppUserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final DeferredItemRepository deferredItemRepository;

    public AdminTaskService(TaskRepository taskRepository,
                            AppUserRepository userRepository,
                            ProjectRepository projectRepository,
                            TimeBlockRepository timeBlockRepository,
                            DeferredItemRepository deferredItemRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.deferredItemRepository = deferredItemRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminTaskResponse> listAll() {
        return taskRepository.findAll().stream()
                .map(AdminTaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminTaskResponse get(UUID id) {
        return AdminTaskResponse.from(findTask(id));
    }

    public AdminTaskResponse create(AdminTaskRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Project not found: " + request.projectId()));
        Task task = new Task(user, project, request.title());
        applyFields(task, request);
        return AdminTaskResponse.from(taskRepository.save(task));
    }

    public AdminTaskResponse update(UUID id, AdminTaskRequest request) {
        Task task = findTask(id);
        if (request.userId() != null) {
            AppUser user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
            task.setUser(user);
        }
        if (request.projectId() != null) {
            Project project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Project not found: " + request.projectId()));
            task.setProject(project);
        }
        task.setTitle(request.title());
        applyFields(task, request);
        return AdminTaskResponse.from(task);
    }

    public void delete(UUID id) {
        findTask(id);
        timeBlockRepository.deleteByTaskId(id);
        deferredItemRepository.deleteByResolvedTaskId(id);
        taskRepository.deleteByParentTaskId(id);
        taskRepository.deleteById(id);
    }

    private void applyFields(Task task, AdminTaskRequest request) {
        task.setDescription(request.description());
        if (request.status() != null) {
            task.setStatus(TaskStatus.valueOf(request.status()));
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setPointsEstimate(request.pointsEstimate());
        task.setActualMinutes(request.actualMinutes());
        if (request.energyLevel() != null) {
            task.setEnergyLevel(EnergyLevel.valueOf(request.energyLevel()));
        } else {
            task.setEnergyLevel(null);
        }
        task.setDueDate(request.dueDate());
        if (request.sortOrder() != null) {
            task.setSortOrder(request.sortOrder());
        }
        if (request.parentTaskId() != null) {
            task.setParentTask(taskRepository.findById(request.parentTaskId()).orElse(null));
        } else {
            task.setParentTask(null);
        }
        if (request.blockedByTaskId() != null) {
            task.setBlockedByTask(taskRepository.findById(request.blockedByTaskId()).orElse(null));
        } else {
            task.setBlockedByTask(null);
        }
    }

    private Task findTask(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Task not found: " + id));
    }
}
