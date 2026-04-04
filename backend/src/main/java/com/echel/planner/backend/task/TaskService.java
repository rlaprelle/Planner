package com.echel.planner.backend.task;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.dto.TaskCreateRequest;
import com.echel.planner.backend.task.dto.TaskResponse;
import com.echel.planner.backend.task.dto.TaskStatusRequest;
import com.echel.planner.backend.task.dto.TaskUpdateRequest;
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

    @Transactional(readOnly = true)
    public List<TaskResponse> listCompletedToday(AppUser user) {
        Instant startOfDay = LocalDate.now(java.time.ZoneOffset.UTC)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant();
        List<Task> tasks = taskRepository.findCompletedTodayForUser(user.getId(), startOfDay);
        return tasks.stream()
                .map(t -> TaskResponse.from(t, computeDeadlineGroup(t.getDueDate()), List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listSuggested(AppUser user, LocalDate date, int limit) {
        List<Task> tasks = taskRepository.findSuggestedForUser(user.getId(), date);
        return sortAndMap(tasks, user).stream().limit(limit).toList();
    }

    public TaskResponse update(AppUser user, UUID id, TaskUpdateRequest request) {
        // Note: parentTaskId is intentionally not mutable via PUT.
        // Parent-child relationships are set at creation time only (Slice 1 design decision).
        Task task = findOwnedTask(user, id);

        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.status() != null) task.setStatus(request.status());
        if (request.priority() != null) task.setPriority(request.priority());
        if (request.pointsEstimate() != null) task.setPointsEstimate(request.pointsEstimate());
        if (request.actualMinutes() != null) task.setActualMinutes(request.actualMinutes());
        if (request.energyLevel() != null) task.setEnergyLevel(request.energyLevel());
        task.setDueDate(request.dueDate()); // intentionally unconditional — allows clearing due date
        if (request.sortOrder() != null) task.setSortOrder(request.sortOrder());

        if (request.projectId() != null && !request.projectId().equals(task.getProject().getId())) {
            Project newProject = findOwnedProject(user, request.projectId());
            task.setProject(newProject);
            cascadeProjectToChildren(task.getId(), user.getId(), newProject);
        }

        List<TaskResponse> children = buildChildResponses(task.getId(), user);
        return TaskResponse.from(task, computeDeadlineGroup(task.getDueDate()), children);
    }

    public TaskResponse archive(AppUser user, UUID id) {
        Task task = findOwnedTask(user, id);
        task.setArchivedAt(Instant.now());
        // Cascade archive to direct children
        List<Task> children = taskRepository.findByParentTaskIdAndUserId(task.getId(), user.getId());
        Instant now = task.getArchivedAt();
        children.forEach(child -> child.setArchivedAt(now));
        return TaskResponse.from(task, computeDeadlineGroup(task.getDueDate()), List.of());
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

    private void cascadeProjectToChildren(UUID parentTaskId, UUID userId, Project project) {
        List<Task> children = taskRepository.findByParentTaskIdAndUserId(parentTaskId, userId);
        for (Task child : children) {
            child.setProject(project);
        }
    }

    private List<TaskResponse> buildChildResponses(UUID parentTaskId, AppUser user) {
        List<Task> children = taskRepository.findByParentTaskIdAndUserIdAndArchivedAtIsNull(parentTaskId, user.getId());
        return sortAndMap(children, user);
    }

    private List<TaskResponse> sortAndMap(List<Task> tasks, AppUser user) {
        LocalDate today = LocalDate.now();
        // Rolling 7-day window (not calendar week boundary) — intentional for ADHD-friendly "next 7 days" UX
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
        if (!dueDate.isAfter(today)) return DeadlineGroup.TODAY;  // today AND overdue
        return DeadlineGroup.THIS_WEEK;  // after today and within 7 days
    }

    private int deadlineGroupOrder(LocalDate dueDate, LocalDate today, LocalDate endOfWeek) {
        // Use ordinal directly — DeadlineGroup is declared TODAY(0), THIS_WEEK(1), NO_DEADLINE(2)
        // Avoids the synthetic $1 class the compiler generates for enum switch expressions
        return computeDeadlineGroup(dueDate, today, endOfWeek).ordinal();
    }

    private Task findOwnedTask(AppUser user, UUID id) {
        return taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + id));
    }

    private Project findOwnedProject(AppUser user, UUID projectId) {
        return projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new TaskValidationException("Project not found or not accessible: " + projectId));
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
