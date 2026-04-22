# Admin Role Management

**Status:** Approved
**Date:** 2026-04-21
**Scope:** Backend admin user CRUD + Admin Users table UI

## Problem

The `AppUser.Role` enum (`USER`, `ADMIN`) and the `app_user.role` column already exist, and `SecurityConfig` enforces `ROLE_ADMIN` on `/api/v1/admin/**`. However, role is not exposed through the admin user CRUD: `AdminUserRequest` and `AdminUserResponse` omit it, `AdminUserService` neither reads nor writes it, and `AdminUsersTable.jsx` has no UI for it. The only way to grant admin today is `DevAdminSeeder` (dev profile only) or a direct database update. Admins need a way to grant and revoke admin privilege through the admin panel.

## Goals

- Admins can promote a user to `ADMIN` and demote an admin to `USER` from the existing user edit form.
- Admins can see at a glance which users are admins.
- The system cannot be locked out of admin access via the UI.

## Non-Goals

- No new roles beyond `USER` and `ADMIN`.
- No per-row "Promote" / "Demote" quick actions — edit form only.
- No audit trail of role changes.
- No JWT invalidation on role change. The existing 15-minute access-token TTL governs propagation; the UI surfaces this lag.

## Design

### Backend

**`AdminUserRequest`** gains a required `role`:

```java
public record AdminUserRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @Size(min = 6) String password,
    @NotBlank @Size(max = 255) String displayName,
    @Size(max = 100) String timezone,
    @NotNull AppUser.Role role
) {}
```

**`AdminUserResponse`** gains a `role` field, populated from `user.getRole()` in `AdminUserResponse.from`.

**`AdminUserService.create`** sets the new user's role from the request. Admins may create another admin directly.

**`AdminUserService.update`** updates `role` when the request value differs from the persisted value. Two guards run before the change is applied:

1. **Self-edit guard.** If the target `id` matches the currently authenticated admin's id, throw `StateConflictException("You cannot change your own role.")`. The current admin is read from `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`, which is an `AppUser` because `SecurityConfig.userDetailsService` returns `AppUser` (which implements `UserDetails`).
2. **Last-admin guard.** If the persisted role is `ADMIN` and the new role is `USER`, count admins via a new `AppUserRepository.countByRole(AppUser.Role role)` query. If the count is `<= 1`, throw `StateConflictException("Cannot demote the last admin.")`.

Both guards only run when role is actually changing — a no-op update with role unchanged passes through. Both throw `StateConflictException` (already mapped to HTTP 409 by the global exception handler in `common/`).

**`AppUserRepository`** gains:

```java
long countByRole(AppUser.Role role);
```

**`AdminUserController`** is unchanged — only the DTO shape changes.

### Frontend

**`AdminUsersTable.jsx`**:

- Add a `role` column. Render admins with a colored "Admin" badge (lavender pill, matching the project palette) and users with plain text. The exact rendering API depends on what `AdminTable.jsx` supports today; if it has no per-cell render hook, add a minimal one rather than special-casing in the table component.
- Add a `role` form field as a select with options `User` and `Admin`. Default `User` on create. Required on both create and update.
- When the edit form is open and the user changes the role away from its original value, render an inline note beneath the field:

  > *"Existing sessions may keep the old role for up to 15 minutes until the access token refreshes."*

  Plain text, calm tone — informative, not a warning modal. No interaction required.

**Server-side error display.** The two new `StateConflictException` errors should surface in the form the same way other admin CRUD errors already do (the existing `useAdminCrud` hook handles error mapping; this design adopts that pattern as-is).

**i18n.** New keys in the `admin` namespace:

- `role` — column header and field label
- `roleUser`, `roleAdmin` — option labels and badge text
- `roleChangeLagNote` — the inline note text
- `errorCannotChangeOwnRole`, `errorCannotDemoteLastAdmin` — surfaced for the 409 responses

### Testing

Backend integration tests, added to whichever test class covers `AdminUserService` today (or a new one matching that pattern):

1. Update `USER` → `ADMIN` succeeds; the response reflects the new role; a subsequent request from that user against `/api/v1/admin/**` is authorized once they obtain a fresh token.
2. An admin attempting to change their own role gets HTTP 409.
3. With one admin in the system, demoting that admin returns HTTP 409 ("last admin").
4. With two admins, demoting one of them succeeds.
5. Create with `role = ADMIN` produces an admin user.

No frontend tests. There is no Storybook story for `AdminUsersTable` today, and adding one is out of scope.

## Trade-offs and Alternatives Considered

- **No JWT invalidation on role change.** A demoted admin retains admin privileges for up to 15 minutes (until access-token expiry). Accepted because role changes are rare in this internal-feeling tool, and per-request token-version checks would add stateful overhead to every authenticated request. Revisit if the threat model changes.
- **Form select vs. row-level promote/demote action.** Form-only chosen for consistency with every other admin entity edit. The two server-side guards (last-admin, self-edit) carry the safety load that a confirm dialog would otherwise carry.
- **Both guards rather than one or neither.** "Last admin" prevents catastrophic lockout (no UI recovery in production). "Self-edit" prevents the most common accident. Combined cost is two short checks. The consequence — only a *different* admin can demote you — is a feature: in a single-admin deployment, you must create a second admin first. Documented here so it's not a surprise.
