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
1. User opens app → sees welcome prompt "Ready to plan your day?"
2. Step 1: "What are your 1-3 top priorities today?" (user types free text)
3. Step 2: "How many work sessions can you realistically do today?" (user sees their max_daily_tasks limit as a soft suggestion)
4. Step 3: "Pull up suggested tasks" or "manually pick tasks"
   - If suggested: system shows top tasks by deadline/priority/points
   - If manual: user browses their task list
5. Step 4: "Arrange them into time blocks" (visual drag-and-drop)
   - Show available hours (user can set work hours in settings)
   - Auto-suggest break blocks between work
   - User places tasks into blocks
6. Step 5: Review and confirm plan
   - "Here's your day. Feel good?" → Save or adjust
7. System creates today's DailyPlan with TimeBlocks

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
   - Options: (a) Convert to task, (b) Add to existing project, (c) Dismiss (not needed)
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
**Goal**: Focus on work with gentle support (timer, break reminders)

**Flow**:
1. User clicks "Start" on a time block
2. Active task view appears:
   - Task title prominent
   - Timer starts (countdown or countup depending on user preference)
   - Subtasks displayed as checklist
3. User works...
4. At 80% of time: gentle chime, "You've got 5 minutes left"
5. At 100% of time: gentle chime, "Time's up. Good work!"
6. User can:
   - Click "Complete" → marks time block done, updates task.actual_minutes
   - Click "Extend 10 min" → adds time
   - Click "Skip" → marks incomplete, asks for notes ("What happened?")
7. System suggests: "Break time! Take 5 minutes?"
   - If yes: opens break block
   - If no: user can start next task

**End state**: Work is tracked, actual time is recorded, user feels the work they did

**Duration**: Depends on task (25 min to many hours)

---

### Use Case 5: Plan Adjustment (Mid-Day Re-plan)

**Actor**: User
**Trigger**: Something changes (task blocked, new urgent request, energy drops) → user realizes plan needs adjustment
**Goal**: Consciously change the plan while being aware of trade-offs

**Flow**:
1. User realizes their plan isn't working (blocked on Task A, new request for Task X, feeling exhausted)
2. User clicks "Reassess plan" or "Adjust today"
3. System shows current plan:
   - Remaining time blocks
   - Completed blocks (greyed out)
   - Incomplete blocks
4. System prompts: "What's changed?"
   - Task blocked?
   - New request?
   - Feeling overwhelmed?
   - Low energy?
5. Based on selection, system suggests adjustments:
   - "Swap Task A for Task X?"
   - "Remove one task to recover energy?"
   - "Take a longer break?"
6. User sees trade-offs:
   - "If you skip Task C, you'll miss the 3pm deadline"
   - "If you pivot to Task X, Task A moves to tomorrow"
7. User approves changes
8. Plan updates, user can capture deferred note: "Blocked on A because..."

**End state**: Plan is adjusted consciously, user is aware of trade-offs

**Duration**: 2-5 minutes

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

1. **Morning**: How many tasks today? Which tasks matter most?
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
