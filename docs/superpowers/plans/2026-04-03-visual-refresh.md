# Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the app from a clinical SaaS look to a warm "cozy journal" feel by replacing the default Tailwind indigo/gray palette with custom lavender design tokens across all pages.

**Architecture:** Extend tailwind.config.js with semantic color/shadow tokens, set global body styles in index.css, extract shared Card/CardLabel/ProgressBar components to `components/ui/`, then sweep all pages replacing default Tailwind classes with new tokens. No layout or functional changes.

**Tech Stack:** Tailwind CSS 3.x (config extension), React (JSX class replacements)

**Spec:** `docs/superpowers/specs/2026-04-03-visual-refresh-design.md`

---

### Task 0: Foundation — Tailwind Config + Global Styles + Shared Components

**Goal:** Establish the design token foundation that all other tasks depend on.

**Files:**
- Modify: `frontend/tailwind.config.js`
- Modify: `frontend/src/index.css`
- Create: `frontend/src/components/ui/Card.jsx`
- Create: `frontend/src/components/ui/CardLabel.jsx`
- Create: `frontend/src/components/ui/ProgressBar.jsx`

**Acceptance Criteria:**
- [ ] Tailwind config has all color tokens (surface-*, primary-*, ink-*, edge-*, status colors)
- [ ] Tailwind config has all shadow tokens (card, card-hover, modal, soft)
- [ ] index.css sets body defaults via @layer base
- [ ] Card component renders with new token classes
- [ ] CardLabel component renders with ink-muted text
- [ ] ProgressBar component uses primary-100 track and primary-500 fill
- [ ] `npm run dev` starts without errors

**Verify:** `cd frontend && npx tailwindcss --help > /dev/null 2>&1 && echo "OK"` → OK (config parseable)

**Steps:**

- [ ] **Step 1: Extend tailwind.config.js with all design tokens**

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        surface: {
          DEFAULT: '#FAF8F6',
          raised: '#FFFFFF',
          soft: '#F3EEF4',
          accent: '#EDE7F0',
        },
        primary: {
          50: '#F3EEF8',
          100: '#E8E0F0',
          200: '#D4C8E2',
          300: '#B8A6CF',
          400: '#9B89B8',
          500: '#7C6B9E',
          600: '#6A5A8A',
          700: '#574876',
          800: '#443761',
          900: '#2D2545',
        },
        ink: {
          heading: '#2D2A33',
          body: '#4A4553',
          secondary: '#6B6573',
          muted: '#9B95A3',
          faint: '#C4BFC9',
        },
        edge: {
          DEFAULT: '#E5DFE8',
          subtle: '#F0ECF2',
          focus: '#9B89B8',
        },
        success: {
          DEFAULT: '#6B9E7C',
          bg: '#E6F0EA',
          dark: '#4A7A5C',
        },
        error: {
          DEFAULT: '#C07070',
          bg: '#F5E6E6',
        },
        deadline: {
          'today-bg': '#F5E6E6',
          'today-text': '#9E4B4B',
          'week-bg': '#F5EDE0',
          'week-text': '#8A6E3E',
        },
      },
      boxShadow: {
        card: '0 1px 3px rgba(124,107,158,0.08)',
        'card-hover': '0 4px 12px rgba(124,107,158,0.12)',
        modal: '0 8px 32px rgba(124,107,158,0.18)',
        soft: '0 1px 2px rgba(124,107,158,0.05)',
      },
    },
  },
  plugins: [],
}
```

- [ ] **Step 2: Update index.css with base layer**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  body {
    @apply bg-surface text-ink-body;
  }
}
```

- [ ] **Step 3: Create Card component**

Create `frontend/src/components/ui/Card.jsx`:

```jsx
export function Card({ children, className = '', hoverable = false, onClick, ...rest }) {
  const base = 'bg-surface-raised border border-edge rounded-xl p-5 shadow-card'
  const hover = hoverable || onClick ? 'hover:shadow-card-hover transition-shadow' : ''
  const clickable = onClick ? 'cursor-pointer' : ''
  return (
    <div
      className={`${base} ${hover} ${clickable} ${className}`}
      onClick={onClick}
      {...rest}
    >
      {children}
    </div>
  )
}
```

- [ ] **Step 4: Create CardLabel component**

Create `frontend/src/components/ui/CardLabel.jsx`:

```jsx
export function CardLabel({ children }) {
  return (
    <p className="text-xs font-semibold text-ink-muted uppercase tracking-wider mb-2">{children}</p>
  )
}
```

- [ ] **Step 5: Create ProgressBar component**

Create `frontend/src/components/ui/ProgressBar.jsx`:

