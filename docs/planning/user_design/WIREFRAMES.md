# Phase 6: Wireframes of Major Views

**Purpose**: Create low-fidelity visual layouts for the 6 critical views. These are ASCII/text wireframes designed for easy review and iteration.

---

## View 1: Morning Planning View

**Layout**: Two-panel vertical split (top/bottom)

```
┌─────────────────────────────────────────────────────────────┐
│ MORNING PLANNING                                       [≡]   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  PROJECT COLUMNS (Horizontal Scroll)                        │
│  ┌──────────────┬──────────────┬──────────────┐            │
│  │ Auth System  │ UI Project   │ Dental Care  │            │
│  ├──────────────┼──────────────┼──────────────┤            │
│  │ ☑ Fix login  │ ☐ Design     │ ☐ Schedule   │            │
│  │   🔴 Today   │   📅 Fri     │   Next Wed   │            │
│  │   3 pts      │   5 pts      │   2 pts      │            │
│  │              │              │              │            │
│  │ ☑ Reset pwd  │ ☐ Mobile nav │              │            │
│  │   Mon | 4pts │   🔴 Today   │              │            │
│  │              │   3 pts      │              │            │
│  └──────────────┴──────────────┴──────────────┘            │
│                                                              │
│  Selected: 2/5 tasks                                         │
│                             [+ Add to calendar]             │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│                       TODAY'S CALENDAR                       │
│                     (9:00 AM - 5:00 PM)                     │
│                                                              │
│  ┌─ 9:00-10:00 ─────────────────────────────────────────┐  │
│  │ Fix login bug (Auth System, 3pts)          [drag]    │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌─ 10:00-10:15 ────────────────────────────────────────┐  │
│  │ ☕ BREAK                                              │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌─ 10:15-12:00 ────────────────────────────────────────┐  │
│  │ Design dashboard (UI Project, 5pts)       [drag]     │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌─ 12:00-1:00 PM ──────────────────────────────────────┐  │
│  │ ☰ LUNCH / BREAK                                      │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  Tasks: 2 selected                                           │
│                                                              │
│                                [Confirm plan]               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Key Interactions**:
- ☑ Checkbox: Select/deselect tasks
- [drag]: Drag tasks between panels or reorder in calendar
- [+ Add to calendar]: Button to auto-place selected tasks
- [Confirm plan]: Save today's plan, creates TimeBlocks

---

## View 2: Active Work Session View

**Layout**: Full-screen focus mode (vertical, centered)

```
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│                                                              │
│                     FIX LOGIN BUG                           │
│                   (Auth System)                             │
│                                                              │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Time: 45:30 remaining                                  │ │
│  │ ████████████░░░░░░░░░░░░ 65% complete                │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│                      STEPS                                  │
│                                                              │
│   ☑ Review login endpoint code                              │
│   ☑ Identify auth bug                                       │
│   ☐ Write unit tests for fix                                │
│   ☐ Implement fix                                           │
│   ☐ Run full test suite                                     │
│                                                              │
│                                                              │
│                                                              │
│    ┌──────────────┬──────────────┬──────────────┐           │
│    │  Complete    │  Extend      │  Done for now│           │
│    └──────────────┴──────────────┴──────────────┘           │
│                                                              │
│                                                              │
│                                                              │
│                                                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Key Interactions**:
- ☑ Checkboxes: Check off subtasks as you complete them (with celebration animation)
- [Complete]: Finished the block, mark time block as done
- [Extend]: Extend time (prompts for duration: 15min, 30min, 1hr, custom)
- [Done for now]: Stop working, block incomplete
- Timer updates every second
- **Single gentle chime at 100%** (no 80% warning, no customizable reminders — Phase 2+)
- No notes field in MVP (add notes later via Task Details if needed — Phase 2+)

---

## View 3: Evening Clean-up View - Phase 1: Deferred Items

**Layout**: Stack of cards, one visible at a time

