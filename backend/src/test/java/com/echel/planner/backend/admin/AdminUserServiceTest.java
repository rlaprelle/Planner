package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminUserRequest;
import com.echel.planner.backend.admin.dto.AdminUserResponse;
import com.echel.planner.backend.admin.dto.DependentCountResponse;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.common.StateConflictException;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.task.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private DeferredItemRepository deferredItemRepository;
    @Mock
    private DailyReflectionRepository reflectionRepository;
    @Mock
    private TimeBlockRepository timeBlockRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

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

    // -------------------------------------------------------------------------
    // listAll
    // -------------------------------------------------------------------------

    @Test
    void listAll_returnsMappedResponses() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        AppUser u1 = buildUser(id1, "alice@example.com");
        AppUser u2 = buildUser(id2, "bob@example.com");

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<AdminUserResponse> responses = adminUserService.listAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(id1);
        assertThat(responses.get(0).email()).isEqualTo("alice@example.com");
        assertThat(responses.get(1).id()).isEqualTo(id2);
        assertThat(responses.get(1).email()).isEqualTo("bob@example.com");
    }

    // -------------------------------------------------------------------------
    // get
    // -------------------------------------------------------------------------

    @Test
    void get_returnsUserById() {
        UUID id = UUID.randomUUID();
        AppUser user = buildUser(id, "user@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        AdminUserResponse response = adminUserService.get(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo("user@example.com");
    }

    @Test
    void get_throwsAdminNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.get(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_savesUserWithEncodedPassword() {
        AdminUserRequest request = new AdminUserRequest(
                "new@example.com", "plaintext", "New User", "America/New_York", AppUser.Role.USER);

        UUID savedId = UUID.randomUUID();
        AppUser saved = buildUser(savedId, "new@example.com");

        when(passwordEncoder.encode("plaintext")).thenReturn("encoded-hash");
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        adminUserService.create(request);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());

        AppUser captured = captor.getValue();
        assertThat(captured.getEmail()).isEqualTo("new@example.com");
        assertThat(captured.getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(captured.getDisplayName()).isEqualTo("New User");
        assertThat(captured.getTimezone()).isEqualTo("America/New_York");
    }

    @Test
    void create_defaultsTimezoneToUtcWhenNull() {
        AdminUserRequest request = new AdminUserRequest(
                "no-tz@example.com", "pass", "No TZ User", null, AppUser.Role.USER);

        UUID savedId = UUID.randomUUID();
        AppUser saved = buildUser(savedId, "no-tz@example.com");

        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        adminUserService.create(request);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getTimezone()).isEqualTo("UTC");
    }

    @Test
    void create_persistsRoleFromRequest() {
        AdminUserRequest request = new AdminUserRequest(
                "admin@example.com", "password1", "Admin User", "UTC", AppUser.Role.ADMIN);

        UUID savedId = UUID.randomUUID();
        AppUser saved = buildUser(savedId, "admin@example.com");

        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        adminUserService.create(request);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(AppUser.Role.ADMIN);
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_setsFieldsAndEncodesPasswordWhenProvided() {
        UUID id = UUID.randomUUID();
        UUID currentAdminId = UUID.randomUUID();
        AppUser user = buildUser(id, "old@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("new-encoded-hash");

        AdminUserRequest request = new AdminUserRequest(
                "updated@example.com", "newpass", "Updated Name", "Europe/London", AppUser.Role.USER);

        adminUserService.update(id, request, currentAdminId);

        assertThat(user.getEmail()).isEqualTo("updated@example.com");
        assertThat(user.getDisplayName()).isEqualTo("Updated Name");
        assertThat(user.getTimezone()).isEqualTo("Europe/London");
        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-hash");
        verify(passwordEncoder).encode("newpass");
    }

    @Test
    void update_doesNotEncodePasswordWhenNullOrBlank() {
        UUID id = UUID.randomUUID();
        UUID currentAdminId = UUID.randomUUID();
        AppUser user = buildUser(id, "user@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // null password
        AdminUserRequest nullPasswordRequest = new AdminUserRequest(
                "user@example.com", null, "Display", "UTC", AppUser.Role.USER);
        adminUserService.update(id, nullPasswordRequest, currentAdminId);
        verify(passwordEncoder, never()).encode(any());

        // blank password
        AdminUserRequest blankPasswordRequest = new AdminUserRequest(
                "user@example.com", "   ", "Display", "UTC", AppUser.Role.USER);
        adminUserService.update(id, blankPasswordRequest, currentAdminId);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void update_setsRoleFromRequest() {
        UUID id = UUID.randomUUID();
        UUID currentAdminId = UUID.randomUUID();
        AppUser user = buildUser(id, "user@example.com");
        // user defaults to USER role

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        AdminUserRequest request = new AdminUserRequest(
                "user@example.com", null, "Display", "UTC", AppUser.Role.ADMIN);

        adminUserService.update(id, request, currentAdminId);

        assertThat(user.getRole()).isEqualTo(AppUser.Role.ADMIN);
    }

    @Test
    void update_changingOwnRoleThrowsStateConflict() {
        UUID id = UUID.randomUUID();
        AppUser user = buildUser(id, "self@example.com");
        user.setRole(AppUser.Role.ADMIN);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        AdminUserRequest request = new AdminUserRequest(
                "self@example.com", null, "Self", "UTC", AppUser.Role.USER);

        assertThatThrownBy(() -> adminUserService.update(id, request, id))
                .isInstanceOf(StateConflictException.class)
                .hasMessageContaining("your own role");
    }

    @Test
    void update_sameRoleAsCurrentDoesNotInvokeGuards() {
        UUID id = UUID.randomUUID();
        AppUser user = buildUser(id, "self@example.com");
        user.setRole(AppUser.Role.ADMIN);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // role unchanged, even though id == currentAdminId
        AdminUserRequest request = new AdminUserRequest(
                "self@example.com", null, "Self", "UTC", AppUser.Role.ADMIN);

        adminUserService.update(id, request, id); // should not throw
        verify(userRepository, never()).countByRole(any());
    }

    @Test
    void update_demotingLastAdminThrowsStateConflict() {
        UUID adminId = UUID.randomUUID();
        UUID currentAdminId = UUID.randomUUID(); // a different admin doing the demotion
        AppUser admin = buildUser(adminId, "admin@example.com");
        admin.setRole(AppUser.Role.ADMIN);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(AppUser.Role.ADMIN)).thenReturn(1L);

        AdminUserRequest request = new AdminUserRequest(
                "admin@example.com", null, "Admin", "UTC", AppUser.Role.USER);

        assertThatThrownBy(() -> adminUserService.update(adminId, request, currentAdminId))
                .isInstanceOf(StateConflictException.class)
                .hasMessageContaining("last admin");
    }

    @Test
    void update_demotingAdminWhenAnotherAdminExistsSucceeds() {
        UUID adminId = UUID.randomUUID();
        UUID currentAdminId = UUID.randomUUID();
        AppUser admin = buildUser(adminId, "admin@example.com");
        admin.setRole(AppUser.Role.ADMIN);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(AppUser.Role.ADMIN)).thenReturn(2L);

        AdminUserRequest request = new AdminUserRequest(
                "admin@example.com", null, "Admin", "UTC", AppUser.Role.USER);

        adminUserService.update(adminId, request, currentAdminId);

        assertThat(admin.getRole()).isEqualTo(AppUser.Role.USER);
    }

    @Test
    void update_promotingUserDoesNotCheckLastAdminGuard() {
        UUID userId = UUID.randomUUID();
        UUID currentAdminId = UUID.randomUUID();
        AppUser user = buildUser(userId, "user@example.com");
        // user defaults to USER role

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AdminUserRequest request = new AdminUserRequest(
                "user@example.com", null, "User", "UTC", AppUser.Role.ADMIN);

        adminUserService.update(userId, request, currentAdminId);

        assertThat(user.getRole()).isEqualTo(AppUser.Role.ADMIN);
        verify(userRepository, never()).countByRole(any());
    }

    // -------------------------------------------------------------------------
    // getDependentCounts
    // -------------------------------------------------------------------------

    @Test
    void getDependentCounts_returnsCountsFromAllRepositories() {
        UUID id = UUID.randomUUID();
        AppUser user = buildUser(id, "user@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(projectRepository.countByUserId(id)).thenReturn(3L);
        when(taskRepository.countByUserId(id)).thenReturn(10L);
        when(deferredItemRepository.countByUserId(id)).thenReturn(2L);
        when(reflectionRepository.countByUserId(id)).thenReturn(5L);
        when(timeBlockRepository.countByUserId(id)).thenReturn(8L);

        DependentCountResponse counts = adminUserService.getDependentCounts(id);

        assertThat(counts.projects()).isEqualTo(3L);
        assertThat(counts.tasks()).isEqualTo(10L);
        assertThat(counts.deferredItems()).isEqualTo(2L);
        assertThat(counts.reflections()).isEqualTo(5L);
        assertThat(counts.timeBlocks()).isEqualTo(8L);
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_cascadesDeletesInCorrectOrder() {
        UUID id = UUID.randomUUID();
        AppUser user = buildUser(id, "user@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        adminUserService.delete(id);

        var inOrder = inOrder(
                timeBlockRepository,
                deferredItemRepository,
                reflectionRepository,
                taskRepository,
                projectRepository,
                userRepository);

        inOrder.verify(timeBlockRepository).deleteByUserId(id);
        inOrder.verify(deferredItemRepository).deleteByUserId(id);
        inOrder.verify(reflectionRepository).deleteByUserId(id);
        inOrder.verify(taskRepository).deleteByUserId(id);
        inOrder.verify(projectRepository).deleteByUserId(id);
        inOrder.verify(userRepository).deleteById(id);
    }
}