```jsx
export function ProgressBar({ value, max }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0
  return (
    <div className="mt-2 h-2 bg-primary-100 rounded-full overflow-hidden">
      <div
        data-testid="progress-fill"
        className="h-full bg-primary-500 rounded-full transition-all"
        style={{ width: `${pct}%` }}
      />
    </div>
  )
}
```

- [ ] **Step 6: Verify dev server starts**

Run: `cd frontend && npm run dev -- --host 2>&1 | head -5`
Expected: Vite dev server starts without errors

- [ ] **Step 7: Commit**

```bash
git add frontend/tailwind.config.js frontend/src/index.css frontend/src/components/ui/
git commit -m "feat: add cozy journal design tokens and shared UI components"
```

---

### Task 1: App Shell — Sidebar + Quick Capture

**Goal:** Apply the warm palette to the sidebar and quick capture modal — the elements visible on every page.

**Files:**
- Modify: `frontend/src/layouts/AppLayout.jsx`
- Modify: `frontend/src/components/QuickCapture.jsx`

**Acceptance Criteria:**
- [ ] Sidebar uses surface/edge/primary tokens instead of gray/indigo
- [ ] Active nav items use surface-accent background
- [ ] Focus rings use edge-focus color
- [ ] Quick capture modal uses new shadow/border tokens
- [ ] Header timer uses primary-50/primary-700

**Verify:** `cd frontend && npm run lint` → no errors

**Steps:**

- [ ] **Step 1: Update AppLayout.jsx**

Replace all Tailwind class references:
- `bg-gray-50` → `bg-surface` (outer flex container)
- `bg-white` → `bg-surface-raised` (sidebar)
- `border-gray-200` → `border-edge` (sidebar border-r)
- `border-gray-100` → `border-edge-subtle` (internal dividers)
- `text-gray-900` → `text-ink-heading` (brand)
- `text-gray-600` → `text-ink-secondary` (nav inactive text)
- `hover:bg-gray-100` → `hover:bg-surface-soft` (nav hover)
- `hover:text-gray-900` → `hover:text-ink-heading` (nav hover text)
- `bg-indigo-50 text-indigo-700` → `bg-surface-accent text-primary-700` (nav active)
- `bg-indigo-100 text-indigo-800` → `bg-primary-100 text-primary-800` (ritual active)
- `text-indigo-600` → `text-primary-400` (ritual inactive)
- `hover:bg-indigo-50` → `hover:bg-primary-50` (ritual hover)
- `hover:text-indigo-800` → `hover:text-primary-700` (ritual hover text)
- `bg-indigo-100 text-indigo-700` → `bg-primary-100 text-primary-700` (badge)
- `focus:ring-indigo-500` → `focus:ring-edge-focus` (all focus rings)
- `text-gray-400` → `text-ink-muted` (section label)
- `text-gray-500` → `text-ink-muted` (user display name)
- `text-gray-600 hover:bg-gray-100 hover:text-gray-800` → `text-ink-secondary hover:bg-surface-soft hover:text-ink-heading` (logout)
- HeaderTimer: `bg-indigo-50 text-indigo-700 hover:bg-indigo-100` → `bg-primary-50 text-primary-700 hover:bg-primary-100`

- [ ] **Step 2: Update QuickCapture.jsx**

Replace all Tailwind class references:
- Trigger button: `text-indigo-600 hover:bg-indigo-50 hover:text-indigo-700` → `text-primary-500 hover:bg-primary-50 hover:text-primary-700`
- `focus:ring-indigo-500` → `focus:ring-edge-focus` (all focus rings)
- Modal content: `bg-white` → `bg-surface-raised`, `shadow-xl` → `shadow-modal`
- Textarea: `border-gray-300` → `border-edge`, `focus:ring-indigo-500 focus:border-indigo-500` → `focus:ring-edge-focus focus:border-edge-focus`
- `text-gray-900` → `text-ink-heading` (modal title)
- `text-gray-400` → `text-ink-muted` (placeholder attribute stays as-is, it uses native)
- `text-gray-700` → `text-ink-body` (captured text)
- `text-gray-600` → `text-ink-secondary` (cancel button)
- `hover:text-gray-900` → `hover:text-ink-heading` (cancel hover)
- `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600` (save button)
- Checkmark: `text-green-500` → `text-success`
- Error: `text-red-600` → `text-error`

- [ ] **Step 3: Verify and commit**

Run: `cd frontend && npm run lint`
Expected: No errors

```bash
git add frontend/src/layouts/AppLayout.jsx frontend/src/components/QuickCapture.jsx
git commit -m "feat: apply cozy journal palette to sidebar and quick capture"
```

---

### Task 2: Dashboard

**Goal:** Transform the main dashboard page with warm palette and time-based greeting.

