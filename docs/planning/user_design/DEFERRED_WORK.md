# Deferred Work & Future Features

**Purpose**: Backlog of features intentionally deferred from MVP. Prevents scope creep while documenting the roadmap.

---

## Phase 2+ Features

### Customizable Timer & Reminders
- **Description**: Customize timer warnings (e.g., 80% completion), water/food/break reminders with custom intervals, audio confirmation toggles. MVP uses a single gentle chime at end of time block with no customization.
- **Rationale**: Users will want personalized timer behavior after experiencing the MVP defaults.
- **Effort**: Medium (settings view + notification system enhancements)
- **Priority**: High

### Quick Capture Brain-Dump Mode
- **Description**: After saving a capture, keep the modal open and ready for another entry instead of auto-dismissing — useful when the user is in a "brain dump" flow with multiple thoughts to offload at once. MVP auto-dismisses after ~1 second (frictionless single-thought capture).
- **Rationale**: Valuable for ADHD users who have bursts of ideas to offload at once.
- **Effort**: Very Low (change dismiss behavior + add a "capture another" affordance or just don't auto-close)
- **Priority**: Medium

### Notes Capture During Active Work
- **Description**: Capture notes/context when ending a work session (at completion or "Done for now" moment). Prompt: "Notes for next time?" or "Any blockers to record?" Optional notes field in Active Work view or modal. MVP has no notes capture during work; users can add notes later via Task Details.
- **Rationale**: Useful for tracking blockers and preserving context between sessions.
- **Effort**: Low (add modal/form, store in task.notes)
- **Priority**: Medium

### Configurable Work Day Hours
- **Description**: Allow users to set their own work day start and end times for the Morning Planning calendar. MVP defaults to 8 AM – 5 PM. Time blocks cannot be placed outside these hours. Settings would live in the Settings/Preferences view.
- **Rationale**: ADHD users have highly variable schedules — night owls, part-time workers, and caregivers may have very different productive windows. A fixed 8–5 window may not fit their reality.
- **Effort**: Low (settings field + pass bounds to calendar component)
- **Priority**: Medium
- **Affects**: Morning Planning calendar bounds, `GET /api/v1/schedule/today` (return configured hours), Settings view

### Settings/Preferences View
- **Description**: Dedicated settings screen for user preferences: timer type (countdown vs countup), work hours (start/end times), max_daily_tasks (daily task limit), reminder toggles and intervals, visual/audio confirmation preferences, streak criteria. MVP uses reasonable defaults.
- **Rationale**: Allows users to tailor the tool to their personal workflow once they've established habits with the defaults.
- **Effort**: Medium
- **Priority**: Medium

### Weekly/Monthly/Yearly Review Ceremonies
- **Description**: Structured review workflows at multiple time scales. Weekly review: review week's stats, process weekly deferred items, plan next week. Monthly review: reflect on month's progress, adjust areas/projects. Yearly review: big-picture achievements and priorities.
- **Rationale**: Important for ADHD users to see progress patterns across longer time horizons. Also expected to help with re-entry after gaps by encouraging users to archive stale work.
- **Effort**: Medium (stats aggregation + reflection UIs for what remains)
- **Priority**: High
- **Partially built (2026-04-10)**: The ritual system is in place with 6 rituals (Start Month, Start Week, Start Day, End Day, End Week, End Month). End Day has full task triage + inbox processing + daily reflection. End Week and End Month pages exist and include all End Day phases, but their **weekly and monthly reflection phases are placeholder stubs** that need to be fleshed out:
  - **End Week reflection**: weekly wins summary, what went well / what was hard, review of tasks completed vs deferred vs cancelled that week
  - **End Month reflection**: everything in End Week, plus project health check ("Is this project still important to you?" per active project), bigger-picture reflection
  - Both should show aggregated stats for the period (tasks completed, focus time, energy/mood trends)
  - The `daily_reflection` table already supports `reflection_type = 'WEEKLY' | 'MONTHLY'` — backend is ready, just needs frontend UIs

### Task Breakdown Assistance / "Breakthrough" UIs
- **Description**: Optional guided help for breaking down overwhelming tasks. "Just make it smaller" paradigm with optional "Suggest help" button. Flexible architecture for adding new "breakthrough" UIs for different freeze types. Microservices for workflow/guidance suggestions. MVP doesn't include guided task breakdown; users manually add child tasks.
- **Rationale**: Core to ADHD support — Brown identifies task initiation/decomposition as one of the most impaired executive functions.
- **Effort**: High (requires flexible architecture + potentially LLM integration)
- **Priority**: High

### Recurring Task Templates
- **Description**: Support for recurring tasks (daily standups, weekly planning, etc.). Recurrence rules (iCal RRULE format), automatic task generation based on schedule, recurring template management. MVP doesn't include recurring tasks; users manually create tasks each time.
- **Rationale**: Reduces repetitive manual task creation for users with regular routines.
- **Effort**: Medium
- **Priority**: Medium

### LLM-Integrated Morning Ritual
- **Description**: Morning planning flows like a conversation with a supportive coach. Natural language prompts instead of rigid step-by-step form. LLM adapts based on user responses. Contextual suggestions based on past patterns. MVP uses simple step-by-step form.
- **Rationale**: Aligns with the core vision of a "natural" ritual that feels like a grounding practice, not a chore.
- **Effort**: High (requires LLM API integration, prompt engineering)
- **Priority**: High

### Estimation Accuracy Learning
- **Description**: Track how accurate user's point estimates are vs actual time. Show "You estimated 3 pts, took 2 hours" insights. Learn patterns over time (e.g., "Your estimates are usually 2x too optimistic"). Suggest adjusted estimates based on history. Requires adding `estimated_minutes` to Task (MVP uses points only + actual_minutes).
- **Rationale**: Helps users develop better self-awareness about their time estimation — a known ADHD challenge (Barkley's prospective time estimation research).
- **Effort**: Low-Medium (mostly data analysis + schema addition)
- **Priority**: Medium

### Daily Task Limits (max_daily_tasks)
- **Description**: Optional daily task limit as a guardrail against over-scheduling. User sets a soft cap on how many tasks to schedule per day. System warns (but doesn't block) when limit is reached. Configurable per user in settings. MVP has no hard task limit.
- **Rationale**: Helpful for ADHD users who tend to over-schedule, leading to overwhelm and plan abandonment.
- **Effort**: Low (settings field + UI indicator in Morning Planning)
- **Priority**: Medium

### Task Notes
- **Description**: Timestamped notes attached to tasks for tracking work-in-progress context, blockers, and next steps. Notes live in their own table (each note is its own entity). Displayed chronologically on the Task Details view. Can be added during or after work sessions. MVP uses task description only.
- **Rationale**: Valuable for preserving context across sessions, especially for tasks worked on over multiple days.
- **Effort**: Low-Medium (new table + UI component on Task Details)
- **Priority**: Medium

### Cross-Day Task Rescheduling
- **Description**: Move tasks between days via Morning Planning View or other mechanisms. "Move Task A to tomorrow." Bulk rescheduling for overwhelmed days. MVP plan adjustment reuses Morning Planning (today only).
- **Rationale**: Reduces friction when plans need to shift across days.
- **Effort**: Low (mostly UI in Plan Adjustment)
- **Priority**: Low

### Multi-Device Sync & Desktop App
- **Description**: Support for desktop app, mobile app, sync across devices. Desktop client (built with Electron or similar). Mobile-responsive React app (already responsive-friendly). Real-time sync via API. MVP is web-only.
- **Rationale**: Long-term vision for accessibility — the tool should be available wherever the user is.
- **Effort**: Very High (new platforms, sync infrastructure)
- **Priority**: High

### Team/Collaborative Features
- **Description**: Share projects, collaborate on tasks, team dashboards. MVP is single-user only.
- **Rationale**: Could extend the tool beyond personal productivity, but out of scope for ADHD personal productivity focus.
- **Effort**: Very High (multi-user architecture, permissions, sync)
- **Priority**: Low

### Advanced Analytics & Insights
- **Description**: Deep analytics on productivity patterns, trends, predictions. "You're most productive on Tuesdays." Burndown charts, velocity tracking. Predictive task completion. MVP shows basic stats (streak, weekly summary, energy/mood trends).
- **Rationale**: Helps users understand their own patterns and optimize their workflow.
- **Effort**: High
- **Priority**: Medium

---

## ADHD Research Audit — Future Enhancements

*Based on audit of design against `docs/research/adhd_research_v1.2.md` (March 2026). Each item was individually reviewed and approved.*

### Streak Redesign (Shame Prevention)
- **Description**: Redesign streak display to prevent shame when streaks break. Ratio framing: "You've planned 12 of the last 14 days!" — shown only when above a celebration threshold (specific ratio and threshold to be determined with real usage data). Always show longest streak as a personal best. When returning after a gap, actively celebrate the return: "Welcome back! Your longest streak was 28 days in April of 2026. Can you beat that?" Never call out a broken streak or display any discouraging messaging.
- **Rationale**: Principle 6 (Hallowell/Littman) — broken streaks are more demotivating for ADHD users than maintained streaks are motivating. The shame of a broken streak can trigger the self-reinforcing cycle of avoidance that Littman describes.
- **Effort**: Low-Medium (frontend display changes + new cumulative stats endpoint)
- **Priority**: High
- **Affects**: Dashboard streak tracker, evening reflection celebration, `GET /api/v1/stats/streak` endpoint (needs cumulative counterpart)

### Three-Panel Morning Planning Layout
- **Description**: Replace the single task browser with a three-tier layout. Top: Full Project/Task List (browse all). Middle: Tasks Due Today | Tasks Due This Week (pre-sorted by urgency). Bottom: Time block calendar. Also includes a new `start_date` field on tasks — tasks before their start date are hidden from Morning Planning by default (with a "show all tasks" option). Weekly/Monthly rituals (see Phase 2+ section) will encourage the user to defer or archive tasks to keep the active board small.
- **Rationale**: Principle 2 (reduce friction) and Principle 3 (time blindness) — reduces cognitive load by pre-sorting tasks by urgency, and makes temporal proximity visible through spatial layout rather than requiring the user to scan all projects.
- **Effort**: Medium (frontend layout rework + task schema addition + query filtering)
- **Priority**: High
- **Affects**: Morning Planning view layout, task schema (`start_date` field), task query filtering

### Initiation Support at Work Session Start
- **Description**: "Start here" highlighting: when Active Work Session opens with child tasks, highlight only the first unchecked child task and visually de-emphasize the rest. Motivational questions: during task creation and morning rituals, encourage (never force) the user to answer upbeat questions like "Who's day will be improved if you knock this out of the park?" — all questions must be positive, never negative framing. Display motivational answers at session start as encouragement to begin.
- **Rationale**: Principle 5 (Brown — Activation) and Principle 7 (Littman — inattentive profile) — initiation is one of the most impaired executive functions; the first step must feel trivially easy. Motivational questions also address Principle 4 (Dodson — INCUP Passion component) and Principle 8 (Hallowell — connection) by linking tasks to people and meaning.
- **Effort**: Medium (frontend changes + task schema for motivational answer storage)
- **Priority**: Medium
- **Affects**: Active Work Session view, task creation form, Morning Planning view, task schema

### Progressive Child Task Reveal
- **Description**: In Active Work Session, show only the next 2-3 unchecked child tasks. Completed tasks collapsed above, remaining tasks hidden below. As user checks off items, next ones reveal. "Show all" toggle available for users who prefer the full list. Connects to "Start here" highlighting in Initiation Support.
- **Rationale**: Principle 1 (Barkley) — "Checklists with no more than 2-3 items in view at once, reducing overwhelm." Focuses attention on the immediate next action rather than the full scope of work.
- **Effort**: Low (frontend display logic)
- **Priority**: Medium
- **Affects**: Active Work Session view

### Lightweight Task Breakdown Prompt
- **Description**: When a user creates a task with high points_estimate (4-5), passively display a "What's the very first step?" prompt with a text field to create one child task — the user can ignore it without needing to dismiss. Add a "Help" button on the Active Work Session that calls a microservice for context-aware strategy suggestions (break down complex tasks, suggest low-energy starting approaches, etc.). Full guided breakdown, "suggest help" breakthrough UIs, and LLM-assisted breakdown remain in the Phase 2+ "Task Breakdown Assistance" item.
- **Rationale**: Principle 5 (Brown — Activation) — task decomposition is the exact executive function the user struggles with most, and even a lightweight prompt can reduce the initiation barrier.
- **Effort**: Medium (frontend prompt + new microservice endpoint for strategy suggestions)
- **Priority**: Medium
- **Affects**: Task creation form, Active Work Session view, new microservice endpoint

### INCUP-Aligned Prompts in Morning Planning
- **Description**: During Morning Planning, include a lightweight prompt like "Which of these are you most excited about today?" Not a required step — a gentle reframe inviting the user to consider interest and energy alongside urgency. Surface motivational answers (from Initiation Support feature) during planning to remind the user why a task matters.
- **Rationale**: Principle 4 (Dodson's INCUP framework) — the ADHD brain engages under conditions of Interest, Novelty, Challenge, Urgency, and Passion; current design only addresses Urgency and Challenge.
- **Effort**: Low (frontend UX copy + optional display of motivational data)
- **Priority**: Low
- **Affects**: Morning Planning view

### Cumulative Progress on Tasks with Children
- **Description**: Wherever tasks with children are displayed (task lists, Morning Planning, Dashboard), show a progress indicator (e.g., "5/8" or mini progress bar) based on completed child task ratio. When marking a subtask complete, animate a pencil crossing it off the list. Keep crossed-off items visible at the top of the subtask list as a clear visual cue of progress and accomplishment.
- **Rationale**: Principle 7 (Littman — inattentive profile) — "slow progress is real progress; visible movement on long tasks rather than binary done/not-done." Crossed-off items and animations also serve as micro-rewards (Principle 4).
- **Effort**: Low-Medium (frontend display + animation)
- **Priority**: Low
- **Affects**: Task list components, Active Work Session view, Morning Planning view, Dashboard

### Email & Password Change
- **Description**: Allow users to change their email address and password from the settings page. Email change requires re-verification (send confirmation to new address before switching). Password change requires current-password confirmation.
- **Rationale**: Standard account management features, but more complex than preference fields due to security implications (re-auth, verification flows). Deferred to keep the initial settings page focused on workflow preferences.
- **Effort**: Medium (backend verification flow for email, re-auth for password, frontend forms with validation)
- **Priority**: Medium

### Language / Internationalization (i18n)
- **Description**: Support for multiple languages in the UI. All user-facing strings externalized to resource bundles. Language selector in user settings. MVP is English-only.
- **Rationale**: Broadens accessibility for non-English speakers. Requires systematic string extraction across the frontend, so easier to do before the UI surface area grows further.
- **Effort**: High (string extraction across all components, translation pipeline, locale-aware date/number formatting)
- **Priority**: Low

### Open Design Gaps

These gaps were identified in the audit but do not yet have a firm design. They need further design work before implementation.

#### Deadline Proximity (Time Blindness)
- **Description**: Current deadline badges (TODAY / THIS WEEK) categorize urgency but don't communicate felt proximity. A task due in 2 hours and a task due at midnight both show "TODAY." Countdown timers were rejected as likely to induce anxiety.
- **Rationale**: Principle 3 (Barkley — time blindness) — "the future is effectively invisible"; deadlines need to communicate felt proximity, not just category.
- **Proposed solutions (need further design)**:
  - Visual warmth/weight gradient — tasks due sooner get visually warmer/bolder without explicit numbers
  - Spatial timeline in Morning Planning — show upcoming deadlines so proximity is spatial rather than numeric
  - Gentle prompts during Morning Planning only — "This task is due Friday — would you like to work on it today?"
- **Note**: The three-panel Morning Planning layout partially addresses this by spatially separating "Due Today" from "Due This Week"

#### Welcome Back Flow (Re-Entry After Gap)
- **Description**: No design exists for users returning after multiple days away. Stale time blocks and incomplete work from gap days could feel overwhelming.
- **Rationale**: Principle 2 (reduce friction) and Principle 6 (emotional safety / Littman's shame spirals).
- **Partial solution**: The streak redesign provides a "Welcome back!" celebration message.
- **Expected solution**: Weekly/monthly review rituals will encourage the user to archive stale work and re-engage, which should naturally address the re-entry problem.

#### Energy/Mood Feed-Forward
- **Description**: Evening reflection captures energy_rating and mood_rating but this data never feeds forward into the next day's planning.
- **Rationale**: Principle 6 (emotional safety) and Principle 7 (Littman — emotional state variability as barrier to task initiation).
- **Possible solutions (need further design)**:
  - Recommend particular tasks based on mood (e.g., surface low-energy tasks on low-energy days)
  - Motivational messaging based on mood
