# Deferred Work & Phase 2+ Features

**Purpose**: Track features, enhancements, and design decisions intentionally deferred from MVP to Phase 2 and beyond. This prevents scope creep in Phase 1 while documenting the roadmap.

---

## Phase 2+ Features (Deferred from MVP)

### Customizable Timer & Reminders
- **Description**: Customize timer warnings (e.g., 80% completion), water/food/break reminders with custom intervals, audio confirmation toggles
- **Why deferred**: MVP keeps it simple: single gentle chime at end of time block, no customization
- **Estimated effort**: Medium (settings view + notification system enhancements)
- **Priority**: High (users will want these after MVP)

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
- **Why deferred**: MVP just captures estimated_minutes and actual_minutes; no analysis
- **Estimated effort**: Low-Medium (mostly data analysis)
- **Priority**: Medium (useful for improvement, not essential)

### Cross-Day Task Rescheduling
- **Description**: Move tasks between days via Plan Adjustment View or other mechanisms
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
- ✅ Project organization (organize tasks by project/area)
- ✅ Points-based estimation (1-5 scale, deliberately vague)
- ✅ Basic streak tracking (consecutive days of morning planning)
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
   - `Task.estimated_minutes` and `actual_minutes` for estimation learning
   - `Task.is_recurring` and `recurrence_rule` for recurring tasks (even if not used in MVP)
   - `DeferredItem.deferral_count` to track deferred behavior patterns
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
