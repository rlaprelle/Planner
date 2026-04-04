# Admin Page Design

## Overview

A standalone admin interface at `/admin` providing table-based CRUD for all database entities. Designed for direct data management — inspecting, editing, and cleaning up records across all users. No access control; anyone with the URL can use it.

## Entities

Six tables, each with full Create / Read / Update / Delete:

1. **Users** (`app_user`) — email, displayName, timezone, passwordHash, createdAt, updatedAt
2. **Projects** (`project`) — name, description, color, icon, isActive, sortOrder, archivedAt, user (FK)
3. **Tasks** (`task`) — title, description, status, priority, pointsEstimate, actualMinutes, energyLevel, dueDate, sortOrder, archivedAt, completedAt, project (FK), user (FK), parentTask (FK), blockedByTask (FK)
4. **Deferred Items** (`deferred_item`) — rawText, isProcessed, capturedAt, processedAt, deferredUntilDate, deferralCount, user (FK), resolvedTask (FK), resolvedProject (FK)
5. **Daily Reflections** (`daily_reflection`) — reflectionDate, energyRating, moodRating, reflectionNotes, isFinalized, user (FK)
6. **Time Blocks** (`time_block`) — blockDate, startTime, endTime, sortOrder, actualStart, actualEnd, wasCompleted, user (FK), task (FK)

## Layout

- **Sidebar navigation** — persistent left sidebar listing all six entities
- **Main area** — data table for the selected entity with a "Create" button
- **Completely separate from the main app** — no `AppLayout`, no sidebar from the regular app. The `/admin` route renders its own layout component.

## CRUD Behavior

### List (Read)
- Table shows all records across all users
- Every table includes a "User" column (displays email or displayName) so you can tell whose data you're looking at
- UUID `id` column shown but truncated (first 8 chars) with copy-on-click
- Timestamp columns formatted as readable dates
- Foreign key columns display a meaningful label (e.g., project name, task title, user email) rather than raw UUIDs

### Create
- Modal form with fields matching the entity
- Foreign key fields use a dropdown/select populated from the related table
- Password field for user creation (hashed on backend before storage)
- Timestamps (createdAt, updatedAt) are auto-managed — not shown in create form

### Edit
- Same modal as create, pre-populated with current values
- Same field rules as create

### Delete
- **Cascade with confirmation** — clicking delete opens a confirmation dialog
- The dialog lists all dependent records that will be cascade-deleted (e.g., "This will also delete 3 projects, 12 tasks, 2 reflections...")
- Backend computes the dependency count and returns it before deletion
- Actual deletion cascades through foreign keys

## Backend

### New endpoints under `/api/v1/admin/`

