# Phase 5: Information Architecture & Content Inventory

**Purpose**: Define what data is essential on each view, how information is organized, and what can be hidden or deferred.

---

## Core Entities & Their Relationships

### Entities

**Project**
- id, name, description
- color (hex), icon
- sort_order, is_active, archived_at
- created_at, updated_at

**Task**
- id, title, description
- project_id (which project this belongs to)
- status (TODO, IN_PROGRESS, BLOCKED, DONE, SKIPPED)
- priority (1-5)
- points_estimate (1-5 or higher, deliberately vague)
- due_date
- created_at, completed_at, archived_at
- blocked_by_task_id (self-reference for dependencies)
- is_recurring, recurrence_rule (future)

**Subtask**
- id, task_id, title
- is_done, completed_at
- sort_order, created_at

**DailyPlan**
- id, user_id, plan_date (unique per user per date)
- energy_rating, mood_rating, reflection_notes
- is_finalized
- created_at, updated_at

**TimeBlock**
- id, daily_plan_id, task_id (nullable for breaks)
- block_type (WORK, BREAK, BUFFER, ADMIN)
- start_time, end_time
- actual_start, actual_end, was_completed
- notes, sort_order
- created_at, updated_at

**DeferredItem**
- id, user_id, raw_text
- is_processed, captured_at, processed_at
- resolved_task_id, resolved_project_id (nullable)
- deferral_count (how many times deferred)
- created_at

---

## Information Architecture by View

### 1. Morning Planning View

**Essential Information**:
- **Top Panel: Task Browser**
  - Project name (column header)
  - Task name
  - Due date
  - Visual deadline badge (TODAY / THIS WEEK)
  - Points estimate
  - Checkbox to select

**Hidden/Secondary**:
- Task description
- Task priority (implied by project organization)
- Task dependencies
- Task history

**Actions**:
- Select/deselect task
- Click task for details panel
- "Add to calendar" button
- Drag tasks to calendar

---

### 2. Active Work Session View

**Essential Information**:
- Task title (prominent)
- Timer (countdown/countup)
- Progress bar (% of block time elapsed)
- Subtasks list with checkboxes
- Current time block duration

**Hidden/Secondary**:
- Task description (could be revealed in a side panel if user clicks "Details")
- Task notes
- Project name (already context from planning)
- Other tasks in the plan

**Actions**:
- Check off subtasks
- "Complete" / "Extend" / "Done for now" buttons
- Customizable reminders (water, food, break) appear as notifications
- Click task title to see full details (optional)

---

### 3. Evening Clean-up View

#### Phase 1: Deferred Items Processing

**Essential Information**:
- Deferred item count ("You have 5 items")
- Raw text of current item (one at a time)
- Action buttons: Convert / Defer / Dismiss
- If converting:
  - Project dropdown
  - "Add subtasks?" toggle
  - Points estimate field
  - Due date field

**Hidden/Secondary**:
- Item creation date
- Number of times deferred
- Other deferred items (shown one at a time to avoid overwhelm)

**Actions**:
- Convert to task
- Defer (1 day / 1 week / 1 month)
- Dismiss
- Move to next item

#### Phase 2: End-of-Day Reflection

**Essential Information**:
- Energy level slider (1-5)
- Mood slider (1-5)
- Reflection notes text field
- Completed time blocks from today (auto-populated)
- User can add notes about significance

**Hidden/Secondary**:
- Incomplete tasks (not shown in reflection)
- Future plans (only today matters)
- Statistics (save that for dashboard)

**Actions**:
- Adjust sliders
- Type reflection
- Add notes to completed tasks
- Save

**Celebration**: After save, show streak update and gentle message

---

### 4. Dashboard / Home View

**Essential Information**:
- **Today at a Glance**: Today's scheduled time blocks, % completed, current task (if active)
- **Streak Tracker**: "7-day planning streak! 🎉" with visual indicator
- **Upcoming Deadlines**: Next 3-5 tasks due soonest (project + task name + due date)
- **Deferred Inbox Badge**: Count of unprocessed items (e.g., "3 items waiting")
- **Quick Action Buttons**:
  - "Start morning planning"
  - "Start evening clean-up"
  - "Capture a thought" (Ctrl+Space)

**Hidden/Secondary**:
- Past tasks/completed tasks (could be accessed via stats later)
- Long-term projects (future)
- Weekly/monthly summaries (Phase 2+)

**Actions**:
- Click on task to view details
- Click on deadline to jump to morning planning
- Click on streak to see history (future)
- Click quick action buttons

---

### 5. Task Details View

**Essential Information** (when user clicks on a task):
- Task title
- Project (dropdown to change)
- Due date
- Points estimate
- Status (dropdown)
- Description
- Subtasks list (with add/remove/reorder)
- Notes field (blockers, context, etc.)

