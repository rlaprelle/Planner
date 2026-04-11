# User Settings Page — Design Spec

**Date:** 2026-04-11
**Status:** Draft

## Overview

Add a user settings/preferences page that lets users configure their preferred name, schedule defaults, and ritual preferences. This is the first iteration — additional preference fields (timer, locale, etc.) will be added as their features ship.

## Data Model

All preferences are stored as columns on the existing `app_user` table. Each column has a sensible default so existing users are unaffected.

### Active columns (exposed in v1 settings UI)

| Column | Type | Default | Constraint | Notes |
|--------|------|---------|------------|-------|
| `default_start_time` | TIME | `08:00` | 15-min aligned | Day boundary for time blocks |
| `default_end_time` | TIME | `17:00` | 15-min aligned, must be after start | Day boundary for time blocks |
| `default_session_minutes` | SMALLINT | `60` | Multiple of 15, range 15-240 | Default time block duration |
| `week_start_day` | SMALLINT | `1` | CHECK 1-7 (ISO day-of-week) | 1=Monday, 7=Sunday |
| `ceremony_day` | SMALLINT | `5` | CHECK 1-7 (ISO day-of-week) | Day for end-of-week ritual. 5=Friday |

Existing columns surfaced in the settings UI (no migration needed):
- `display_name` (VARCHAR, already exists) — labeled "Preferred name" in the UI
- `timezone` (VARCHAR, already exists) — IANA timezone string

### Dormant columns (added in migration, no UI yet)

These columns support deferred features. Adding them now avoids future migrations for known requirements.

| Column | Type | Default | Notes |
|--------|------|---------|-------|
| `timer_warning_minutes` | SMALLINT | `5` | Minutes before block end to show warning |
| `timer_type` | VARCHAR(10) | `'countdown'` | `countdown` or `countup` |
| `enable_chime` | BOOLEAN | `true` | Play chime at end of time block |
| `quick_capture_keep_open` | BOOLEAN | `false` | Keep capture modal open after save |
| `max_daily_tasks` | SMALLINT | `NULL` | Soft cap on daily scheduled tasks (NULL = no limit) |
| `streak_celebration_threshold` | SMALLINT | `NULL` | Min ratio for streak celebration (NULL = system default) |
| `locale` | VARCHAR(10) | `'en'` | UI language |

### Flyway migration

Single migration file (next version number) that:
1. Adds all 12 columns with defaults via `ALTER TABLE app_user ADD COLUMN ... DEFAULT ...`
2. Adds CHECK constraints for `week_start_day` and `ceremony_day` (1-7 range)
3. Adds CHECK constraint for `default_session_minutes` (value % 15 = 0)
4. Adds CHECK constraint for `timer_type` (value IN ('countdown', 'countup'))

All columns are nullable with defaults, so existing rows are backfilled automatically by PostgreSQL.

## API Design

### Endpoint: `GET /api/v1/user/preferences`

Returns all active preference fields for the authenticated user.

**Response (200):**
```json
{
  "displayName": "Rob",
  "timezone": "America/Los_Angeles",
  "defaultStartTime": "08:00",
  "defaultEndTime": "17:00",
  "defaultSessionMinutes": 60,
  "weekStartDay": "MONDAY",
  "ceremonyDay": "FRIDAY"
}
```

- `weekStartDay` and `ceremonyDay` are serialized as Java `DayOfWeek` enum names (MONDAY–SUNDAY), not ISO integers.
- `defaultStartTime` and `defaultEndTime` are serialized as `HH:mm` strings.
- Dormant columns are **not** included in the response until their features ship.

### Endpoint: `PATCH /api/v1/user/preferences`

Partial update — send only fields that changed. Returns the full updated preferences object.

**Request (example — change one field):**
```json
{
  "ceremonyDay": "FRIDAY"
}
```

**Response (200):** Same shape as GET response, with updated values.

**Validation rules:**
- `displayName`: required if present, non-blank, max 100 chars
- `timezone`: must be a valid IANA timezone (`ZoneId.of()` must not throw)
- `defaultStartTime`: must be before `defaultEndTime`, 15-minute aligned
- `defaultEndTime`: must be after `defaultStartTime`, 15-minute aligned
- `defaultSessionMinutes`: multiple of 15, range 15-240
- `weekStartDay`: valid DayOfWeek enum value
- `ceremonyDay`: valid DayOfWeek enum value