Dedicated admin controllers — not reusing existing user-scoped controllers. This keeps admin logic cleanly separated and avoids accidentally weakening user-scoped security.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin/users` | List all users |
| GET | `/api/v1/admin/users/{id}` | Get single user |
| POST | `/api/v1/admin/users` | Create user |
| PUT | `/api/v1/admin/users/{id}` | Update user |
| GET | `/api/v1/admin/users/{id}/dependents` | Count dependent records (for delete confirmation) |
| DELETE | `/api/v1/admin/users/{id}` | Cascade delete user and all dependent records |
| GET | `/api/v1/admin/projects` | List all projects |
| GET | `/api/v1/admin/projects/{id}` | Get single project |
| POST | `/api/v1/admin/projects` | Create project |
| PUT | `/api/v1/admin/projects/{id}` | Update project |
| DELETE | `/api/v1/admin/projects/{id}` | Delete project (cascade tasks, deferred items) |
| GET | `/api/v1/admin/tasks` | List all tasks |
| GET | `/api/v1/admin/tasks/{id}` | Get single task |
| POST | `/api/v1/admin/tasks` | Create task |
| PUT | `/api/v1/admin/tasks/{id}` | Update task |
| DELETE | `/api/v1/admin/tasks/{id}` | Delete task (cascade time blocks, child tasks) |
| GET | `/api/v1/admin/deferred-items` | List all deferred items |
| GET | `/api/v1/admin/deferred-items/{id}` | Get single deferred item |
| POST | `/api/v1/admin/deferred-items` | Create deferred item |
| PUT | `/api/v1/admin/deferred-items/{id}` | Update deferred item |
| DELETE | `/api/v1/admin/deferred-items/{id}` | Delete deferred item |
| GET | `/api/v1/admin/reflections` | List all reflections |
| GET | `/api/v1/admin/reflections/{id}` | Get single reflection |
| POST | `/api/v1/admin/reflections` | Create reflection |
| PUT | `/api/v1/admin/reflections/{id}` | Update reflection |
| DELETE | `/api/v1/admin/reflections/{id}` | Delete reflection |
| GET | `/api/v1/admin/time-blocks` | List all time blocks |
| GET | `/api/v1/admin/time-blocks/{id}` | Get single time block |
| POST | `/api/v1/admin/time-blocks` | Create time block |
| PUT | `/api/v1/admin/time-blocks/{id}` | Update time block |
| DELETE | `/api/v1/admin/time-blocks/{id}` | Delete time block |

### Security

- All `/api/v1/admin/**` endpoints are excluded from JWT authentication in Spring Security config
- No auth required — matches the "no access control" decision

### Cascade delete logic

Delete endpoints handle cascades in this order:
- **User delete**: time_blocks → deferred_items → daily_reflections → tasks → projects → user
- **Project delete**: deferred_items (resolved_project) → tasks → project
- **Task delete**: time_blocks → deferred_items (resolved_task) → child tasks → task

The `/dependents` endpoint (users only, since users are the root entity with the most cascading) returns counts per entity type so the frontend can display them in the confirmation dialog.

## Frontend

### New files

```
frontend/src/
  pages/admin/
    AdminPage.jsx          — Layout with sidebar + router outlet
    AdminUsersTable.jsx    — Users table + create/edit modal
    AdminProjectsTable.jsx — Projects table + create/edit modal
    AdminTasksTable.jsx    — Tasks table + create/edit modal
    AdminDeferredTable.jsx — Deferred items table + create/edit modal
    AdminReflectionsTable.jsx — Reflections table + create/edit modal
    AdminTimeBlocksTable.jsx  — Time blocks table + create/edit modal
    components/
      AdminTable.jsx       — Reusable table component (columns, data, actions)
      AdminFormModal.jsx   — Reusable modal with dynamic form fields
      DeleteConfirmDialog.jsx — Cascade delete confirmation dialog
  api/
    admin.js               — API client functions for all admin endpoints
```

### Routing

Add to `App.jsx`:
```
/admin          → AdminPage (own layout, no AppLayout)
  /admin/users       → AdminUsersTable (default)
  /admin/projects    → AdminProjectsTable
  /admin/tasks       → AdminTasksTable
  /admin/deferred    → AdminDeferredTable
  /admin/reflections → AdminReflectionsTable
  /admin/time-blocks → AdminTimeBlocksTable
```

### Shared components

- **AdminTable** — accepts column definitions and data array, renders a styled table with Edit/Delete action buttons per row. Handles empty states.
- **AdminFormModal** — accepts field definitions (name, type, required, options for selects), renders a Radix UI dialog with a form. Used for both create and edit.
- **DeleteConfirmDialog** — shows entity name and cascade counts, with a confirm button to proceed.

### Styling

- Uses Tailwind CSS consistent with the rest of the app
- Admin-specific layout: no soft lavender journal feel — this is a utility interface. Clean, dense, functional.
- Sidebar: dark background, white text, highlighted active entity
- Tables: compact rows, alternating row colors, clear column headers

## Testing

- **Backend**: Integration tests for admin controllers (same pattern as existing tests — hit real database)
- **E2E**: New Playwright test file for admin page with mocked `/api/v1/admin/*` routes
- **No frontend unit tests** (consistent with current project — no frontend unit tests yet)

## Out of scope

- Pagination / search / filtering (can be added later if tables get large)
- Audit logging of admin actions
- Bulk operations (multi-select delete, bulk edit)
- Export/import