**Files:**
- Modify: `frontend/src/pages/DashboardPage.jsx`

**Acceptance Criteria:**
- [ ] Uses shared Card/CardLabel/ProgressBar from components/ui/
- [ ] Page heading shows time-based greeting with user's first name
- [ ] All colors use design token classes
- [ ] Deadline badges use deadline-* tokens
- [ ] Buttons use primary-* tokens

**Verify:** `cd frontend && npm run lint` → no errors

**Steps:**

- [ ] **Step 1: Update imports and remove local Card/CardLabel/ProgressBar**

Replace the local `Card`, `CardLabel`, `ProgressBar` function definitions with imports:

```jsx
import { Card } from '@/components/ui/Card'
import { CardLabel } from '@/components/ui/CardLabel'
import { ProgressBar } from '@/components/ui/ProgressBar'
import { useAuth } from '@/auth/useAuth'
```

Remove the three local function definitions for `Card`, `CardLabel`, and `ProgressBar`.

- [ ] **Step 2: Add greeting helper and update heading**

Add at the top of the component function:

```jsx
const { user } = useAuth()
const firstName = user?.displayName?.split(' ')[0]

function getGreeting() {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}
```

Replace the heading:
```jsx
<h1 className="text-2xl font-semibold text-ink-heading mb-6">
  {firstName ? `${getGreeting()}, ${firstName}` : getGreeting()}
</h1>
```

- [ ] **Step 3: Update all color classes**

Toast: `bg-indigo-50 border-indigo-200 text-indigo-800` → `bg-primary-50 border-primary-200 text-primary-800`

WeeklyBanner:
- `text-gray-900` → `text-ink-heading`
- `text-gray-400` → `text-ink-faint` (separator dots)
- `text-gray-500` → `text-ink-secondary` (trend text and "No activity" text)

DEADLINE_BADGE:
```jsx
const DEADLINE_BADGE = {
  TODAY: 'bg-deadline-today-bg text-deadline-today-text',
  THIS_WEEK: 'bg-deadline-week-bg text-deadline-week-text',
}
```

Card 1 (Today at a Glance):
- `text-gray-900` → `text-ink-heading`
- `text-gray-500` → `text-ink-secondary`
- `bg-indigo-500 hover:bg-indigo-600` → `bg-primary-500 hover:bg-primary-600` (start button)
- `text-indigo-600 hover:text-indigo-800` → `text-primary-500 hover:text-primary-700` (start planning link)

Card 2 (Streak):
- `text-gray-900` → `text-ink-heading`
- `text-gray-500` → `text-ink-secondary`
- `text-gray-400` → `text-ink-muted`

Card 3 (Deadlines):
- `text-gray-800` → `text-ink-body`
- `hover:bg-gray-50` → `hover:bg-surface-soft`
- `text-gray-400` → `text-ink-muted` (empty message: "No upcoming deadlines. Nice.")

Card 4 (Inbox):
- `hover:border-indigo-300` → `hover:border-primary-300`
- `text-gray-900` → `text-ink-heading`
- `text-gray-500` → `text-ink-secondary`
- `text-indigo-500` → `text-primary-500` ("Click to review")
- `text-gray-400` → `text-ink-muted` ("Inbox clear.")

Quick action buttons:
- `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`
- `border-gray-300 text-gray-700 hover:bg-gray-50` → `border-edge text-ink-secondary hover:bg-surface-soft`

Loading state: `text-gray-400` → `text-ink-muted`

Update focus rings: all `focus:ring-indigo-500` → `focus:ring-edge-focus`

- [ ] **Step 4: Verify and commit**

Run: `cd frontend && npm run lint`

```bash
git add frontend/src/pages/DashboardPage.jsx
git commit -m "feat: apply cozy journal palette to dashboard with greeting"
```

---

### Task 3: Auth Pages — Login + Register

**Goal:** Apply warm palette to the login and register pages.

**Files:**
- Modify: `frontend/src/pages/LoginPage.jsx`
- Modify: `frontend/src/pages/RegisterPage.jsx`

**Acceptance Criteria:**
- [ ] Page backgrounds use surface token
- [ ] Cards use surface-raised with shadow-modal
- [ ] Form inputs use edge-* border tokens
- [ ] Buttons use primary-* tokens
- [ ] Error text uses error token

**Verify:** `cd frontend && npm run lint` → no errors

**Steps:**

- [ ] **Step 1: Update LoginPage.jsx**

