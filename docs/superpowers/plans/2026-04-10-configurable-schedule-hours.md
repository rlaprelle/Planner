# Configurable Schedule Hours Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Allow users to change the schedule grid's hour range from the default 8 AM–5 PM to any whole-hour range via dropdowns above the grid.

**Architecture:** The hardcoded day-start/day-end constants become parameters threaded from React state in `StartDayPage` down through `useTimeGrid`, `TimeBlockGrid`, and into the backend's `savePlan` validation. All grid math is already percentage-based, so it adapts automatically once the boundaries are parameterized.

**Tech Stack:** React 18, dnd-kit, Spring Boot 3.x, JUnit 5 + Mockito

---

### Task 1: Parameterize useTimeGrid hook

**Goal:** Make `useTimeGrid` accept dynamic day boundaries instead of using hardcoded constants, so all grid math adapts to any hour range.

**Files:**
- Modify: `frontend/src/pages/start-day/useTimeGrid.js`

**Acceptance Criteria:**
- [x] `useTimeGrid(dayStartMinutes, dayEndMinutes)` accepts two parameters
- [x] All internal math uses the parameters instead of constants
- [x] Default constants still exported for backward compatibility
- [x] `startResize` uses the passed `dayEndMinutes` for clamping

**Verify:** `cd frontend && npm run lint` → no errors

**Steps:**

- [x] **Step 1: Update useTimeGrid signature and internal math**

Change the hook to accept parameters and compute duration internally:

```javascript
export function useTimeGrid(dayStartMinutes = DAY_START_MINUTES, dayEndMinutes = DAY_END_MINUTES) {
  const gridRef = useRef(null)
  const resizeRef = useRef(null)

  const dayDuration = dayEndMinutes - dayStartMinutes

  function minutesToPercent(minutes) {
    return ((minutes - dayStartMinutes) / dayDuration) * 100
  }

  function durationToPercent(durationMinutes) {
    return (durationMinutes / dayDuration) * 100
  }

  function pixelDeltaToMinutes(deltaX) {
    if (!gridRef.current) return 0
    const { width } = gridRef.current.getBoundingClientRect()
    return (deltaX / width) * dayDuration
  }

  function clientXToMinutes(clientX) {
    if (!gridRef.current) return dayStartMinutes
    const { left, width } = gridRef.current.getBoundingClientRect()
    const ratio = Math.max(0, Math.min(1, (clientX - left) / width))
    return dayStartMinutes + ratio * dayDuration
  }
```

- [x] **Step 2: Update startResize to use dynamic dayEndMinutes**

In the `startResize` function, change the `pushBlocks` call on line 85:

```javascript
const pushed = pushBlocks(newBlocks, ref.blockIndex, dayEndMinutes)
```

This already references `dayEndMinutes` but currently it's the module-level constant. After step 1, it will close over the parameter instead. No code change needed — it naturally captures the parameter.

- [x] **Step 3: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [x] **Step 4: Commit**

```bash
git add frontend/src/pages/start-day/useTimeGrid.js
git commit -m "refactor: parameterize useTimeGrid with dynamic day boundaries"
```

---

### Task 2: Parameterize TimeBlockGrid component

**Goal:** Make `TimeBlockGrid` accept dynamic day boundaries as props and compute hour marks, grid lines, and sub-lines dynamically.

**Files:**
- Modify: `frontend/src/pages/start-day/TimeBlockGrid.jsx`

**Acceptance Criteria:**
- [x] Accepts `dayStartMinutes` and `dayEndMinutes` props
- [x] Hour marks computed from props, not imported constants
- [x] Grid lines and 15-min sub-lines adapt to the range
- [x] Positioning math uses props

**Verify:** `cd frontend && npm run lint` → no errors

**Steps:**

- [x] **Step 1: Update component to accept and use dynamic range props**

Replace the constant imports and derived values at the top of the file, and add the props:

```jsx
export function TimeBlockGrid({
  blocks,
  onBlocksChange,
  onRemoveBlock,
  dropPreview,
  gridRef,
  minutesToPercent,
  durationToPercent,
  startResize,
  dayStartMinutes,
  dayEndMinutes,
}) {
```

