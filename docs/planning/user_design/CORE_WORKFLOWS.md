# Phase 4: Core Workflows & Views

**Purpose**: Map use cases to the key screens/views the app needs, and define state transitions.

---

## Critical Views (Must-Build for MVP)

These are the 6 most important screens/interactions users see daily.

### 1. Morning Planning View

**Purpose**: Execute Use Case 1 (Morning Planning Ritual)
**When**: User opens app in morning
**Layout**: Two-panel (top/bottom split)

**Top Panel: Task Browser (Table View by Project)**
- **Column headers**: Project names across the top (Projects can be traditional work projects like "Auth System" or life categories like "Dental Care")
- **Under each project**: List of tasks for that project
- **For each task**:
  - Task name
  - Due date (displayed next to name)
  - **Visual deadline indicators**:
    - Large icon/badge (🔴 or "TODAY") for tasks due today
    - Small icon/badge (📅 or "THIS WEEK") for tasks due this week
  - Points estimate
- User selects tasks and clicks "Add to calendar" to auto-place them
- User can then drag tasks around to fine-tune order

**Bottom Panel: Today's Calendar**
- Visual representation of work day (e.g., 9am-5pm)
- Empty time blocks to fill
- User drags tasks from top panel into these blocks
- System auto-suggests break blocks between work blocks
- "Confirm plan" button at bottom
- **No hard task limit** (max_daily_tasks removed from MVP)

**User Flow**:
1. User internally reflects on "What are my 1-3 top priorities today?" (grounding exercise, internal thinking)
2. User browses tasks in the table and selects which ones they want to work on
3. User clicks "Add to calendar" to auto-place selected tasks on the calendar below
4. User manually drags tasks around the calendar to arrange them in desired order
5. When accessed mid-day: completed blocks shown greyed out, remaining blocks still editable

**End state**: DailyReflection created with TimeBlocks for the day

---

### 2. Active Work Session View

**Purpose**: Execute Use Case 4 (Active Work Session)
**When**: User clicks "Start" on a time block
**Layout**: Full-screen focus mode

**Header**:
- Task title (prominent)
- Timer (countdown or countup, depending on user setting)
- Progress bar (% of time elapsed)

**Middle**:
- Child tasks as a checklist
- User can check off subtasks as they complete them
- Each completion shows a small animation/celebration

**Footer**:
- Action buttons: "Complete", "Extend", "Done for now"

**At end of time block**:
- **Single gentle chime** at 100% + "Time's up. Good work!"
- No 80% warning, no customizable reminders (Phase 2+)

**End state**: User chooses Complete/Extend/Done for now, actual_minutes recorded

---

### 3. Evening Clean-up View

**Purpose**: Execute Use Case 3 (Evening Clean-up Ritual)
**When**: End of work day
**Layout**: Two-phase ritual

**Phase 1: Deferred Items Processing**

Sub-view: Inbox
- Shows count: "You have 5 items in your inbox"
- Items displayed as a stack of cards (one visible at a time)
- Each card shows: raw text of the deferred item

Action buttons for each item:
- "Convert to task" → opens mini-form to create a task in selected project
  - Pre-filled with deferred item text as title
  - Optional fields: description, due date, priority, points estimate, child tasks
  - All fields optional (user can save with just title)
- "Defer" → select 1 day / 1 week / 1 month → item re-queued
- "Dismiss" → item is marked processed, removed

If "Convert to task":
- Mini-form appears with optional fields:
  - Project (required, dropdown to select which project)
  - Title (pre-filled with deferred item text, editable)
  - Description (optional)
  - Due date (optional)
  - Priority (optional, defaults to 3)
  - Points estimate (optional)
  - Add child tasks? (optional toggle)
- Save → task created with filled fields, item marked processed, move to next item

After all items processed (or deferred):
- "Inbox Zero! Great work! 🎉"

**Phase 2: End-of-Day Reflection**

Sub-view: Reflection Form
- "How was your day?"
- Energy level: 1-5 slider
- Mood: 1-5 slider
- Reflection notes: optional free text
- "Completed tasks to celebrate" section
  - System shows all completed time blocks from today
  - User can add notes about what was significant
- "Save" button

After saving:
- Daily plan marked as finalized
- Streak updated: "7-day planning streak! 🎉"
- Gentle prompt: "Rest well, see you tomorrow"

**End state**: Deferred items processed, reflection saved, day is closed out

---

### 4. Dashboard / Home View

**Purpose**: Daily landing page, quick status overview
**When**: User opens app (not during morning/evening rituals)
**Layout**: Card-based dashboard

**Cards**:
- **Today at a Glance**: Shows today's time blocks, completed %, current task (if active)
- **Streak Tracker**: "7-day planning streak!" with visual indicator
- **Upcoming Deadlines**: Next 3-5 tasks with deadlines (soonest first)
- **Deferred Inbox Badge**: "3 items waiting" (clickable to review anytime)
- **Quick Actions**:
  - "Start morning planning"
  - "Start evening clean-up"
  - "Capture a thought" (Ctrl+Space)