- `bg-gray-50` → `bg-surface` (page background)
- `bg-white` → `bg-surface-raised` (card)
- `shadow-md` → `shadow-modal` (card shadow)
- `text-gray-900` → `text-ink-heading` (headings)
- `text-gray-500` → `text-ink-secondary` (subtitle)
- `text-gray-700` → `text-ink-body` (labels)
- `border-gray-300` → `border-edge` (inputs)
- `border-red-400` → `border-error` (error state input)
- `focus:ring-indigo-500 focus:border-indigo-500` → `focus:ring-edge-focus focus:border-edge-focus` (inputs)
- `bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400` → `bg-primary-500 hover:bg-primary-600 disabled:bg-primary-400` (submit)
- `focus:ring-indigo-500` → `focus:ring-edge-focus` (submit focus)
- `text-red-600` → `text-error` (error text)
- `text-indigo-600 hover:text-indigo-700` → `text-primary-500 hover:text-primary-700` (link)

- [ ] **Step 2: Update RegisterPage.jsx**

Same pattern as LoginPage. Also update the `inputClass` helper function:
```jsx
const inputClass = (hasError) =>
  `w-full rounded-lg border px-3 py-2 text-ink-heading text-sm shadow-sm
   focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus
   ${hasError ? 'border-error' : 'border-edge'}`
```

And all other instances matching the LoginPage replacements.

- [ ] **Step 3: Verify and commit**

```bash
git add frontend/src/pages/LoginPage.jsx frontend/src/pages/RegisterPage.jsx
git commit -m "feat: apply cozy journal palette to login and register pages"
```

---

### Task 4: Projects — List, Detail, Tasks

**Goal:** Apply warm palette to the projects page, project detail, task rows, task detail panel, task list, and add task modal.

**Files:**
- Modify: `frontend/src/pages/ProjectsPage.jsx`
- Modify: `frontend/src/pages/ProjectDetailPage.jsx`
- Modify: `frontend/src/pages/project-detail/TaskRow.jsx`
- Modify: `frontend/src/pages/project-detail/TaskDetailPanel.jsx`
- Modify: `frontend/src/pages/project-detail/TaskList.jsx`
- Modify: `frontend/src/pages/project-detail/AddTaskModal.jsx`

**Acceptance Criteria:**
- [ ] Project rows use shadow-card with hover:shadow-card-hover
- [ ] Modals use shadow-modal and surface-raised background
- [ ] All indigo references replaced with primary-*
- [ ] All gray references replaced with ink-*/edge-*/surface-*
- [ ] Focus rings use edge-focus
- [ ] Task checkboxes use success color when checked

**Verify:** `cd frontend && npm run lint` → no errors

**Steps:**

- [ ] **Step 1: Update ProjectsPage.jsx**

Spinner: `border-indigo-200 border-t-indigo-600` → `border-primary-200 border-t-primary-600`

ColorPicker: `focus:ring-indigo-500` → `focus:ring-edge-focus`, `ring-gray-600` → `ring-ink-secondary`

ProjectFormModal:
- Overlay: keep `bg-black/40`
- Content: `bg-white` → `bg-surface-raised`, `shadow-xl` → `shadow-modal`
- Title: `text-gray-900` → `text-ink-heading`
- Labels: `text-gray-700` → `text-ink-body`, `text-gray-400` → `text-ink-muted`
- Inputs: `border-gray-300` → `border-edge`, `focus:ring-indigo-500` → `focus:ring-edge-focus`
- Error input: `border-red-400` → `border-error`
- Error text: `text-red-500` → `text-error`
- Cancel button: `text-gray-700 border-gray-300 hover:bg-gray-50` → `text-ink-body border-edge hover:bg-surface-soft`
- Save button: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`

ArchiveConfirmModal:
- Content: `bg-white` → `bg-surface-raised`, `shadow-xl` → `shadow-modal`
- Title: `text-gray-900` → `text-ink-heading`
- Description: `text-gray-600` → `text-ink-secondary`
- Cancel: `text-gray-700 border-gray-300 hover:bg-gray-50` → `text-ink-body border-edge hover:bg-surface-soft`
- Archive button: keep red styling for destructive action

ProjectRow:
- `bg-white border-gray-200 hover:border-gray-300 hover:shadow-sm` → `bg-surface-raised border-edge shadow-card hover:shadow-card-hover`
- `hover:text-indigo-700` → `hover:text-primary-700` (project name link)
- `text-gray-900` → `text-ink-heading` (project name)
- `text-gray-500` → `text-ink-muted` (description)
- Action buttons: `text-gray-400 hover:text-gray-700 hover:bg-gray-100` → `text-ink-muted hover:text-ink-secondary hover:bg-surface-soft`
- Archive hover: keep `hover:text-red-600 hover:bg-red-50`

Page:
- `text-gray-900` → `text-ink-heading` (heading)
- New project button: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`
- Error: `text-red-500` → `text-error`
- Empty state: `text-gray-500` → `text-ink-muted`, button: `text-indigo-600 border-indigo-300 hover:bg-indigo-50` → `text-primary-500 border-primary-300 hover:bg-primary-50`

