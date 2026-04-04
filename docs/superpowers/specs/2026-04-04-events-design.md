# Events Feature Design

**Date:** 2026-04-04
**Status:** Approved

## Overview

Add "Events" as an alternative to Tasks. Events represent time-bound commitments (meetings, appointments, etc.) that appear on the daily schedule as immovable blocks. They can be created directly or converted from deferred notes.

## Key Design Decisions

- **Separate entity** — Events are not a Task subtype. They have a distinct data model, lifecycle, and schedule behavior.
- **Required project** — Events must belong to a project, same as Tasks. Future "default" projects (e.g., "Personal") will handle cases like dentist appointments.
- **No recurrence yet** — Every Event is one-off with a specific date. The schema shape supports recurrence later without migration.
- **No session tracking** — Events have no start/complete/done-for-now flow. They exist as fixed blocks on the schedule.
- **Materialized at plan time** — Creating an Event does not create a TimeBlock. TimeBlocks for Events are created when the user plans their day, fitting the existing delete-and-recreate savePlan pattern.

## Data Model

### `event` table (new)

| Column        | Type         | Constraints                          |
|---------------|--------------|--------------------------------------|
| `id`          | UUID         | PK, default gen_random_uuid()        |
| `user_id`     | UUID         | FK → app_user, NOT NULL              |
| `project_id`  | UUID         | FK → project, NOT NULL               |
| `title`       | VARCHAR(255) | NOT NULL                             |
| `description` | TEXT         | Nullable                             |
| `energy_level`| VARCHAR(20)  | Nullable, enum: LOW / MEDIUM / HIGH  |
| `block_date`  | DATE         | NOT NULL (nullable when recurrence added) |
| `start_time`  | TIME         | NOT NULL, template/default start     |
| `end_time`    | TIME         | NOT NULL, template/default end       |
| `archived_at` | TIMESTAMPTZ  | Nullable, soft-delete                |
| `created_at`  | TIMESTAMPTZ  | NOT NULL, default now()              |
| `updated_at`  | TIMESTAMPTZ  | NOT NULL, auto-updated via trigger   |

Indexes:
- `(user_id, block_date)` — efficient daily queries
- `(project_id)` — project-level listing

### `time_block` table changes

- Add nullable `event_id` (FK → event)
- Add check constraint: exactly one of `task_id` or `event_id` must be non-null
- `task_id` becomes explicitly nullable if not already

This uses two separate FK columns (not a polymorphic ID + type enum) because:
- Database-enforced referential integrity — each FK points to its specific table
- Clean JPA mapping via standard `@ManyToOne` annotations
- Hibernate recommends this over `@Any` for polymorphic references

### `deferred_item` table changes

- Add nullable `resolved_event_id` (FK → event)

## API Endpoints

### Event CRUD

| Method  | Path                                  | Notes                                                    |
|---------|---------------------------------------|----------------------------------------------------------|
| `POST`  | `/api/v1/projects/{projectId}/events` | Create event (title, description, energyLevel, blockDate, startTime, endTime) |
| `GET`   | `/api/v1/projects/{projectId}/events` | List events for a project                                |
| `GET`   | `/api/v1/events/{id}`                 | Get single event                                         |
| `PUT`   | `/api/v1/events/{id}`                 | Update event                                             |
| `PATCH` | `/api/v1/events/{id}/archive`         | Soft-delete                                              |

### Deferred item conversion

- `POST /api/v1/deferred/{id}/convert-to-event` — new endpoint alongside existing `convert`. Accepts: projectId, title, blockDate, startTime, endTime, optional description, optional energyLevel. Creates the Event, marks the deferred item processed, sets `resolved_event_id`.

### Schedule changes

- `GET /api/v1/schedule/today` — response TimeBlocks now include an `event` field (null for task blocks, populated for event blocks). The response DTO gains an `EventSummary` nested object (id, title, projectId, projectName, projectColor, energyLevel).
- `POST /api/v1/schedule/today/plan` — BlockEntry remains task-only (no `eventId` field). The frontend sends only task blocks. The backend auto-queries Events for the target date, creates TimeBlocks for them, then validates no overlaps between the submitted task blocks and the auto-generated event blocks before persisting everything together.

### Validation rules

- Event times must be 15-minute aligned
- Must fall within the 8am-5pm window
- `end_time` must be after `start_time`
- No overlaps with other blocks (task or event) — enforced at plan-save time

## Schedule Behavior

### Events as hard boundaries

- Event TimeBlocks are **immovable** — they are never pushed by the `pushBlocks()` algorithm
- When a task block resize or drag would overlap an Event, the operation is **rejected** (returns null, same as exceeding 5pm)
- Task blocks can be pushed up to an Event's edge but not through it
- After an Event, task blocks can fill the remaining gap before the next block

### Drag-and-drop rules

- Event blocks have **no drag handle** — they cannot be moved on the grid
- Event blocks have **no resize handle** — their times are fixed
- Tasks cannot be dropped onto a time range that overlaps an Event
- The ghost preview during task drag accounts for Event positions

### Visual differentiation

- Event blocks are visually distinct from task blocks (different background treatment, possibly an icon) so the user can immediately see what's fixed vs movable
- They still show the project color for consistency
- The visual treatment signals "this is locked/immovable"

### Day planning flow

1. User opens "plan my day" for a given date
2. System queries Events where `block_date` = target date and `archived_at` is null
3. Events are auto-populated on the grid as immovable blocks
4. User arranges task blocks around the Events
5. On save: service creates TimeBlocks for both events and tasks, validates no overlaps, persists

## Deferred Item Conversion UX

### Current flow
Each deferred item has a "Convert" button → opens task creation form.

### New flow
Each deferred item gets two buttons:
- **"Convert to Task"** — existing behavior, unchanged
- **"Convert to Event"** — opens a form with: project selector, title (pre-filled from `rawText`), optional description, block date, start time, end time, optional energy level

On conversion:
1. Create the Event entity
2. Mark the deferred item as processed (`is_processed = true`, `processed_at = now()`)
3. Set `resolved_event_id` to the new Event's ID

## Admin Panel

Add an Event admin CRUD page following the existing `AdminCrudPage` / `useAdminCrud` pattern:
- Table view with columns: title, project, date, start time, end time, energy level, archived status
- Create/edit form with all Event fields
- Archive/unarchive action
- Follows the same layout and conventions as the existing admin pages

## Future Considerations

### Recurrence (not in scope)
The schema supports recurrence by:
- Adding a `recurrence_rule` column to `event` (e.g., iCal RRULE string)
- Making `block_date` nullable (recurring events don't have a single date)
- `start_time` / `end_time` become template times for generated occurrences
- At plan time, the service evaluates recurrence rules to find matching Events and materializes TimeBlocks with the template times
- Per-occurrence overrides are represented by the TimeBlock's own `start_time` / `end_time` differing from the template
