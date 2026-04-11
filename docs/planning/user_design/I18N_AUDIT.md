# i18n String Audit

**Purpose**: File-by-file catalog of every hardcoded user-facing string in the frontend, with namespace assignments and special handling flags. This is the reference artifact for Phase 1 of the [Internationalization plan](INTERNATIONALIZATION.md).

**Audited**: 2026-04-11  
**Files audited**: 57  
**Estimated distinct strings**: ~280+

---

## Legend

**Type**: heading, label, button, placeholder, error, link, aria-label, toast, text  
**Special Handling**:
- `interpolation` — string contains dynamic variables
- `plural` — string has singular/plural logic
- `tone` — ADHD-friendly microcopy requiring careful, tone-aware translation
- (blank) — straightforward, no special handling needed

---

## Namespace: `auth`

### `frontend/src/pages/LoginPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Echel Planner" | heading | product name |
| "Planning that works with your brain, not against it." | text | tone |
| "Email" | label | |
| "you@example.com" | placeholder | |
| "Password" | label | |
| "••••••••" | placeholder | |
| "Invalid email or password." | error | |
| "Something went wrong. Please try again." | error | |
| "Logging in…" | button | |
| "Log in" | button | |
| "Don't have an account?" | text | |
| "Create one" | link | |

### `frontend/src/pages/RegisterPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Echel Planner" | heading | product name |
| "Planning that works with your brain, not against it." | text | tone |
| "Display name" | label | |
| "Your name" | placeholder | |
| "Email" | label | |
| "you@example.com" | placeholder | |
| "Password" | label | |
| "••••••••" | placeholder | |
| "An account with that email already exists." | error | |
| "Please check your details and try again." | error | |
| "Something went wrong. Please try again." | error | |
| "Creating account…" | button | |
| "Create account" | button | |
| "Already have an account?" | text | |
| "Log in" | link | |

### `frontend/src/auth/AuthContext.jsx`

No user-facing strings. Internal logic only.

### `frontend/src/auth/ProtectedRoute.jsx`

No user-facing strings. Renders a loading spinner without an accessible label.

**Accessibility note**: Consider adding `aria-label="Loading"` to the spinner.

---

## Namespace: `common`

### `frontend/src/layouts/AppLayout.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Echel Planner" | text | product name — may or may not be translated |
| "Main navigation" | aria-label | |
| "Dashboard" | link | |
| "Projects" | link | |
| "Inbox" | link | |
| "Today" | link | |
| "Rituals" | label | |
| "Daily" | label | |
| "Start Day" | link | |
| "End Day" | link | |
| "Weekly" | label | |
| "Start Week" | link | |
| "End Week" | link | |
| "Monthly" | label | |
| "Start Month" | link | |
| "End Month" | link | |
| "99+" | text | badge truncation for counts > 99 |
| "Log out" | button | |

### `frontend/src/App.jsx`

No user-facing strings. Routing configuration only.

### `frontend/src/main.jsx`

No user-facing strings. React DOM initialization only.

### `frontend/src/components/ui/Card.jsx`

No user-facing strings. Pure presentational component.

### `frontend/src/components/ui/CardLabel.jsx`

No user-facing strings. Pure presentational component.

### `frontend/src/components/ui/ProgressBar.jsx`

No user-facing strings. Pure presentational component.

### `frontend/src/components/EchelLogo.jsx`

No user-facing strings. SVG logo component with `aria-hidden="true"`.

### `frontend/src/pages/project-detail/icons.jsx`

No user-facing strings. SVG icon library with `aria-hidden="true"`.

---

## Namespace: `dashboard`