- [ ] **Step 2: Update ProjectDetailPage.jsx**

- Header: `border-gray-200 bg-white` → `border-edge bg-surface-raised`
- Back link: `text-indigo-600` → `text-primary-500`
- Heading: `text-gray-900` → `text-ink-heading`
- Add task button: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`
- Focus: `focus:ring-indigo-500` → `focus:ring-edge-focus`
- Error: `text-red-500` → `text-error`
- Detail panel border: `border-gray-200` → `border-edge`

- [ ] **Step 3: Update TaskRow.jsx**

- Selected row: `bg-indigo-50 border-indigo-200` → `bg-surface-accent border-primary-200`
- Hover row: `hover:bg-gray-50` → `hover:bg-surface-soft`
- Chevron: `text-gray-400 hover:text-gray-600` → `text-ink-muted hover:text-ink-secondary`
- StatusCheckbox (done): keep `bg-green-500 border-green-500` → `bg-success border-success`
- StatusCheckbox (not done): `border-gray-300 hover:border-green-400` → `border-primary-300 hover:border-primary-400`
- Focus: `focus:ring-indigo-400` → `focus:ring-edge-focus`
- Title (done): `text-gray-400` → `text-ink-muted`
- Title (active): `text-gray-800` → `text-ink-body`
- Detail arrow: `text-gray-300 hover:text-gray-600` → `text-ink-faint hover:text-ink-secondary`

- [ ] **Step 4: Update TaskDetailPanel.jsx**

- Panel: `bg-white border-gray-200` → `bg-surface-raised border-edge`
- Header border: `border-gray-100` → `border-edge-subtle`
- Title input: `text-gray-900` → `text-ink-heading`, `hover:border-gray-300 focus:border-indigo-400` → `hover:border-edge focus:border-edge-focus`
- Project name: `text-gray-400` → `text-ink-muted`
- Close button: `text-gray-400 hover:text-gray-600` → `text-ink-muted hover:text-ink-secondary`, `focus:ring-indigo-400` → `focus:ring-edge-focus`
- FieldLabel: `text-gray-500` → `text-ink-secondary`
- Select fields: `border-gray-200` → `border-edge`, `focus:ring-indigo-400` → `focus:ring-edge-focus`
- Textarea: same pattern
- Date input: same pattern
- "Add subtask" link: `text-indigo-600 hover:text-indigo-800` → `text-primary-500 hover:text-primary-700`
- Empty subtasks: `text-gray-400` → `text-ink-muted`
- ChildTaskItem checkbox: same as TaskRow StatusCheckbox
- ChildTaskItem text (done): `text-gray-400` → `text-ink-muted`
- ChildTaskItem text (active): `text-gray-700` → `text-ink-body`
- Archive button: `text-gray-500 hover:text-red-600` → `text-ink-muted hover:text-error`
- Footer border: `border-gray-100` → `border-edge-subtle`
- Error text: `text-red-500` → `text-error`
- ArchiveConfirmModal: same as ProjectsPage pattern
- AddTaskModal (nested): see step 5

- [ ] **Step 5: Update TaskList.jsx**

- Group heading: `text-gray-500` → `text-ink-muted`
- Empty state: `text-gray-400` → `text-ink-muted`

- [ ] **Step 6: Update AddTaskModal.jsx**

Same modal pattern as ProjectFormModal:
- Content: `bg-white` → `bg-surface-raised`, `shadow-xl` → `shadow-modal`
- Title: `text-gray-900` → `text-ink-heading`
- Labels: `text-gray-700` → `text-ink-body`, `text-gray-400` → `text-ink-muted`
- Required asterisk: keep `text-red-500` → `text-error`
- Inputs: `border-gray-300` → `border-edge`, `focus:ring-indigo-500` → `focus:ring-edge-focus`
- Error input: `border-red-400` → `border-error`
- Error text: `text-red-500` → `text-error`
- Cancel: `text-gray-700 border-gray-300 hover:bg-gray-50` → `text-ink-body border-edge hover:bg-surface-soft`
- Submit: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`

- [ ] **Step 7: Verify and commit**

```bash
git add frontend/src/pages/ProjectsPage.jsx frontend/src/pages/ProjectDetailPage.jsx frontend/src/pages/project-detail/
git commit -m "feat: apply cozy journal palette to projects and task views"
```

---

### Task 5: Start Day — Planning Page

**Goal:** Apply warm palette to the morning planning page with time grid, task browser, and task cards.