**Visual style**: Calm, spacious, encouraging

---

### 5. Task Details View

**Purpose**: View/edit a single task
**When**: User clicks on a task from any list
**Layout**: Side panel or modal

**Fields**:
- Task name
- Description
- Project (dropdown)
- Points estimate
- Due date
- Priority (1-5)
- Status (TODO / IN_PROGRESS / BLOCKED / DONE / SKIPPED)
- Child tasks list (can add/remove/reorder)
- Notes field (for storing work context, blockers, etc.)

**Actions**:
- Edit any field
- Create/manage subtasks
- Move to different project
- Mark done
- Add note

---

### 6. Quick Capture Modal

**Purpose**: Execute low-friction capture (Use Case 2)
**When**: User presses Ctrl+Space or clicks floating button
**Layout**: Modal that appears over current screen

**Content**:
- Text input: "What's on your mind?"
- Auto-focus (cursor is ready)

**Actions**:
- Hit Enter to save
- Hit Escape to cancel

**Confirmation**:
- Brief visual checkmark: "✓ Saved"
- Optional soft chime (customizable)
- Modal closes immediately

**End state**: Deferred item created, user back to their work

---

## State Transitions

How the app flows between views:

```
Dashboard
├── Morning Planning (top/bottom split)
│   └── confirm → creates DailyReflection
│       └── navigate to Dashboard or Active Work
│   └── (mid-day: reopen via sidebar to adjust plan)
├── Active Work Session (from time block)
│   └── Complete/Extend/Done for now → updates session
│       └── back to Dashboard or next block
├── Quick Capture Modal (Ctrl+Space)
│   └── Enter → creates DeferredItem
│       └── back to previous screen
├── Evening Clean-up (from "Evening" menu or prompt)
│   ├── Deferred Inbox
│   │   ├── Convert/Defer/Dismiss each item
│   │   └── "Inbox Zero" → move to Reflection
│   └── Reflection Form
│       └── Save → finalize DailyReflection, celebrate
├── Task Details View (from any task list)
│   └── Edit/Save → back to previous screen
└── Dashboard
    └── Cycle repeats tomorrow
```

---

## Key Design Decisions

1. **Two-panel Morning Planning**: Task browser on top, calendar below, drag-and-drop between them. This makes prioritization and scheduling visual and tactile.

2. **One-at-a-time Deferred Items**: Stack-of-cards UI prevents overwhelming users with too many decisions at once.

3. **Full-screen Active Work**: Focus mode minimizes distractions, timer is prominent.

4. **Streak Celebration on Dashboard**: Visible reminder of consistency, motivational.

5. **Quick Capture Modal**: Stays in focus, minimal friction, disappears immediately after save.

6. **Plan Adjustment via Morning Planning**: Reuse the same view for mid-day adjustments — no separate view needed. Completed blocks shown greyed out for context.

---

## Information Architecture

**Top-level navigation** (sidebar or bottom nav):
- Dashboard (home)
- Today (morning planning + active work)
- Projects (view/manage projects)
- Tasks (browse all tasks)
- Inbox (deferred items, anytime review)

**Within-screen navigation**: Modal/panel overlays for details, quick capture

---

## ADHD-Specific UX Decisions

1. **Overwhelm Prevention**: Showing only today's tasks + next week visible; long-term items exist but not cluttering view
2. **Quick Capture**: Floating button + Ctrl+Space, zero required fields, instant confirmation
3. **Visual Time Blocking**: See the day visually; easier than task lists to understand what fits
4. **Active Task Timer**: Single gentle chime at 100% (no 80% warning, not customizable)
5. **One-at-a-Time Processing**: Deferred items shown as stack, not list; Inbox Zero celebration
6. **Dopamine-Friendly Progress**: Subtask completion animations, streak counter, weekly summaries
7. **Flexible Pivoting**: Encouraged to adjust plan mid-day; "Done for now" vs "Complete" distinction
8. **Minimal Navigation Depth**: Everything reachable in 2 clicks from sidebar
9. **Supportive Tone**: "Done for now" not "Skip"; "Time's up. Good work!" not "Timer expired"
10. **Calm Visual Hierarchy**: One clear action at a time; group related info, hide non-essential details
11. **Visual Confirmation**: Every action has feedback (checkmark, celebration, streak update)
12. **Prevent Decision Paralysis**: Morning planning guides you through steps; evening ritual processes one item at a time

---

## Views Summary Table

| View | Purpose | Frequency | Key Interaction |
|---|---|---|---|
| Dashboard | Daily overview, status | Every open | See streak, deadlines, current task |
| Morning Planning | Create/adjust day's plan | 1x/day + as needed | Drag tasks to calendar |
| Active Work Session | Focus on task with support | Multiple/day | Start/complete/extend/done for now |
| Evening Clean-up | Process inbox & reflect | 1x/day | Convert/defer/dismiss items |
| Quick Capture | Jot thought | Many/day | Type + Enter |
| Task Details | View/edit task | As needed | Edit fields, manage subtasks |
