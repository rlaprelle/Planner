# Planner - Architecture & Design Document

## Project Vision

**The Planner is a daily mindfulness and work management tool designed for ADHD brains.** It helps users ground themselves in the present, capture distractions without losing focus, and complete meaningful work on schedule — while celebrating genuine accomplishment and building sustainable routines.

---

## Core Design Principles

1. **Ritual & Routine**: Morning planning is a 5-10 minute guided ceremony (step-by-step, but natural and conversational)
2. **Calm & Simple Interface**: Visual simplicity is non-negotiable
3. **Ubiquitous Escape Hatch**: Deferred items (jot-a-note) available in almost every flow — the core affordance that prevents feeling trapped
4. **Capture & Forget**: Once captured, user can mentally let go; system holds and processes it
5. **Organized But Invisible**: Future items exist but don't clutter the current view
6. **Cadenced Reviews**: Guided end-of-day processing, plus daily/weekly/monthly/yearly reviews to consciously organize
7. **Multidimensional Accomplishment**: Celebrate tasks by effort + impact + difficulty, not just completion
8. **Flexible Prioritization**: Set rough priorities, but actively encourage conscious plan changes with visible trade-offs
9. **Distraction Handling**: Deferred items let users capture anything without losing focus
10. **Friendly Encouragement**: Supportive presence during work, not a demanding manager

---

## MVP Scope

### Must-Have Features
- ✅ Guided morning planning ritual (5-10 min, step-by-step)
- ✅ Guided evening clean-up ritual: process deferred items, end-of-day reflection
- ✅ Deferred items: jot-a-note with visual/audio confirmation, anytime review
- ✅ Visual time blocking: see your day laid out in time blocks
- ✅ Points-based task estimation (not time-based)
- ✅ Calm, simple, uncluttered interface
- ✅ Streak tracking + weekly/monthly summaries
- ✅ Intelligent celebration (by hours, complexity, effort)
- ✅ Deadline tracking as prioritization signal
- ✅ Flexible plan changes with visible trade-offs

### Deferred to Phase 2+ (Post-MVP)
- Daily task limits (max_daily_tasks)
- Customizable timer reminders (break reminders, water/food prompts)
- Notes capture during active work sessions
- Task breakdown assistance ("just make it smaller" + optional help)
- Recurring task templates
- LLM-integrated morning ritual (conversational coach)
- Estimation accuracy learning (estimated_minutes tracking)
- Weekly/monthly/yearly review ceremonies
- Multi-user team features
- Advanced analytics & insights

---

## 5 Core User Flows (MVP)

### 1. Morning Planning Ritual (5-10 min)
**User Goal**: Ground themselves and plan their day

**Flow**:
1. User opens app → navigates to Morning Planning
2. **Internal reflection** (internal, no UI): "What are my 1-3 top priorities today?"
3. User browses tasks table (grouped by project, ordered by deadline then priority)
4. User selects tasks they want to work on today
5. User clicks "Add to calendar" → selected tasks auto-placed on today's timeline
6. User drags tasks around calendar to arrange them
7. User confirms plan → DailyReflection created with TimeBlocks
8. Morning planning complete

**Key Design**:
- Task browser: Projects in columns, tasks listed under project with deadline badges (TODAY, THIS WEEK)
- Visual calendar: Vertical timeline, drag-and-drop rearrangement
- **No hard task limit** (max_daily_tasks removed)

---

### 2. Capture Distraction (10-30 sec, anytime)
**User Goal**: Jot down ideas/distractions without losing focus

**Flow**:
1. User hits **Ctrl+Space** or clicks floating button
2. Modal appears: "What's on your mind?"
3. User types unstructured text
4. User hits Enter
5. System: Brief visual checkmark + audio confirmation ("✓ Saved")
6. Modal closes immediately
7. Item added to deferred_items table, ready for evening processing

**Key Design**:
- Minimal friction: text only, no required fields
- Instant confirmation: visual + audio, immediate closure
- Always accessible: button visible in all views, Ctrl+Space hotkey

---

### 3. Evening Clean-up Ritual (10-15 min, daily)
**User Goal**: Process captured ideas and reflect on the day

**Sub-flow 3a: Process Deferred Items**
1. User accesses Evening Clean-up view
2. System shows count: "You have 5 items to process"
3. Items shown one-by-one (stack of cards)
4. For each item:
   - Display raw text
   - Options: (a) Convert to task, (b) Defer, (c) Dismiss
   - If defer: user selects 1 day / 1 week / 1 month → item re-queued for that date's evening ritual
   - If convert: mini-form with Project, Title (pre-filled), Description, Due date, Priority, Points, Child tasks toggle
5. After processing all items: "Inbox Zero! 🎉"

**Sub-flow 3b: End-of-Day Reflection**
1. User sees: "How was your day?"
2. Form: Energy 1-5 slider, Mood 1-5 slider, Reflection notes
3. Optional: "Completed tasks to celebrate" section
4. User saves → DailyReflection finalized, Streak updated

