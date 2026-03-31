# Deferred Work & Phase 2+ Features

**Purpose**: Track features, enhancements, and design decisions intentionally deferred from MVP to Phase 2 and beyond. This prevents scope creep in Phase 1 while documenting the roadmap.

---

## Phase 2+ Features (Deferred from MVP)

### Customizable Timer & Reminders
- **Description**: Customize timer warnings (e.g., 80% completion), water/food/break reminders with custom intervals, audio confirmation toggles
- **Why deferred**: MVP keeps it simple: single gentle chime at end of time block, no customization
- **Estimated effort**: Medium (settings view + notification system enhancements)
- **Priority**: High (users will want these after MVP)

### Quick Capture Brain-Dump Mode
- **Description**: After saving a capture, keep the modal open and ready for another entry instead of auto-dismissing — useful when the user is in a "brain dump" flow with multiple thoughts to offload at once
- **Why deferred**: MVP auto-dismisses after ~1 second (frictionless single-thought capture); brain-dump mode is a useful enhancement but not core
- **Estimated effort**: Very Low (change dismiss behavior + add a "capture another" affordance or just don't auto-close)
- **Priority**: Medium (valuable for ADHD users who have bursts of ideas)

### Notes Capture During Active Work
- **Description**: Capture notes/context when ending a work session (at completion or "Done for now" moment)
  - Prompt: "Notes for next time?" or "Any blockers to record?"
  - Optional notes field in Active Work view or modal
- **Why deferred**: MVP has no notes capture during work; users can add notes later via Task Details
- **Estimated effort**: Low (add modal/form, store in task.notes)
- **Priority**: Medium (useful for tracking blockers, but not essential)

### Settings/Preferences View
- **Description**: Dedicated settings screen for user preferences
  - Timer type (countdown vs countup)
  - Work hours (start/end times)
  - max_daily_tasks (daily task limit)
  - Reminder toggles and intervals
  - Visual/audio confirmation preferences
  - Streak criteria
- **Why deferred**: MVP uses reasonable defaults; can add customization later
- **Estimated effort**: Medium
- **Priority**: Medium (nice-to-have, not core to MVP)

### Weekly/Monthly/Yearly Review Ceremonies
- **Description**: Structured review workflows at multiple time scales
  - Weekly review: review week's stats, process weekly deferred items, plan next week
  - Monthly review: reflect on month's progress, adjust areas/projects
  - Yearly review: big-picture achievements and priorities
- **Why deferred**: MVP focuses on daily ritual only
- **Estimated effort**: High (new views, stats aggregation, prompts)
- **Priority**: High (important for ADHD users to see progress patterns)

### Task Breakdown Assistance / "Breakthrough" UIs
- **Description**: Optional guided help for breaking down overwhelming tasks
  - "Just make it smaller" paradigm with optional "Suggest help" button
  - Flexible architecture for adding new "breakthrough" UIs for different freeze types
  - Microservices for workflow/guidance suggestions
- **Why deferred**: MVP doesn't include task breakdown assistance; users manually add child tasks
- **Estimated effort**: High (requires flexible architecture + potentially LLM integration)
- **Priority**: High (core to ADHD support, but can start simple)

### Recurring Task Templates
- **Description**: Support for recurring tasks (daily standups, weekly planning, etc.)
  - Recurrence rules (iCal RRULE format)
  - Automatic task generation based on schedule
  - Recurring template management
- **Why deferred**: MVP doesn't include recurring tasks; users manually create tasks each time
- **Estimated effort**: Medium
- **Priority**: Medium (useful but not essential for MVP)

### LLM-Integrated Morning Ritual
- **Description**: Morning planning flows like a conversation with a supportive coach
  - Natural language prompts instead of rigid step-by-step form
  - LLM adapts based on user responses
  - Contextual suggestions based on past patterns
- **Why deferred**: MVP uses simple step-by-step form; LLM integration adds complexity
- **Estimated effort**: High (requires LLM API integration, prompt engineering)
- **Priority**: High (aligns with vision of "natural" ritual, not rigid)

### Estimation Accuracy Learning
- **Description**: Track how accurate user's point estimates are vs actual time
  - Show "You estimated 3 pts, took 2 hours" insights
  - Learn patterns over time (e.g., "Your estimates are usually 2x too optimistic")
  - Suggest adjusted estimates based on history
  - Requires adding `estimated_minutes` to Task (MVP uses points only + actual_minutes)
- **Why deferred**: MVP uses points-based estimation only; no time-based estimates or accuracy analysis
- **Estimated effort**: Low-Medium (mostly data analysis + schema addition)
- **Priority**: Medium (useful for improvement, not essential)

### Daily Task Limits (max_daily_tasks)
- **Description**: Optional daily task limit as a guardrail against over-scheduling
  - User sets a soft cap on how many tasks to schedule per day
  - System warns (but doesn't block) when limit is reached
  - Configurable per user in settings
- **Why deferred**: MVP has no hard task limit; simplifies planning and avoids unnecessary constraints for users who don't need them
- **Estimated effort**: Low (settings field + UI indicator in Morning Planning)
- **Priority**: Medium (helpful for some ADHD users who tend to over-schedule)

### Task Notes
- **Description**: Timestamped notes attached to tasks for tracking work-in-progress context, blockers, and next steps
  - Notes live in their own table (each note is its own entity)
  - Displayed chronologically on the Task Details view
  - Can be added during or after work sessions
- **Why deferred**: MVP uses task description only; separate notes system adds complexity
- **Estimated effort**: Low-Medium (new table + UI component on Task Details)
- **Priority**: Medium (valuable for ongoing context, especially across sessions)

### Cross-Day Task Rescheduling
- **Description**: Move tasks between days via Morning Planning View or other mechanisms
  - "Move Task A to tomorrow"
  - Bulk rescheduling for overwhelmed days
- **Why deferred**: MVP doesn't support moving tasks between days; plan adjustment reuses Morning Planning (today only)
- **Estimated effort**: Low (mostly UI in Plan Adjustment)
- **Priority**: Low (nice-to-have, users can just re-plan tomorrow)

### Multi-Device Sync & Desktop App
- **Description**: Support for desktop app, mobile app, sync across devices
  - Desktop client (built with Electron or similar)
  - Mobile-responsive React app (already responsive-friendly)
  - Real-time sync via API
- **Why deferred**: MVP is web-only
- **Estimated effort**: Very High (new platforms, sync infrastructure)
- **Priority**: High (long-term vision for accessibility)

### Team/Collaborative Features
- **Description**: Share projects, collaborate on tasks, team dashboards
- **Why deferred**: MVP is single-user only
- **Estimated effort**: Very High (multi-user architecture, permissions, sync)
- **Priority**: Low (out of scope for ADHD personal productivity tool)

### Advanced Analytics & Insights
- **Description**: Deep analytics on productivity patterns, trends, predictions
  - "You're most productive on Tuesdays"
  - Burndown charts, velocity tracking
  - Predictive task completion
- **Why deferred**: MVP shows basic stats (streak, weekly summary, energy/mood trends)
- **Estimated effort**: High
- **Priority**: Medium

---

## MVP Scope (What We're Building)

### Core Features
- ✅ Guided morning planning ritual (5-10 min, step-by-step, no customization)
- ✅ Guided evening clean-up ritual (process deferred items one-by-one + reflection)
- ✅ Deferred items capture (Ctrl+Space, low friction)
- ✅ Visual time blocking (drag-and-drop calendar)
- ✅ Active work session (timer with single chime at end)
- ✅ Task management (create, read, update, delete tasks and child tasks)
- ✅ Project organization (organize tasks by project)
- ✅ Points-based estimation (1-5 scale, deliberately vague)
- ✅ Basic streak tracking (consecutive days with both planning and reflection)
- ✅ Weekly/monthly summaries (stats view, not full review ceremony)
- ✅ Multidimensional celebration (recognize effort, impact, difficulty)
- ✅ Deadline tracking (visual indicators: TODAY, THIS WEEK)
- ✅ Flexible plan adjustments (reopen Morning Planning to adjust mid-day)
- ✅ Dashboard overview (today at a glance, upcoming deadlines, inbox badge)

### Not in MVP
- ❌ Customizable timers/reminders (Phase 2+)
- ❌ Settings view (Phase 2+)
- ❌ Weekly/monthly/yearly review ceremonies (Phase 2+)
- ❌ Task breakdown assistance (Phase 2+)
- ❌ Recurring tasks (Phase 2+)
- ❌ LLM-integrated morning ritual (Phase 2+)
- ❌ Estimation accuracy learning (Phase 2+)
- ❌ Cross-day task rescheduling (Phase 2+)
- ❌ Desktop/mobile apps (Phase 2+)
- ❌ Team collaboration (Phase 3+)

---

## Implementation Notes

1. **Architecture for Extensibility**: Build Phase 1 with enough abstraction that Phase 2+ features can be added without major refactors
   - Modular reminder system
   - Pluggable "breakthrough" UI components
   - Clean API boundaries for future LLM integration

2. **Database Schema Readiness**: Phase 1 schema should support Phase 2+ features
   - `Task.actual_minutes` for time tracking (estimation learning will add `estimated_minutes` in Phase 2+)
   - Recurring task fields (`is_recurring`, `recurrence_rule`) can be added to Task in Phase 2+
   - `DeferredItem.deferral_count` and `deferred_until_date` to track deferred behavior patterns
   - `DailyReflection.energy_rating`, `mood_rating` for weekly trends

3. **API Design**: Ensure API endpoints can support future customization
   - Reminder toggles endpoint (for Phase 2 settings)
   - Analytics endpoint (for Phase 2 insights)
   - Recurrence generation endpoint (for Phase 2 recurring tasks)

---

## Tracking & Prioritization

This document is the source of truth for what's deferred. Before adding any Phase 2+ features to MVP:

1. Check this document
2. Confirm it's listed as Phase 2+ (not in scope)
3. Create an issue/task for Phase 2 roadmap
4. Do not add to MVP scope

This prevents scope creep and keeps Phase 1 focused on the core daily loop.