### `frontend/src/pages/DashboardPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Loading…" | text | |
| "Good morning" | text | tone |
| "Good afternoon" | text | tone |
| "Good evening" | text | tone |
| "Your Week" | heading | |
| "No activity yet this week." | text | |
| "Today at a Glance" | heading | |
| "tasks done" | label | |
| "No plan yet." | text | |
| "Start planning →" | link | |
| "Planning Streak" | heading | |
| "Upcoming Deadlines" | heading | |
| "No upcoming deadlines. Nice." | text | tone |
| "TODAY" | label | |
| "Inbox" | heading | |
| "Click to review →" | text | |
| "Inbox clear." | text | tone |
| "Start Morning Planning" | button | |
| "Evening Clean-up" | button | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `${getGreeting()}, ${firstName}` | interpolation |
| `${summary.streakDays}-day streak` | interpolation |
| `Energy: ${summary.energyTrend}` | interpolation |
| `Mood: ${summary.moodTrend}` | interpolation |
| `${tasksCompleted} task(s) completed · ${totalPoints} point(s) · ${totalFocusMinutes} focused` | interpolation, plural |
| `${todayCompletedCount} / ${todayBlockCount}` | interpolation |
| `${streakDays} day(s) in a row` | interpolation, plural |
| `Keep it going — finish tonight's reflection.` | tone |
| `Start your streak tonight — finish today's reflection.` | tone |
| `${deferredItemCount} item(s) waiting` | interpolation, plural |

**Date/time formatting**:
- `new Date().toISOString().slice(0, 10)` — ISO date (YYYY-MM-DD), not user-visible
- `new Date().getHours()` — hour-based greeting logic

**String concatenation patterns**:
- `trendParts.join(' · ')` — bullet separator for trend metrics (may need locale-specific separator)
- Multiple ternary plural patterns: `count === 1 ? 'task' : 'tasks'`, `'point' : 'points'`, `'item' : 'items'`, `'day' : 'days'`

---

## Namespace: `ritual`

### `frontend/src/pages/EndDayPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "End of Day" | heading | |
| "Loading…" | text | |

**Note**: Most End Day UI is rendered by the ritual phase components below. EndDayPage itself is a thin orchestrator.

### `frontend/src/pages/EndRitualPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "End of Day" | heading | |
| "End of Week" | heading | |
| "End of Month" | heading | |
| "How was your week?" | heading | |
| "Coming soon — weekly reflection will be available here." | text | |
| "Continue" | button | |
| "How was your month?" | heading | |
| "Coming soon — monthly reflection will be available here." | text | |

### `frontend/src/pages/StartWeekPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Start of Week" | heading | |
| "Spread your tasks across the week. Anything you skip will show up in Start Day." | text | |
| "Week planned. Let's go." | toast | tone |

### `frontend/src/pages/StartMonthPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Start of Month" | heading | |
| "Spread your tasks across the month. Anything you skip will show up in Start Day." | text | |
| "Month planned. Let's go." | toast | tone |

### `frontend/src/components/ritual/TaskTriagePhase.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Review your tasks" | heading | |
| "Due:" | label | |
| "Change deadline" | label | |
| "Change deadline" | button | |
| "Save" | button | |
| "Cancel" | button | |
| "Tomorrow" | button | |
| "Next week" | button | |
| "Next month" | button | |
| "Keep for tomorrow" | button | tone |
| "Cancel task" | button | |
| "Loading tasks…" | text | |
| "Something went wrong. Try again." | error | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `${currentIndex + 1} of ${tasks.length}` | interpolation |
| `Deferred ${deferralCount} times — is this still on your radar?` | interpolation, tone |
| `Deferred ${deferralCount} time(s)` | interpolation, plural |
| `${pointsEstimate} pt(s)` | interpolation, plural |
| `Defer all ${remaining} remaining to tomorrow` | interpolation |

### `frontend/src/components/ritual/InboxPhase.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Loading…" | text | |
| "Inbox Zero!" | heading | tone |
| "All caught up." | text | tone |
| "Process your inbox" | heading | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `${processedCount + 1} of ${totalItems}` | interpolation |

### `frontend/src/components/ritual/DailyReflectionPhase.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "How did today go?" | heading | |
| "Completed today" | label | |
| "✓" | text | |
| "Notable today" | label | |
| "Energy" | label | |
| "Drained" | text | |
| "Low" | text | |
| "Okay" | text | |
| "Good" | text | |
| "Energized" | text | |
| "Mood" | label | |
| "Rough" | text | |
| "Meh" | text | |
| "Okay" | text | |
| "Good" | text | |
| "Great" | text | |
| "Anything on your mind? (optional)" | label | |
| "(optional)" | text | |
| "Anything on your mind?" | placeholder | tone |
| "Something went wrong. Try again." | error | |
| "Saving…" | text | |
| "Continue" | button | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `Energy — ${ENERGY_LABELS[energy]}` | interpolation |
| `Mood — ${MOOD_LABELS[mood]}` | interpolation |
| `${projectName} — ${reason}` | interpolation |

