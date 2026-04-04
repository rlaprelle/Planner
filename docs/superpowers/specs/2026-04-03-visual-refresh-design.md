# Visual Refresh: Cozy Journal Design System

## Overview

A whole-app visual polish that transforms the current clinical SaaS look into a warm, cozy journal feel. The app should feel like opening a personal notebook — soft paper-like warmth, lavender accents, gentle rounded shapes, and generous breathing room.

**Approach:** Extend `tailwind.config.js` with semantic design tokens (custom colors, shadows, border-radius defaults), then sweep every page and component to replace default Tailwind indigo/gray/white with the new palette. Formalize the existing Card/CardLabel/ProgressBar components into a small shared component set.

**What changes:** Color palette, backgrounds, shadows, border styles, empty state presentation, sidebar styling, button styling, typography warmth. **What stays the same:** Layout structure, component hierarchy, page routing, all functionality.

---

## 1. Design Tokens (tailwind.config.js)

### Color Palette

#### Backgrounds
| Token | Hex | Usage |
|-------|-----|-------|
| `surface` | `#FAF8F6` | Page background — warm off-white with cream undertone |
| `surface-raised` | `#FFFFFF` | Cards, modals — white that pops against warm bg |
| `surface-soft` | `#F3EEF4` | Subtle highlight areas — light lavender tint |
| `surface-accent` | `#EDE7F0` | Active nav items, selected states — dusty lavender |

#### Primary (replaces indigo)
| Token | Hex | Usage |
|-------|-----|-------|
| `primary-50` | `#F3EEF8` | Very subtle tints, banner backgrounds |
| `primary-100` | `#E8E0F0` | Light badges, hover backgrounds |
| `primary-200` | `#D4C8E2` | Progress bar tracks, subtle accents |
| `primary-300` | `#B8A6CF` | Decorative elements |
| `primary-400` | `#9B89B8` | Icons, secondary actions |
| `primary-500` | `#7C6B9E` | Primary buttons, active states — dusty purple |
| `primary-600` | `#6A5A8A` | Hover state |
| `primary-700` | `#574876` | Pressed state, dark accents |
| `primary-800` | `#443761` | Heavy emphasis |
| `primary-900` | `#2D2545` | Darkest accent |

#### Ink (text colors — warm grays with purple undertone)

Named `ink-*` to avoid collision with Tailwind's `text-*` utility prefix. Usage: `text-ink-heading`, `text-ink-body`, etc.

| Token | Hex | Usage |
|-------|-----|-------|
| `ink-heading` | `#2D2A33` | Headings, strong text |
| `ink-body` | `#4A4553` | Body text, readable secondary |
| `ink-secondary` | `#6B6573` | Less important text |
| `ink-muted` | `#9B95A3` | Labels, metadata, placeholders |
| `ink-faint` | `#C4BFC9` | Disabled text, very subtle hints |

#### Borders

Named `edge-*` to avoid collision with Tailwind's `border` utility. Usage: `border-edge`, `border-edge-subtle`, etc.

| Token | Hex | Usage |
|-------|-----|-------|
| `edge` | `#E5DFE8` | Default card/container borders |
| `edge-subtle` | `#F0ECF2` | Dividers, inner separations |
| `edge-focus` | `#9B89B8` | Focus ring color |

#### Status (warmer, softer versions)
| Token | Hex | Usage |
|-------|-----|-------|
| `deadline-today-bg` | `#F5E6E6` | Today deadline background |
| `deadline-today-text` | `#9E4B4B` | Today deadline text |
| `deadline-week-bg` | `#F5EDE0` | This week deadline background |
| `deadline-week-text` | `#8A6E3E` | This week deadline text |
| `success-bg` | `#E6F0EA` | Success background |
| `success` | `#6B9E7C` | Checkmarks, completion — dusty sage |
| `success-dark` | `#4A7A5C` | Success text on light bg |
| `error` | `#C07070` | Error text — muted rose |
| `error-bg` | `#F5E6E6` | Error background |

### Shadows (purple-tinted, softer)
| Token | Value | Usage |
|-------|-------|-------|
| `shadow-card` | `0 1px 3px rgba(124,107,158,0.08)` | Default card shadow |
| `shadow-card-hover` | `0 4px 12px rgba(124,107,158,0.12)` | Hovered card shadow |
| `shadow-modal` | `0 8px 32px rgba(124,107,158,0.18)` | Modal/overlay shadow |
| `shadow-soft` | `0 1px 2px rgba(124,107,158,0.05)` | Very subtle, for inputs |

