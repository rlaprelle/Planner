# Admin Role Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose `AppUser.Role` (USER/ADMIN) through the admin user CRUD with self-edit and last-admin guards, and surface mutation errors in the admin form so guard violations are visible.

**Architecture:** Backend adds `role` to the user CRUD DTOs, threads the current admin's `UUID` from controller (`@AuthenticationPrincipal AppUser`) into `AdminUserService.update`, and enforces two guards before any role change. Frontend adds a role column with badge, a role select to the user form, an inline lag note when role is being changed on edit, and a generic "save error" surface in `AdminFormModal` so 409 responses are visible.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Security, JUnit 5, Mockito, MockMvc; React 18, TanStack Query, react-i18next, Tailwind, Radix Dialog.

**Spec:** [docs/superpowers/specs/2026-04-21-admin-role-management-design.md](../specs/2026-04-21-admin-role-management-design.md)

---

## File Map

**Backend — modify:**
- `backend/src/main/java/com/echel/planner/backend/auth/AppUserRepository.java` — add `countByRole`
- `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminUserRequest.java` — add `role` field
- `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminUserResponse.java` — add `role` field
- `backend/src/main/java/com/echel/planner/backend/admin/AdminUserService.java` — read/write role + guards on `update`
- `backend/src/main/java/com/echel/planner/backend/admin/AdminUserController.java` — pass current admin id into `update`
- `backend/src/test/java/com/echel/planner/backend/admin/AdminUserServiceTest.java` — new tests
- `backend/src/test/java/com/echel/planner/backend/admin/AdminUserControllerIntegrationTest.java` — fix DTO calls

**Frontend — modify:**
- `frontend/src/pages/admin/hooks/useAdminCrud.js` — expose `saveError`
- `frontend/src/pages/admin/components/AdminFormModal.jsx` — render `saveError`, support optional per-field `dynamicHint`
- `frontend/src/pages/admin/components/AdminCrudPage.jsx` — pass `saveError` through
- `frontend/src/pages/admin/AdminUsersTable.jsx` — role column + role select + lag note
- `frontend/src/locales/en/admin.json` — new keys

---

## Task 1: Backend — add `role` to DTOs and round-trip through service (no guards yet)

**Goal:** `role` is required on create/update, returned on read, and persisted — all existing tests still pass and the new round-trip is covered.

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminUserRequest.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminUserResponse.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/admin/AdminUserService.java`
- Modify: `backend/src/test/java/com/echel/planner/backend/admin/AdminUserServiceTest.java`
- Modify: `backend/src/test/java/com/echel/planner/backend/admin/AdminUserControllerIntegrationTest.java`

**Acceptance Criteria:**
- [ ] `AdminUserRequest` has a non-null `role` field
- [ ] `AdminUserResponse` exposes `role`
- [ ] `AdminUserService.create` sets the role from the request
- [ ] `AdminUserService.update` sets the role from the request (no guards yet — those land in Task 2)
- [ ] All existing tests in `AdminUserServiceTest` and `AdminUserControllerIntegrationTest` still pass after their constructors are updated
- [ ] New unit test verifies `create` persists the role
- [ ] New unit test verifies `update` mutates the role on the entity

**Verify:** `cd backend && mvn test -Dtest=AdminUserServiceTest,AdminUserControllerIntegrationTest` → all green.

**Steps:**

- [ ] **Step 1: Update `AdminUserRequest` to include role**

```java
package com.echel.planner.backend.admin.dto;

import com.echel.planner.backend.auth.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Size(min = 6) String password,
        @NotBlank @Size(max = 255) String displayName,
        @Size(max = 100) String timezone,
        @NotNull AppUser.Role role
) {}
```

- [ ] **Step 2: Update `AdminUserResponse` to include role**

```java
package com.echel.planner.backend.admin.dto;

