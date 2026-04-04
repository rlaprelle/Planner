package com.echel.planner.backend.task;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.dto.TaskCreateRequest;
import com.echel.planner.backend.task.dto.TaskResponse;
import com.echel.planner.backend.task.dto.TaskStatusRequest;
import com.echel.planner.backend.task.dto.TaskUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

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
    void create_withRequiredFieldsOnly_savesAndReturnsResponse() {
        TaskCreateRequest request = new TaskCreateRequest(
                "My Task", null, null, null, null, null, null, null, null, null);

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        when(taskRepository.save(captor.capture())).thenAnswer(inv -> {
            Task saved = inv.getArgument(0);
            setField(saved, "id", UUID.randomUUID());
            return saved;
        });
        TaskResponse response = taskService.create(user, projectId, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("My Task");
        assertThat(response.projectId()).isEqualTo(projectId);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.parentTaskId()).isNull();
        assertThat(captor.getValue().getTitle()).isEqualTo("My Task");
    }

    @Test
    void create_withParentTaskInSameProject_setsParentTask() throws Exception {
        UUID parentTaskId = UUID.randomUUID();
        Task parentTask = new Task(user, project, "Parent Task");
        setField(parentTask, "id", parentTaskId);

        TaskCreateRequest request = new TaskCreateRequest(
                "Child Task", null, parentTaskId, null, null, null, null, null, null, null);

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));
        when(taskRepository.findByIdAndUserId(parentTaskId, userId))
                .thenReturn(Optional.of(parentTask));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        when(taskRepository.save(captor.capture())).thenAnswer(inv -> {
            Task saved = inv.getArgument(0);
            setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        TaskResponse response = taskService.create(user, projectId, request);

        assertThat(response.parentTaskId()).isEqualTo(parentTaskId);
        assertThat(captor.getValue().getParentTask()).isEqualTo(parentTask);
    }

    @Test
    void create_throwsWhenProjectNotFound() {
        TaskCreateRequest request = new TaskCreateRequest(
                "My Task", null, null, null, null, null, null, null, null, null);

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(user, projectId, request))
                .isInstanceOf(TaskService.TaskValidationException.class)
                .hasMessageContaining(projectId.toString());
    }

    @Test
    void create_throwsWhenParentTaskBelongsToDifferentProject() throws Exception {
        UUID parentTaskId = UUID.randomUUID();
        UUID otherProjectId = UUID.randomUUID();
        Project otherProject = new Project(user, "Other Project");
        setField(otherProject, "id", otherProjectId);

        Task parentTask = new Task(user, otherProject, "Parent in other project");
        setField(parentTask, "id", parentTaskId);

        TaskCreateRequest request = new TaskCreateRequest(
                "Child Task", null, parentTaskId, null, null, null, null, null, null, null);

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));
        when(taskRepository.findByIdAndUserId(parentTaskId, userId))
                .thenReturn(Optional.of(parentTask));

        assertThatThrownBy(() -> taskService.create(user, projectId, request))
                .isInstanceOf(TaskService.TaskValidationException.class)
                .hasMessageContaining(projectId.toString());
    }

    // --- changeStatus ---

    @Test
    void changeStatus_toDONE_setsCompletedAt() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(user, project, "Some Task");
        setField(task, "id", taskId);

        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(task));
        when(taskRepository.findByParentTaskIdAndUserIdAndArchivedAtIsNull(taskId, userId))
                .thenReturn(List.of());

        TaskStatusRequest request = new TaskStatusRequest(TaskStatus.DONE);
        TaskResponse response = taskService.changeStatus(user, taskId, request);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    void changeStatus_fromDONE_clearsCompletedAt() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(user, project, "Some Task");
        setField(task, "id", taskId);
        task.setStatus(TaskStatus.DONE);
        task.setCompletedAt(java.time.Instant.now());

        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(task));
        when(taskRepository.findByParentTaskIdAndUserIdAndArchivedAtIsNull(taskId, userId))
                .thenReturn(List.of());

        TaskStatusRequest request = new TaskStatusRequest(TaskStatus.IN_PROGRESS);
        TaskResponse response = taskService.changeStatus(user, taskId, request);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getCompletedAt()).isNull();
        assertThat(response.completedAt()).isNull();
    }

    // --- archive ---

    @Test
    void archive_setsArchivedAt() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(user, project, "Task to archive");
        setField(task, "id", taskId);

        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(task));
        when(taskRepository.findByParentTaskIdAndUserId(taskId, userId))
                .thenReturn(List.of());

        TaskResponse response = taskService.archive(user, taskId);

        assertThat(task.getArchivedAt()).isNotNull();
        assertThat(response.archivedAt()).isNotNull();
    }

    @Test
    void archive_cascadesToChildren() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task parent = new Task(user, project, "Parent Task");
        setField(parent, "id", taskId);

        Task child1 = new Task(user, project, "Child 1");
        setField(child1, "id", UUID.randomUUID());
        Task child2 = new Task(user, project, "Child 2");
        setField(child2, "id", UUID.randomUUID());

        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(parent));
        when(taskRepository.findByParentTaskIdAndUserId(taskId, userId))
                .thenReturn(List.of(child1, child2));

        taskService.archive(user, taskId);

        assertThat(parent.getArchivedAt()).isNotNull();
        assertThat(child1.getArchivedAt()).isNotNull();
        assertThat(child2.getArchivedAt()).isNotNull();
        assertThat(child1.getArchivedAt()).isEqualTo(parent.getArchivedAt());
        assertThat(child2.getArchivedAt()).isEqualTo(parent.getArchivedAt());
    }

    // --- update ---

    @Test
    void update_cascadesProjectChangeToChildren() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID newProjectId = UUID.randomUUID();
        Project newProject = new Project(user, "New Project");
        setField(newProject, "id", newProjectId);
        newProject.setColor("#112233");

        Task parent = new Task(user, project, "Parent Task");
        setField(parent, "id", taskId);

        Task child = new Task(user, project, "Child Task");
        setField(child, "id", UUID.randomUUID());

        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(parent));
        when(projectRepository.findByIdAndUserId(newProjectId, userId))
                .thenReturn(Optional.of(newProject));
        when(taskRepository.findByParentTaskIdAndUserId(taskId, userId))
                .thenReturn(List.of(child));
        when(taskRepository.findByParentTaskIdAndUserIdAndArchivedAtIsNull(taskId, userId))
                .thenReturn(List.of(child));
        // child's children
        when(taskRepository.findByParentTaskIdAndUserIdAndArchivedAtIsNull(child.getId(), userId))
                .thenReturn(List.of());

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated Title", null, null, null, null, null, null, null, null, newProjectId);

        TaskResponse response = taskService.update(user, taskId, request);

        assertThat(parent.getProject()).isEqualTo(newProject);
        assertThat(child.getProject()).isEqualTo(newProject);
        assertThat(response.projectId()).isEqualTo(newProjectId);
    }

    @Test
    void update_withSameProject_doesNotCascade() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(user, project, "My Task");
        setField(task, "id", taskId);

        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(task));
        when(taskRepository.findByParentTaskIdAndUserIdAndArchivedAtIsNull(taskId, userId))
                .thenReturn(List.of());

        // projectId is the same as current project — no cascade should happen
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated Title", "New description", null, null, null, null, null, null, null, projectId);

        TaskResponse response = taskService.update(user, taskId, request);

        assertThat(response.title()).isEqualTo("Updated Title");
        // findByParentTaskIdAndUserId (cascade) should not be called
        verify(taskRepository, org.mockito.Mockito.never())
                .findByParentTaskIdAndUserId(any(), any());
    }

    // --- get ---

    @Test
    void get_throwsWhenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.get(user, taskId))
                .isInstanceOf(TaskService.TaskNotFoundException.class)
                .hasMessageContaining(taskId.toString());
    }

    @Test
    void get_returnsResponseWithChildren() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(user, project, "Parent Task");
        setField(task, "id", taskId);

        UUID childId = UUID.randomUUID();
        Task child = new Task(user, project, "Child Task");
        setField(child, "id", childId);

        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(task));
        when(taskRepository.findByParentTaskIdAndUserIdAndArchivedAtIsNull(taskId, userId))
                .thenReturn(List.of(child));
        when(taskRepository.findByParentTaskIdAndUserIdAndArchivedAtIsNull(childId, userId))
                .thenReturn(List.of());

        TaskResponse response = taskService.get(user, taskId);

        assertThat(response.id()).isEqualTo(taskId);
        assertThat(response.children()).hasSize(1);
        assertThat(response.children().get(0).id()).isEqualTo(childId);
    }

    // --- Utility ---

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