**Error response (400):**
```json
{
  "error": "Validation failed",
  "fields": {
    "defaultSessionMinutes": "Must be a multiple of 15"
  }
}
```

### Implementation

- New `UserPreferencesController` in `backend/src/main/java/com/echel/planner/backend/auth/` (co-located with user entity since it modifies `AppUser`)
- `UserPreferencesService` handles validation and persistence
- `PreferencesResponse` record DTO for the response
- `UpdatePreferencesRequest` record DTO for the PATCH request — all fields optional (nullable)
- Uses `@AuthenticationPrincipal` to get the current user, consistent with other controllers

## Frontend

### Routing

New `/settings` route added to the protected routes inside `AppLayout` in `App.jsx`. Link added to the sidebar navigation.

### Page: `SettingsPage.jsx`

Located at `frontend/src/pages/SettingsPage.jsx`. Single page with three sections:

**Profile section:**
- Preferred name — text input

**Schedule section:**
- Timezone — searchable dropdown (Radix Combobox or filtered list, since there are ~400 IANA timezones)
- Default start time — dropdown with 15-minute intervals (00:00 through 23:45)
- Default end time — dropdown with 15-minute intervals, filtered to be after start time
- Default session duration — dropdown (15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180, 195, 210, 225, 240 minutes)

**Rituals section:**
- Week start day — dropdown (Monday, Sunday). DB supports any day (1-7) but UI restricts to the two common conventions for now.
- End-of-week ceremony day — dropdown (Monday through Sunday)

### Form behavior

- Load current values from `GET /api/v1/user/preferences` on mount
- Single "Save" button at the bottom of the form
- Button disabled while saving; success toast on completion, error toast on failure
- No auto-save — explicit save matches the app's calm, deliberate design
- Validation errors displayed inline next to the relevant field

### API layer

New file: `frontend/src/api/preferences.js`
- `usePreferences()` — TanStack Query hook wrapping `GET /api/v1/user/preferences`
- `useUpdatePreferences()` — TanStack mutation wrapping `PATCH /api/v1/user/preferences`, invalidates the preferences query on success

Follows existing patterns in `frontend/src/api/` (authFetch wrappers, query key conventions).

## Integration Points

### Schedule service defaults

`ScheduleService.savePlan()` currently hardcodes `startHour=8` and `endHour=17` as fallback defaults. Change the fallback to read from the authenticated user's `defaultStartTime` and `defaultEndTime` preferences. The method already accepts optional start/end in `SavePlanRequest`, so explicit values in the request continue to override.

### Week start day in TaskSchedulePhase

`TaskSchedulePhase` in "week" mode hardcodes Monday as the first day of the week for calendar rendering. Change to read the user's `weekStartDay` preference (fetched via the `usePreferences()` hook).

### Deferral calculations

When deferring a task to "next week", the `visible_from` date calculation must respect the user's configured `weekStartDay`. Currently assumes Monday. This affects both the `TaskSchedulePhase` frontend component and any backend deferral logic.

### Default session duration

When creating new time blocks in the schedule planner, the default block duration should use the user's `defaultSessionMinutes` preference instead of requiring manual sizing.

### Ceremony day (deferred integration)

The `ceremonyDay` column is stored and editable but has no behavioral integration in this iteration. Future work: show a sidebar nudge or dashboard reminder on the configured day. This is a fast-follow, not in scope for the initial implementation.

### How preferences reach the frontend

The `usePreferences()` TanStack Query hook is called on the settings page and by any component that needs preference values. TanStack Query's caching ensures no redundant requests — components share the cached result via the same query key. Preferences are not embedded in the JWT or auth response.

## Out of Scope

- Email and password change (deferred — see DEFERRED_WORK.md)
- Language/i18n (deferred — column added as dormant `locale`)
- Timer customization UI (deferred — columns added as dormant)
- Quick capture behavior toggle UI (deferred — column added as dormant)
- Daily task limit UI (deferred — column added as dormant)
- Streak threshold UI (deferred — column added as dormant)
- Ceremony day behavioral nudge (fast-follow)