import com.echel.planner.backend.auth.AppUser;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        String timezone,
        AppUser.Role role,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminUserResponse from(AppUser user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getTimezone(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 3: Update `AdminUserService.create` and `update` to handle role (no guards yet)**

In `AdminUserService.create`, after constructing the `AppUser`, set the role:

```java
public AdminUserResponse create(AdminUserRequest request) {
    AppUser user = new AppUser(
            request.email(),
            passwordEncoder.encode(request.password()),
            request.displayName(),
            request.timezone() != null ? request.timezone() : "UTC"
    );
    user.setRole(request.role());
    return AdminUserResponse.from(userRepository.save(user));
}
```

In `AdminUserService.update`, set the role unconditionally (guards land in Task 2):

```java
public AdminUserResponse update(UUID id, AdminUserRequest request) {
    AppUser user = findUser(id);
    user.setEmail(request.email());
    user.setDisplayName(request.displayName());
    if (request.timezone() != null) {
        user.setTimezone(request.timezone());
    }
    if (request.password() != null && !request.password().isBlank()) {
        user.setPasswordHash(passwordEncoder.encode(request.password()));
    }
    user.setRole(request.role());
    return AdminUserResponse.from(user);
}
```

- [ ] **Step 4: Update existing `AdminUserServiceTest` constructors and add role-handling tests**

Every `new AdminUserRequest(...)` in this file must add a final `AppUser.Role` argument. For existing tests that don't care about role, default to `AppUser.Role.USER`.

Add these new tests in the appropriate sections:

```java
// In the "create" section
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

// In the "update" section
@Test
void update_setsRoleFromRequest() {
    UUID id = UUID.randomUUID();
    AppUser user = buildUser(id, "user@example.com");
    // user defaults to USER role

    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    AdminUserRequest request = new AdminUserRequest(
            "user@example.com", null, "Display", "UTC", AppUser.Role.ADMIN);

    adminUserService.update(id, request);

    assertThat(user.getRole()).isEqualTo(AppUser.Role.ADMIN);
}
```

- [ ] **Step 5: Update `AdminUserControllerIntegrationTest` for the new DTO**

Every `new AdminUserRequest(...)` in this file gains a trailing `AppUser.Role` argument. The `sampleResponse()` helper must also include a role:

```java
private AdminUserResponse sampleResponse() {
    return new AdminUserResponse(
            USER_ID,
            "alice@example.com",
            "Alice",
            "UTC",
            AppUser.Role.USER,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z")
    );
}
```

For the create and update tests, pass `AppUser.Role.USER` as the trailing arg in the request and add a `jsonPath("$.role").value("USER")` assertion to the create test.

- [ ] **Step 6: Run tests to confirm everything still green**

Run: `cd backend && mvn test -Dtest=AdminUserServiceTest,AdminUserControllerIntegrationTest`
Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/admin/dto/AdminUserRequest.java \
        backend/src/main/java/com/echel/planner/backend/admin/dto/AdminUserResponse.java \
        backend/src/main/java/com/echel/planner/backend/admin/AdminUserService.java \
        backend/src/test/java/com/echel/planner/backend/admin/AdminUserServiceTest.java \
        backend/src/test/java/com/echel/planner/backend/admin/AdminUserControllerIntegrationTest.java
git commit -m "feat(admin): plumb user role through admin user CRUD"
```

---

## Task 2: Backend — guards on role change in `AdminUserService.update`

**Goal:** Block self-edit of role and block demoting the last admin. Both guards run only when the role is actually changing.

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/auth/AppUserRepository.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/admin/AdminUserService.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/admin/AdminUserController.java`
- Modify: `backend/src/test/java/com/echel/planner/backend/admin/AdminUserServiceTest.java`
- Modify: `backend/src/test/java/com/echel/planner/backend/admin/AdminUserControllerIntegrationTest.java`

**Acceptance Criteria:**
- [ ] `AppUserRepository.countByRole(Role)` returns the count of users with that role
- [ ] `AdminUserService.update(id, request, currentAdminId)` is the new signature
- [ ] If `id.equals(currentAdminId)` and `request.role() != user.getRole()`, throw `StateConflictException("You cannot change your own role.")`
- [ ] If `user.getRole() == ADMIN`, `request.role() == USER`, and `countByRole(ADMIN) <= 1`, throw `StateConflictException("Cannot demote the last admin.")`
- [ ] When the role is unchanged the guards do not run (no extra repo calls)
- [ ] `AdminUserController.update` extracts the current admin via `@AuthenticationPrincipal AppUser` and passes the id through
- [ ] Unit tests cover both guards (positive and negative)
- [ ] `StateConflictException` returns HTTP 409 (already wired in the global exception handler)

**Verify:** `cd backend && mvn test -Dtest=AdminUserServiceTest,AdminUserControllerIntegrationTest` → all green.

**Steps:**

- [ ] **Step 1: Add `countByRole` to `AppUserRepository`**

```java
package com.echel.planner.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);

    long countByRole(AppUser.Role role);
}
```

- [ ] **Step 2: Change the `update` signature on `AdminUserService` and add the two guards**

```java
public AdminUserResponse update(UUID id, AdminUserRequest request, UUID currentAdminId) {
    AppUser user = findUser(id);

    AppUser.Role oldRole = user.getRole();
    AppUser.Role newRole = request.role();
    if (newRole != oldRole) {
        if (id.equals(currentAdminId)) {
            throw new StateConflictException("You cannot change your own role.");
        }
        if (oldRole == AppUser.Role.ADMIN
                && newRole == AppUser.Role.USER
                && userRepository.countByRole(AppUser.Role.ADMIN) <= 1) {
            throw new StateConflictException("Cannot demote the last admin.");
        }
    }

    user.setEmail(request.email());
    user.setDisplayName(request.displayName());
    if (request.timezone() != null) {
        user.setTimezone(request.timezone());
    }
    if (request.password() != null && !request.password().isBlank()) {
        user.setPasswordHash(passwordEncoder.encode(request.password()));
    }
    user.setRole(newRole);
    return AdminUserResponse.from(user);
}
```

Add the import: `import com.echel.planner.backend.common.StateConflictException;`

- [ ] **Step 3: Update `AdminUserController.update` to thread the current admin's id**

```java
@PutMapping("/{id}")
public ResponseEntity<AdminUserResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody AdminUserRequest request,
        @AuthenticationPrincipal AppUser currentAdmin) {
    return ResponseEntity.ok(adminUserService.update(id, request, currentAdmin.getId()));
}
```

Add imports:
```java
import com.echel.planner.backend.auth.AppUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
```

- [ ] **Step 4: Add unit tests for the guards in `AdminUserServiceTest`**

All existing `update` tests in this file must be updated to pass a third argument to `adminUserService.update(...)`. Use a fresh `UUID currentAdminId = UUID.randomUUID()` (different from the target user id) so the self-edit guard does not fire.

Then add these new tests in the "update" section:

```java
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
```

Add the import: `import com.echel.planner.backend.common.StateConflictException;`

- [ ] **Step 5: Update `AdminUserControllerIntegrationTest` for the new service signature**

The existing `update_existingId_returns200WithUpdatedUser` test stubs `adminUserService.update(eq(USER_ID), any(AdminUserRequest.class))`. Change it to:

```java
when(adminUserService.update(eq(USER_ID), any(AdminUserRequest.class), any(UUID.class)))
        .thenReturn(updated);
```

The `@WithMockUser(roles = "ADMIN")` annotation produces a `User` principal (a `String` username), not an `AppUser`. To make the controller's `@AuthenticationPrincipal AppUser` resolve, replace the class-level `@WithMockUser(roles = "ADMIN")` and the field/method usage with a custom security context that supplies an `AppUser` principal. The simplest approach: register a `WithSecurityContextFactory` for an existing `AppUser`. To keep the diff small, use `@WithMockUser` per-method only on tests that don't exercise `@AuthenticationPrincipal`, and for `update_existingId_returns200WithUpdatedUser` use Spring Security's `SecurityMockMvcRequestPostProcessors.user(...)` with an `AppUser` instance:

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@Test
void update_existingId_returns200WithUpdatedUser() throws Exception {
    AppUser currentAdmin = new AppUser("admin@example.com", "hash", "Admin", "UTC");
    currentAdmin.setRole(AppUser.Role.ADMIN);
    // give it an id via reflection (mirroring AdminUserServiceTest.buildUser)
    var idField = AppUser.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(currentAdmin, UUID.fromString("00000000-0000-0000-0000-000000000099"));

    AdminUserRequest request = new AdminUserRequest(
            "alice-updated@example.com", null, "Alice Updated", "America/New_York", AppUser.Role.USER);
    AdminUserResponse updated = new AdminUserResponse(
            USER_ID,
            "alice-updated@example.com",
            "Alice Updated",
            "America/New_York",
            AppUser.Role.USER,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-06-01T00:00:00Z")
    );
    when(adminUserService.update(eq(USER_ID), any(AdminUserRequest.class), any(UUID.class)))
            .thenReturn(updated);

    mockMvc.perform(put("/api/v1/admin/users/{id}", USER_ID)
                    .with(user(currentAdmin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("alice-updated@example.com"))
            .andExpect(jsonPath("$.displayName").value("Alice Updated"))
            .andExpect(jsonPath("$.role").value("USER"));
}
```

Other tests in the class are unaffected because they don't hit the update endpoint. Leave the class-level `@WithMockUser(roles = "ADMIN")` in place; per-method `.with(user(...))` overrides it.

- [ ] **Step 6: Run tests**

Run: `cd backend && mvn test -Dtest=AdminUserServiceTest,AdminUserControllerIntegrationTest,AdminAuthorizationIntegrationTest`
Expected: all green.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/auth/AppUserRepository.java \
        backend/src/main/java/com/echel/planner/backend/admin/AdminUserService.java \
        backend/src/main/java/com/echel/planner/backend/admin/AdminUserController.java \
        backend/src/test/java/com/echel/planner/backend/admin/AdminUserServiceTest.java \
        backend/src/test/java/com/echel/planner/backend/admin/AdminUserControllerIntegrationTest.java
git commit -m "feat(admin): block self role-edit and last-admin demotion"
```

---

## Task 3: Frontend — surface mutation errors in `AdminFormModal`

**Goal:** When a create/update mutation in any admin CRUD page fails, the error message returned by the server is shown inside the form modal. This is the channel through which Task 2's 409s become visible to the admin in Task 4.

**Files:**
- Modify: `frontend/src/pages/admin/hooks/useAdminCrud.js`
- Modify: `frontend/src/pages/admin/components/AdminFormModal.jsx`
- Modify: `frontend/src/pages/admin/components/AdminCrudPage.jsx`

**Acceptance Criteria:**
- [ ] `useAdminCrud` exposes `saveError` (the most recent create/update mutation error, or `null`)
- [ ] `saveError` is cleared when the form is opened (so a stale error from a previous failed save doesn't carry into the next attempt)
- [ ] `AdminFormModal` accepts a `saveError` prop and renders it as a soft red banner above the form buttons when truthy
- [ ] `AdminCrudPage` passes `crud.saveError` through to the modal
- [ ] Existing admin pages (Projects, Tasks, etc.) continue to render unchanged when no error occurs

**Verify:**
- `cd frontend && npm run lint` → clean
- Manual: trigger a 409 from an existing admin page (the simplest is duplicating a project name if a uniqueness constraint exists, or just temporarily throw in the backend) and confirm the message appears. If no easy trigger exists in current admin pages, defer manual verification to Task 4 where the role guards provide a real trigger.

**Steps:**

- [ ] **Step 1: Update `useAdminCrud` to expose `saveError`**

```javascript
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

export function useAdminCrud({ queryKey, listFn, createFn, updateFn, deleteFn }) {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data = [], isLoading } = useQuery({ queryKey, queryFn: listFn })

  const createMutation = useMutation({
    mutationFn: createFn,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateFn(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: deleteFn,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey }); setDeleteItem(null) },
  })

  const openCreate = () => {
    createMutation.reset()
    updateMutation.reset()
    setEditItem(null)
    setFormOpen(true)
  }
  const openEdit = (row) => {
    createMutation.reset()
    updateMutation.reset()
    setEditItem(row)
    setFormOpen(true)
  }
  const openDelete = (row) => setDeleteItem(row)

  const handleFormClose = (open) => {
    setFormOpen(open)
    if (!open) {
      setEditItem(null)
      createMutation.reset()
      updateMutation.reset()
    }
  }
  const handleDeleteClose = (open) => { if (!open) setDeleteItem(null) }

  const handleSubmit = (formData) => {
    if (editItem) {
      updateMutation.mutate({ id: editItem.id, data: formData })
    } else {
      createMutation.mutate(formData)
    }
  }

  const confirmDelete = () => deleteMutation.mutate(deleteItem.id)

  const saveError = updateMutation.error || createMutation.error || null

  return {
    data, isLoading,
    formOpen, editItem, deleteItem,
    openCreate, openEdit, openDelete, setDeleteItem,
    handleFormClose, handleDeleteClose,
    handleSubmit, confirmDelete,
    isSaving: createMutation.isPending || updateMutation.isPending,
    isDeleting: deleteMutation.isPending,
    saveError,
  }
}
```

- [ ] **Step 2: Update `AdminFormModal` to render `saveError`**

The error from a TanStack Query mutation is whatever the `mutationFn` (i.e. the api wrapper) threw. The admin api wrappers (in `frontend/src/api/admin.js`) throw `Error` instances; check that file to see how the message is shaped. If the message is a JSON string, parse it; otherwise display `error.message` directly. Add a small helper:

```javascript
function formatSaveError(error) {
  if (!error) return null
  // The admin api wrappers throw Error with the response body as the message.
  // Try to parse as JSON to extract { message } / { error }; fall back to raw.
  try {
    const parsed = JSON.parse(error.message)
    return parsed.message || parsed.error || error.message
  } catch {
    return error.message
  }
}
```

Then in `AdminFormModal`, accept `saveError` and pass it into `FormContent`:

```javascript
export function AdminFormModal({ open, onOpenChange, title, fields, initialValues, onSubmit, isPending, saveError }) {
  const formKey = open ? `${initialValues?.id ?? 'new'}-${open}` : 'closed'

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-white rounded-xl shadow-xl p-6 w-full max-w-lg max-h-[85vh] overflow-y-auto">
          <Dialog.Title className="text-lg font-semibold text-gray-900 mb-4">{title}</Dialog.Title>
          {open && (
            <FormContent
              key={formKey}
              fields={fields}
              initialValues={initialValues}
              onSubmit={onSubmit}
              isPending={isPending}
              saveError={saveError}
            />
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
```

In `FormContent`, render the formatted error above the action buttons:

```javascript
function FormContent({ fields, initialValues, onSubmit, isPending, saveError }) {
  const { t } = useTranslation('admin')
  const [values, setValues] = useState(() => buildDefaults(fields, initialValues))

  const handleChange = (name, value) => {
    setValues(prev => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    onSubmit(cleanFormValues(fields, values))
  }

  const errorMessage = formatSaveError(saveError)

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      {fields.map(f => (
        // ...existing field rendering unchanged...
      ))}
      {errorMessage && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {errorMessage}
        </div>
      )}
      <div className="flex justify-end gap-2 pt-2">
        {/* ...existing buttons unchanged... */}
      </div>
    </form>
  )
}
```

- [ ] **Step 3: Pass `saveError` through `AdminCrudPage`**

```javascript
export function AdminCrudPage({
  title, entityName, columns, fields, crud,
  dependentCounts, children,
}) {
  const { t } = useTranslation('admin')
  const {
    data, isLoading,
    formOpen, editItem, deleteItem,
    openCreate, openEdit, openDelete,
    handleFormClose, handleDeleteClose,
    handleSubmit, confirmDelete,
    isSaving, isDeleting,
    saveError,
  } = crud

  if (isLoading) return <div className="text-gray-400 py-8 text-center">{t('common:loading')}</div>

  return (
    <div>
      {/* ...existing header unchanged... */}

      <AdminTable columns={columns} data={data} onEdit={openEdit} onDelete={openDelete} entityName={title.toLowerCase()} />

      <AdminFormModal
        open={formOpen}
        onOpenChange={handleFormClose}
        title={editItem ? t('editEntity', { entity: entityName }) : t('createEntityHeading', { entity: entityName })}
        fields={fields}
        initialValues={editItem}
        onSubmit={handleSubmit}
        isPending={isSaving}
        saveError={saveError}
      />

      {/* ...delete dialog and children unchanged... */}
    </div>
  )
}
```

- [ ] **Step 4: Lint**

Run: `cd frontend && npm run lint`
Expected: clean.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/admin/hooks/useAdminCrud.js \
        frontend/src/pages/admin/components/AdminFormModal.jsx \
        frontend/src/pages/admin/components/AdminCrudPage.jsx
git commit -m "feat(admin): surface save errors in admin form modal"
```

---

## Task 4: Frontend — role column, role select, and lag note in `AdminUsersTable`

**Goal:** Admins can see who is an admin and edit any user's role. When changing role on edit, an inline note explains the up-to-15-minute propagation lag.

**Files:**
- Modify: `frontend/src/pages/admin/components/AdminFormModal.jsx` — support optional `dynamicHint` per field
- Modify: `frontend/src/pages/admin/AdminUsersTable.jsx`
- Modify: `frontend/src/locales/en/admin.json`

**Acceptance Criteria:**
- [ ] Users table shows a "Role" column
- [ ] Admin rows render the role text inside a lavender pill (`bg-primary-100 text-primary-800`); user rows render plain text
- [ ] Create/edit form has a required "Role" select with options "User" and "Admin"; default is User on create
- [ ] On edit, when the user changes the role select away from its initial value, a single-line note appears under the select: *"Existing sessions may keep the old role for up to 15 minutes until the access token refreshes."*
- [ ] All new strings are looked up via `t(...)` from the `admin` namespace
- [ ] Manual: demoting the last admin in dev shows the 409 message in the form
- [ ] Manual: promoting a user to admin and the user reloading the page shows them as admin in the navbar within 15 minutes

**Verify:**
- `cd frontend && npm run lint` → clean
- Manual visual check: start dev stack (`./start.sh`), log in as `admin@echel.dev`, open Admin → Users, confirm column + badge + form work end-to-end (covers both server guards because Task 2 already hardened them)

**Steps:**

- [ ] **Step 1: Verify the lavender token exists**

Tailwind config may or may not define `primary` colors. Run:

```bash
grep -E "primary|lavender" frontend/tailwind.config.js
```

If `bg-primary-100` and `text-primary-800` are not defined, substitute the closest equivalent from the existing palette (e.g. `bg-purple-100 text-purple-800`). Pick once and use the same pair throughout this task.

- [ ] **Step 2: Add `dynamicHint` support to `AdminFormModal`**

In `FormContent`, after each field's input is rendered, render an optional hint computed from `(values[f.name], initialValues?.[f.name])`. Modify the field map block:

```javascript
{fields.map(f => (
  <div key={f.name}>
    <label className="block text-xs font-medium text-gray-600 mb-1">
      {f.label}{f.required && <span className="text-red-500 ml-0.5">*</span>}
    </label>
    {/* ...existing select / textarea / checkbox / input rendering unchanged... */}
    {f.dynamicHint && (() => {
      const hint = f.dynamicHint(values[f.name], initialValues?.[f.name])
      return hint ? <p className="mt-1 text-xs text-gray-500">{hint}</p> : null
    })()}
  </div>
))}
```

- [ ] **Step 3: Add i18n keys to `frontend/src/locales/en/admin.json`**

Insert these keys (alphabetic placement is not required — match the existing file's loose grouping near the user-related keys):

```json
"role": "Role",
"roleUser": "User",
"roleAdmin": "Admin",
"roleChangeLagNote": "Existing sessions may keep the old role for up to 15 minutes until the access token refreshes.",
```

- [ ] **Step 4: Update `AdminUsersTable.jsx` to add the column, the field, and the lag hint**

```javascript
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getUsers, createUser, updateUser, deleteUser, getUserDependents } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

function RoleBadge({ value, t }) {
  if (value === 'ADMIN') {
    return (
      <span className="inline-flex items-center rounded-full bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-800">
        {t('roleAdmin')}
      </span>
    )
  }
  return <span className="text-gray-700">{t('roleUser')}</span>
}

export default function AdminUsersTable() {
  const { t } = useTranslation('admin')
  const [dependents, setDependents] = useState(null)

  const columns = [
    { key: 'email', label: t('email') },
    { key: 'displayName', label: t('displayName') },
    { key: 'role', label: t('role'), render: (value) => <RoleBadge value={value} t={t} /> },
    { key: 'timezone', label: t('timezone') },
    { key: 'createdAt', label: t('created') },
  ]

  const formFields = [
    { name: 'email', label: t('email'), type: 'email', required: true },
    { name: 'password', label: t('password'), type: 'password', required: false, placeholder: t('passwordPlaceholder') },
    { name: 'displayName', label: t('displayName'), required: true },
    { name: 'timezone', label: t('timezone'), defaultValue: 'UTC' },
    {
      name: 'role',
      label: t('role'),
      type: 'select',
      required: true,
      defaultValue: 'USER',
      options: [
        { value: 'USER', label: t('roleUser') },
        { value: 'ADMIN', label: t('roleAdmin') },
      ],
      dynamicHint: (current, initial) => (initial && current && current !== initial ? t('roleChangeLagNote') : null),
    },
  ]

  const crud = useAdminCrud({
    queryKey: ['admin', 'users'],
    listFn: getUsers, createFn: createUser, updateFn: updateUser, deleteFn: deleteUser,
  })

  const handleDelete = async (row) => {
    const counts = await getUserDependents(row.id)
    setDependents(counts)
    crud.setDeleteItem(row)
  }

  // Password is required only when creating a new user
  const activeFormFields = crud.editItem
    ? formFields
    : formFields.map(f => f.name === 'password' ? { ...f, required: true } : f)

  return (
    <AdminCrudPage
      title={t('users')}
      entityName={t('user')}
      columns={columns}
      fields={activeFormFields}
      crud={{ ...crud, openDelete: handleDelete }}
      dependentCounts={dependents}
    />
  )
}
```

Note: the `dynamicHint` function only fires the lag note when the user is editing (so `initial` is set) and has changed the value.

- [ ] **Step 5: Lint**

Run: `cd frontend && npm run lint`
Expected: clean. Ignore any false "unused" warnings about `RoleBadge` (per CLAUDE.md "Known Issues / Quirks").

- [ ] **Step 6: Manual verification (visual + behavior)**

Start the dev stack (`./start.sh`) and log in as the seeded admin (`admin@echel.dev` / `adminadmin`).

Verify each of:
1. Admin → Users shows a "Role" column. The seeded admin row has a purple "Admin" pill; other users show "User" in plain text.
2. Click "Edit" on the seeded admin user (this is yourself). Change the role select from Admin to User and click Save. The form shows the red "You cannot change your own role." banner; nothing is persisted.
3. Click "+ Create User" and create a new user with role = Admin. Confirm the new row appears with the Admin pill.
4. Edit the original admin again (this still tests the second guard, but now with two admins it should succeed if you actually wanted to demote — don't actually demote yourself; instead verify the lag-note text appears under the role select when you change it, then cancel without saving).
5. Edit the new admin and demote to User; confirm it persists. Then try to demote yourself (the now-only admin) to User — the "Cannot demote the last admin." banner should appear.
6. Re-promote the new user to Admin so the dev environment is back to two admins for future runs.

If any step fails, do not commit until fixed.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/admin/components/AdminFormModal.jsx \
        frontend/src/pages/admin/AdminUsersTable.jsx \
        frontend/src/locales/en/admin.json
git commit -m "feat(admin): add role column, select, and lag note to users table"
```

---

## Done

After Task 4 commits, the feature is complete. The user controls merge cadence (per CLAUDE.md): do not merge to `dev` or push without explicit instruction. Hand back with a short summary and wait.