**Files:**
- Modify: `frontend/src/pages/StartDayPage.jsx`
- Modify: `frontend/src/pages/start-day/TimeBlockGrid.jsx`
- Modify: `frontend/src/pages/start-day/TimeBlock.jsx`
- Modify: `frontend/src/pages/start-day/TaskBrowserRow.jsx`
- Modify: `frontend/src/pages/start-day/TaskCard.jsx`

**Acceptance Criteria:**
- [ ] Section containers use shadow-card with edge borders
- [ ] Section labels use ink-muted (replacing indigo-700)
- [ ] Due Today/This Week sections use deadline-* border tokens
- [ ] Time grid lines use edge-subtle
- [ ] Time blocks use primary-500 instead of indigo-500
- [ ] Task cards use edge/primary tokens
- [ ] Drag overlay uses primary-500

**Verify:** `cd frontend && npm run lint` → no errors

**Steps:**

- [ ] **Step 1: Update StartDayPage.jsx**

- Heading: `text-gray-900` → `text-ink-heading`
- Loading: `text-gray-400` → `text-ink-muted`
- Section containers: `bg-white border-gray-200` → `bg-surface-raised border-edge shadow-card`
- Section labels: `text-indigo-700` → `text-primary-700`, subtitle span `text-gray-400` → `text-ink-muted`
- Due Today border: `border-red-200` → `border-deadline-today-bg`, label `text-red-700` → `text-deadline-today-text`
- Due This Week border: `border-amber-200` → `border-deadline-week-bg`, label `text-amber-700` → `text-deadline-week-text`
- Selected count: `text-gray-500` → `text-ink-muted`
- Add to calendar button: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`
- Warning: `text-amber-600` → `text-deadline-week-text`
- Error: `text-red-500` → `text-error`
- Confirm plan button: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`
- DragOverlay: `bg-indigo-500` → `bg-primary-500`

- [ ] **Step 2: Update TimeBlockGrid.jsx**

- Container: `border-gray-200` → `border-edge`, `bg-white` → `bg-surface-raised`, `bg-indigo-50` (isOver) → `bg-primary-50`
- Hour label bar: `border-gray-100` → `border-edge-subtle`
- Hour labels: `text-gray-400` → `text-ink-muted`
- Hour lines: `border-gray-200` → `border-edge`
- 15-min sub-lines: `border-gray-200` → `border-edge`, `border-gray-100` → `border-edge-subtle`
- Drop preview: `border-indigo-400 bg-indigo-200/40` → `border-primary-400 bg-primary-200/40`

- [ ] **Step 3: Update TimeBlock.jsx**

- Completed block: `bg-gray-100 border-gray-200 text-gray-400` → `bg-surface-soft border-edge text-ink-muted`
- Active block: `bg-indigo-500 border-indigo-600 text-white` → `bg-primary-500 border-primary-600 text-white`
- Time label (active): `text-indigo-200` → `text-primary-200`
- Time label (done): `text-gray-400` → `text-ink-muted`
- Start button: `bg-indigo-600/50 hover:bg-indigo-600/80` → `bg-primary-600/50 hover:bg-primary-600/80`
- Remove button: keep `bg-black/20 hover:bg-red-500`

- [ ] **Step 4: Update TaskBrowserRow.jsx**

- Empty message: `text-gray-400` → `text-ink-muted`
- Project column background: `bg-indigo-50` → `bg-primary-50`
- Project name: `text-indigo-800` → `text-primary-800`

- [ ] **Step 5: Update TaskCard.jsx**

DEADLINE_BADGE:
```jsx
const DEADLINE_BADGE = {
  TODAY: { label: 'TODAY', className: 'bg-deadline-today-bg text-deadline-today-text' },
  THIS_WEEK: { label: 'THIS WK', className: 'bg-deadline-week-bg text-deadline-week-text' },
}
```

Card classes:
- `border-gray-200` → `border-edge`
- `border-indigo-300` (dragging and hover) → `border-primary-300`
- `hover:border-indigo-300 hover:shadow-sm` → `hover:border-primary-300 hover:shadow-card`
- `text-gray-800` → `text-ink-body` (title)
- `text-gray-400` → `text-ink-muted` (points)
- `accent-indigo-600` → `accent-primary-500` (checkbox)

- [ ] **Step 6: Verify and commit**

```bash
git add frontend/src/pages/StartDayPage.jsx frontend/src/pages/start-day/
git commit -m "feat: apply cozy journal palette to morning planning"
```

---

### Task 6: Active Session

**Goal:** Apply warm palette to the focus session page, timer circle, and subtask checklist.

