package com.echel.planner.backend.project;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.project.dto.ProjectCreateRequest;
import com.echel.planner.backend.project.dto.ProjectResponse;
import com.echel.planner.backend.project.dto.ProjectUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
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
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private AppUser user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new AppUser("test@example.com", "hash", "Test User", "UTC");
        // Reflect the id in since AppUser uses @GeneratedValue (not set by constructor)
        // We set it via a helper project that wraps user
    }

    // Helper: build a minimal Project with a known id for stub returns
    private Project buildProject(UUID id, AppUser owner, String name) {
        Project p = new Project(owner, name);
        // Use reflection to set the generated id so ProjectResponse.from() can read it
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(p, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return p;
    }

    // Helper: build an AppUser with a known id
    private AppUser buildUser(UUID id) {
        AppUser u = new AppUser("user@example.com", "hash", "Display", "UTC");
        try {
            var field = AppUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(u, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return u;
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_savesProjectWithAllFields() {
        AppUser owner = buildUser(userId);
        ProjectCreateRequest request = new ProjectCreateRequest(
                "My Project", "A description", "#ff0000", "star", 3);

        UUID projectId = UUID.randomUUID();
        Project saved = buildProject(projectId, owner, "My Project");
        saved.setDescription("A description");
        saved.setColor("#ff0000");
        saved.setIcon("star");
        saved.setSortOrder(3);

        when(projectRepository.save(any(Project.class))).thenReturn(saved);

        ProjectResponse response = projectService.create(owner, request);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());

        Project captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("My Project");
        assertThat(captured.getDescription()).isEqualTo("A description");
        assertThat(captured.getColor()).isEqualTo("#ff0000");
        assertThat(captured.getIcon()).isEqualTo("star");
        assertThat(captured.getSortOrder()).isEqualTo(3);

        assertThat(response.id()).isEqualTo(projectId);
        assertThat(response.name()).isEqualTo("My Project");
    }

    @Test
    void create_defaultsSortOrderWhenNull() {
        AppUser owner = buildUser(userId);
        ProjectCreateRequest request = new ProjectCreateRequest(
                "No Order", null, null, null, null);

        UUID projectId = UUID.randomUUID();
        Project saved = buildProject(projectId, owner, "No Order");
        // Default sortOrder is 0 per field initializer

        when(projectRepository.save(any(Project.class))).thenReturn(saved);

        projectService.create(owner, request);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());

        // sortOrder should remain the default (0) when request.sortOrder() is null
        assertThat(captor.getValue().getSortOrder()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // listActive
    // -------------------------------------------------------------------------

    @Test
    void listActive_returnsMappedResponsesForUser() {
        AppUser owner = buildUser(userId);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Project p1 = buildProject(id1, owner, "Alpha");
        Project p2 = buildProject(id2, owner, "Beta");

        when(projectRepository.findByUserIdAndIsActiveTrueOrderBySortOrderAsc(userId))
                .thenReturn(List.of(p1, p2));

        List<ProjectResponse> responses = projectService.listActive(owner);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(id1);
        assertThat(responses.get(0).name()).isEqualTo("Alpha");
        assertThat(responses.get(1).id()).isEqualTo(id2);
        assertThat(responses.get(1).name()).isEqualTo("Beta");
    }

    @Test
    void listActive_returnsEmptyListWhenNoProjects() {
        AppUser owner = buildUser(userId);

        when(projectRepository.findByUserIdAndIsActiveTrueOrderBySortOrderAsc(userId))
                .thenReturn(List.of());

        List<ProjectResponse> responses = projectService.listActive(owner);

        assertThat(responses).isEmpty();
    }

    // -------------------------------------------------------------------------
    // get
    // -------------------------------------------------------------------------

    @Test
    void get_returnsProjectOwnedByUser() {
        AppUser owner = buildUser(userId);
        UUID projectId = UUID.randomUUID();
        Project project = buildProject(projectId, owner, "Owned Project");
        project.setDescription("Some desc");

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));

        ProjectResponse response = projectService.get(owner, projectId);

        assertThat(response.id()).isEqualTo(projectId);
        assertThat(response.name()).isEqualTo("Owned Project");
        assertThat(response.description()).isEqualTo("Some desc");
    }

    @Test
    void get_throwsProjectNotFoundExceptionWhenNotFound() {
        AppUser owner = buildUser(userId);
        UUID projectId = UUID.randomUUID();

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.get(owner, projectId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(projectId.toString());
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_setsAllFields() {
        AppUser owner = buildUser(userId);
        UUID projectId = UUID.randomUUID();
        Project project = buildProject(projectId, owner, "Old Name");
        project.setDescription("Old desc");
        project.setColor("#000000");
        project.setIcon("circle");
        project.setSortOrder(1);

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));

        ProjectUpdateRequest request = new ProjectUpdateRequest(
                "New Name", "New desc", "#ffffff", "square", 5);

        ProjectResponse response = projectService.update(owner, projectId, request);

        assertThat(project.getName()).isEqualTo("New Name");
        assertThat(project.getDescription()).isEqualTo("New desc");
        assertThat(project.getColor()).isEqualTo("#ffffff");
        assertThat(project.getIcon()).isEqualTo("square");
        assertThat(project.getSortOrder()).isEqualTo(5);

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.sortOrder()).isEqualTo(5);
    }

    @Test
    void update_doesNotChangeSortOrderWhenNull() {
        AppUser owner = buildUser(userId);
        UUID projectId = UUID.randomUUID();
        Project project = buildProject(projectId, owner, "Project");
        project.setSortOrder(7);

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));

        ProjectUpdateRequest request = new ProjectUpdateRequest(
                "Project", null, null, null, null);

        projectService.update(owner, projectId, request);

        // sortOrder must remain unchanged when request.sortOrder() is null
        assertThat(project.getSortOrder()).isEqualTo(7);
    }

    @Test
    void update_throwsProjectNotFoundExceptionWhenNotFound() {
        AppUser owner = buildUser(userId);
        UUID projectId = UUID.randomUUID();

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());

        ProjectUpdateRequest request = new ProjectUpdateRequest(
                "Name", null, null, null, null);

        assertThatThrownBy(() -> projectService.update(owner, projectId, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // archive
    // -------------------------------------------------------------------------

    @Test
    void archive_setsIsActiveFalseAndArchivedAt() {
        AppUser owner = buildUser(userId);
        UUID projectId = UUID.randomUUID();
        Project project = buildProject(projectId, owner, "Active Project");

        assertThat(project.isActive()).isTrue();
        assertThat(project.getArchivedAt()).isNull();

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));

        ProjectResponse response = projectService.archive(owner, projectId);

        assertThat(project.isActive()).isFalse();
        assertThat(project.getArchivedAt()).isNotNull();
        assertThat(response.isActive()).isFalse();
        assertThat(response.archivedAt()).isNotNull();
    }

    @Test
    void archive_throwsProjectNotFoundExceptionWhenNotFound() {
        AppUser owner = buildUser(userId);
        UUID projectId = UUID.randomUUID();

        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.archive(owner, projectId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
