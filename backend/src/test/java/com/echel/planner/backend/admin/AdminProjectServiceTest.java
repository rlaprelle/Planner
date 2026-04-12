package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminProjectRequest;
import com.echel.planner.backend.admin.dto.AdminProjectResponse;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.TaskRepository;
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
class AdminProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private DeferredItemRepository deferredItemRepository;

    @InjectMocks
    private AdminProjectService adminProjectService;

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

    // -------------------------------------------------------------------------
    // listAll
    // -------------------------------------------------------------------------

    @Test
    void listAll_returnsMappedResponses() {
        UUID userId = UUID.randomUUID();
        AppUser user = buildUser(userId, "owner@example.com");

        UUID pid1 = UUID.randomUUID();
        UUID pid2 = UUID.randomUUID();
        Project p1 = buildProject(pid1, user, "Alpha");
        Project p2 = buildProject(pid2, user, "Beta");

        when(projectRepository.findAll()).thenReturn(List.of(p1, p2));

        List<AdminProjectResponse> responses = adminProjectService.listAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(pid1);
        assertThat(responses.get(0).name()).isEqualTo("Alpha");
        assertThat(responses.get(1).id()).isEqualTo(pid2);
        assertThat(responses.get(1).name()).isEqualTo("Beta");
    }

    // -------------------------------------------------------------------------
    // get
    // -------------------------------------------------------------------------

    @Test
    void get_throwsAdminNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminProjectService.get(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_looksUpUserAndSetsAllFields() {
        UUID userId = UUID.randomUUID();
        AppUser user = buildUser(userId, "owner@example.com");

        AdminProjectRequest request = new AdminProjectRequest(
                userId, "New Project", "A description", "#aabbcc", "rocket", true, 5);

        UUID savedId = UUID.randomUUID();
        Project saved = buildProject(savedId, user, "New Project");
        saved.setDescription("A description");
        saved.setColor("#aabbcc");
        saved.setIcon("rocket");
        saved.setActive(true);
        saved.setSortOrder(5);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectRepository.save(any(Project.class))).thenReturn(saved);

        AdminProjectResponse response = adminProjectService.create(request);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());

        Project captured = captor.getValue();
        assertThat(captured.getUser()).isEqualTo(user);
        assertThat(captured.getName()).isEqualTo("New Project");
        assertThat(captured.getDescription()).isEqualTo("A description");
        assertThat(captured.getColor()).isEqualTo("#aabbcc");
        assertThat(captured.getIcon()).isEqualTo("rocket");
        assertThat(captured.isActive()).isTrue();
        assertThat(captured.getSortOrder()).isEqualTo(5);

        assertThat(response.id()).isEqualTo(savedId);
    }

    @Test
    void create_throwsAdminNotFoundExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        AdminProjectRequest request = new AdminProjectRequest(
                userId, "Project", null, null, null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminProjectService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_withUserIdChangesUserAndSetsFields() {
        UUID projectId = UUID.randomUUID();
        UUID originalUserId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();

        AppUser originalUser = buildUser(originalUserId, "original@example.com");
        AppUser newUser = buildUser(newUserId, "new@example.com");
        Project project = buildProject(projectId, originalUser, "Old Name");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(newUserId)).thenReturn(Optional.of(newUser));

        AdminProjectRequest request = new AdminProjectRequest(
                newUserId, "New Name", "Updated desc", "#112233", "star", true, 2);

        adminProjectService.update(projectId, request);

        assertThat(project.getUser()).isEqualTo(newUser);
        assertThat(project.getName()).isEqualTo("New Name");
        assertThat(project.getDescription()).isEqualTo("Updated desc");
        assertThat(project.getColor()).isEqualTo("#112233");
        assertThat(project.getIcon()).isEqualTo("star");
        assertThat(project.isActive()).isTrue();
        assertThat(project.getSortOrder()).isEqualTo(2);
    }

    @Test
    void update_setsArchivedAtWhenIsActiveFalse() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AppUser user = buildUser(userId, "user@example.com");
        Project project = buildProject(projectId, user, "Active Project");

        assertThat(project.getArchivedAt()).isNull();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        AdminProjectRequest request = new AdminProjectRequest(
                null, "Active Project", null, null, null, false, null);

        adminProjectService.update(projectId, request);

        assertThat(project.isActive()).isFalse();
        assertThat(project.getArchivedAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_cascadesInCorrectOrder() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AppUser user = buildUser(userId, "user@example.com");
        Project project = buildProject(projectId, user, "To Delete");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        adminProjectService.delete(projectId);

        var inOrder = inOrder(deferredItemRepository, taskRepository, projectRepository);
        inOrder.verify(deferredItemRepository).deleteByResolvedProjectId(projectId);
        inOrder.verify(taskRepository).deleteByProjectId(projectId);
        inOrder.verify(projectRepository).delete(project);
    }
}