Replace the module-level constants with computed values inside the component:

```jsx
  const dayDuration = dayEndMinutes - dayStartMinutes
  const totalHours = dayDuration / 60

  const hourMarks = Array.from(
    { length: totalHours + 1 },
    (_, i) => dayStartMinutes / 60 + i
  )

  function hourToPercent(h) {
    return ((h * 60 - dayStartMinutes) / dayDuration) * 100
  }
```

Remove the module-level `TOTAL_HOURS`, `HOUR_MARKS`, and `hourToPercent` — they're now inside the component.

Keep the `formatHour` function at module level (it's range-independent).

Update the 15-min sub-lines to use `totalHours` instead of `TOTAL_HOURS`:

```jsx
{Array.from({ length: totalHours }, (_, i) => {
  const spanStartPct = (i / totalHours) * 100
  const spanWidthPct = 100 / totalHours
  return [0.25, 0.5, 0.75].map((frac) => (
    <div
      key={`s-${i}-${frac}`}
      className={`absolute top-0 bottom-0 border-l ${
        frac === 0.5 ? 'border-edge' : 'border-edge-subtle'
      }`}
      style={{ left: `${spanStartPct + frac * spanWidthPct}%` }}
    />
  ))
})}
```

Remove the import of `DAY_START_MINUTES` and `DAY_DURATION` from `useTimeGrid`.

- [x] **Step 2: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [x] **Step 3: Commit**

```bash
git add frontend/src/pages/start-day/TimeBlockGrid.jsx
git commit -m "refactor: parameterize TimeBlockGrid with dynamic day boundaries"
```

---

### Task 3: Add hour-range dropdowns and wire up StartDayPage

**Goal:** Add the hour-range state, dropdown UI, range-change validation, and thread dynamic boundaries through all DnD handlers and child components.

**Files:**
- Modify: `frontend/src/pages/StartDayPage.jsx`

**Acceptance Criteria:**
- [x] Two `<select>` dropdowns appear above the grid, right-aligned, defaulting to 8 AM / 5 PM
- [x] Changing the range updates all grid math (positions, DnD clamping, push boundaries)
- [x] Narrowing the range past existing blocks shows an inline warning and is rejected
- [x] `savePlan` sends `startHour` and `endHour` to the backend
- [x] `handleAddToCalendar` uses dynamic boundaries for overflow detection

**Verify:** `cd frontend && npm run lint` → no errors; manual test in browser at http://localhost:5174

**Steps:**

- [x] **Step 1: Add state and derive minute values**

Add state at the top of `StartDayPage`:

```jsx
const [dayStartHour, setDayStartHour] = useState(8)
const [dayEndHour, setDayEndHour] = useState(17)

const dayStartMinutes = dayStartHour * 60
const dayEndMinutes = dayEndHour * 60
```

- [x] **Step 2: Pass dynamic boundaries to useTimeGrid**

Update the `useTimeGrid` call:

```jsx
const { gridRef, minutesToPercent, durationToPercent, pixelDeltaToMinutes, clientXToMinutes, startResize } =
  useTimeGrid(dayStartMinutes, dayEndMinutes)
```

Remove the import of `DAY_START_MINUTES` and `DAY_END_MINUTES` from `useTimeGrid` (no longer needed at this level — the state-derived values replace them).

- [x] **Step 3: Update all DnD handlers to use dynamic boundaries**

Replace every reference to `DAY_START_MINUTES` and `DAY_END_MINUTES` in the component with the state-derived `dayStartMinutes` and `dayEndMinutes`:

In `cursorToSnappedStart`:
```jsx
function cursorToSnappedStart(clientX) {
  const rawCenter = clientXToMinutes(clientX)
  const rawStart = rawCenter - 30
  return Math.max(dayStartMinutes, Math.min(dayEndMinutes - 60, snapTo15(rawStart)))
}
```

In `handleDragEnd` calendar-block branch:
```jsx
const snapped = Math.max(dayStartMinutes, Math.min(dayEndMinutes - duration, snapTo15(rawStart)))
// ...
const pushed = pushBlocks(sorted, movedIndex, dayEndMinutes)
```

In `handleDragEnd` task-card branch:
```jsx
const pushed = pushBlocks(combined, insertedIdx, dayEndMinutes)
```

In `handleAddToCalendar`:
```jsx
const lastEnd =
  gridBlocks.length > 0 ? Math.max(...gridBlocks.map((b) => b.endMinutes)) : dayStartMinutes
// ...
if (currentStart + 60 > dayEndMinutes) break
```

- [x] **Step 4: Add range-change validation handler**

```jsx
function handleStartHourChange(newHour) {
  const earliestBlock = gridBlocks.length > 0
    ? Math.min(...gridBlocks.map((b) => b.startMinutes))
    : Infinity
  if (newHour * 60 > earliestBlock) {
    setAddWarning(`You have blocks before ${formatDropdownHour(newHour)}`)
    return
  }
  setAddWarning(null)
  setDayStartHour(newHour)
}

function handleEndHourChange(newHour) {
  const latestBlock = gridBlocks.length > 0
    ? Math.max(...gridBlocks.map((b) => b.endMinutes))
    : -Infinity
  if (newHour * 60 < latestBlock) {
    setAddWarning(`You have blocks after ${formatDropdownHour(newHour)}`)
    return
  }
  setAddWarning(null)
  setDayEndHour(newHour)
}

function formatDropdownHour(h) {
  if (h === 0 || h === 24) return '12 AM'
  if (h === 12) return '12 PM'
  if (h < 12) return `${h} AM`
  return `${h - 12} PM`
}
```

- [x] **Step 5: Render the dropdown UI**

Add the dropdowns inside the "Today's Plan" section, above the `<TimeBlockGrid>`:

```jsx
<div className="flex items-center justify-between mb-3">
  <div className="text-xs font-semibold text-primary-700 uppercase tracking-wider">
    Today's Plan
    <span className="ml-2 font-normal text-ink-muted normal-case tracking-normal">
      — drag blocks to move · drag right edge to resize
    </span>
  </div>
  <div className="flex items-center gap-1.5 text-xs text-ink-muted">
    <span>Hours:</span>
    <select
      value={dayStartHour}
      onChange={(e) => handleStartHourChange(Number(e.target.value))}
      className="border border-edge rounded-md px-1.5 py-0.5 text-xs bg-surface-raised text-ink-base focus:outline-none focus:ring-1 focus:ring-primary-400"
    >
      {Array.from({ length: 24 }, (_, i) => i).filter((h) => h < dayEndHour).map((h) => (
        <option key={h} value={h}>{formatDropdownHour(h)}</option>
      ))}
    </select>
    <span>–</span>
    <select
      value={dayEndHour}
      onChange={(e) => handleEndHourChange(Number(e.target.value))}
      className="border border-edge rounded-md px-1.5 py-0.5 text-xs bg-surface-raised text-ink-base focus:outline-none focus:ring-1 focus:ring-primary-400"
    >
      {Array.from({ length: 24 }, (_, i) => i + 1).filter((h) => h > dayStartHour).map((h) => (
        <option key={h} value={h}>{formatDropdownHour(h)}</option>
      ))}
    </select>
  </div>
</div>
```

Remove the old static heading `<div>` that contained "Today's Plan" — it's replaced by the flex container above.

- [x] **Step 6: Pass dynamic boundaries to TimeBlockGrid**

```jsx
<TimeBlockGrid
  blocks={gridBlocks}
  onBlocksChange={setBlocks}
  onRemoveBlock={handleRemoveBlock}
  dropPreview={dropPreview}
  gridRef={gridRef}
  minutesToPercent={minutesToPercent}
  durationToPercent={durationToPercent}
  startResize={startResize}
  dayStartMinutes={dayStartMinutes}
  dayEndMinutes={dayEndMinutes}
/>
```

- [x] **Step 7: Update savePlan call to include hour range**

```jsx
const saveMutation = useMutation({
  mutationFn: () =>
    savePlan(
      TODAY,
      gridBlocks
        .filter((b) => !b.isEvent)
        .map((b) => ({
          taskId: b.task.id,
          startTime: minutesToTime(b.startMinutes),
          endTime: minutesToTime(b.endMinutes),
        })),
      dayStartHour,
      dayEndHour
    ),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['schedule'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    navigate('/', { state: { successMessage: 'Plan saved. Good luck today!' } })
  },
})
```

- [x] **Step 8: Update the API wrapper to accept hour range**

In `frontend/src/api/schedule.js`, update `savePlan`:

```javascript
export async function savePlan(blockDate, blocks, startHour, endHour) {
  const res = await authFetch(`${BASE}/schedule/today/plan`, {
    method: 'POST',
    body: JSON.stringify({ blockDate, blocks, startHour, endHour }),
  })
  return handleResponse(res)
}
```

- [x] **Step 9: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [x] **Step 10: Commit**

```bash
git add frontend/src/pages/StartDayPage.jsx frontend/src/api/schedule.js
git commit -m "feat: add hour-range dropdowns to Start Day schedule grid"
```

---

### Task 4: Backend — accept dynamic hour range in savePlan

**Goal:** Make the backend accept optional `startHour`/`endHour` in the save-plan request and validate block times against the provided range instead of hardcoded 8–17.

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/dto/SavePlanRequest.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java`
- Modify: `backend/src/test/java/com/echel/planner/backend/schedule/ScheduleServiceTest.java`
- Modify: `backend/src/test/java/com/echel/planner/backend/schedule/ScheduleServiceEventTest.java`
- Modify: `backend/src/test/java/com/echel/planner/backend/schedule/ScheduleControllerIntegrationTest.java`

**Acceptance Criteria:**
- [x] `SavePlanRequest` has optional `startHour` and `endHour` fields
- [x] Defaults to 8/17 when null
- [x] Validation enforces blocks within the provided range
- [x] Existing tests updated to reflect the new validation message format
- [x] New tests cover: custom range accepted, blocks outside custom range rejected, invalid range rejected

**Verify:** `cd backend && mvn test -Dtest="ScheduleServiceTest,ScheduleServiceEventTest,ScheduleControllerIntegrationTest"` → all tests pass

**Steps:**

- [x] **Step 1: Update SavePlanRequest DTO**

```java
public record SavePlanRequest(
        @NotNull LocalDate blockDate,
        @NotNull List<@Valid BlockEntry> blocks,
        Integer startHour,
        Integer endHour
) {
    public record BlockEntry(
            @NotNull UUID taskId,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime
    ) {}
}
```

- [x] **Step 2: Update ScheduleService validation**

Replace the hardcoded `DAY_START`/`DAY_END` constants and update `savePlan` and `validateBlocks`:

```java
private static final int DEFAULT_START_HOUR = 8;
private static final int DEFAULT_END_HOUR = 17;

public List<TimeBlockResponse> savePlan(AppUser user, SavePlanRequest request) {
    int startHour = request.startHour() != null ? request.startHour() : DEFAULT_START_HOUR;
    int endHour = request.endHour() != null ? request.endHour() : DEFAULT_END_HOUR;

    if (startHour < 0 || startHour > 23) {
        throw new ScheduleValidationException("startHour must be between 0 and 23");
    }
    if (endHour < 1 || endHour > 24) {
        throw new ScheduleValidationException("endHour must be between 1 and 24");
    }
    if (startHour >= endHour) {
        throw new ScheduleValidationException("startHour must be less than endHour");
    }

    LocalTime dayStart = LocalTime.of(startHour, 0);
    LocalTime dayEnd = endHour == 24 ? LocalTime.MAX : LocalTime.of(endHour, 0);

    validateBlocks(request.blocks(), dayStart, dayEnd);
    // ... rest of method unchanged
```

Update `validateBlocks` signature and the range check:

```java
private void validateBlocks(List<SavePlanRequest.BlockEntry> blocks, LocalTime dayStart, LocalTime dayEnd) {
    for (int i = 0; i < blocks.size(); i++) {
        SavePlanRequest.BlockEntry b = blocks.get(i);
        // ... 15-minute and ordering checks unchanged ...
        if (b.startTime().isBefore(dayStart) || b.endTime().isAfter(dayEnd)) {
            throw new ScheduleValidationException(
                    "Block " + i + ": times must be within "
                    + dayStart.toString().substring(0, 5) + "–" + (dayEnd.equals(LocalTime.MAX) ? "24:00" : dayEnd.toString().substring(0, 5)));
        }
    }
    // ... overlap check unchanged ...
}
```

- [x] **Step 3: Update existing tests for new validation message**

The `savePlan_rejectsBlockBeforeDayStart` and `savePlan_rejectsBlockAfterDayEnd` tests check for `"within 08:00"`. The message format now includes the full range. Update the assertions:

```java
@Test
void savePlan_rejectsBlockBeforeDayStart() {
    var entry = new SavePlanRequest.BlockEntry(
            taskId,
            LocalTime.of(7, 0),
            LocalTime.of(7, 30)
    );
    var request = new SavePlanRequest(LocalDate.now(), List.of(entry), null, null);

    assertThatThrownBy(() -> scheduleService.savePlan(user, request))
            .isInstanceOf(ScheduleValidationException.class)
            .hasMessageContaining("within 08:00");
}

@Test
void savePlan_rejectsBlockAfterDayEnd() {
    var entry = new SavePlanRequest.BlockEntry(
            taskId,
            LocalTime.of(16, 45),
            LocalTime.of(17, 15)
    );
    var request = new SavePlanRequest(LocalDate.now(), List.of(entry), null, null);

    assertThatThrownBy(() -> scheduleService.savePlan(user, request))
            .isInstanceOf(ScheduleValidationException.class)
            .hasMessageContaining("within 08:00");
}
```

Update ALL existing test calls that construct `SavePlanRequest` to include the two new `null` parameters (since the record now has 4 fields). This affects three test files:

- `ScheduleServiceTest.java` — all `new SavePlanRequest(...)` calls
- `ScheduleServiceEventTest.java` — all `new SavePlanRequest(...)` calls (lines 87, 126, 151, 167, 183)
- `ScheduleControllerIntegrationTest.java` — all `new SavePlanRequest(...)` calls (lines 82, 107)

```java
var request = new SavePlanRequest(LocalDate.now(), List.of(entry), null, null);
```

- [x] **Step 4: Add new tests for dynamic range**

```java
@Test
void savePlan_acceptsBlocksInCustomRange() {
    var entry = new SavePlanRequest.BlockEntry(
            taskId,
            LocalTime.of(6, 0),
            LocalTime.of(7, 0)
    );
    var request = new SavePlanRequest(LocalDate.now(), List.of(entry), 5, 22);

    when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(task));
    when(eventService.findForDate(eq(user), any(LocalDate.class))).thenReturn(Collections.emptyList());

    TimeBlock savedBlock = new TimeBlock(user, LocalDate.now(), task, LocalTime.of(6, 0), LocalTime.of(7, 0), 0);
    when(timeBlockRepository.saveAll(any())).thenReturn(List.of(savedBlock));

    List<TimeBlockResponse> result = scheduleService.savePlan(user, request);
    assertThat(result).hasSize(1);
}

@Test
void savePlan_rejectsBlockOutsideCustomRange() {
    var entry = new SavePlanRequest.BlockEntry(
            taskId,
            LocalTime.of(6, 0),
            LocalTime.of(7, 0)
    );
    var request = new SavePlanRequest(LocalDate.now(), List.of(entry), 7, 20);

    assertThatThrownBy(() -> scheduleService.savePlan(user, request))
            .isInstanceOf(ScheduleValidationException.class)
            .hasMessageContaining("within 07:00");
}

@Test
void savePlan_rejectsInvalidHourRange() {
    var entry = new SavePlanRequest.BlockEntry(
            taskId,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
    );
    var request = new SavePlanRequest(LocalDate.now(), List.of(entry), 17, 8);

    assertThatThrownBy(() -> scheduleService.savePlan(user, request))
            .isInstanceOf(ScheduleValidationException.class)
            .hasMessageContaining("startHour must be less than endHour");
}

@Test
void savePlan_defaultsToStandardRangeWhenNull() {
    var entry = new SavePlanRequest.BlockEntry(
            taskId,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
    );
    var request = new SavePlanRequest(LocalDate.now(), List.of(entry), null, null);

    when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(task));
    when(eventService.findForDate(eq(user), any(LocalDate.class))).thenReturn(Collections.emptyList());

    TimeBlock savedBlock = new TimeBlock(user, LocalDate.now(), task, LocalTime.of(9, 0), LocalTime.of(10, 0), 0);
    when(timeBlockRepository.saveAll(any())).thenReturn(List.of(savedBlock));

    List<TimeBlockResponse> result = scheduleService.savePlan(user, request);
    assertThat(result).hasSize(1);
}
```

- [x] **Step 5: Run tests**

Run: `cd backend && mvn test -Dtest=ScheduleServiceTest`
Expected: All tests pass

- [x] **Step 6: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/dto/SavePlanRequest.java \
       backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java \
       backend/src/test/java/com/echel/planner/backend/schedule/ScheduleServiceTest.java \
       backend/src/test/java/com/echel/planner/backend/schedule/ScheduleServiceEventTest.java \
       backend/src/test/java/com/echel/planner/backend/schedule/ScheduleControllerIntegrationTest.java
git commit -m "feat: accept dynamic hour range in savePlan backend validation"
```