**Note**: `ENERGY_LABELS` and `MOOD_LABELS` are hardcoded constant objects — extract to translation files.

### `frontend/src/components/ritual/TaskSchedulePhase.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Mon" | text | |
| "Tue" | text | |
| "Wed" | text | |
| "Thu" | text | |
| "Fri" | text | |
| "Sat" | text | |
| "Sun" | text | |
| "Loading tasks…" | text | |
| "Assign to a week:" | label | |
| "Assign to a day:" | label | |
| "Leave for now" | button | tone |
| "Something went wrong. Try again." | error | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `${currentIndex + 1} of ${tasks.length}` | interpolation |
| `Due: ${task.dueDate}` | interpolation |
| `Skip remaining ${remaining} tasks →` | interpolation |

**Date/time formatting**:
- `${d.getMonth() + 1}/${d.getDate()}` — manual MM/DD format for day labels
- `${monday.getMonth() + 1}/${monday.getDate()} – ${sunday.getMonth() + 1}/${sunday.getDate()}` — manual date range for week labels

### `frontend/src/components/ritual/CompletionPhase.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Day 1 — you showed up." | text | tone |
| "Good work today." | text | tone |
| "That's a wrap for today." | text | tone |
| "That's a wrap for the week." | text | tone |
| "That's a wrap for the month." | text | tone |
| "That's a wrap." | text | tone |
| "Done" | button | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `${streak} days in a row. Keep it going.` | interpolation, tone |

---

## Namespace: `tasks`

### `frontend/src/pages/ProjectsPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Projects" | heading | |
| "New project" | button | |
| "Edit project" | heading | |
| "New project" | heading | |
| "Name" | label | |
| "e.g. Website redesign" | placeholder | |
| "Description" | label | |
| "(optional)" | label | |
| "What's this project about?" | placeholder | tone |
| "Color" | label | |
| "Icon" | label | |
| "e.g. 🚀 or any text" | placeholder | |
| "Name is required." | error | |
| "Something went wrong. Please try again." | error | |
| "Cancel" | button | |
| "Saving…" | button | |
| "Save" | button | |
| "Archive this project?" | heading | |
| "Cancel" | button | |
| "Archiving…" | button | |
| "Archive" | button | |
| "No active tasks" | text | |
| "New task" | button | |
| "Failed to load projects. Please try again." | error | |
| "No projects yet. Create your first project." | text | |
| "Create a project" | button | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `${project.name} will be archived and hidden from your active projects. You can restore it later.` | interpolation |
| `Add task to ${project.name}` | interpolation (aria-label) |
| `Edit ${project.name}` | interpolation (aria-label) |
| `Archive ${project.name}` | interpolation (aria-label) |
| `+${overflow} more` | interpolation |

### `frontend/src/pages/ProjectDetailPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "← Projects" | link | |
| "Project" | heading | |
| "Add task" | button | |
| "Failed to load tasks. Please try again." | error | |

### `frontend/src/pages/TodayPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Today" | heading | |
| "Coming soon." | text | |

### `frontend/src/pages/project-detail/AddTaskModal.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Add task" | heading | |
| "Title" | label | |
| "What needs to be done?" | placeholder | tone |
| "Due date" | label | |
| "(optional)" | label | |
| "Priority" | label | |
| "— none —" | text | |
| "Title is required." | error | |
| "Something went wrong. Please try again." | error | |
| "Cancel" | button | |
| "Adding…" | button | |
| "Add task" | button | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `Add subtask to "${parentTitle}"` | interpolation |

### `frontend/src/pages/project-detail/TaskDetailModal.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Task title" | aria-label | |
| "Close" | aria-label | |
| "Status" | label | |
| "Priority" | label | |
| "Points estimate" | label | |
| "— none —" | text | |
| "Energy level" | label | |
| "Due date" | label | |
| "Description" | label | |
| "Add some details…" | placeholder | tone |
| "Subtasks" | label | |
| "Add subtask" | button | |
| "No subtasks yet." | text | |
| "Archive this task?" | heading | |
| "Cancel" | button | |
| "Archiving…" | button | |
| "Archive" | button | |
| "Archive task" | button | |
| "Mark as Open" | aria-label | |
| "Mark as Completed" | aria-label | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `${taskTitle} will be archived and hidden from your task list.` | interpolation |
| `Save failed: ${error.message}` or `"Please try again."` | interpolation |

