# Planner - Implementation Plan & Progress

## Project Status: Design Phase Complete ✅

**Current Date**: March 2026
**Phase**: Phase 5 Complete (Wireframe-to-Use-Case Cross-Review)
**Next Phase**: Phase 6 (Update Design Documents with Gap Resolutions)

---

## Summary of Completed Work

### Phase 1-4: User Design Process ✅

1. **Phase 1: User Wishes & Aspirations** ✅
   - Captured user's vision: mindfulness exercise, capture-and-forget for distractions, flexible planning
   - Documented pain points: paralysis, decision fatigue, distractions, time pressure

2. **Phase 2: Realistic Goals & Vision Statement** ✅
   - Defined core design principles (10 principles)
   - Scoped MVP vs Phase 2+ features
   - Identified 5 core use cases

3. **Phase 3: Use Cases & User Flows** ✅
   - Documented 5 core MVP use cases with detailed flows
   - Mapped each use case to wireframes and views

4. **Phase 4: Core Workflows & Views** ✅
   - Defined 6 critical views (originally 7, Plan Adjustment View removed and merged with Morning Planning)
   - Created ASCII wireframes for all views
   - Documented information architecture and data model

5. **Phase 5: Wireframe-to-Use-Case Cross-Review** ✅
   - Comprehensive gap review identified 13 design inconsistencies
   - All gaps resolved through iterative user clarification
   - 4 priority action item batches identified

---

## Phase 5 Results: 13 Design Gaps Identified & Resolved

### Gap Summary

| # | Gap | Resolution | Category |
|----|-----|-----------|----------|
| 1 | 80% timer warning in Use Case 4 | Deferred to Phase 2+; MVP has single 100% chime only | Scope |
| 2 | Notes capture during Active Work | Deferred to Phase 2+; removed from MVP UX | Scope |
| 3 | "Add to Project" option for deferred items | Removed; simplified to Convert/Defer/Dismiss | UX |
| 4 | Completed blocks display (mid-day) | Show all blocks, completed ones greyed out | Data/UX |
| 5 | Break reminders in Use Case 4 | Deferred to Phase 2+; not in MVP | Scope |
| 6 | Use Case 5 (Plan Adjustment) flow mismatch | Simplify to reuse Morning Planning View instead of dedicated view | Architecture |
| 7 | max_daily_tasks concept | Deferred to later version; MVP has no hard task limit | Scope |
| 8 | Deferred items re-queueing mechanism | Add deferred_until_date field to data model for MVP | Data |
| 9 | Data model inconsistency (area_hint/project_hint) | Remove both; unnecessary and conflicting with other design | Data |
| 10 | Active Work button naming ("Skip" vs "Done for now") | Change to "Done for now" (more ADHD-friendly) | UX/Tone |
| 11 | Active task timer description | References 80% chime (deferred); update to 100% only | Documentation |
| 12 | Mid-day plan adjustment access | Accessible via sidebar menu option | UX/Navigation |
| 13 | Task estimation data model (estimated_minutes) | Remove from MVP; use points only, actual_minutes for time tracking | Data |

---

## Phase 6: Design Document Updates (Next Steps)

### Action Items - Priority Order

#### Batch 1: Use Cases Update
**File**: `docs/planning/user_design/USE_CASES.md`
- [ ] Remove Step 7 from Use Case 4 (Active Work Session) — removes break reminder reference
- [ ] Change "Skip" to "Done for now" in Use Case 4 action options
- [ ] Simplify Use Case 5 (Plan Adjustment) — remove "What's changed?" dropdown, system suggestions; describe simpler flow with Morning Planning reuse
- [ ] Remove Step 2 from Use Case 1 (Morning Planning) — remove "How many work sessions can you realistically do today?" and max_daily_tasks references

#### Batch 2: Information Architecture Update
**File**: `docs/planning/user_design/INFORMATION_ARCHITECTURE.md`
- [ ] Remove `max_daily_tasks` from app_user entity
- [ ] Remove `estimated_minutes` from Task entity (keep `actual_minutes`)
- [ ] Remove `area_hint` and `project_hint` from DeferredItem entity
- [ ] Add `deferred_until_date` (nullable) to DeferredItem for tracking re-queue dates
- [ ] Update task ordering logic description to remove max_daily_tasks references