**Files:**
- Modify: `frontend/src/pages/ActiveSessionPage.jsx`
- Modify: `frontend/src/pages/active-session/TimerCircle.jsx`
- Modify: `frontend/src/pages/active-session/SubtaskChecklist.jsx`

**Acceptance Criteria:**
- [ ] Page gradient uses surface/primary-50 tokens
- [ ] Timer SVG colors use primary-200 and primary-500 hex values
- [ ] Action buttons use primary-* tokens
- [ ] Subtask checkboxes use primary/success tokens

**Verify:** `cd frontend && npm run lint` → no errors

**Steps:**

- [ ] **Step 1: Update ActiveSessionPage.jsx**

- Background gradient: `from-gray-50 to-indigo-50/30` → `from-surface to-primary-50/30`
- Loading: `bg-gray-50` → `bg-surface`, `text-gray-400` → `text-ink-muted`
- Project name: `text-gray-400` → `text-ink-muted`
- Task title: `text-gray-800` → `text-ink-heading`
- Flash: `text-indigo-600` → `text-primary-600`
- Error: `bg-red-50 border-red-200 text-red-700` → `bg-error-bg border-error text-error`, dismiss: `text-red-500 hover:text-red-700` → `text-error hover:text-ink-heading`
- Complete button: `bg-indigo-500 hover:bg-indigo-600` → `bg-primary-500 hover:bg-primary-600`
- Extend/Done for now buttons: `bg-indigo-50 text-indigo-600 hover:bg-indigo-100` → `bg-primary-50 text-primary-700 hover:bg-primary-100`
- Extend menu: `border-gray-200` → `border-edge`, `text-gray-700 hover:bg-indigo-50` → `text-ink-body hover:bg-primary-50`

- [ ] **Step 2: Update TimerCircle.jsx**

- SVG track circle stroke: `#e8e4f0` → `#D4C8E2` (primary-200)
- SVG progress circle stroke: `#8b7ec8` → `#7C6B9E` (primary-500)
- Overtime time text: `text-gray-400` → `text-ink-muted`
- Overtime message: `text-indigo-400` → `text-primary-400`
- Normal time text: `text-gray-800` → `text-ink-heading`
- "of N min" label: `text-gray-400` → `text-ink-muted`

- [ ] **Step 3: Update SubtaskChecklist.jsx**

- Card: `bg-white` → `bg-surface-raised`
- Label: `text-gray-400` → `text-ink-muted`
- Hover row: `hover:bg-gray-50` → `hover:bg-surface-soft`
- Checkbox: `border-gray-300 text-indigo-500 focus:ring-indigo-400` → `border-primary-300 text-primary-500 focus:ring-edge-focus`
- Text (done): `text-gray-400` → `text-ink-muted`
- Text (active): `text-gray-700` → `text-ink-body`

- [ ] **Step 4: Verify and commit**

```bash
git add frontend/src/pages/ActiveSessionPage.jsx frontend/src/pages/active-session/
git commit -m "feat: apply cozy journal palette to active session"
```

---

### Task 7: End Day + Inbox + Remaining

**Goal:** Apply warm palette to the end-of-day flow, inbox, deferred item actions, convert form, and today page.

**Files:**
- Modify: `frontend/src/pages/EndDayPage.jsx`
- Modify: `frontend/src/pages/InboxPage.jsx`
- Modify: `frontend/src/pages/TodayPage.jsx`
- Modify: `frontend/src/components/deferred/DeferredItemActions.jsx`
- Modify: `frontend/src/components/deferred/ConvertForm.jsx`

**Acceptance Criteria:**
- [ ] End Day phase backgrounds use surface token
- [ ] Reflection form uses edge-*/primary-* tokens
- [ ] Celebration callout uses primary-50/primary-100
- [ ] Inbox items use Card-style treatment
- [ ] Deferred action buttons use primary/edge tokens
- [ ] ConvertForm inputs use edge/primary tokens
- [ ] Today placeholder uses ink tokens

**Verify:** `cd frontend && npm run lint` → no errors

**Steps:**

- [ ] **Step 1: Update EndDayPage.jsx**

Phase 2 (reflection):
- Heading: `text-gray-900` → `text-ink-heading`
- "Completed today" label: `text-gray-400` → `text-ink-muted`
- Task items: `text-gray-700` → `text-ink-body`, checkmark `text-indigo-400` → `text-primary-400`
- "Notable today" label: `text-gray-400` → `text-ink-muted`
- Celebration callout: `bg-indigo-50/70 border-indigo-100` → `bg-primary-50/70 border-primary-100`
- Callout text: `text-gray-800` → `text-ink-heading`, `text-gray-500` → `text-ink-muted`
- Form labels: `text-gray-700` → `text-ink-body`, slider label accent: `text-indigo-600` → `text-primary-500`
- Slider accent: `accent-indigo-600` → `accent-primary-500`
- Slider range labels: `text-gray-400` → `text-ink-muted`
- Textarea: `border-gray-300 focus:ring-indigo-500` → `border-edge focus:ring-edge-focus`
- Error: `text-red-500` → `text-error`
- Submit button: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`
- Streak message: `text-gray-900` → `text-ink-heading`
- "That's a wrap": `text-gray-500` → `text-ink-secondary`
- "Done" button: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`