### `frontend/src/pages/project-detail/TaskDetailPanel.jsx`

Same strings as TaskDetailModal (shared UI, different container). Only difference:
- "Close panel" instead of "Close" for the close button aria-label.

### `frontend/src/pages/project-detail/TaskList.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "No tasks yet." | text | |
| "Today" | heading | |
| "This Week" | heading | |
| "No Deadline" | heading | |

### `frontend/src/pages/project-detail/TaskRow.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Mark as Open" | aria-label | |
| "Mark as Completed" | aria-label | |
| "Collapse subtasks" | aria-label | |
| "Expand subtasks" | aria-label | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `Priority ${priority}` | interpolation |
| `Open details for ${task.title}` | interpolation (aria-label) |

**Date formatting**: Uses `formatDate()` — returns MM/DD/YY format.

### `frontend/src/pages/project-detail/EventList.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Events" | heading | |
| "AM" | text | |
| "PM" | text | |
| "Archive" | button | |
| "Event" | text | fallback |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `Archive ${event.title}` | interpolation (aria-label) |

**Date/time formatting**:
- `formatDate(dateStr)` — returns `MM/DD/YY`
- `formatTime(timeStr)` — 12-hour with AM/PM suffix

### `frontend/src/pages/ActiveSessionPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Loading session..." | text | |
| "Focus time" | text | fallback for missing task title |
| "Nice work!" | toast | tone |
| "Complete" | button | |
| "Extend" | button | |
| "Done for now" | button | tone |
| "Dismiss" | button | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `${mins} min` | interpolation |
| `Failed to start session: ${err.message}` | interpolation |
| `Failed to complete: ${err.message}` | interpolation |
| `Something went wrong: ${err.message}` | interpolation |
| `Failed to extend: ${err.message}` | interpolation |

### `frontend/src/pages/active-session/SubtaskChecklist.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Subtasks" | label | |

### `frontend/src/pages/active-session/TimerCircle.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Time's up. Good work!" | toast | tone |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `of ${totalMinutes} min` | interpolation |
| `+${timeStr}` (overtime display) | interpolation |

**Date/time formatting**:
- Timer display uses `MM:SS` format with zero-padding — universal, no localization needed.

### `frontend/src/contexts/ActiveSessionContext.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "useActiveSession must be used within ActiveSessionProvider" | error | developer-only runtime error — not user-facing, skip |

---

## Namespace: `deferred`

### `frontend/src/pages/InboxPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Loading…" | text | |
| "Inbox" | heading | |
| "All clear. Nothing waiting for you." | text | tone |

### `frontend/src/components/QuickCapture.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "+ Quick capture" | button | |
| "Quick capture" | aria-label | |
| "Quick capture" | heading | |
| "Type a quick thought to save it to your inbox." | text | |
| "What's on your mind?" | placeholder | tone |
| "Couldn't save — try again." | error | tone |
| "Captured." | text | tone |
| "Cancel" | button | |
| "Saving…" | button | |
| "Capture" | button | |

### `frontend/src/components/deferred/ConvertForm.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Project *" | label | |
| "Select a project…" | text | |
| "Title *" | label | |
| "Due date" | label | |
| "Priority (1–5)" | label | |
| "Points estimate" | label | |
| "Description" | label | |
| "Something went wrong. Try again." | error | |
| "Cancel" | button | |
| "Creating…" | button | |
| "Create task" | button | |

### `frontend/src/components/deferred/ConvertToEventForm.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Project *" | label | |
| "Select a project…" | text | |
| "Title *" | label | |
| "Date *" | label | |
| "Start *" | label | |
| "End *" | label | |
| "Energy level" | label | |
| "Any" | text | |
| "Low" | text | |
| "Medium" | text | |
| "High" | text | |
| "Description" | label | |
| "Something went wrong. Try again." | error | |
| "Cancel" | button | |
| "Creating…" | button | |
| "Create event" | button | |

### `frontend/src/components/deferred/DeferredItemActions.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Task" | button | |
| "Event" | button | |
| "Defer" | button | |
| "Dismiss" | button | |
| "Defer for:" | text | |
| "1 day" | text | |
| "1 week" | text | |
| "1 month" | text | |
| "✕" | button | |
| "Sure?" | text | tone |
| "Yes, dismiss" | button | |
| "Cancel" | button | |