**Hidden/Secondary**:
- Task history (created_at, last modified)
- Task priority (not prominently shown, implied by deadline urgency)
- Dependent tasks (future feature)
- Time spent (actual_minutes, available after completion)

**Actions**:
- Edit any field
- Add/remove/reorder subtasks
- Mark done
- Change status
- Add notes
- Close

---

### 6. Quick Capture Modal

**Essential Information**:
- Text input with placeholder "What's on your mind?"
- Nothing else

**Hidden/Secondary**:
- All optional metadata
- Projects (not shown)
- Categories (not shown)

**Actions**:
- Type and hit Enter to save
- Hit Escape to cancel

**Feedback**: Brief checkmark confirmation "✓ Saved"

---

### 7. Plan Adjustment View

**Essential Information**:
- Current time blocks (completed greyed out)
- Remaining time blocks with task names
- "What's changed?" dropdown:
  - Task blocked
  - New request
  - Feeling overwhelmed
  - Low energy
  - Other
- System suggestions based on selection
- Trade-offs display ("If you skip X, you'll miss Y deadline")

**Hidden/Secondary**:
- Task details (only names shown)
- Full project view
- Historical data

**Actions**:
- Select what changed
- Accept/reject suggestion
- Optional note field
- "Make this change" button

---

## Navigation Structure

### Primary Navigation (Sidebar or Bottom Nav)
- **Dashboard** — Home/overview
- **Today** — Morning planning + active work + plan adjustment
- **Projects** — View/manage projects
- **Tasks** — Browse all tasks (future, or searchable from morning planning)
- **Inbox** — Deferred items, anytime review

### Secondary Navigation (Within Views)
- Modal overlays for task details
- Quick capture modal (Ctrl+Space, always available)
- Settings (gear icon, access user preferences)

---

## Information Density by View

| View | Information Density | Rationale |
|---|---|---|
| Morning Planning | **Medium** | Need to see all options (projects + tasks) but keep visual clean |
| Active Work | **Low** | Focus mode: only current task, timer, subtasks |
| Evening Clean-up | **Low** (one at a time) | Prevent overwhelm by processing deferred items sequentially |
| Dashboard | **Medium** | Overview of day + upcoming + streak, but not cluttered |
| Task Details | **High** | User clicked for details, can handle full information |
| Quick Capture | **Minimal** | Zero friction: just a text box |
| Plan Adjustment | **Medium** | Show current plan + options, but focused on change |

---

## Content Visibility Rules

**Always Visible**:
- Current task/block info (when active)
- Streak indicator
- Deferred inbox count
- Quick capture button (Ctrl+Space)

**Visible by Default, Can Hide**:
- Upcoming deadlines
- Projects in task browser
- Completed time blocks in dashboard

**Hidden by Default, Can Show**:
- Task descriptions
- Full task notes
- Historical data
- Weekly/monthly stats (Phase 2+)

**Never Visible (Out of Scope)**:
- Future projects/tasks beyond next week
- Long-term planning UI (reserved for future)
- Estimation accuracy graphs (Phase 2+)

---

## Naming Conventions

**For Users**:
- "Project" — A bucket for related work (e.g., "Auth System", "Dental Care")
- "Task" — A unit of work that can be broken down
- "Subtask" — A small step within a task
- "Time Block" — A scheduled period to work on a task
- "Deferred Item" — A quick note captured for later processing
- "Daily Plan" — Your schedule for today

**For Developers**:
- `project_id` / `task_id` / `daily_plan_id` — Foreign keys
- `time_block` vs `TimeBlock` — Database vs code entity
- `is_processed`, `is_done` — Boolean flags
- `deferral_count` — Integer counter

---

## Information Hierarchy Summary

```
ESSENTIAL (show always):
├── What user is working on now (active task)
├── How much time is left (timer)
├── What needs to happen today (schedule)
└── How they're doing (mood/energy at end of day)

IMPORTANT (show prominently):
├── What's due soon (deadlines)
├── How many items captured (inbox count)
├── Consistency rewards (streak)
└── Available tasks to choose from (projects/tasks in morning)

SECONDARY (show if asked):
├── Task details
├── Project descriptions
├── Notes and blockers
└── Historical performance data

NOT YET (Phase 2+):
├── Weekly/monthly reviews
├── Estimation accuracy
├── Recurring task management
└── Long-term planning
```

---

## Accessibility & Cognitive Load Notes

- **One task at a time**: Morning planning shows tasks grouped by project, evening processing shows deferred items one at a time
- **Visual hierarchy**: Important info (deadline badges, streak) is prominent; secondary info (descriptions, notes) is accessible but not overwhelming
- **Consistent locations**: Deferred inbox count always in same spot; quick capture always available (Ctrl+Space); streak always visible on dashboard
- **Progressive disclosure**: Click "Details" to see more; don't show everything at once
- **Clear calls to action**: Every view has 1-3 primary actions (e.g., "Select tasks" → "Add to calendar" → "Arrange")
