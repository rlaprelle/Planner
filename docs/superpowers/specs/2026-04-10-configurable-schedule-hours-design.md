# Configurable Schedule Hour Range

**Date:** 2026-04-10
**Status:** Approved

## Problem

The Start Day schedule grid is hardcoded to 8 AM–5 PM. Users who work outside
those hours (early risers, evening workers) cannot plan their full day.

## Solution

Add two dropdown selectors above the schedule grid that control the visible hour
range. The grid, all DnD logic, and backend validation adapt to the chosen range.

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Interaction model | Dropdown selectors | Clear, explicit, no accidental changes |
| Placement | Above grid, right-aligned | Separated from grid labels; unobtrusive "toolbar" position |
| Hour granularity | Whole hours only | Keeps grid lines clean and simple |
| Available range | 12 AM (0) – 12 AM (24) | No artificial restrictions; users set any range they want |
| Default | 8 AM – 5 PM | Matches current behavior; no regression |
| Persistence | Reset daily | Future user-settings feature can add persistence later |
| Blocks outside range | Prevent the change | Show warning; no data loss or surprise auto-adjustments |

## Frontend Changes

### 1. StartDayPage.jsx — State & UI

- Add `dayStartHour` / `dayEndHour` state, defaulting to `8` / `17`.
- Render two `<select>` elements above `<TimeBlockGrid>`, right-aligned, with a
  label like "Hours:" and an en-dash separator.
- Start dropdown: hours 0–23, constrained so `startHour < endHour`.
- End dropdown: hours 1–24, constrained so `endHour > startHour`.
- On change: check whether any existing blocks fall outside the proposed new
  range. If so, show a brief inline warning (e.g., "You have blocks before
  10 AM") and reject the change. The user must remove those blocks first.
- Derive `dayStartMinutes = dayStartHour * 60` and
  `dayEndMinutes = dayEndHour * 60` and pass them to `useTimeGrid`,
  `TimeBlockGrid`, and DnD handlers.

### 2. useTimeGrid.js — Parameterize Constants

- Change `useTimeGrid()` signature to accept `(dayStartMinutes, dayEndMinutes)`.
- Compute `DAY_DURATION` internally as `dayEndMinutes - dayStartMinutes`.
- All math functions (`minutesToPercent`, `durationToPercent`,
  `pixelDeltaToMinutes`, `clientXToMinutes`) already use `DAY_START_MINUTES` and
  `DAY_DURATION` — replace those references with the computed values.
- Continue exporting default constants (`DAY_START_MINUTES = 480`,
  `DAY_END_MINUTES = 1020`) for backward compatibility where needed, but the
  hook itself uses the parameters.

### 3. TimeBlockGrid.jsx — Dynamic Hour Marks

- Accept `dayStartMinutes` and `dayEndMinutes` as props.
- Compute `HOUR_MARKS` dynamically from the range instead of deriving from
  imported constants.
- Compute `TOTAL_HOURS` from the prop range.
- `hourToPercent` and `formatHour` use the dynamic range. All positioning is
  already percentage-based and adapts automatically.

### 4. pushBlocks.js — Parameterize Day Boundaries

- `pushBlocks()` already accepts `dayEndMinutes` — no signature change needed
  for the end boundary. Verify start-boundary clamping also respects the
  dynamic start.
- `cursorToSnappedStart()` in `StartDayPage` uses `DAY_START_MINUTES` and
  `DAY_END_MINUTES` for clamping — update to use the state-derived values.
- `snapTo15()` is range-independent; no change needed.
- `toGridBlock()` and `minutesToTime()` / `timeToMinutes()` are absolute-time
  utilities; no change needed.

### 5. DnD Handlers in StartDayPage.jsx

- `handleDragMove` ghost preview clamping uses `DAY_START_MINUTES` /
  `DAY_END_MINUTES` — update to use state-derived values.
- `handleDragEnd` block creation clamping — same update.
- Block move handler (in `TimeBlock`) receives clamping bounds via
  `useTimeGrid` which is already parameterized (step 2).
- Resize handler — same, via `useTimeGrid`.

## Backend Changes

### 6. ScheduleService.java — Accept Dynamic Range

- Add `startHour` (Integer, nullable) and `endHour` (Integer, nullable) to the
  save-plan request DTO (`SavePlanRequest`).
- Default to 8 / 17 when null (backward compatibility).
- Validation:
  - `startHour` must be 0–23, `endHour` must be 1–24.
  - `startHour < endHour`.
  - All block times must fall within `startHour:00` – `endHour:00`.
- Replace hardcoded `DAY_START` / `DAY_END` constants with the request values.

### 7. SavePlanRequest DTO

- Add optional `Integer startHour` and `Integer endHour` fields.

## What Doesn't Change

- **Database schema** — times stored as absolute `TIME` values; no migration.
- **Block percentage positioning** — already relative to grid bounds.
- **15-minute snap** — range-independent.
- **Event blocks** — use absolute times; grid displays them within whatever
  range is active. Events outside the range are a natural consequence of the
  range being too narrow — the "prevent change" validation handles this.
- **Active session page** — uses saved blocks with absolute times; unaffected.

## Edge Cases

- **Events outside default range:** If an event exists at 7 AM and the grid
  defaults to 8 AM, the event won't be visible until the user expands the
  range. The "prevent narrowing" validation treats event blocks the same as
  task blocks — you cannot narrow the range past any placed block, event or
  task. A future enhancement could auto-expand the default range when events
  exist outside 8–5.
- **Very wide ranges:** A 5 AM–11 PM grid (18 hours) makes each hour column
  quite narrow. This is acceptable — the user chose it. Grid labels may need
  to skip every other hour at very wide ranges, but this is a polish concern
  for later.
- **Minimum range:** Start must be strictly less than end, so the minimum is a
  1-hour grid. This is an edge case unlikely to cause issues.