**Date/time formatting**:
- `formatDistanceToNow(new Date(item.capturedAt), { addSuffix: true })` — date-fns relative time; needs explicit `locale` option for i18n.

---

## Namespace: `timeBlocking`

### `frontend/src/pages/StartDayPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Loading your tasks…" | text | |
| "All Tasks" | label | |
| "— drag or check to schedule" | text | |
| "None selected" | text | |
| "+ Add to calendar" | button | |
| "Due Today" | label | |
| "Nothing due today" | text | |
| "Due This Week" | label | |
| "Nothing due this week" | text | |
| "Today's Plan" | heading | |
| "— drag blocks to move · drag right edge to resize" | text | |
| "Hours:" | label | |
| "–" | text | |
| "Plan saved. Good luck today!" | toast | tone |
| "Something went wrong. Please try again." | error | |
| "Saving…" | button | |
| "Confirm plan" | button | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `Start Day — ${date}` | interpolation |
| `${selectedCount} selected` | interpolation |
| `${skipped} task(s) didn't fit — you can resize blocks to make room` | interpolation, plural |
| `You have blocks before ${hour}` | interpolation |
| `You have blocks after ${hour}` | interpolation |

**Date/time formatting**:
- `new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })` — **hardcoded `'en-US'` locale**
- `formatDropdownHour()` — builds `"12 AM"`, `"12 PM"`, `"${h} AM"`, `"${h - 12} PM"` — 12-hour format with AM/PM

### `frontend/src/pages/start-day/EventBlock.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Event" | text | fallback |

**Date/time formatting**:
- Manual time formatting: `${Math.floor(block.startMinutes / 60)}:${String(block.startMinutes % 60).padStart(2, '0')}`
- Time range: `${startLabel}–${endLabel}`

### `frontend/src/pages/start-day/TaskBrowserRow.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Unknown" | text | fallback |
| "Nothing here" | text | default empty message |

### `frontend/src/pages/start-day/TaskCard.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "TODAY" | label | |
| "THIS WK" | label | |
| "pt" | label | points suffix |

### `frontend/src/pages/start-day/TimeBlock.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Untitled" | text | fallback |
| "Start" | button | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `Remove ${taskTitle} from plan` | interpolation (aria-label) |

**Date/time formatting**:
- Same manual time formatting as EventBlock
- Time range: `${startLabel}–${endLabel}`

### `frontend/src/pages/start-day/TimeBlockGrid.jsx`

No additional user-facing strings beyond hour labels (same `formatDropdownHour` pattern as StartDayPage — `"12 AM"`, `"12 PM"`, `"${h} AM"`, `"${h - 12} PM"`).

---

## Namespace: `admin`

### `frontend/src/pages/admin/AdminPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Admin" | heading | |
| "Users" | label | |
| "Projects" | label | |
| "Tasks" | label | |
| "Deferred Items" | label | |
| "Reflections" | label | |
| "Time Blocks" | label | |
| "Events" | label | |
| "← Back to app" | link | |

### `frontend/src/pages/admin/AdminUsersTable.jsx`

**Column headers**: "Email", "Display Name", "Timezone", "Created"  
**Form fields**: "Email", "Display Name", "Timezone", "Password"  
**Placeholders**: "Leave blank to keep current", "UTC"  
**Entity name**: "User"

### `frontend/src/pages/admin/AdminProjectsTable.jsx`

**Column headers**: "User", "Name", "Description", "Color", "Active", "Created"  
**Form fields**: "Name", "Description", "Color (hex)", "Icon", "Sort Order"  
**Placeholders**: "#6b4c9a"  
**Entity name**: "Project"

### `frontend/src/pages/admin/AdminTasksTable.jsx`

**Column headers**: "User", "Project", "Title", "Status", "Priority", "Due"  
**Form fields**: "Title", "Description", "Status", "Priority", "Points Estimate", "Energy Level", "Due Date", "Sort Order"  
**Status options**: "Open", "Completed", "Cancelled"  
**Priority options**: "None", "Low", "Medium", "High"  
**Entity name**: "Task"

**String concatenation**: `` `${p.name} (${p.userEmail})` `` — project dropdown with email suffix.

