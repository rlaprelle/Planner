# Planner - Implementation Plan

**Status**: Design phase complete. Ready for implementation.

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
| Flat project list (no area entity) | Simpler organization; projects can be colored/iconed for visual grouping |
| No notes during work (Phase 2+) | MVP focus on completion; notes can be added post-session in reflection |

---

## Implementation Roadmap

| Phase | Timeline | Scope | Validates |
|-------|----------|-------|-----------|
| **Phase 1: Foundation** | Week 1 | Auth, DB, skeleton UIs | System works end-to-end |
| **Phase 2: Core CRUD** | Week 2 | Projects, Tasks | Data hierarchy works |
| **Phase 3: Deferred Items & Evening Ritual** | Week 3 | Quick capture, inbox, processing, evening clean-up (reflection form, streak) | Distraction handling + daily closure works |
| **Phase 4: Daily Planning** | Week 3-4 | Morning ritual, time blocks, active work session, suggestions | Planning + execution flow works |

**Total MVP timeline**: ~4 weeks to validate core vision.

---

## Notes for Next Agent

1. **Context**: ADHD-friendly daily work planning tool. Design phase complete, all documents aligned.

2. **Critical Decisions**:
   - Single chime timer at 100% only (no 80% warning)
   - Reuse Morning Planning View for mid-day adjustments (no separate view)
   - Points-based estimation (points_estimate), not time-based
   - One-at-a-time deferred item processing
   - "Done for now" instead of "Skip"
   - Flat project list (no area entity)
   - Task hierarchy via parent_task_id (no separate subtask table)
   - Child tasks must share parent's project_id (enforced in service layer)
   - Project status is simple active/inactive (no priority)

3. **Communication Style**: User is clear, decisive, prefers direct feedback. Values working incrementally with user feedback before moving forward.