#### Batch 3: Core Workflows Update
**File**: `docs/planning/user_design/CORE_WORKFLOWS.md`
- [ ] Remove "Shows current task count vs max_daily_tasks limit" from Morning Planning View description
- [ ] Update Active Task Timer description to reference only 100% chime (remove 80% warning reference)
- [ ] Confirm "Done for now" action button label in Active Work Session View

#### Batch 4: Deferred Work Update
**File**: `docs/planning/user_design/DEFERRED_WORK.md`
- [ ] Add "Daily Task Limits (max_daily_tasks)" to Phase 2+ features section with rationale
- [ ] Add "Configurable Break Reminders" to Phase 2+ features if not already present
- [ ] Verify all Phase 2+ features are documented

---

## Key Design Decisions Implemented

### Simplified Features
- ✅ Removed 7th view (Plan Adjustment View) → reuse Morning Planning instead
- ✅ Removed max_daily_tasks limit from MVP (deferrred to Phase 2+)
- ✅ Reduced Active Work timer to single chime at 100% (no 80% warning, no customization)
- ✅ Removed notes capture during work sessions (Phase 2+)
- ✅ Simplified deferred item actions to Convert/Defer/Dismiss only

### Data Model Changes
- ✅ Added `deferred_until_date` to DeferredItem (for re-queueing)
- ✅ Removed `area_hint`, `project_hint` from DeferredItem
- ✅ Removed `estimated_minutes` from Task (keep `actual_minutes`)
- ✅ Removed `area` entity — flat project list with color/icon on project
- ✅ Removed separate `subtask` table — use `parent_task_id` on task (self-referencing hierarchy)
- ✅ Renamed `points` to `points_estimate`

### UX/Tone Improvements
- ✅ Changed "Skip" to "Done for now" (more ADHD-friendly)
- ✅ Clarified mid-day plan adjustment access (sidebar menu option)
- ✅ Confirmed completed time blocks shown greyed out in Morning Planning

---

## 6 Critical Views (Final)

1. **Morning Planning View** — Task browser (top) + visual calendar (bottom)
   - Supports: morning planning + mid-day adjustment (shows completed blocks greyed out)
   - Status: ✅ Defined

2. **Active Work Session View** — Full-screen focus, timer, child tasks checklist
   - Supports: work session with gentle 100% chime
   - Status: ✅ Defined (100% chime only)

3. **Evening Clean-up View** — Deferred items stack + reflection form
   - Supports: one-at-a-time item processing, reflection
   - Status: ✅ Defined

4. **Dashboard / Home View** — Overview, streak, upcoming deadlines, quick actions
   - Status: ✅ Defined

5. **Task Details View** — View/edit single task with child tasks
   - Status: ✅ Defined

6. **Quick Capture Modal** — Ctrl+Space, text input, instant confirmation
   - Status: ✅ Defined

---

## 5 Core Use Cases (Final)

1. **Morning Planning Ritual** (5-10 min)
   - Select tasks, add to calendar, arrange visually
   - Status: ✅ Defined (max_daily_tasks step removed)

2. **Capture Distraction** (10-30 sec, anytime)
   - Ctrl+Space, text, instant confirmation
   - Status: ✅ Defined

3. **Evening Clean-up Ritual** (10-15 min, daily)
   - Process deferred items one-at-a-time, reflect on day
   - Status: ✅ Defined

4. **Active Work Session** (time block duration)
   - Focus on task, timer, progress tracking
   - Status: ✅ Defined (100% chime only, "Done for now" action)

5. **Plan Adjustment** (mid-day, 5-10 min)
   - Reassess and adjust using Morning Planning View
   - Status: ✅ Defined (reuses Morning Planning, shows completed blocks greyed out)

---

## Data Model (Final)

### Entities (6 total)

1. **app_user** — Authentication, preferences
   - ~~max_daily_tasks~~ (removed)

2. **project** — Concrete project (flat list, no area grouping)
   - ✅ color, icon (moved from removed area entity)