### `frontend/src/pages/admin/AdminDeferredTable.jsx`

**Column headers**: "User", "Text", "Processed", "Deferrals", "Deferred Until", "Captured"  
**Form fields**: "Text", "Deferral Count"  
**Entity name**: "Deferred Item"

### `frontend/src/pages/admin/AdminReflectionsTable.jsx`

**Column headers**: "User", "Date", "Energy", "Mood", "Notes", "Finalized"  
**Form fields**: "Date", "Energy Rating (1-5)", "Mood Rating (1-5)", "Notes"  
**Entity name**: "Reflection"

### `frontend/src/pages/admin/AdminEventsTable.jsx`

**Column headers**: "User", "Project", "Title", "Date", "Start", "End", "Energy"  
**Form fields**: "Title", "Description", "Date", "Start Time", "End Time", "Energy Level"  
**Energy options**: "Not set", "LOW", "MEDIUM", "HIGH"  
**Entity name**: "Event"

### `frontend/src/pages/admin/AdminTimeBlocksTable.jsx`

**Column headers**: "User", "Date", "Task", "Start", "End", "Completed"  
**Form fields**: "Date", "Start Time", "End Time", "Sort Order"  
**Task dropdown**: "None"  
**Entity name**: "Time Block"

**String concatenation**: `` `${t.title} (${t.userEmail})` `` — task dropdown with email suffix.

### `frontend/src/pages/admin/components/AdminCrudPage.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "Loading..." | text | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `+ Create ${entityName}` | interpolation |
| `Edit ${entityName}` / `Create ${entityName}` | interpolation |

### `frontend/src/pages/admin/components/AdminFormModal.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "— Select —" | label | |
| "Cancel" | button | |
| "Saving..." | text | |
| "Save" | button | |

### `frontend/src/pages/admin/components/AdminTable.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "ID" | label | |
| "Actions" | label | |
| "Edit" | button | |
| "Delete" | button | |
| "Yes" | text | boolean true display |
| "No" | text | boolean false display |
| "—" | text | null/empty indicator |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `No ${entityName} found` | interpolation |

**Date formatting**: `new Date(val).toLocaleDateString()` — uses default locale (good, but should explicitly use active locale after i18n setup).

### `frontend/src/pages/admin/components/DeleteConfirmDialog.jsx`

| String | Type | Special Handling |
|--------|------|-----------------|
| "This action cannot be undone." | text | |
| "This will also delete:" | text | |
| "Cancel" | button | |
| "Deleting..." | text | |
| "Delete" | button | |

**Strings requiring interpolation**:

| String pattern | Special Handling |
|----------------|-----------------|
| `Delete ${entityName}?` | interpolation |
| `${count} project(s)` | interpolation, plural |
| `${count} task(s)` | interpolation, plural |
| `${count} deferred item(s)` | interpolation, plural |
| `${count} reflection(s)` | interpolation, plural |
| `${count} time block(s)` | interpolation, plural |

---

## Cross-Cutting Concerns

### Strings appearing in multiple files (candidates for `common`)

| String | Files | Recommendation |
|--------|-------|---------------|
| "Cancel" | ~15 files | `common.cancel` |
| "Save" | ~5 files | `common.save` |
| "Saving…" / "Saving..." | ~6 files | `common.saving` |
| "Loading…" / "Loading..." | ~5 files | `common.loading` |
| "Something went wrong. Please try again." / "Try again." | ~8 files | `common.genericError` |
| "Description" | ~5 files | `common.description` |
| "Due date" / "Due Date" | ~4 files | `common.dueDate` |
| "(optional)" | ~3 files | `common.optional` |
| "— none —" / "— Select —" / "None" | ~5 files | `common.noneSelected` |
| "Mark as Open" / "Mark as Completed" | 3 files | `common.markAsOpen`, `common.markAsCompleted` |
| "Archive" / "Archiving…" | ~4 files | `common.archive`, `common.archiving` |

### Date/time formatting sites

