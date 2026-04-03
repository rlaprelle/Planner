package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminDeferredItemRequest;
import com.echel.planner.backend.admin.dto.AdminDeferredItemResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.deferred.DeferredItem;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDeferredItemServiceTest {

    @Mock
    private DeferredItemRepository deferredItemRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private AdminDeferredItemService service;

    private AppUser user;
    private UUID userId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        user = new AppUser("admin@example.com", "hash", "Admin User", "UTC");
        userId = UUID.randomUUID();
        itemId = UUID.randomUUID();
    }

    // --- listAll ---

    @Test
    void listAll_returnsMappedResponses() {
        DeferredItem item1 = new DeferredItem(user, "Buy groceries");
        DeferredItem item2 = new DeferredItem(user, "Call plumber");
        when(deferredItemRepository.findAll()).thenReturn(List.of(item1, item2));

        List<AdminDeferredItemResponse> responses = service.listAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).rawText()).isEqualTo("Buy groceries");
        assertThat(responses.get(1).rawText()).isEqualTo("Call plumber");
    }

    // --- get ---

    @Test
    void get_throwsAdminNotFoundExceptionWhenItemAbsent() {
        when(deferredItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(itemId))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(itemId.toString());
    }

    // --- create ---

    @Test
    void create_looksUpUserAndSetsRawText() {
        AdminDeferredItemRequest request = new AdminDeferredItemRequest(
                userId, "Fix the leak", null, null, null, null, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        DeferredItem saved = new DeferredItem(user, "Fix the leak");
        when(deferredItemRepository.save(any(DeferredItem.class))).thenReturn(saved);

        AdminDeferredItemResponse response = service.create(request);

        ArgumentCaptor<DeferredItem> captor = ArgumentCaptor.forClass(DeferredItem.class);
        verify(deferredItemRepository).save(captor.capture());
        assertThat(captor.getValue().getRawText()).isEqualTo("Fix the leak");
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(response.rawText()).isEqualTo("Fix the leak");
    }

    @Test
    void create_throwsAdminNotFoundExceptionWhenUserAbsent() {
        AdminDeferredItemRequest request = new AdminDeferredItemRequest(
                userId, "Some text", null, null, null, null, null);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void create_appliesOptionalFields() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        LocalDate deferDate = LocalDate.of(2026, 5, 1);

        Task task = mock(Task.class);
        Project project = mock(Project.class);

        AdminDeferredItemRequest request = new AdminDeferredItemRequest(
                userId, "Review doc", true, taskId, projectId, deferDate, 3);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        ArgumentCaptor<DeferredItem> captor = ArgumentCaptor.forClass(DeferredItem.class);
        DeferredItem saved = new DeferredItem(user, "Review doc");
        saved.setProcessed(true);
        saved.setResolvedTask(task);
        saved.setResolvedProject(project);
        saved.setDeferredUntilDate(deferDate);
        saved.setDeferralCount(3);
        when(deferredItemRepository.save(any(DeferredItem.class))).thenReturn(saved);

        service.create(request);

        verify(deferredItemRepository).save(captor.capture());
        DeferredItem captured = captor.getValue();
        assertThat(captured.isProcessed()).isTrue();
        assertThat(captured.getProcessedAt()).isNotNull();
        assertThat(captured.getResolvedTask()).isSameAs(task);
        assertThat(captured.getResolvedProject()).isSameAs(project);
        assertThat(captured.getDeferredUntilDate()).isEqualTo(deferDate);
        assertThat(captured.getDeferralCount()).isEqualTo(3);
    }

    // --- update ---

    @Test
    void update_setsRawTextAndAppliesOptionalFields() {
        LocalDate deferDate = LocalDate.of(2026, 6, 15);
        DeferredItem existing = new DeferredItem(user, "Old text");
        when(deferredItemRepository.findById(itemId)).thenReturn(Optional.of(existing));

        AdminDeferredItemRequest request = new AdminDeferredItemRequest(
                userId, "Updated text", false, null, null, deferDate, 2);

        service.update(itemId, request);

        assertThat(existing.getRawText()).isEqualTo("Updated text");
        assertThat(existing.isProcessed()).isFalse();
        assertThat(existing.getProcessedAt()).isNull();
        assertThat(existing.getDeferredUntilDate()).isEqualTo(deferDate);
        assertThat(existing.getDeferralCount()).isEqualTo(2);
    }

    @Test
    void update_throwsAdminNotFoundExceptionWhenItemAbsent() {
        when(deferredItemRepository.findById(itemId)).thenReturn(Optional.empty());
        AdminDeferredItemRequest request = new AdminDeferredItemRequest(
                userId, "text", null, null, null, null, null);

        assertThatThrownBy(() -> service.update(itemId, request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(itemId.toString());
    }

    // --- delete ---

    @Test
    void delete_removesItemById() {
        DeferredItem existing = new DeferredItem(user, "To delete");
        when(deferredItemRepository.findById(itemId)).thenReturn(Optional.of(existing));

        service.delete(itemId);

        verify(deferredItemRepository).deleteById(itemId);
    }

    @Test
    void delete_throwsAdminNotFoundExceptionWhenItemAbsent() {
        when(deferredItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(itemId))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(itemId.toString());
    }
}
