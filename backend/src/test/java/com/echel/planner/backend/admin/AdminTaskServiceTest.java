package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminTaskRequest;
import com.echel.planner.backend.admin.dto.AdminTaskResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TimeBlockRepository timeBlockRepository;
    @Mock
    private DeferredItemRepository deferredItemRepository;

    @InjectMocks
    private AdminTaskService adminTaskService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AppUser buildUser(UUID id, String email) {
        AppUser user = new AppUser(email, "hash", "Display Name", "UTC");
        try {
            var field = AppUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private Project buildProject(UUID id, AppUser user, String name) {
        Project project = new Project(user, name);
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    private Task buildTask(UUID id, AppUser user, Project project, String title) {
        Task task = new Task(user, project, title);
        try {
            var field = Task.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(task, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return task;
    }

    // -------------------------------------------------------------------------
    // listAll
    // -------------------------------------------------------------------------

    @Test
    void listAll_returnsMappedResponses() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        AppUser user = buildUser(userId, "user@example.com");
        Project project = buildProject(projectId, user, "My Project");

        UUID tid1 = UUID.randomUUID();
        UUID tid2 = UUID.randomUUID();
        Task t1 = buildTask(tid1, user, project, "Task One");
        Task t2 = buildTask(tid2, user, project, "Task Two");

        when(taskRepository.findAll()).thenReturn(List.of(t1, t2));

        List<AdminTaskResponse> responses = adminTaskService.listAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(tid1);
        assertThat(responses.get(0).title()).isEqualTo("Task One");
        assertThat(responses.get(1).id()).isEqualTo(tid2);
        assertThat(responses.get(1).title()).isEqualTo("Task Two");
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_looksUpUserAndProjectAndAppliesFields() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        AppUser user = buildUser(userId, "user@example.com");
        Project project = buildProject(projectId, user, "My Project");

        AdminTaskRequest request = new AdminTaskRequest(
                userId, projectId, "New Task", "A description",
                null, "IN_PROGRESS", (short) 2, (short) 3,
                90, "HIGH", null, 4, null);

        UUID savedId = UUID.randomUUID();
        Task saved = buildTask(savedId, user, project, "New Task");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        AdminTaskResponse response = adminTaskService.create(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());

        Task captured = captor.getValue();
        assertThat(captured.getUser()).isEqualTo(user);
        assertThat(captured.getProject()).isEqualTo(project);
        assertThat(captured.getTitle()).isEqualTo("New Task");
        assertThat(captured.getDescription()).isEqualTo("A description");
        assertThat(captured.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        assertThat(response.id()).isEqualTo(savedId);
    }

    @Test
    void create_throwsAdminNotFoundExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        AdminTaskRequest request = new AdminTaskRequest(
                userId, projectId, "Task", null,
                null, null, null, null,
                null, null, null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTaskService.create(request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(projectRepository, never()).findById(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void create_throwsAdminNotFoundExceptionWhenProjectNotFound() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        AppUser user = buildUser(userId, "user@example.com");

        AdminTaskRequest request = new AdminTaskRequest(
                userId, projectId, "Task", null,
                null, null, null, null,
                null, null, null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTaskService.create(request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(projectId.toString());

        verify(taskRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_withUserIdAndProjectIdChangesReferences() {
        UUID taskId = UUID.randomUUID();
        UUID originalUserId = UUID.randomUUID();
        UUID originalProjectId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();
        UUID newProjectId = UUID.randomUUID();

        AppUser originalUser = buildUser(originalUserId, "original@example.com");
        AppUser newUser = buildUser(newUserId, "new@example.com");
        Project originalProject = buildProject(originalProjectId, originalUser, "Old Project");
        Project newProject = buildProject(newProjectId, newUser, "New Project");
        Task task = buildTask(taskId, originalUser, originalProject, "Old Title");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(newUserId)).thenReturn(Optional.of(newUser));
        when(projectRepository.findById(newProjectId)).thenReturn(Optional.of(newProject));

        AdminTaskRequest request = new AdminTaskRequest(
                newUserId, newProjectId, "Updated Title", "Updated desc",
                null, "TODO", (short) 1, null,
                null, null, null, null, null);

        adminTaskService.update(taskId, request);

        assertThat(task.getUser()).isEqualTo(newUser);
        assertThat(task.getProject()).isEqualTo(newProject);
        assertThat(task.getTitle()).isEqualTo("Updated Title");
        assertThat(task.getDescription()).isEqualTo("Updated desc");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_cascadesInCorrectOrder() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        AppUser user = buildUser(userId, "user@example.com");
        Project project = buildProject(projectId, user, "My Project");
        Task task = buildTask(taskId, user, project, "Task to Delete");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        adminTaskService.delete(taskId);

        var inOrder = inOrder(timeBlockRepository, deferredItemRepository, taskRepository);
        inOrder.verify(timeBlockRepository).deleteByTaskId(taskId);
        inOrder.verify(deferredItemRepository).deleteByResolvedTaskId(taskId);
        inOrder.verify(taskRepository).deleteByParentTaskId(taskId);
        inOrder.verify(taskRepository).deleteById(taskId);
    }
}