| File | Code | Issue |
|------|------|-------|
| `DashboardPage.jsx` | `new Date().getHours()` | Greeting logic — no locale issue |
| `StartDayPage.jsx` | `toLocaleDateString('en-US', ...)` | **Hardcoded `'en-US'`** — must use `i18n.language` |
| `StartDayPage.jsx` | `formatDropdownHour()` — builds `"8 AM"` | **Hardcoded 12-hour AM/PM** — some locales use 24-hour |
| `TaskSchedulePhase.jsx` | `${d.getMonth()+1}/${d.getDate()}` | **Manual MM/DD** — must use `Intl.DateTimeFormat` |
| `TaskSchedulePhase.jsx` | week range: `M/D – M/D` | **Manual date range** — must use locale-aware formatting |
| `EventList.jsx` | `formatDate()` → `MM/DD/YY` | **Hardcoded US format** — must use `Intl.DateTimeFormat` |
| `EventList.jsx` | `formatTime()` → 12-hour AM/PM | **Hardcoded 12-hour** — some locales use 24-hour |
| `TaskRow.jsx` | `formatDate()` → `MM/DD/YY` | **Hardcoded US format** — same as EventList |
| `EventBlock.jsx` | manual `H:MM` from minutes | Numeric only — OK but could use `Intl` |
| `TimeBlock.jsx` | manual `H:MM` from minutes | Same as EventBlock |
| `TimeBlockGrid.jsx` | `formatDropdownHour()` | Same AM/PM issue as StartDayPage |
| `DeferredItemActions.jsx` | `formatDistanceToNow(..., { addSuffix: true })` | **Missing `locale` option** — needs date-fns locale |
| `AdminTable.jsx` | `new Date(val).toLocaleDateString()` | Uses default locale — OK but should be explicit |

### String concatenation patterns needing refactoring

| File | Current code | Suggested `t()` key |
|------|-------------|---------------------|
| `DashboardPage.jsx` | `` `${greeting}, ${firstName}` `` | `dashboard.greeting` with `{{greeting}}`, `{{name}}` |
| `DashboardPage.jsx` | `` `${count} task/tasks completed` `` | `dashboard.tasksCompleted` with `count` (plural) |
| `DashboardPage.jsx` | `` `${count} point/points` `` | `dashboard.points` with `count` (plural) |
| `DashboardPage.jsx` | `` `${count} day/days in a row` `` | `dashboard.streakDays` with `count` (plural) |
| `DashboardPage.jsx` | `` `${count} item/items waiting` `` | `dashboard.inboxWaiting` with `count` (plural) |
| `DashboardPage.jsx` | `` `${days}-day streak` `` | `dashboard.dayStreak` with `{{count}}` |
| `TaskTriagePhase.jsx` | `` `Deferred ${n} time(s)` `` | `ritual.deferralCount` with `count` (plural) |
| `TaskTriagePhase.jsx` | `` `${pts} pt(s)` `` | `ritual.points` with `count` (plural) |
| `TaskTriagePhase.jsx` | `` `${n} of ${total}` `` | `ritual.progress` with `{{current}}`, `{{total}}` |
| `TaskTriagePhase.jsx` | `` `Defer all ${n} remaining` `` | `ritual.deferAllRemaining` with `{{count}}` |
| `CompletionPhase.jsx` | `` `${n} days in a row. Keep it going.` `` | `ritual.streakMessage` with `count` (plural, tone) |
| `StartDayPage.jsx` | `` `${n} task(s) didn't fit` `` | `timeBlocking.tasksDontFit` with `count` (plural) |
| `StartDayPage.jsx` | `` `${count} selected` `` | `timeBlocking.selectedCount` with `{{count}}` |
| `AdminCrudPage.jsx` | `` `Create ${entity}` `` | `admin.createEntity` with `{{entity}}` |
| `AdminTable.jsx` | `` `No ${entity} found` `` | `admin.noEntityFound` with `{{entity}}` |
| `DeleteConfirmDialog.jsx` | `` `Delete ${entity}?` `` | `admin.deleteConfirm` with `{{entity}}` |
| `DeleteConfirmDialog.jsx` | `` `${n} project(s)` `` etc. | `admin.dependentCount` with `count`, `{{type}}` (plural) |
| `ProjectsPage.jsx` | `` `${name} will be archived...` `` | `tasks.archiveProjectConfirm` with `{{name}}` |
| `TaskDetailModal.jsx` | `` `${title} will be archived...` `` | `tasks.archiveTaskConfirm` with `{{title}}` |
| `ActiveSessionPage.jsx` | `` `${mins} min` `` | `tasks.minutesShort` with `{{count}}` |