### Border Radius
Keep existing `rounded-xl` for cards, `rounded-lg` for buttons/inputs, `rounded-full` for badges. No changes needed — the current rounding already fits the journal feel.

### Typography
No font change — the system font stack is fine. Adjustments are to weight and color only, applied through the new text color tokens.

---

## 2. Global Styles (index.css)

Add a base layer to set the warm page background and default text color:

```css
@layer base {
  body {
    @apply bg-surface text-ink-body;
  }
}
```

This eliminates the need to set `bg-gray-50` on individual page wrappers.

---

## 3. Component-Level Changes

### 3.1 Sidebar (AppLayout.jsx)

**Current:** White background, `border-r border-gray-200`, indigo active states.

**New:**
- Background: `bg-surface-raised` (white) — stays white but the warm page bg behind it creates natural contrast
- Border: `border-r border-edge-subtle` — softer separator
- Brand text: `text-ink-heading` with slightly warmer feel
- Nav items (inactive): `text-ink-secondary hover:bg-surface-soft hover:text-ink-heading`
- Nav items (active): `bg-surface-accent text-primary-700`
- Ritual section label: `text-ink-muted`
- Ritual items (inactive): `text-primary-400 hover:bg-primary-50 hover:text-primary-700`
- Ritual items (active): `bg-primary-100 text-primary-800`
- Badge: `bg-primary-100 text-primary-700`
- Quick capture button: `text-primary-500 hover:bg-primary-50 hover:text-primary-700`
- Focus rings: `focus:ring-2 focus:ring-edge-focus focus:ring-offset-1`
- Header timer: `bg-primary-50 text-primary-700 hover:bg-primary-100`

### 3.2 Dashboard (DashboardPage.jsx)

**Current:** `bg-white` cards with `border-gray-200`, indigo buttons, plain "Dashboard" heading.

**New:**
- Page heading: `text-ink-heading` — consider a friendlier greeting: time-of-day based ("Good morning", "Good afternoon", "Good evening") using the user's display name. Falls back to "Dashboard" if no name.
- Toast: `bg-primary-50 border-primary-200 text-primary-800`
- Cards: `bg-surface-raised border-edge shadow-card rounded-xl hover:shadow-card-hover transition-shadow`
- CardLabel: `text-ink-muted` (replaces `text-gray-400`)
- Weekly banner stats text: `text-ink-heading`
- Weekly banner secondary: `text-ink-secondary`
- Progress bar track: `bg-primary-100` (replaces `bg-gray-100`)
- Progress bar fill: `bg-primary-500` (replaces `bg-indigo-500`)
- "Start: [task]" button: `bg-primary-500 hover:bg-primary-600 text-white`
- "Start planning" link: `text-primary-500 hover:text-primary-700`
- Deadline badges: Use new `deadline-today-*` and `deadline-week-*` tokens
- Inbox card hover: `hover:border-primary-300`
- "Click to review" text: `text-primary-500`
- "Start Morning Planning" button: `bg-primary-500 hover:bg-primary-600 text-white`
- "Evening Clean-up" button: `border-edge text-ink-secondary hover:bg-surface-soft`
- Empty state text: `text-ink-muted` with slightly warmer messaging

### 3.3 Projects Page (ProjectsPage.jsx)

**Current:** Plain list with `bg-white` rows, thin borders.