---

### Task 5: E2E tests for hour-range dropdowns

**Goal:** Add E2E tests verifying the dropdown UI, range change behavior, and block-prevents-narrowing validation.

**Files:**
- Modify: `e2e/tests/start-day.spec.ts`

**Acceptance Criteria:**
- [x] Test: dropdowns default to 8 AM / 5 PM
- [x] Test: changing range updates grid hour labels
- [x] Test: narrowing range past an existing block shows warning

**Verify:** `cd e2e && npx playwright test tests/start-day.spec.ts` → all tests pass

**Steps:**

- [x] **Step 1: Add test for default dropdown values**

```typescript
test('hour range dropdowns default to 8 AM and 5 PM', async ({ page }) => {
  await mockSuggestedTasks(page, [])
  await mockScheduleToday(page, [])
  await page.goto('/start-day')

  const planSection = page.locator('section').filter({ hasText: "Today's Plan" })
  const selects = planSection.locator('select')

  await expect(selects.first()).toHaveValue('8')
  await expect(selects.last()).toHaveValue('17')
})
```

- [x] **Step 2: Add test for range change updating grid labels**

```typescript
test('changing start hour updates grid labels', async ({ page }) => {
  await mockSuggestedTasks(page, [])
  await mockScheduleToday(page, [])
  await page.goto('/start-day')

  const planSection = page.locator('section').filter({ hasText: "Today's Plan" })
  const startSelect = planSection.locator('select').first()

  // Change start to 6 AM
  await startSelect.selectOption('6')

  // Grid should now show 6 AM label
  await expect(planSection.getByText('6 AM')).toBeVisible()
})
```

- [x] **Step 3: Add test for block-prevents-narrowing**

```typescript
test('narrowing range past existing block shows warning', async ({ page }) => {
  await mockSuggestedTasks(page, TASKS.slice(0, 1))
  // Block at 9:00-10:00
  await mockScheduleToday(page, [BLOCKS[0]])
  await page.goto('/start-day')

  const planSection = page.locator('section').filter({ hasText: "Today's Plan" })

  // Wait for block to appear
  await expect(planSection.getByText('Write tests')).toBeVisible()

  // Try to set start hour to 10 AM (would hide the 9:00 block)
  const startSelect = planSection.locator('select').first()
  await startSelect.selectOption('10')

  // Warning should appear and start hour should remain at 8
  await expect(page.getByText(/You have blocks before/)).toBeVisible()
  await expect(startSelect).toHaveValue('8')
})
```

- [x] **Step 4: Run E2E tests**

Run: `cd e2e && npx playwright test tests/start-day.spec.ts`
Expected: All tests pass

- [x] **Step 5: Commit**

```bash
git add e2e/tests/start-day.spec.ts
git commit -m "test: add E2E tests for hour-range dropdowns"
```
