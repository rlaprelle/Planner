# Planner - Implementation Plan

> **Audience:** Engineers and coding agents understanding why things were built a certain way.
> **Out of scope:** Current feature status or deferred work — see [DEFERRED_WORK.md](DEFERRED_WORK.md).

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

## Implementation Plan

Vertical slices, built sequentially. Each slice delivers a complete feature end-to-end (backend + frontend).

| Slice | Name | Summary |
|-------|------|---------|
| 0 | Foundation | Dev environment, Docker Compose, auth (JWT) |
| 1 | Core CRUD | Projects, tasks, child tasks, task details |
| 2 | Quick Capture | Deferred items, Ctrl+Space modal, inbox badge |
| 3 | Evening Clean-up | Process items (convert/defer/dismiss), reflection, streak |
| 4 | Morning Planning | Task browser, visual timeline, drag-and-drop, dashboard |
| 5 | Active Work Session | Focus mode, timer, chime, child task checklist |
| 6 | Polish & Stats | Weekly summary, intelligent celebration, UI polish |

Full spec: [`docs/planning/2026-03-30-implementation-plan-design.md`](planning/2026-03-30-implementation-plan-design.md)

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
