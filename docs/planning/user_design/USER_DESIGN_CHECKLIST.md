# User Design Process Checklist

This document tracks the structured user design process for the ADHD-Friendly Work Planner. Each phase produces documentation to guide implementation.

## Overview

The goal is to move from high-level vision to concrete wireframes and workflows, ensuring the product truly serves ADHD users' needs.

---

## Phase 1: User Wishes & Aspirations
**Status**: 🔄 In Progress

**Objective**: Understand the user's full vision for what they want to accomplish and how this tool should make them feel.

**Deliverable**: `USER_WISHES.md`
- Raw user aspirations (not yet filtered for feasibility)
- Emotional outcomes desired
- Pain points the tool should address
- Behaviors the tool should encourage

**Questions to Answer**:
- What does a "successful day" feel like with this tool?
- What are the biggest ADHD-related obstacles to productivity?
- What would feel magical or surprising about this tool?
- How should the tool make you *feel* emotionally as you use it?

---

## Phase 2: Realistic Goals & Vision Statement
**Status**: ⏳ Not Started

**Objective**: Filter aspirations into achievable goals for an MVP and future roadmap.

**Deliverable**: `VISION_AND_GOALS.md`
- Vision statement (1-2 sentences)
- MVP scope (what's must-have vs. nice-to-have vs. future)
- Success metrics (how do we know this works?)
- Out-of-scope (what we're NOT building)

**Questions to Answer**:
- Which ADHD challenges matter most?
- What can realistically be built in phase 1?
- How do we measure if this tool is actually helping?
- What trade-offs are we willing to make?

---

## Phase 3: Use Cases & User Flows
**Status**: ⏳ Not Started

**Objective**: Define the primary scenarios in which the user interacts with the tool.

**Deliverable**: `USE_CASES.md`
- Primary use cases (e.g., "Morning Planning Session", "Capture a random task idea")
- Secondary use cases (e.g., "Review last week's progress")
- Actor/system interactions as numbered steps
- Decision points and branches

**Questions to Answer**:
- When do you open the app each day?
- What are the top 5 actions you'll take?
- What interruptions or context-switches happen?
- How often do you context-switch between different work areas?

---

## Phase 4: Core Workflows & Views
**Status**: ⏳ Not Started

**Objective**: Map use cases to the key screens/workflows the app needs.

**Deliverable**: `CORE_WORKFLOWS.md`
- Workflow diagram/descriptions for major user journeys
- List of "critical views" (screens the user must see daily)
- State transitions (e.g., task moves from TODO -> IN_PROGRESS -> DONE)
- Edge cases and error scenarios

**Questions to Answer**:
- What are the 5-7 most important screens?
- How do you move between them?
- What happens if you make a mistake?
- What happens if you get interrupted mid-task?

---

## Phase 5: Information Architecture & Content Inventory
**Status**: ⏳ Not Started

**Objective**: Define what data lives where and how it's organized.

**Deliverable**: `INFORMATION_ARCHITECTURE.md`
- Hierarchy of content (areas -> projects -> tasks -> subtasks)
- What information is essential on each view
- What can be "hidden" or in a sidebar
- Naming conventions and labels that feel natural to you

**Questions to Answer**:
- Do you think in terms of "Areas of Work" or something else?
- How granular should tasks be? (hours? minutes?)
- What metadata matters most? (due date, energy level, time estimate?)
- Should you see everything at once or browse deeper?

---

## Phase 6: Wireframes of Major Views
**Status**: ⏳ Not Started

**Objective**: Create low-fidelity visual layouts for key screens.

**Deliverable**: `WIREFRAMES.md` (with ASCII or simple sketches)
- Dashboard/Home view
- Daily Planner view (morning + during-day)
- Quick Capture modal
- Deferred Items inbox
- Areas/Projects/Tasks management
- Weekly overview
- Any other critical screens

**Questions to Answer**:
- Where should the timer be? How big?
- How should you visualize time blocks?
- What's the quickest way to capture an idea?
- How should progress be shown?

---

## Phase 7: Design Refinement & Validation
**Status**: ⏳ Not Started

**Objective**: Review and iterate on designs before implementation.

**Deliverable**: `DESIGN_NOTES.md`
- Feedback from review
- Refined wireframes
- Accessibility considerations
- Mobile/responsive behavior notes

**Questions to Answer**:
- Do the wireframes match your mental model?
- Are any actions taking too many clicks?
- Is the information hierarchy clear?
- What would make this feel more ADHD-friendly?

---

## Suggested Next Steps

1. **Start with Phase 1** by creating `USER_WISHES.md`
   - Take time to brainstorm freely (no constraints)
   - Think about how you'd use this tool day-to-day
   - Describe the emotional experience you want

2. **After Phase 1**, we can proceed to Phase 2 to filter and prioritize

3. **Each phase output is temporary** — it's for discovery, not long-term documentation
   - Once implementation starts, we'll produce the persistent design docs (DESIGN.md)
   - These working docs can be refined or discarded

---

## Timeline Estimate

| Phase | Effort | Suggested Duration |
|---|---|---|
| 1: Wishes | Brainstorming | 1-2 sessions |
| 2: Vision | Synthesis | 1 session |
| 3: Use Cases | Analysis | 1-2 sessions |
| 4: Workflows | Mapping | 1-2 sessions |
| 5: IA | Structure | 1 session |
| 6: Wireframes | Sketching | 2-3 sessions |
| 7: Refinement | Iteration | 1-2 sessions |

**Total**: ~2-3 weeks of work, can be parallelized or compressed.

---

## Notes

- This is a **user-centered design process**, not a technical requirements doc
- The goal is to build something you'll actually *want* to use every day
- We'll revisit and refine as implementation reveals new constraints
- Feedback loops are intentional — design isn't "done" until you feel confident