EndDayPage main:
- All `bg-gray-50` → `bg-surface`
- Loading: `text-gray-400` → `text-ink-muted`
- "Inbox is clear" text: `text-gray-500` → `text-ink-secondary`
- Item counter: `text-gray-400` → `text-ink-muted`
- Item card: `bg-white border-gray-200 shadow-sm` → `bg-surface-raised border-edge shadow-card`
- "Continue to reflection" button: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`
- Celebration: `text-gray-900` → `text-ink-heading`, `text-gray-500` → `text-ink-secondary`

- [ ] **Step 2: Update InboxPage.jsx**

- Heading: `text-gray-900` → `text-ink-heading`
- Loading: `text-gray-400` → `text-ink-muted`
- Empty state: `text-gray-400` → `text-ink-muted`, change text to "All clear. Nothing waiting for you."
- Item cards: `bg-white border-gray-200` → `bg-surface-raised border-edge shadow-card`

- [ ] **Step 3: Update DeferredItemActions.jsx**

- Captured ago: `text-gray-500` → `text-ink-muted`
- Raw text: `text-gray-900` → `text-ink-heading`
- Convert button: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`
- Defer/Dismiss buttons: `border-gray-300 text-gray-700 hover:bg-gray-50` → `border-edge text-ink-secondary hover:bg-surface-soft`
- Dismiss text: `text-gray-500` → `text-ink-muted`
- Defer label: `text-gray-500` → `text-ink-muted`
- Cancel button: `text-gray-400 hover:text-gray-600` → `text-ink-muted hover:text-ink-secondary`
- Dismiss confirm: `bg-red-50 text-red-700 border-red-200 hover:bg-red-100` → keep (destructive action)
- "Sure?" text: `text-gray-500` → `text-ink-muted`
- Form border: `border-gray-100` → `border-edge-subtle`

- [ ] **Step 4: Update ConvertForm.jsx**

- Border top: `border-gray-100` → `border-edge-subtle`
- Labels: `text-gray-600` → `text-ink-secondary`
- All inputs: `border-gray-300 focus:ring-indigo-500` → `border-edge focus:ring-edge-focus`
- Error: `text-red-500` → `text-error`
- Cancel: `text-gray-600 hover:bg-gray-100` → `text-ink-secondary hover:bg-surface-soft`
- Submit: `bg-indigo-600 hover:bg-indigo-700` → `bg-primary-500 hover:bg-primary-600`

- [ ] **Step 5: Update TodayPage.jsx**

```jsx
export function TodayPage() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-semibold text-ink-heading">Today</h1>
      <p className="mt-2 text-ink-secondary">Coming soon.</p>
    </div>
  )
}
```

- [ ] **Step 6: Verify and commit**

```bash
git add frontend/src/pages/EndDayPage.jsx frontend/src/pages/InboxPage.jsx frontend/src/pages/TodayPage.jsx frontend/src/components/deferred/
git commit -m "feat: apply cozy journal palette to end day, inbox, and remaining pages"
```

---

### Task 8: E2E Tests + Lint Verification

**Goal:** Verify E2E tests still pass (they mock APIs, so CSS changes shouldn't break them) and run final lint check.

**Files:**
- No modifications expected — verification only

**Acceptance Criteria:**
- [ ] `npm run lint` passes with no errors
- [ ] E2E tests pass (or failures are unrelated to visual changes)

**Verify:** `cd frontend && npm run lint && cd ../e2e && npx playwright test --reporter=list 2>&1 | tail -5`

**Steps:**

- [ ] **Step 1: Run frontend lint**

Run: `cd frontend && npm run lint`
Expected: 0 errors

- [ ] **Step 2: Run E2E tests**

Run: `cd e2e && npx playwright test --reporter=list`
Expected: All tests pass (they mock API calls and test behavior, not CSS)

- [ ] **Step 3: If lint errors, fix them**

Address any lint issues from the class replacements (unused imports, etc.)

- [ ] **Step 4: If E2E failures related to visual changes, investigate**

Check if any tests assert on specific CSS classes. If so, update those assertions.

- [ ] **Step 5: Commit any fixes**

```bash
git add -A
git commit -m "fix: address lint/test issues from visual refresh"
```