```
┌─────────────────────────────────────────────────────────────┐
│ EVENING CLEAN-UP                                       [≡]   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│                  INBOX: 5 ITEMS TO PROCESS                 │
│                                                              │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                                                     │   │
│  │  Email Dave about the invoice                      │   │
│  │                                                     │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │                                                     │   │
│  │  ○ Convert to task                                 │   │
│  │  ○ Defer                                           │   │
│  │  ○ Dismiss                                         │   │
│  │                                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
│         Item 1 of 5                                          │
│                                                              │
│                                                              │
│  [If "Convert to task" selected]                            │
│                                                              │
│  ┌─ Create Task ─────────────────────────────────────────┐ │
│  │                                                       │ │
│  │  Project: [Auth System ▼]                            │ │
│  │                                                       │ │
│  │  Title: Email Dave about the invoice                 │ │
│  │                                                       │ │
│  │  Description: (optional)                             │ │
│  │  ________________________                             │ │
│  │                                                       │ │
│  │  Due date: (optional) [____]                         │ │
│  │                                                       │ │
│  │  Priority: [3 ▼]                                     │ │
│  │                                                       │ │
│  │  Points: [__ ▼]                                      │ │
│  │                                                       │ │
│  │  Add child tasks? ☐                                 │ │
│  │                                                       │ │
│  │              [Cancel]  [Save]                        │ │
│  │                                                       │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                              │
│                                                              │
│  [If "Defer" selected]                                      │
│                                                              │
│  Defer until:  ○ 1 day   ○ 1 week   ○ 1 month             │
│                                                              │
│              [Defer]   [Cancel]                             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Key Interactions**:
- ○ Radio buttons: Select action (Convert / Defer / Dismiss)
- Project dropdown: Choose which project for the new task
- All form fields optional (user can just save with title)
- [Save]: Creates task, marks deferred item processed, moves to next item
- [Defer]: Shows defer options, re-queues item for that date
- [Dismiss]: Marks processed, moves to next item

---

## View 3: Evening Clean-up View - Phase 2: End-of-Day Reflection

**Layout**: Form with sliders and text fields

```
┌─────────────────────────────────────────────────────────────┐
│ END-OF-DAY REFLECTION                                  [≡]   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│              HOW WAS YOUR DAY?                              │
│                                                              │
│                                                              │
│  Energy Level                                               │
│  ○─────●─────○                                              │
│  1    3(●)    5                                              │
│                                                              │
│                                                              │
│  Mood                                                        │
│  ○─────●─────○                                              │
│  1    3(●)    5                                              │
│                                                              │
│                                                              │
│  Reflection Notes (optional)                                │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Good progress on the login bug. Got blocked on        │ │
│  │ the API response but pivoted to dashboard design.     │ │
│  │ Feeling productive overall.                           │ │
│  │                                                        │ │
│  │                                                        │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│                                                              │
│  COMPLETED TODAY                                            │
│                                                              │
│  ✓ Fix login bug (Auth System, 3pts)                        │
│    Notes: Completed main fix, wrote tests                   │
│                                                              │
│  ✓ Design dashboard (UI Project, 5pts)                      │
│    Notes: Got most wireframes done, need refinement         │
│                                                              │
│                                                              │
│                                                              │
│                                 [Save & Close]              │
│                                                              │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ 🎉 7-DAY PLANNING STREAK!                             │ │
│  │                                                        │ │
│  │ Rest well, see you tomorrow                           │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Key Interactions**:
- Sliders: Energy and mood 1-5
- Text area: Optional reflection notes
- Completed tasks shown with checkmarks; user can add notes to each
- [Save & Close]: Marks DailyReflection as finalized, shows streak celebration

---

## View 4: Dashboard / Home View

**Layout**: Card-based grid

```
┌─────────────────────────────────────────────────────────────┐
│ DASHBOARD                                              [≡]   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌───────────────────────────┐  ┌───────────────────────────┐
│  │ TODAY AT A GLANCE         │  │ STREAK TRACKER            │
│  ├───────────────────────────┤  ├───────────────────────────┤
│  │                           │  │                           │
│  │ Scheduled: 3 / 5 tasks    │  │  🔥 7 DAYS PLANNING       │
│  │ Completed: 2 / 3 blocks   │  │    STREAK!                │
│  │ Progress: ██████░░░░ 65%  │  │                           │
│  │                           │  │  Keep it going! 💪        │
│  │ Current:                  │  │                           │
│  │ Fix login bug             │  │                           │
│  │ Time left: 45:30          │  │                           │
│  │                           │  │                           │
│  └───────────────────────────┘  └───────────────────────────┘
│                                                              │
│  ┌───────────────────────────┐  ┌───────────────────────────┐
│  │ UPCOMING DEADLINES        │  │ DEFERRED INBOX            │
│  ├───────────────────────────┤  ├───────────────────────────┤
│  │                           │  │                           │
│  │ 🔴 Fix login bug (Today)  │  │ 3 items waiting           │
│  │    Auth System, P1        │  │                           │
│  │                           │  │ [Review] [Process]        │
│  │ 📅 Design dashboard (Fri) │  │                           │
│  │    UI Project, P1         │  │                           │
│  │                           │  │                           │
│  │ Schedule checkup (Wed)    │  │                           │
│  │    Dental Care, P2        │  │                           │
│  │                           │  │                           │
│  └───────────────────────────┘  └───────────────────────────┘
│                                                              │
│                                                              │
│  QUICK ACTIONS                                              │
│                                                              │
│  [Start Morning Planning]  [Start Evening Clean-up]         │
│  [Capture a thought (Ctrl+Space)]                           │
│                                                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Key Interactions**:
- Cards show key info at a glance
- Streak is prominent (motivational)
- Upcoming deadlines clickable (jump to morning planning)
- Deferred inbox count shows new items waiting
- Quick action buttons for rituals

---

## View 5: Task Details View

**Layout**: Side panel (overlays main content)

```
┌─────────────────────────────────────────────────────────────┐
│ TASK DETAILS                                           [✕]   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Title                                                      │
│  Fix login bug                                              │
│                                                              │
│  Project                                                    │
│  [Auth System ▼]                                            │
│                                                              │
│  Parent Task (if applicable)                                │
│  None                                                       │
│                                                              │
│  Due Date                                                   │
│  [Today ▼]                                                  │
│                                                              │
│  Priority                                                   │
│  [1 ▼]  ████████░░                                          │
│                                                              │
│  Points Estimate                                            │
│  [3 ▼]                                                      │
│                                                              │
│  Status                                                     │
│  [IN_PROGRESS ▼]                                            │
│                                                              │
│  Description                                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ The login endpoint is returning 403 errors           │  │
│  │ when valid credentials are provided. Need to         │  │
│  │ debug the auth middleware.                           │  │
│  │                                                      │  │
│  │                                                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  Child Tasks                                                │
│  ☐ Review login endpoint code                               │
│  ☑ Identify auth bug                                        │
│  ☐ Write unit tests for fix                                 │
│  ☐ Implement fix                                            │
│                                                              │
│  [+ Add child task]                                         │
│                                                              │
│                                                              │
│  [Mark Done]  [Save]  [Delete]                              │
│                                                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Key Interactions**:
- All fields editable (click to edit)
- Dropdowns for project, status, priority, points
- Child tasks shown as checklist; can check off or manage
- [Mark Done]: Quickly mark task complete
- [Save]: Save any changes
- [Delete]: Archive the task

