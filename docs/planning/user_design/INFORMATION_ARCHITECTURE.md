# Phase 5: Information Architecture & Content Inventory

**Purpose**: Define what data is essential on each view, how information is organized, and what can be hidden or deferred.

---

## Core Entities & Their Relationships

### Entities

**app_user**
- id (UUID), email, password_hash, display_name, timezone
- created_at, updated_at

**Project** (flat list)
- id, user_id, name, description
- color (hex), icon
- is_active (boolean, default true)
- sort_order, archived_at
- created_at, updated_at

**Task** (supports hierarchy via parent_task_id)
- id, project_id (required, non-nullable), user_id, title, description
- parent_task_id (nullable; null for top-level tasks, references parent for child tasks)
- **Rule**: Child tasks must have the same project_id as their parent. Enforced in service layer; cascades on parent project change.
- status (TODO, IN_PROGRESS, BLOCKED, DONE, SKIPPED)
- priority (1-5)
- points_estimate (1-5 complexity, deliberately vague)
- actual_minutes (time spent)
- energy_level (LOW/MEDIUM/HIGH)
- due_date
- sort_order (for ordering within parent)
- blocked_by_task_id (self-reference for dependencies)
- archived_at, completed_at, created_at, updated_at

**DailyReflection**
- id, user_id, reflection_date (unique per user per date)
- energy_rating (1-5), mood_rating (1-5)
- reflection_notes (optional free text)
- is_finalized (whether evening ritual is complete)
- created_at, updated_at

**TimeBlock**
- id, user_id, block_date (date of the block)
- task_id (nullable for breaks/buffers)
- block_type (WORK, BREAK, BUFFER, ADMIN)
- start_time, end_time (clock times: 9:00 AM, 10:30 AM, etc.)
- actual_start, actual_end (timestamps when user actually started/ended)
- was_completed (boolean: completed or done for now/skipped)
- sort_order (chronological order within the day)
- created_at, updated_at

**DeferredItem**
- id, user_id, raw_text
- is_processed, captured_at, processed_at
- resolved_task_id, resolved_project_id (nullable)
- deferred_until_date (nullable, for re-queueing to future evening ritual: 1 day / 1 week / 1 month)
- deferral_count (how many times deferred)
- created_at, updated_at

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
- Task dependencies
- Task history

**Ordering Logic**:
Tasks within each project column are ordered:
1. Today's deadlines (sorted by priority 1-5, highest first)
2. This week's deadlines (sorted by priority 1-5, highest first)
3. Everything else / no deadline (sorted by priority 1-5, highest first)

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
- Child tasks list with checkboxes
- Current time block duration

**Hidden/Secondary**:
- Task description (could be revealed in a side panel if user clicks "Details")
- Task notes
- Project name (already context from planning)
- Other tasks in the plan

**Actions**:
- Check off subtasks
- "Complete" / "Extend" / "Done for now" buttons
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
- Project (dropdown to change; only for top-level tasks)
- Parent task (if this is a child task; dropdown to change or remove)
- Due date
- Priority (1-5 dropdown, used for ordering)
- Points estimate
- Status (dropdown)
- Description
- Child tasks list (with add/remove/reorder)
- Notes field (blockers, context, etc.)

**Hidden/Secondary**:
- Task history (created_at, last modified)
- Task priority (not prominently shown, implied by deadline urgency)
- Dependent tasks (future feature)
- Time spent (actual_minutes, available after completion)

**Actions**:
- Edit any field
- Add/remove/reorder child tasks
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

## Navigation Structure

### Primary Navigation (Sidebar or Bottom Nav)
- **Dashboard** — Home/overview
- **Today** — Morning planning + active work (mid-day adjustments reuse Morning Planning view)
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
- "Task" — A unit of work that can be broken down into child tasks
- "Subtask" — A child task (same entity as task, linked via parent_task_id)
- "Time Block" — A scheduled period to work on a task
- "Deferred Item" — A quick note captured for later processing
- "Daily Plan" — Your schedule for today

**For Developers**:
- `project_id` / `task_id` / `parent_task_id` / `daily_plan_id` — Foreign keys
- `time_block` vs `TimeBlock` — Database vs code entity
- `is_processed`, `is_done` — Boolean flags
- `deferral_count` — Integer counter

---

## Entity Relationships

```
app_user
├── Projects (multiple)
│   └── Tasks (multiple, top-level: parent_task_id is null)
│       └── Child Tasks (via parent_task_id, recursive)
├── DeferredItems (multiple, unprocessed captures)
└── Per date:
    ├── TimeBlocks (multiple)
    │   ├── One per scheduled work/break/buffer block
    │   └── Reference task_id (nullable for breaks)
    └── DailyReflection (one)
        ├── Energy/mood ratings
        ├── Reflection notes
        └── is_finalized flag
```

## Information Hierarchy Summary

```
ESSENTIAL (show always):
├── What user is working on now (active task)
├── How much time is left (timer)
├── What needs to happen today (schedule via TimeBlocks)
└── How they're doing (mood/energy from DailyReflection)

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
