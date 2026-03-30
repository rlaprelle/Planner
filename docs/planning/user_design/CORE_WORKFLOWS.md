# Phase 4: Core Workflows & Views

**Purpose**: Map use cases to the key screens/views the app needs, and define state transitions.

---

## Critical Views (Must-Build for MVP)

These are the 5-7 most important screens users see daily.

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
- Shows current task count vs max_daily_tasks limit
- "Confirm plan" button at bottom

**User Flow**:
1. User internally reflects on "What are my 1-3 top priorities today?" (grounding exercise, internal thinking)
2. User browses tasks in the table and selects which ones they want to work on
3. User clicks "Add to calendar" to auto-place selected tasks on the calendar below
4. User manually drags tasks around the calendar to arrange them in desired order

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
- Customizable reminders: water, food, break/exercise (inline notifications that don't break focus)

**During Session**:
- At 80% time (customizable): gentle notification "X minutes left"
- At 100% time: gentle chime + "Time's up. Good work!"
- Reminders appear as gentle notifications (not intrusive)

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
- "Convert to task" → opens mini-form
- "Add to project" → pick existing project
- "Defer" → select 1 day / 1 week / 1 month → item re-queued
- "Dismiss" → item is marked processed, removed

If "Convert to task":
- Mini-form appears: project dropdown, "Add subtasks?" toggle, points estimate
- Save → task created, item marked processed, move to next item

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

### 7. Plan Adjustment View

**Purpose**: Execute mid-day plan changes (Use Case 5)
**When**: User clicks "Reassess plan" during the day
**Layout**: Modal or panel showing today's remaining plan

**Content**:
- Current time blocks (completed ones greyed out)
- Remaining time blocks with tasks
- Dropdown: "What's changed?"
  - Task blocked
  - New request
  - Feeling overwhelmed
  - Low energy
  - Other

**System suggestions** (based on selection):
- "Swap Task A for Task X?"
- "Remove one task to recover energy?"
- "Take a longer break?"

**Trade-offs shown**:
- "If you skip Task C, you'll miss the 3pm deadline"
- "If you pivot to Task X, Task A moves to tomorrow"

**Actions**:
- "Make this change" → plan updates, celebration shown
- "Keep current plan"
- Optional: Add note to task ("Blocked, will revisit tomorrow")

**End state**: Plan adjusted, user encouraged, clear about trade-offs

---

## State Transitions

How the app flows between views:

```
Dashboard
├── Morning Planning (top/bottom split)
│   └── confirm → creates DailyReflection
│       └── navigate to Dashboard or Active Work
├── Active Work Session (from time block)
│   └── Complete/Extend/Done → updates session
│       └── back to Dashboard or next block
├── Plan Adjustment (from "Reassess" button)
│   └── confirm change → updates DailyReflection
│       └── back to Dashboard
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

6. **Plan Adjustment with Trade-offs**: Shows consequences of changes so user is conscious of what they're deprioritizing.

---

## Information Architecture

**Top-level navigation** (sidebar or bottom nav):
- Dashboard (home)
- Today (morning planning + active work)
- Areas (view/manage areas)
- Projects (view/manage projects)
- Tasks (browse all tasks)
- Inbox (deferred items, anytime review)

**Within-screen navigation**: Modal/panel overlays for details, quick capture, plan adjustment

---

## Accessibility & ADHD Considerations

- **Calm visual hierarchy**: One clear action at a time
- **Reduce cognitive load**: Group related info, hide non-essential details
- **Visual confirmation**: Every action has feedback (checkmark, celebration, streak update)
- **Prevent decision paralysis**: Morning wizard guides you through steps; evening ritual processes one item at a time
- **Flexible interaction**: Can always change the plan, always capture thoughts, always review inbox

---

## Views Summary Table

| View | Purpose | Frequency | Key Interaction |
|---|---|---|---|
| Dashboard | Daily overview, status | Every open | See streak, deadlines, current task |
| Morning Planning | Create day's plan | 1x/day | Drag tasks to calendar |
| Active Work Session | Focus on task with support | Multiple/day | Start/complete/extend/done |
| Evening Clean-up | Process inbox & reflect | 1x/day | Convert/defer/dismiss items |
| Plan Adjustment | Adapt plan mid-day | As needed | Swap tasks, show trade-offs |
| Quick Capture | Jot thought | Many/day | Type + Enter |
| Task Details | View/edit task | As needed | Edit fields, manage subtasks |
