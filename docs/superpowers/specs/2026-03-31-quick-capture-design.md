# Slice 2: Quick Capture — Design Spec

**Date:** 2026-03-31
**Status:** Approved

---

## Overview

Quick Capture is a low-friction mechanism for the user to offload a thought instantly without leaving their current context. A thought is saved as a `DeferredItem` with raw text only — no project, no metadata, no decisions required at capture time. Items accumulate in the Inbox and are processed during the Evening Ritual (a later slice).

---

## Backend

### Database

**New Flyway migration:** `V4__create_deferred_item.sql`

Creates the `deferred_item` table:

| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK → app_user, not null |
| raw_text | TEXT | not null |
| is_processed | BOOLEAN | default false |
| captured_at | TIMESTAMPTZ | not null |
| processed_at | TIMESTAMPTZ | nullable |
| resolved_task_id | UUID | nullable, FK → task |
| resolved_project_id | UUID | nullable, FK → project |
| deferred_until_date | DATE | nullable |
| deferral_count | INTEGER | default 0 |
| created_at | TIMESTAMPTZ | not null |
| updated_at | TIMESTAMPTZ | not null |

### API Endpoints

**`POST /api/v1/deferred`**
- Auth: JWT required
- Request body: `{ "rawText": "string" }`
- Creates a `DeferredItem` with `isProcessed = false`, `capturedAt = now()`, `deferralCount = 0`
- Returns: 201 with `DeferredItemResponse`

**`GET /api/v1/deferred`**
- Auth: JWT required
- Returns all unprocessed items for the current user: `isProcessed = false` AND (`deferredUntilDate IS NULL` OR `deferredUntilDate <= today`), where "today" is evaluated in the user's timezone (consistent with the architecture's policy on date boundary queries)
- Returns: 200 with `List<DeferredItemResponse>`

> **Not in this slice:** `POST /{id}/convert`, `POST /{id}/defer`, `PATCH /{id}/dismiss` — these belong to the Evening Ritual slice.

### Package Structure

Mirrors existing `task/` and `project/` packages:

```
deferred/
  DeferredItem.java
  DeferredItemController.java
  DeferredItemService.java
  DeferredItemRepository.java
  DeferredItemExceptionHandler.java
  dto/
    DeferredItemCreateRequest.java
    DeferredItemResponse.java
```

---

## Frontend

### New Files

**`src/api/deferred.js`**
- `createDeferredItem(rawText)` — `POST /api/v1/deferred`
- `getDeferredItems()` — `GET /api/v1/deferred`
- Follows same `authFetch` / `handleResponse` pattern as `tasks.js`

**`src/components/QuickCapture.jsx`**
- Self-contained component rendered once in `AppLayout.jsx`
- Owns the sidebar button, modal, API call, confirmation, and hotkey

### `QuickCapture.jsx` — Three States

**Idle**
- Renders a `+ Quick capture` button at the bottom of the sidebar, above the logout button
- Registers `Ctrl+Space` hotkey via `useEffect` / `window` `keydown` listener; cleaned up on unmount
- A second `Ctrl+Space` press while open closes the modal

**Open**
- Centered modal overlay (`role="dialog"`, `aria-modal="true"`, `aria-label="Quick capture"`)
- Single autofocused text input, placeholder: `"What's on your mind?"`
- `Enter` submits, `Escape` cancels
- Save button/Enter disabled when input is blank or whitespace-only
- Focus is trapped inside the modal while open
- Uses TanStack Query `useMutation` calling `createDeferredItem`

**Confirmed**
- On successful save: input replaced by a green checkmark + `"Captured."` message
- Web Audio API chime plays simultaneously: ~200ms sine tone at ~880Hz with fast envelope fade
- After ~1 second, modal auto-dismisses
- TanStack Query cache for `getDeferredItems` is invalidated on success to refresh the badge

### Inbox Badge

- `AppLayout.jsx` adds a `useQuery` for `getDeferredItems()`
- `refetchOnWindowFocus: true` (TanStack Query default) keeps the count reasonably fresh
- Inbox nav item renders a small numeric badge when `count > 0`; no badge when count is 0

### `AppLayout.jsx` Changes

Two additions only — no structural layout changes:
1. Import and render `<QuickCapture />` in the sidebar below nav links, above logout
2. Add the `useQuery` for the inbox badge count

---

## Error Handling

| Scenario | Behaviour |
|---|---|
| Empty / whitespace input | Save disabled client-side; no backend call made |
| API failure on save | Modal stays open; input preserved; inline error: `"Couldn't save — try again."` |
| Ctrl+Space when modal already open | Closes the modal |
| Ctrl+Space browser conflict | `event.preventDefault()` called to suppress browser default |

---

## Out of Scope (This Slice)

- Processing / converting deferred items → Evening Ritual slice
- Brain-dump mode (keep modal open after save) → noted in `DEFERRED_WORK.md`
- Audio preference toggle → noted in `DEFERRED_WORK.md` (Customizable Timer & Reminders)
- Full `InboxPage.jsx` UI for browsing items → later slice

---

## Files Changed Summary

| File | Change |
|---|---|
| `backend/.../deferred/DeferredItem.java` | New |
| `backend/.../deferred/DeferredItemController.java` | New |
| `backend/.../deferred/DeferredItemService.java` | New |
| `backend/.../deferred/DeferredItemRepository.java` | New |
| `backend/.../deferred/DeferredItemExceptionHandler.java` | New |
| `backend/.../deferred/dto/DeferredItemCreateRequest.java` | New |
| `backend/.../deferred/dto/DeferredItemResponse.java` | New |
| `backend/src/main/resources/db/migration/V4__create_deferred_item.sql` | New |
| `frontend/src/api/deferred.js` | New |
| `frontend/src/components/QuickCapture.jsx` | New |
| `frontend/src/layouts/AppLayout.jsx` | Modified (add QuickCapture + badge query) |
