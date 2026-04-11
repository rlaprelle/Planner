# Echel Planner Branding

## Overview

Rebrand the app from the generic "Planner" to "Echel Planner" by Echel Software. This is a light refresh: new name, logo, tagline, and favicon. The existing lavender color palette and UI design are unchanged.

## Brand Identity

- **Product name:** Echel Planner
- **Company name:** Echel Software
- **Tagline:** "Planning that works with your brain, not against it."
- **Domain (planned, not yet registered):** echelplanner.com
- **Package root:** `com.echel.*` (already in use)

### Name Origin

"Echel" is a phonetic representation of the founder's initials (Hutchens-LaPrelle → HL → Echel). It's a coined word — distinctive, trademarkable, and free of conflicts in the software/planning space. "Echelon" was considered but rejected due to heavy existing use (Echelon Front sells a competing planner product, plus Echelon Software Group, Echelon Fit, Echelon Corporation, etc.).

## Logo

A warm cocoa-toned notebook with scribble lines and an angled pencil actively drawing the bottom scribble. The icon reinforces the "evoke physical artifacts" design principle already established in the app.

### Visual Description

- **Notebook:** Rounded rectangle with parchment fill (#FAF3EB), chocolate brown stroke (#7A5C3A)
- **Scribble lines:** Two decorative scribbles in muted tan (#D5C3AA), one active scribble in warm brown (#9B7B4F) being drawn by the pencil
- **Pencil:** Angled at ~30 degrees from vertical (leaning right), tip touching the end of the active scribble. Body is warm brown (#9B7B4F) with a dark brown ferrule (#7A5C3A) and gold tip (#D4AA3E). Approximately 1.5x the height of the notebook.
- **Composition:** Everything contained within the icon bounds — no elements bleed outside the viewBox.

### Logo Colors

| Element | Color | Hex |
|---------|-------|-----|
| Notebook fill | Parchment | #FAF3EB |
| Notebook stroke | Chocolate brown | #7A5C3A |
| Decorative scribbles | Muted tan | #D5C3AA |
| Active scribble | Warm brown | #9B7B4F |
| Pencil body | Warm brown | #9B7B4F |
| Pencil ferrule | Dark brown | #7A5C3A |
| Pencil tip | Gold | #D4AA3E |

### Size Variants

The logo is a single SVG that scales across all sizes. At smaller sizes, details simplify:

- **64px:** Full detail — three scribbles, pencil with ferrule and tip
- **48px:** Full detail
- **32px:** Full detail, slightly thicker strokes
- **24px:** Two scribbles (drop the middle one), thicker strokes
- **16px (favicon):** Two scribbles, simplified pencil (no ferrule detail), heaviest strokes

## Changes by File

### Frontend

| File | Change |
|------|--------|
| `frontend/index.html` | Page title: "Planner" → "Echel Planner" |
| `frontend/public/favicon.svg` | Replace purple-cyan gradient with new notebook+pencil logo |
| `frontend/src/layouts/AppLayout.jsx` | Sidebar header: add logo SVG + "Echel Planner" text (replacing plain "Planner" text) |
| `frontend/src/pages/LoginPage.jsx` | Replace "Welcome back" / "Log in to your Planner account" with logo + "Echel Planner" + tagline |
| `frontend/src/pages/RegisterPage.jsx` | Replace "Create your account" / "Start planning with less friction" with logo + "Echel Planner" + tagline |

### Logo Asset

Create the logo SVG as a shared component or asset that can be imported at different sizes:

- `frontend/src/components/EchelLogo.jsx` — React component accepting a `size` prop, renders the appropriate detail level

### No Backend Changes

The backend has no user-facing branding — all changes are frontend-only.

## What's NOT Changing

- Color palette (lavender primary, warm grays, soft shadows)
- Typography
- UI layout and components
- Navigation structure
- Any backend code or API
