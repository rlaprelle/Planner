# Brain-Dump Mode for Quick Capture

**Date:** 2026-04-12
**Status:** Draft
**Scope:** Frontend only (no backend changes)

## Summary

After saving a quick capture, keep the modal open and ready for another entry instead of auto-dismissing. This serves ADHD users who have bursts of ideas to offload at once, reducing the friction of repeatedly opening the modal.

## Phases

- **Phase 1 — Core behavior:** Two submit paths (capture-and-close vs. capture-and-continue), new button and hotkey, inline success flash.
- **Phase 2 — Fly-away animation:** Animated note card flies from the modal toward the inbox icon on each brain-dump capture.

---

## Phase 1: Core Behavior

### Two Submit Paths

| Action | Trigger | Behavior |
|--------|---------|----------|
| **Capture & close** | Enter or "Capture" button | Unchanged — chime, "Captured." confirmation overlay, auto-close after 1 second |
| **Capture & continue** | Ctrl+Enter or "Keep capturing" button | Chime, brief inline "Captured." flash (500ms), clear textarea, refocus — modal stays open |

The existing single-capture path is completely unchanged.

### Footer Layout

Stacked two-row layout pairing each hotkey hint with its button:

```
[ Cancel ]     Enter to capture and close              [ Capture        ]
               Ctrl+Enter to keep capturing            [ Keep capturing ]
```

- **"Capture"** — primary/filled style, rightmost on row 1 (dominant action, existing muscle memory position).
- **"Keep capturing"** — secondary/outline style, rightmost on row 2, aligned under "Capture."
- **Hint text** — muted, small, one hint per row, left of its corresponding button.
- **"Cancel"** — stays left-aligned on row 1.

### Success Feedback (Brain-Dump Path)

When the user triggers "capture and continue":

1. Chime plays (same audio as today).
2. Brief "Captured." flash replaces the textarea area for 500ms, then fades.
3. Textarea clears and refocuses automatically.
4. No confirmation overlay, no 1-second wait — user is back to typing immediately.

### Hotkey Behavior

- **Enter** (no modifier): submits and closes — unchanged from current behavior.
- **Ctrl+Enter**: submits and stays open (brain-dump mode).
- **Shift+Enter**: inserts a newline — unchanged.
- **Escape / backdrop click / Cancel**: closes and resets — unchanged.

### i18n

New keys in the `deferred` namespace (`frontend/src/locales/en/deferred.json`):

| Key | Value |
|-----|-------|
| `keepCapturing` | `"Keep capturing"` |
| `enterToCapture` | `"Enter to capture and close"` |
| `ctrlEnterToKeepCapturing` | `"Ctrl+Enter to keep capturing"` |

All existing keys unchanged.

---

## Phase 2: Fly-Away Animation

Layered on after Phase 1 is complete and verified.

### Animation Sequence

On each brain-dump capture (not on regular capture-and-close):

1. Spawn a small blank "note card" element (~120x40px, rounded rectangle, muted styling).
2. Render via a React portal, absolutely positioned at the modal's textarea location.
3. Animate: translate toward the inbox icon in the sidebar, scale down to ~0.3, fade opacity to 0. Duration ~400-500ms, ease-out curve.
4. On animation complete, remove the portal element.

### Implementation Approach

- Target position obtained via a ref on the inbox icon in AppLayout + `getBoundingClientRect()`, passed to QuickCapture through context or a callback.
- Pure CSS `@keyframes` animation — no new dependencies.
- The card is blank (no captured text displayed on it).

---

## Testing

E2E tests in `e2e/`, mocking `/api/v1/deferred` via `page.route()`:

- **Ctrl+Enter** submits and keeps modal open with textarea cleared and focused.
- **"Keep capturing" button** submits and keeps modal open.
- **Enter** still submits and closes (regression guard).
- **"Capture" button** still submits and closes (regression guard).
- **Multiple rapid captures** in sequence work correctly (brain-dump flow).

No backend tests — this feature is entirely frontend.

---

## Files Affected

### Phase 1
- `frontend/src/components/QuickCapture.jsx` — new submit path, footer layout, hotkey handler
- `frontend/src/locales/en/deferred.json` — new i18n keys
- `e2e/tests/` — new test file for brain-dump mode

### Phase 2
- `frontend/src/components/QuickCapture.jsx` — animation trigger
- `frontend/src/components/FlyAwayCard.jsx` (new) — portal + animation component
- `frontend/src/layouts/AppLayout.jsx` — ref on inbox icon, passed to QuickCapture
