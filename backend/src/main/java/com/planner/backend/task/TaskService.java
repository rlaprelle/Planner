package com.planner.backend.task;

import com.planner.backend.auth.AppUser;
import com.planner.backend.project.Project;
import com.planner.backend.project.ProjectRepository;
import com.planner.backend.task.dto.TaskCreateRequest;
import com.planner.backend.task.dto.TaskResponse;
import com.planner.backend.task.dto.TaskStatusRequest;
import com.planner.backend.task.dto.TaskUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    public TaskResponse create(AppUser user, UUID projectId, TaskCreateRequest request) {
        Project project = findOwnedProject(user, projectId);

        Task task = new Task(user, project, request.title());
        task.setDescription(request.description());
        if (request.status() != null) task.setStatus(request.status());
        if (request.priority() != null) task.setPriority(request.priority());
        if (request.pointsEstimate() != null) task.setPointsEstimate(request.pointsEstimate());
        if (request.actualMinutes() != null) task.setActualMinutes(request.actualMinutes());
        if (request.energyLevel() != null) task.setEnergyLevel(request.energyLevel());
        if (request.dueDate() != null) task.setDueDate(request.dueDate());
        if (request.sortOrder() != null) task.setSortOrder(request.sortOrder());

        if (request.parentTaskId() != null) {
            Task parent = taskRepository.findByIdAndUserId(request.parentTaskId(), user.getId())
                    .orElseThrow(() -> new TaskNotFoundException("Parent task not found: " + request.parentTaskId()));
            if (!parent.getProject().getId().equals(projectId)) {
                throw new TaskValidationException("Parent task does not belong to project: " + projectId);
            }
            task.setParentTask(parent);
        }

        Task saved = taskRepository.save(task);
        List<TaskResponse> children = List.of();
        return TaskResponse.from(saved, computeDeadlineGroup(saved.getDueDate()), children);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTopLevel(AppUser user, UUID projectId) {
        findOwnedProject(user, projectId);
        List<Task> tasks = taskRepository.findByProjectIdAndUserIdAndParentTaskIsNullAndArchivedAtIsNull(projectId, user.getId());
        return sortAndMap(tasks, user);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(AppUser user, UUID id) {
        Task task = findOwnedTask(user, id);
        List<TaskResponse> children = buildChildResponses(task.getId(), user);
        return TaskResponse.from(task, computeDeadlineGroup(task.getDueDate()), children);
    }

    public TaskResponse update(AppUser user, UUID id, TaskUpdateRequest request) {
        Task task = findOwnedTask(user, id);

        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.status() != null) task.setStatus(request.status());
        if (request.priority() != null) task.setPriority(request.priority());
        if (request.pointsEstimate() != null) task.setPointsEstimate(request.pointsEstimate());
        if (request.actualMinutes() != null) task.setActualMinutes(request.actualMinutes());
        if (request.energyLevel() != null) task.setEnergyLevel(request.energyLevel());
        task.setDueDate(request.dueDate());
        if (request.sortOrder() != null) task.setSortOrder(request.sortOrder());

        if (request.projectId() != null && !request.projectId().equals(task.getProject().getId())) {
            Project newProject = findOwnedProject(user, request.projectId());
            task.setProject(newProject);
            cascadeProjectToChildren(task.getId(), newProject);
        }

        List<TaskResponse> children = buildChildResponses(task.getId(), user);
        return TaskResponse.from(task, computeDeadlineGroup(task.getDueDate()), children);
    }

    public TaskResponse archive(AppUser user, UUID id) {
        Task task = findOwnedTask(user, id);
        task.setArchivedAt(Instant.now());
        List<TaskResponse> children = buildChildResponses(task.getId(), user);
        return TaskResponse.from(task, computeDeadlineGroup(task.getDueDate()), children);
    }

    public TaskResponse changeStatus(AppUser user, UUID id, TaskStatusRequest request) {
        Task task = findOwnedTask(user, id);
        task.setStatus(request.status());
        if (request.status() == TaskStatus.DONE) {
            task.setCompletedAt(Instant.now());
        } else {
            task.setCompletedAt(null);
        }
        List<TaskResponse> children = buildChildResponses(task.getId(), user);
        return TaskResponse.from(task, computeDeadlineGroup(task.getDueDate()), children);
    }

    // --- Private helpers ---

    private void cascadeProjectToChildren(UUID parentTaskId, Project project) {
        List<Task> children = taskRepository.findByParentTaskId(parentTaskId);
        for (Task child : children) {
            child.setProject(project);
        }
    }

    private List<TaskResponse> buildChildResponses(UUID parentTaskId, AppUser user) {
        List<Task> children = taskRepository.findByParentTaskIdAndArchivedAtIsNull(parentTaskId);
        return sortAndMap(children, user);
    }

    private List<TaskResponse> sortAndMap(List<Task> tasks, AppUser user) {
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(7);

        Comparator<Task> comparator = Comparator
                .comparingInt((Task t) -> deadlineGroupOrder(t.getDueDate(), today, endOfWeek))
                .thenComparingInt(t -> -t.getPriority());

        return tasks.stream()
                .sorted(comparator)
                .map(task -> {
                    DeadlineGroup group = computeDeadlineGroup(task.getDueDate(), today, endOfWeek);
                    List<TaskResponse> children = buildChildResponses(task.getId(), user);
                    return TaskResponse.from(task, group, children);
                })
                .toList();
    }

    private DeadlineGroup computeDeadlineGroup(LocalDate dueDate) {
        LocalDate today = LocalDate.now();
        return computeDeadlineGroup(dueDate, today, today.plusDays(7));
    }

    private DeadlineGroup computeDeadlineGroup(LocalDate dueDate, LocalDate today, LocalDate endOfWeek) {
        if (dueDate == null || dueDate.isAfter(endOfWeek)) return DeadlineGroup.NO_DEADLINE;
        if (dueDate.isEqual(today)) return DeadlineGroup.TODAY;
        if (dueDate.isAfter(today) && !dueDate.isAfter(endOfWeek)) return DeadlineGroup.THIS_WEEK;
        return DeadlineGroup.NO_DEADLINE;
    }

    private int deadlineGroupOrder(LocalDate dueDate, LocalDate today, LocalDate endOfWeek) {
        return switch (computeDeadlineGroup(dueDate, today, endOfWeek)) {
            case TODAY -> 0;
            case THIS_WEEK -> 1;
            case NO_DEADLINE -> 2;
        };
    }

    private Task findOwnedTask(AppUser user, UUID id) {
        return taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + id));
    }

    private Project findOwnedProject(AppUser user, UUID projectId) {
        return projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new TaskNotFoundException("Project not found: " + projectId));
    }

    public static class TaskNotFoundException extends RuntimeException {
        public TaskNotFoundException(String message) {
            super(message);
        }
    }

    public static class TaskValidationException extends RuntimeException {
        public TaskValidationException(String message) {
            super(message);
        }
    }
}