---

## View 6: Quick Capture Modal

**Layout**: Minimal modal overlay (centered)

```
                    ┌─────────────────────────────┐
                    │ CAPTURE A THOUGHT           │
                    ├─────────────────────────────┤
                    │                             │
                    │ What's on your mind?        │
                    │                             │
                    │ ┌──────────────────────────┐│
                    │ │                          ││
                    │ │                          ││
                    │ └──────────────────────────┘│
                    │                             │
                    │   [Save]   [Cancel]         │
                    │                             │
                    │ (Press Enter to save)       │
                    │ (Press Escape to cancel)    │
                    │                             │
                    └─────────────────────────────┘

                    ✓ Saved

         (Modal closes, brief checkmark shown)
```

**Key Interactions**:
- Text input auto-focuses (cursor ready to type)
- Press Enter to save (quick)
- Press Escape to cancel
- Brief "✓ Saved" confirmation, then closes
- Minimal friction, user back to work immediately

---

## Wireframe Navigation Flow

```
Dashboard
├── [Start Morning Planning] → Morning Planning View
│   └── [Confirm plan] → back to Dashboard
│   └── (Users can also adjust their plan by reopening this view mid-day)
├── [Current task] → Active Work Session View
│   └── [Complete/Extend/Done] → back to Dashboard
├── [Start Evening Clean-up] → Evening Clean-up (Phase 1)
│   ├── [Convert/Defer/Dismiss items] → next item
│   └── [All items processed] → Evening Clean-up (Phase 2: Reflection)
│       └── [Save & Close] → Dashboard + celebration
├── [Deferred Inbox Badge] → Evening Clean-up Phase 1 (anytime review)
├── [Capture thought] (Ctrl+Space) → Quick Capture Modal
│   └── [Save] → back to previous screen
├── [Task from list] → Task Details View (side panel)
│   └── [Save/Close] → back to previous screen
└── [Upcoming deadline] → Jump to Morning Planning
```

**Mid-Day Plan Adjustments**: To reassess/adjust your plan during the day, simply return to the Morning Planning View (same interface used for morning planning). Drag tasks around, add/remove blocks, and save your changes. No separate modal needed.

---

## Design Notes for Implementation

1. **Top/Bottom Split (Morning Planning)**: Use CSS Grid or Flexbox with resizable divider if needed. Top panel scrolls horizontally for projects.

2. **Full-screen Focus (Active Work)**: No sidebar/nav visible. Timer is the focal point. Escape key to exit.

3. **Stack of Cards (Deferred Items)**: Show one card at a time. Previous/next buttons, or just move to next automatically on action.

4. **Cards (Dashboard)**: Responsive grid, stacks on mobile.

5. **Side Panel (Task Details)**: Slide in from right; click X or outside to close. Can be modal or persistent panel.

6. **Modal (Quick Capture & Plan Adjustment)**: Center overlay, semi-transparent background, Escape to close.

7. **Colors**: Use project colors to identify tasks throughout (Auth System = blue, UI Project = purple, Dental Care = green, etc.)

8. **Animations**:
   - Subtle slide/fade transitions between views
   - Celebration animation on subtask completion (confetti, brief shine)
   - Checkmark animation on Quick Capture save

9. **Accessibility**: All form fields labeled, keyboard navigation supported, screen reader friendly

10. **Responsive**: Mobile-first approach; adjust layouts for phone/tablet (stack views vertically, hide non-essential info)