**Key Design**:
- One item at a time (never overwhelming list view)
- Simple actions: Convert/Defer/Dismiss only (no "Add to Project")
- Deferred items tracked with re-queue date (deferred_until_date)
- Streak updates on reflection completion

---

### 4. Active Work Session (time block duration)
**User Goal**: Focus on single task with gentle support

**Flow**:
1. User clicks "Start" on a time block
2. Active Work Session view: Task title, Timer, Progress bar
3. Child tasks displayed as interactive checklist
4. User works...
5. At 100% time: **Gentle chime** + "Time's up. Good work!"
6. User can:
   - Click "Complete" → marks task done, updates task.actual_minutes
   - Click "Extend" → adds time block duration
   - Click "Done for now" → marks incomplete, ready to defer/reschedule
7. Backend updates TimeBlock with actual_start, actual_end, was_completed

**Key Design**:
- **Single gentle chime at 100%** (no 80% warning, no customizable reminders - Phase 2+)
- **No notes capture during work** (deferred to Phase 2+)
- Prominent timer persists across navigation
- Full-screen focus mode available

---

### 5. Plan Adjustment (mid-day, 5-10 min)
**User Goal**: Reassess and adjust plan based on reality

**Flow**:
1. User realizes plan needs adjustment mid-day
2. User accesses Morning Planning view via **sidebar menu option** (or quick action)
3. Morning Planning View shows:
   - All today's time blocks
   - **Completed blocks greyed out** (visual reference of what happened)
   - Remaining blocks still editable
4. User drags/rearranges remaining blocks as needed
5. User confirms updated plan
6. Plan saves and updates

**Key Design**:
- **Reuses Morning Planning View** (no separate Plan Adjustment View)
- Completed blocks visible but greyed out for context
- Same drag-and-drop interaction as morning planning

---

## 6 Critical Views (MVP)

1. **Morning Planning View** — Task browser (top) + visual calendar (bottom), supports both morning planning and mid-day adjustment
2. **Active Work Session View** — Full-screen focus, timer (100% chime only), child tasks checklist
3. **Evening Clean-up View** — Deferred items stack (one-at-a-time) + reflection form
4. **Dashboard / Home View** — Today overview, streak tracker, upcoming deadlines, quick actions
5. **Task Details View** — View/edit single task with child tasks
6. **Quick Capture Modal** — Ctrl+Space, text input, instant confirmation

---

## Data Model (PostgreSQL)

### Core Entities

**app_user**
- id (UUID), email, password_hash, display_name, timezone
- created_at, updated_at

**project** (concrete project, flat list)
- id, user_id, name, description
- color (hex), icon
- is_active (boolean, default true)
- sort_order, archived_at
- created_at, updated_at

**task** (actionable work item, supports hierarchy via parent_task_id)
- id, project_id (required, non-nullable), user_id, title, description
- parent_task_id (nullable; null for top-level tasks, references parent for child tasks)
- **Rule**: Child tasks must have the same project_id as their parent. Enforced in service layer; cascades on parent project change.
- status (TODO/IN_PROGRESS/BLOCKED/DONE/SKIPPED)
- priority (1-5), points_estimate (1-5 complexity), actual_minutes (time spent)
- energy_level (LOW/MEDIUM/HIGH), due_date
- sort_order (for ordering within parent)
- blocked_by_task_id (self-reference: "blocked by another task")
- archived_at, completed_at, created_at, updated_at

**deferred_item** (quick capture)
- id, user_id, raw_text
- is_processed, captured_at, processed_at
- resolved_task_id, resolved_project_id (nullable)
- deferred_until_date (nullable, for re-queueing to future evening ritual)
- deferral_count (how many times deferred)
- created_at, updated_at

**daily_reflection** (end-of-day ritual)
- id, user_id, reflection_date (unique per user per date)
- energy_rating (1-5), mood_rating (1-5)
- reflection_notes (optional)
- is_finalized (whether evening ritual is complete)
- created_at, updated_at

**time_block** (scheduled work)
- id, user_id, block_date
- task_id (nullable for breaks/buffers)
- block_type (WORK/BREAK/BUFFER/ADMIN)
- start_time, end_time (clock times)
- actual_start, actual_end (timestamps)
- was_completed (boolean)
- sort_order (chronological)
- created_at, updated_at

---

## Task Ordering Logic (Morning Planning)

Tasks within each project column are ordered:
1. **Today's deadlines** (sorted by priority 1-5, highest first)
2. **This week's deadlines** (sorted by priority 1-5, highest first)
3. **Everything else / no deadline** (sorted by priority 1-5, highest first)

**Visual Indicators**:
- Deadline badges: TODAY, THIS WEEK
- Priority color scale (1-5)

---

## Backend API Endpoints (Summary)

### Auth
- `POST /api/v1/auth/register` - Create account
- `POST /api/v1/auth/login` - Returns access token + refresh token
- `POST /api/v1/auth/refresh` - Rotate refresh token

