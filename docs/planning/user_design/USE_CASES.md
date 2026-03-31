# Phase 3: Use Cases & User Flows

**Purpose**: Define the primary scenarios in which the user interacts with the tool.

---

## Primary Use Cases

These are the core user journeys that happen in every day/week.

### Use Case 1: Morning Planning Ritual

**Actor**: User
**Trigger**: User opens the app in the morning (ideally same time each day)
**Goal**: Ground themselves, review the day, and create a clear plan for what to work on

**Flow**:
1. User opens app → navigates to Morning Planning
2. User internally reflects: "What are my 1-3 top priorities today?" (grounding exercise, internal thinking)
3. User browses tasks table (grouped by project, ordered by deadline then priority)
4. User selects tasks they want to work on today
5. User clicks "Add to calendar" → selected tasks auto-placed on today's timeline
6. User drags tasks around calendar to arrange them
7. User confirms plan → DailyReflection created with TimeBlocks

**End state**: User has a visual plan for the day, feels grounded, knows what to work on

**Duration**: 5-10 minutes

---

### Use Case 2: Capture Distraction (Mid-Day)

**Actor**: User
**Trigger**: Random idea/task/request arrives while user is working
**Goal**: Capture it without losing focus

**Flow**:
1. User hits Ctrl+Space (or clicks floating button) while in middle of working
2. Modal appears: "What's on your mind?"
3. User types unstructured note: "Email Dave about invoice" or "Check on Acme project budget"
4. User hits Enter
5. **System response**: Brief visual + audio confirmation ("✓ Saved")
6. Modal closes immediately, user is back to their work
7. Item is added to deferred_items table as unprocessed

**End state**: User's brain is clear, distraction is captured, no anxiety about forgetting

**Duration**: 10-30 seconds

---

### Use Case 3: Evening Clean-up Ritual

**Actor**: User
**Trigger**: End of work day (user manually opens "Evening" section or sees prompt)
**Goal**: Process deferred items, reflect on the day, close out work mentally

**Sub-flow 3a: Process Deferred Items**
1. User opens Inbox
2. System shows count: "You have 5 items to process"
3. Items are presented one at a time (stack of cards)
4. For each item:
   - Show the raw text: "Email Dave about invoice"
   - Options: (a) Convert to task, (b) Defer, (c) Dismiss
   - If convert: guided form appears
     - "What project?" (dropdown of existing projects/categories)
     - "Break into subtasks?" (optional)
     - "Points estimate?" (optional)
     - "Due date?" (optional)
     - Save → mark as processed, task created
   - If defer: user selects 1 day / 1 week / 1 month → item re-queued for that date's evening ritual
     - System tracks `deferral_count` and `created_at` for insights
   - If dismiss: mark as processed
5. After all items processed: "Inbox Zero! Great work!"

**Sub-flow 3b: End-of-Day Reflection**
1. User sees prompt: "How was your day?"
2. Form appears:
   - "Energy level: 1-5" (slider)
   - "Mood: 1-5" (slider)
   - "Reflection notes" (optional free text)
   - "Completed tasks to celebrate?" (system shows completed blocks, user can add notes about significance)
3. User hits Save
4. System marks daily_plan as finalized
5. Streak counter updates: "7-day planning streak! 🎉"

**End state**: Deferred items are processed, day is reflected on, user feels closure

**Duration**: 10-15 minutes total

---

### Use Case 4: Active Work Session

**Actor**: User
**Trigger**: User clicks "Start" on a time block during the day
**Goal**: Focus on single task with gentle support

**Flow**:
1. User clicks "Start" on a time block
2. Active Work Session view appears:
   - Task title prominent
   - Timer starts
   - Progress bar shows % of time elapsed
   - Child tasks displayed as interactive checklist
3. User works...
4. At 100% of time: **gentle chime** + "Time's up. Good work!"
5. User can:
   - Click "Complete" → marks task done, updates task.actual_minutes
   - Click "Extend" → adds time block duration
   - Click "Done for now" → marks incomplete, ready to defer/reschedule
6. Backend updates TimeBlock with actual_start, actual_end, was_completed

**End state**: Work is tracked, actual time is recorded, user feels the work they did

**Duration**: Depends on task (25 min to many hours)

---

### Use Case 5: Plan Adjustment (Mid-Day Re-plan)

**Actor**: User
**Trigger**: Something changes (task blocked, new urgent request, energy drops) → user realizes plan needs adjustment
**Goal**: Reassess and adjust plan based on reality

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

**Key Design**: Reuses Morning Planning View (no separate Plan Adjustment View). Same drag-and-drop interaction as morning planning.

**End state**: Plan is adjusted, user has clear view of remaining day

**Duration**: 5-10 minutes

---

## Secondary Use Cases (Lower Priority)

### Use Case 6: Weekly Review (Future, Phase 2+)

**Trigger**: Sunday evening or start of Monday
**Goal**: Review week, process weekly deferred items, plan next week

**Flow**:
1. System prompts: "Ready for weekly review?"
2. Stats appear:
   - Tasks completed
   - Estimated vs actual time
   - Streak count
   - Energy/mood trends
3. Deferred items captured in "Weekly" category (longer notes, ideas for next week)
4. Guided prompts:
   - "What went well?"
   - "What was hard?"
   - "What should we focus on next week?"
5. Tasks are proposed for next week based on deadlines and priorities

### Use Case 7: Task Breakdown Assistance (Future, Phase 2+)

**Trigger**: User creates a large task or feels paralyzed about a task
**Goal**: Help break task into smaller steps without feeling constraining

**Flow**:
1. User creates a large task or clicks on an overwhelming task
2. System offers: "Want help breaking this down?"
3. If user clicks "Suggest help":
   - Guided form: "Let's think about the first few steps"
   - User and system collaborate on subtasks
4. If user clicks "No thanks": they can manually add subtasks themselves

---

## Key Decision Points

These are moments where the user makes conscious choices:

1. **Morning**: Which tasks matter most? How do I arrange my day?
2. **Mid-day capture**: Do I need to switch tasks? Will I handle this later?
3. **Mid-day reassess**: Should I change my plan? What are the trade-offs?
4. **Evening process**: Do I convert this to a real task or dismiss it?
5. **Evening reflect**: How did I actually do? How am I feeling?

---

## Edge Cases & Error Scenarios

1. **User goes offline mid-task**: Timer pauses, notes are not lost
2. **User forgets to finish a task**: Tomorrow's morning ritual reminds them of yesterday's incomplete tasks
3. **User captures same deferred item twice**: Evening processing shows duplicates
4. **User gets overwhelmed by number of deferred items**: System breaks them into smaller groups ("Process 5 items, then take a break")
5. **User misses morning ritual**: Can still plan mid-day, but system notes the miss for streak tracking

---

## Assumptions

- User will open app most days (daily ritual is voluntary but encouraged)
- User will use time blocks as their primary planning unit
- Deferred items are the primary mechanism to prevent context switching anxiety
- Evening ritual is as important as morning ritual for closure