3. **task** — Actionable work item (supports hierarchy via parent_task_id)
   - ~~estimated_minutes~~ (removed)
   - ✅ actual_minutes, points_estimate, energy_level
   - ✅ parent_task_id (subtasks are full tasks, not a separate table)

4. **deferred_item** — Quick capture
   - ✅ deferred_until_date (added for re-queueing)
   - ~~area_hint, project_hint~~ (removed)

5. **daily_reflection** — End-of-day ritual

6. **time_block** — Scheduled work

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

## Phase 2+ Features (Deferred)

See `docs/planning/user_design/DEFERRED_WORK.md` for full details.

---

## Next Steps

Ready for implementation. Next action: begin Phase 1 (Foundation).

---

## Critical File References

### User Design Documents
- `docs/planning/user_design/USER_WISHES.md` — Original vision
- `docs/planning/user_design/USE_CASES.md` — 5 core use cases ✅
- `docs/planning/user_design/CORE_WORKFLOWS.md` — 6 views, detailed flows ✅
- `docs/planning/user_design/INFORMATION_ARCHITECTURE.md` — Entity definitions, task ordering ✅
- `docs/planning/user_design/WIREFRAMES.md` — ASCII wireframes ✅
- `docs/planning/user_design/DEFERRED_WORK.md` — Phase 2+ features ✅
- `docs/planning/user_design/USER_DESIGN_CHECKLIST.md` — Design phase checklist

### Architecture & Implementation
- `docs/ARCHITECTURE.md` — Comprehensive architecture ✅
- `docs/IMPLEMENTATION_PLAN.md` — This document

---

## Verification Checklist

Design document review and alignment (completed):

- [x] All references to max_daily_tasks removed (except Phase 2+ sections)
- [x] All references to estimated_minutes removed (except Phase 2+ sections)
- [x] All references to area_hint/project_hint removed
- [x] deferred_until_date added to DeferredItem description
- [x] "Skip" changed to "Done for now" everywhere
- [x] Plan Adjustment View removed (merged into Morning Planning)
- [x] Use Case 5 simplified to describe Morning Planning reuse
- [x] Active Work 80% chime references removed
- [x] Deferred items re-queueing flow documented (1 day / 1 week / 1 month selection)
- [x] Sidebar menu option for mid-day plan adjustment mentioned
- [x] All Phase 2+ features documented in DEFERRED_WORK.md
- [x] ARCHITECTURE.md matches final design decisions
- [x] area entity removed — flat project list
- [x] subtask table removed — parent_task_id hierarchy on task
- [x] project_id required on all tasks; child tasks inherit parent's project
- [x] recurring_template and recurring_instance removed from MVP data model
- [x] Project status simplified to is_active boolean
- [x] Project priority removed
- [x] points renamed to points_estimate
- [x] Evening clean-up included in implementation roadmap Phase 3

---

## Notes for Next Agent

If another agent picks up this work:

1. **Context**: User is building an ADHD-friendly work planning tool. Design phase is complete and all documents are aligned.

2. **Current State**: All design documents reviewed and updated. ARCHITECTURE.md, IMPLEMENTATION_PLAN.md, and all user design documents are consistent.

3. **Key Insight**: User values simplicity and ADHD-friendly language. Deferred many features (max_daily_tasks, timer customization, notes, break reminders, recurring tasks) to Phase 2+ to keep MVP focused.

4. **Critical Decisions**:
   - Single chime timer at 100% only (no 80% warning)
   - Reuse Morning Planning View for mid-day adjustments (no separate view)
   - Points-based estimation (points_estimate), not time-based
   - One-at-a-time deferred item processing
   - "Done for now" instead of "Skip"
   - Flat project list (no area entity)
   - Task hierarchy via parent_task_id (no separate subtask table)
   - Child tasks must share parent's project_id (enforced in service layer)
   - Project status is simple active/inactive (no priority)

5. **Next Phase**: Ready for implementation (Phase 1: Foundation).

6. **Communication Style**: User is clear, decisive, prefers direct feedback and gap-based discussion. Values working incrementally and getting user feedback before moving forward.

---

**Status**: Design phase complete. All documents aligned. Ready for implementation.