### Core CRUD
- Projects, Tasks, Deferred Items (standard REST operations)

### Daily Workflows
- `GET /api/v1/schedule/today` - Today's plan with time blocks
- `POST /api/v1/schedule/today/plan` - Create/replace time blocks atomically
- `GET /api/v1/tasks/suggested?date=&limit=7` - Ranked tasks for planning
- `PATCH /api/v1/time-blocks/{id}/start` - Record actual_start
- `PATCH /api/v1/time-blocks/{id}/complete` - Record actual_end, update task.actual_minutes
- `PATCH /api/v1/time-blocks/{id}/done-for-now` - Mark incomplete (was_completed = false)
- `POST /api/v1/deferred` - Ultra-low-friction capture
- `POST /api/v1/deferred/{id}/convert` - Convert to real task
- `POST /api/v1/deferred/{id}/defer` - Re-queue for future date
- `PATCH /api/v1/deferred/{id}/dismiss` - Mark processed without creating task
- `POST /api/v1/schedule/today/reflect` - Save energy/mood/notes, finalize day

### Stats
- `GET /api/v1/stats/streak` - Consecutive planned days
- `GET /api/v1/stats/weekly-summary` - Tasks completed, effort, mood trends

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, Vite, Tailwind CSS, Radix UI, dnd-kit, TanStack Query |
| Backend | Java 21, Spring Boot 3.x, Spring Security (JWT), Spring Data JPA |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Containerization | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| API Doc | OpenAPI/Swagger (springdoc-openapi) |

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

---

## Key Design Decisions & Rationale

| Decision | Rationale |
|----------|-----------|
| Points-based estimation (no time tracking) | Removes precision anxiety; captures "how complex is this?" not "how long will this take?" |
| Visual time blocking (calendar, not task list) | ADHD brains work better with spatial/visual representation than text lists |
| Deferred items with re-queueing | Capture-and-forget reduces decision fatigue; re-queue lets items bubble up without inbox bloat |
| Single gentle chime (no customization) | Distraction reduction; MVP avoids settings/customization complexity |
| "Done for now" instead of "Skip" | Psychologically more supportive; acknowledges work done without total completion pressure |
| Reuse Morning Planning for mid-day adjustment | Simpler UX; no separate "Plan Adjustment View" with complex diffing |
| No task limit in MVP | Simplifies MVP; max_daily_tasks as Phase 2+ feature for those who need guardrails |
| Task hierarchy via parent_task_id (not separate subtask table) | Subtasks are full tasks with their own priority, points, due date; supports flexible breakdown |
| No notes during work (Phase 2+) | MVP focus on completion; notes can be added post-session in reflection |

---

## Success Metrics

1. **Anxiety reduction**: User feels confident capturing ideas and letting them go
2. **Completion rate**: Can finish scheduled tasks on schedule more consistently
3. **Progress visibility**: Tangible sense of accomplishment at end of day
4. **Morning ritual**: Feels grounding, not like a chore
5. **Voluntary engagement**: User wants to open the app each day

---

## Implementation Roadmap (After Design Complete)

| Phase | Timeline | Scope | Validates |
|-------|----------|-------|-----------|
| **Phase 1: Foundation** | Week 1 | Auth, DB, skeleton UIs | System works end-to-end |
| **Phase 2: Core CRUD** | Week 2 | Projects, Tasks | Data hierarchy works |
| **Phase 3: Deferred Items & Evening Ritual** | Week 3 | Quick capture, inbox, processing, evening clean-up (reflection form, streak) | Distraction handling + daily closure works |
| **Phase 4: Daily Planning** | Week 3-4 | Morning ritual, time blocks, active work session, suggestions | Planning + execution flow works |

**Total MVP timeline**: ~4 weeks to validate core vision.

---

## Known Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Timezone handling | Store UTC; use user.timezone for display; convert plan_date boundaries in queries |
| Drag-and-drop optimism | TanStack Query onMutate for optimistic reorder, onError for rollback |
| JWT security | Refresh token in HttpOnly cookie (not localStorage); rotate on each refresh |
| Scope creep | Build phases 1-4 first; validate core loop before extras |

---

## Document References

- `docs/planning/user_design/USER_WISHES.md` - User's original vision
- `docs/planning/user_design/USE_CASES.md` - 5 core MVP use cases
- `docs/planning/user_design/CORE_WORKFLOWS.md` - 6 views, detailed flows
- `docs/planning/user_design/INFORMATION_ARCHITECTURE.md` - Entity definitions, task ordering
- `docs/planning/user_design/WIREFRAMES.md` - ASCII wireframes for all views
- `docs/planning/user_design/DEFERRED_WORK.md` - Phase 2+ features with rationale
- `docs/planning/user_design/USER_DESIGN_CHECKLIST.md` - Design phase checklist
- `docs/IMPLEMENTATION_PLAN.md` - Current progress and next steps