**New:**
- Project rows: `bg-surface-raised border-edge shadow-card rounded-xl hover:shadow-card-hover transition-shadow`
- "+ New project" button: `bg-primary-500 hover:bg-primary-600 text-white`
- Project color dot: no change (uses project's custom hex)
- Edit/archive icons: `text-ink-muted hover:text-ink-secondary`
- Modal styling: `bg-surface-raised shadow-modal rounded-xl`
- Modal overlay: `bg-black/30` — keep as-is
- Form inputs: `border-edge focus:border-edge-focus focus:ring-edge-focus`

### 3.4 Project Detail (ProjectDetailPage.jsx, TaskRow, TaskDetailPanel)

**Current:** White background, indigo accents, gray borders.

**New:**
- Task list area: `bg-surface-soft` (subtle lavender tint)
- Task rows: `bg-surface-raised` on hover, `border-edge-subtle` separators
- Selected task row: `bg-surface-accent border-primary-200`
- Task detail panel: `bg-surface-raised border-l border-edge`
- Checkbox borders: `border-primary-300 hover:border-primary-400` (unchecked), `bg-success border-success` (checked)
- Status badges: use primary tokens
- Priority/energy labels: `text-ink-muted`
- "Add task" button: `bg-primary-500 hover:bg-primary-600`
- Form fields in detail panel: `border-edge focus:border-edge-focus`

### 3.5 Start Day (StartDayPage.jsx, TimeBlockGrid, TimeBlock)

**Current:** White sections with indigo accents, thin borders on time grid.

**New:**
- Section containers: `bg-surface-raised border-edge shadow-card rounded-xl`
- "ALL TASKS" / "TODAY'S PLAN" labels: `text-ink-muted`
- Due Today section border: `border-deadline-today-bg` (warmer red)
- Due This Week section border: `border-deadline-week-bg` (warmer amber)
- Time grid lines: `border-edge-subtle` (softer than current gray)
- Time grid hour labels: `text-ink-muted`
- Time blocks: `bg-primary-500` → keep similar but use `bg-primary-500`, slightly warmer
- Completed blocks: `bg-primary-200` (softer than current)
- "Add to calendar" button: `bg-primary-100 text-primary-700 hover:bg-primary-200`
- "Confirm plan" button: `bg-primary-500 hover:bg-primary-600 text-white`
- Drag indicator: `border-2 border-dashed border-primary-400`

### 3.6 Active Session (ActiveSessionPage.jsx, TimerCircle)

**Current:** Gradient background `from-gray-50 to-indigo-50/30`, purple SVG circle (`#8b7ec8`).

**New:**
- Background gradient: `from-surface to-primary-50/30` — slightly warmer
- Timer circle track color: `#D4C8E2` (primary-200) — replaces `#e8e4f0`
- Timer circle active color: `#7C6B9E` (primary-500) — replaces `#8b7ec8`, very close
- Task title: `text-ink-heading`
- Project name: `text-ink-muted`
- "Complete" button: `bg-primary-500 hover:bg-primary-600`
- "Done for now" button: `bg-primary-50 text-primary-700 hover:bg-primary-100`
- Extend buttons: `bg-primary-100 text-primary-700 hover:bg-primary-200`
- Overtime indicator: `text-error` with `animate-pulse`

### 3.7 End Day / Reflection (EndDayPage.jsx)

**Current:** Centered layout, indigo buttons, gray sliders.

**New:**
- Page container: centered with warm background showing through
- "End of Day" heading: `text-ink-heading`
- Celebration callout: `bg-primary-50/70 border-primary-100` — keep but use new tokens
- "Continue to reflection" button: `bg-primary-500 hover:bg-primary-600`
- Slider accent: `accent-primary-500` (replaces `accent-indigo-600`)
- Mood/energy labels: `text-ink-secondary`
- Completed tasks list: `text-ink-body`
- Checkmarks: `text-success`
- Reflection textarea: `border-edge focus:border-edge-focus`
- "Save reflection" button: `bg-primary-500 hover:bg-primary-600`

### 3.8 Inbox (InboxPage.jsx)

**Current:** Plain text empty state.

**New:**
- Empty state: Softer message, `text-ink-muted`, centered vertically with more presence
- Card stack (when items exist): `bg-surface-raised shadow-card rounded-xl border-edge`
- Action buttons: Primary action uses `bg-primary-500`, secondary actions use `border-edge text-ink-secondary`

### 3.9 Login / Register Pages

**Current:** `bg-gray-50` page, white card with `shadow-md`.

**New:**
- Page background: `bg-surface` (warm off-white)
- Card: `bg-surface-raised shadow-modal rounded-2xl` — more substantial shadow for a floating feel
- "Planner" brand text: `text-primary-700`
- Submit button: `bg-primary-500 hover:bg-primary-600`
- Link text: `text-primary-500 hover:text-primary-700`
- Form inputs: `border-edge focus:border-edge-focus focus:ring-edge-focus`
- Error text: `text-error`

### 3.10 Quick Capture Modal (QuickCapture.jsx)

**Current:** White modal with indigo accents, `bg-black/30` overlay.

**New:**
- Overlay: `bg-black/30` — keep as-is
- Modal: `bg-surface-raised shadow-modal rounded-xl`
- Trigger button: `text-primary-500 hover:bg-primary-50 hover:text-primary-700`
- Textarea: `border-edge focus:border-edge-focus`
- Save button: `bg-primary-500 hover:bg-primary-600`
- Confirmation checkmark: `text-success`

---

## 4. Shared Components to Formalize

Extract from DashboardPage into `frontend/src/components/ui/`:

### Card.jsx
```jsx
// Wraps content in the standard card treatment
// Props: className (extra classes), hoverable (adds hover shadow), onClick
function Card({ children, className, hoverable, onClick, ...rest })
```
- Default: `bg-surface-raised border border-edge rounded-xl p-5 shadow-card`
- Hoverable: adds `hover:shadow-card-hover transition-shadow`
- Clickable: adds `cursor-pointer`

### CardLabel.jsx
```jsx
// Uppercase section label inside cards
function CardLabel({ children })
```
- `text-xs font-semibold text-ink-muted uppercase tracking-wider mb-2`

### ProgressBar.jsx
```jsx
// Horizontal progress bar with percentage fill
function ProgressBar({ value, max })
```
- Track: `h-2 bg-primary-100 rounded-full`
- Fill: `bg-primary-500 rounded-full transition-all`

These are small, focused extractions — not a component library. They exist because multiple pages use the exact same card treatment, and having the tokens in one place prevents drift.

---

## 5. Files to Touch

### Config / Global
- `frontend/tailwind.config.js` — extend with all color, shadow, borderRadius tokens
- `frontend/src/index.css` — add `@layer base` for body defaults

### New Files
- `frontend/src/components/ui/Card.jsx`
- `frontend/src/components/ui/CardLabel.jsx`
- `frontend/src/components/ui/ProgressBar.jsx`

### Page Components (class replacement sweep)
- `frontend/src/layouts/AppLayout.jsx`
- `frontend/src/pages/DashboardPage.jsx`
- `frontend/src/pages/ProjectsPage.jsx`
- `frontend/src/pages/ProjectDetailPage.jsx`
- `frontend/src/pages/StartDayPage.jsx`
- `frontend/src/pages/EndDayPage.jsx`
- `frontend/src/pages/ActiveSessionPage.jsx`
- `frontend/src/pages/InboxPage.jsx`
- `frontend/src/pages/LoginPage.jsx`
- `frontend/src/pages/RegisterPage.jsx`
- `frontend/src/pages/TodayPage.jsx`

### Sub-Components (class replacement sweep)
- `frontend/src/pages/active-session/TimerCircle.jsx`
- `frontend/src/pages/active-session/SubtaskChecklist.jsx`
- `frontend/src/pages/start-day/TimeBlockGrid.jsx`
- `frontend/src/pages/start-day/TimeBlock.jsx`
- `frontend/src/pages/start-day/TaskBrowserRow.jsx`
- `frontend/src/pages/start-day/TaskCard.jsx`
- `frontend/src/pages/project-detail/TaskRow.jsx`
- `frontend/src/pages/project-detail/TaskDetailPanel.jsx`
- `frontend/src/pages/project-detail/TaskList.jsx`
- `frontend/src/pages/project-detail/AddTaskModal.jsx`
- `frontend/src/components/QuickCapture.jsx`

### Not Touched
- Admin pages — separate visual domain, leave as-is
- API layer, auth, contexts — no visual changes
- Backend — no changes

---

## 6. Dashboard Greeting

Replace the static "Dashboard" heading with a time-of-day greeting:

- Before 12:00 → "Good morning, {firstName}"
- 12:00–17:00 → "Good afternoon, {firstName}"
- After 17:00 → "Good evening, {firstName}"

Use the user's `displayName` from auth context (first word only for the greeting). Falls back to just "Good morning" / etc. if no display name.

---

## 7. Empty States

Give empty states a warmer feel with centered layout and softer copy:

- **Inbox empty:** "All clear. Nothing waiting for you." (centered, `text-ink-muted`, vertically centered in the content area)
- **No deadlines:** "No upcoming deadlines. Nice." (keep current copy, it's good)
- **No plan yet:** "No plan yet for today." with "Start planning →" link
- **No projects:** "No projects yet. Create your first one to get started."
- **Inbox clear (End Day):** "Inbox is clear." (keep current)

No illustrations — just warmer typography placement and breathing room. The words should feel gentle, not empty.

---

## 8. What This Does NOT Include

- Dark mode (future — the token structure supports it via CSS custom properties later)
- Custom fonts (system stack is fine for journal feel)
- Animations beyond existing transitions
- Layout changes (card grid, sidebar width, page structure all stay)
- New components or features
- Mobile responsiveness improvements
- Admin page styling
