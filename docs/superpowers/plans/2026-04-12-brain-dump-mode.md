# Brain-Dump Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "brain-dump" mode to QuickCapture so users can capture multiple thoughts in rapid succession without the modal closing after each save.

**Architecture:** Two submit paths in the existing QuickCapture component — the current "capture and close" (Enter / "Capture" button) stays unchanged, and a new "capture and continue" path (Ctrl+Enter / "Keep capturing" button) clears the textarea and refocuses instead of closing. New stacked footer layout pairs each action with its hotkey hint. Purely frontend — no backend changes.

**Tech Stack:** React 18, Radix UI Dialog, TanStack Query, react-i18next, Tailwind CSS, Playwright (E2E)

**Spec:** `docs/superpowers/specs/2026-04-12-brain-dump-mode-design.md` (Phase 1 only)

---

### Task 1: Add i18n Keys

**Goal:** Add the three new translation keys needed by the updated QuickCapture footer.

**Files:**
- Modify: `frontend/src/locales/en/deferred.json`

**Acceptance Criteria:**
- [ ] `keepCapturing`, `enterToCapture`, and `ctrlEnterToKeepCapturing` keys exist with correct English values
- [ ] All existing keys unchanged

**Verify:** `node -e "const j=require('./frontend/src/locales/en/deferred.json'); console.log(j.keepCapturing, j.enterToCapture, j.ctrlEnterToKeepCapturing)"` → prints `Keep capturing Enter to capture and close Ctrl+Enter to keep capturing`

**Steps:**

- [ ] **Step 1: Add new keys to deferred.json**

Add these three keys at the end of the object in `frontend/src/locales/en/deferred.json`, before the closing brace:

```json
  "keepCapturing": "Keep capturing",
  "enterToCapture": "Enter to capture and close",
  "ctrlEnterToKeepCapturing": "Ctrl+Enter to keep capturing"
```

- [ ] **Step 2: Run verify command**

Run: `cd frontend && node -e "const j=require('./src/locales/en/deferred.json'); console.log(j.keepCapturing, j.enterToCapture, j.ctrlEnterToKeepCapturing)"`
Expected: `Keep capturing Enter to capture and close Ctrl+Enter to keep capturing`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/locales/en/deferred.json
git commit -m "feat(i18n): add brain-dump mode translation keys"
```

---

### Task 2: Add Brain-Dump Submit Path and Footer Layout

**Goal:** Modify QuickCapture to support two submit paths (capture-and-close vs. capture-and-continue) with the new stacked footer layout and Ctrl+Enter hotkey.

**Files:**
- Modify: `frontend/src/components/QuickCapture.jsx`

**Acceptance Criteria:**
- [ ] Enter and "Capture" button submit and close the modal (existing behavior preserved)
- [ ] Ctrl+Enter and "Keep capturing" button submit, play chime, flash "Captured." for 500ms, clear textarea, and refocus — modal stays open
- [ ] Footer has two rows: row 1 = Cancel + hint + Capture, row 2 = hint + Keep capturing
- [ ] Textarea is disabled during the 500ms "Captured." flash to prevent typing during feedback
- [ ] Multiple rapid Ctrl+Enter captures work without state corruption

**Verify:** Start frontend dev server (`node dev.js start` from project root), open the app, press Ctrl+Space to open QuickCapture, type text, press Ctrl+Enter — modal should stay open with textarea cleared. Type again, click "Capture" — modal should close normally.

**Steps:**

- [ ] **Step 1: Add `brainDumpFlash` state and `textareaRef`**

At the top of the `QuickCapture` component (after the existing state declarations around line 50), add:

```jsx
const [brainDumpFlash, setBrainDumpFlash] = useState(false)
const textareaRef = useRef(null)
```

- [ ] **Step 2: Create the brain-dump submit handler**

Add a new function after `handleTextareaKeyDown` (around line 108):

```jsx
function handleBrainDumpSubmit() {
  if (text.trim() && !mutation.isPending && !brainDumpFlash) {
    const trimmed = text.trim()
    createDeferredItem(trimmed).then(() => {
      queryClient.invalidateQueries({ queryKey: ['deferred'] })
      playChime()
      setBrainDumpFlash(true)
      setText('')
      setError(null)
      setTimeout(() => {
        setBrainDumpFlash(false)
        textareaRef.current?.focus()
      }, 500)
    }).catch(() => {
      setError(t('couldntSave'))
    })
  }
}
```

Note: This intentionally does NOT use the existing `useMutation` hook. The existing mutation's `onSuccess` closes the modal — that's the capture-and-close path. Brain-dump needs different success behavior (clear + refocus instead of close), so it calls the API function directly and manages its own state. This avoids adding conditional logic to the mutation callbacks and keeps the two paths cleanly separated.

- [ ] **Step 3: Update `handleTextareaKeyDown` to handle Ctrl+Enter**

Replace the existing `handleTextareaKeyDown` function:

```jsx
function handleTextareaKeyDown(e) {
  if (e.key === 'Enter' && e.ctrlKey && !e.shiftKey) {
    e.preventDefault()
    handleBrainDumpSubmit()
  } else if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey) {
    e.preventDefault()
    if (text.trim() && !mutation.isPending) mutation.mutate()
  }
}
```

Important: The Ctrl+Enter check must come before the plain Enter check since both match `e.key === 'Enter'`.

- [ ] **Step 4: Add `ref` to the textarea**

Update the textarea element to include the ref. Change the opening `<textarea` tag (around line 150):

```jsx
<textarea
  ref={textareaRef}
  autoFocus
  rows={3}
  className="w-full rounded-md border border-edge px-3 py-2 text-sm text-ink-heading placeholder-ink-muted resize-none focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus"
  placeholder={t('whatsOnYourMind')}
  value={text}
  onChange={e => { setText(e.target.value); setError(null) }}
  onKeyDown={handleTextareaKeyDown}
  disabled={brainDumpFlash}
/>
```

Note the added `ref={textareaRef}` and `disabled={brainDumpFlash}`.

- [ ] **Step 5: Add the brain-dump flash overlay**

After the textarea and error message, before the footer `<div>`, add the flash indicator:

```jsx
{brainDumpFlash && (
  <div className="absolute inset-0 flex items-center justify-center bg-surface-raised/90 rounded-xl z-10 animate-fade-out" style={{ animationDuration: '500ms', animationFillMode: 'forwards' }}>
    <div className="flex flex-col items-center gap-2">
      <svg
        className="text-success w-8 h-8"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        strokeWidth={2}
        aria-hidden="true"
      >
        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
      </svg>
      <p className="text-ink-body font-medium text-sm">{t('captured')}</p>
    </div>
  </div>
)}
```

This reuses the existing `t('captured')` key and the same checkmark SVG as the capture-and-close confirmation. The `animate-fade-out` class needs to be defined — we'll check if it exists in the Tailwind config or add it inline with a style tag. The `absolute inset-0` positioning requires the `Dialog.Content` to have `relative` positioning — add `relative` to its className.

Update the `Dialog.Content` className to include `relative`:

```jsx
<Dialog.Content
  className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-md bg-surface-raised rounded-xl shadow-modal p-6 focus:outline-none relative"
  aria-label={t('quickCapture')}
>
```

- [ ] **Step 6: Add the fade-out keyframe animation**

Check `frontend/tailwind.config.js` for existing animation definitions. If there's no `fade-out` animation, add one. In the `extend` section under `keyframes` and `animation`:

```js
keyframes: {
  'fade-out': {
    '0%': { opacity: '1' },
    '100%': { opacity: '0' },
  },
  // ... existing keyframes
},
animation: {
  'fade-out': 'fade-out 500ms ease-out forwards',
  // ... existing animations
},
```

- [ ] **Step 7: Replace the footer with the stacked two-row layout**

Replace the existing footer `<div>` (the `<div className="mt-4 flex justify-end gap-2">` block, lines 164-177):

```jsx
<div className="mt-4 space-y-2">
  {/* Row 1: Cancel + hint + Capture */}
  <div className="flex items-center">
    <Dialog.Close asChild>
      <button className="px-4 py-2 text-sm text-ink-secondary hover:text-ink-heading focus:outline-none focus:ring-2 focus:ring-edge-focus rounded-md transition-colors duration-100">
        {t('common:cancel')}
      </button>
    </Dialog.Close>
    <span className="flex-1 text-xs text-ink-muted text-right mr-3">
      {t('enterToCapture')}
    </span>
    <button
      disabled={isBlank || mutation.isPending}
      onClick={() => mutation.mutate()}
      className="px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors duration-100"
    >
      {mutation.isPending ? t('common:saving') : t('capture')}
    </button>
  </div>
  {/* Row 2: hint + Keep capturing */}
  <div className="flex items-center justify-end">
    <span className="text-xs text-ink-muted mr-3">
      {t('ctrlEnterToKeepCapturing')}
    </span>
    <button
      disabled={isBlank || mutation.isPending || brainDumpFlash}
      onClick={handleBrainDumpSubmit}
      className="px-4 py-2 text-sm font-medium text-primary-500 border border-primary-300 rounded-md hover:bg-primary-50 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors duration-100"
    >
      {t('keepCapturing')}
    </button>
  </div>
</div>
```

- [ ] **Step 8: Lint check**

Run: `cd frontend && npm run lint`
Expected: No new errors (existing ESLint false-positive warnings about "unused" components are expected and can be ignored).

- [ ] **Step 9: Manual smoke test**

Start the dev server (`node dev.js start` from project root) and test:
1. Ctrl+Space opens modal
2. Type "test thought" → press Enter → modal closes with checkmark (unchanged)
3. Ctrl+Space again → type "thought 1" → press Ctrl+Enter → chime plays, flash shows briefly, textarea clears, modal stays open
4. Type "thought 2" → click "Keep capturing" → same behavior
5. Type "thought 3" → click "Capture" → modal closes normally
6. Cancel and Escape still close the modal

- [ ] **Step 10: Commit**

```bash
git add frontend/src/components/QuickCapture.jsx frontend/tailwind.config.js
git commit -m "feat: add brain-dump mode to QuickCapture

Ctrl+Enter or 'Keep capturing' button submits and keeps the modal
open for rapid multi-capture. Stacked footer layout pairs each
action with its hotkey hint."
```

---

### Task 3: E2E Tests for Brain-Dump Mode

**Goal:** Add Playwright E2E tests covering both submit paths and the brain-dump flow.

**Files:**
- Create: `e2e/tests/quick-capture.spec.ts`

**Acceptance Criteria:**
- [ ] Test: Ctrl+Enter submits and keeps modal open with textarea cleared
- [ ] Test: "Keep capturing" button submits and keeps modal open
- [ ] Test: Enter still submits and closes modal (regression)
- [ ] Test: "Capture" button still submits and closes modal (regression)
- [ ] Test: Multiple rapid brain-dump captures work in sequence
- [ ] All tests mock `/api/v1/deferred` via `page.route()`
- [ ] Tests import from `../fixtures/auth`

**Verify:** `cd e2e && BASE_URL=http://localhost:<port> npx playwright test tests/quick-capture.spec.ts` → all tests pass

**Steps:**

- [ ] **Step 1: Create the test file**

Create `e2e/tests/quick-capture.spec.ts`:

```typescript
import { test, expect } from '../fixtures/auth'

// Helper: open QuickCapture via Ctrl+Space and wait for the dialog
async function openQuickCapture(page) {
  await page.keyboard.press('Control+Space')
  await expect(page.getByRole('dialog')).toBeAttached()
}

// Helper: mock the POST endpoint and track calls
function mockDeferredPost(page) {
  const calls: string[] = []
  page.route('**/api/v1/deferred', (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON()
      calls.push(body.rawText)
      return route.fulfill({ json: { id: `d-${calls.length}`, rawText: body.rawText, status: 'PENDING' } })
    }
    // GET requests fall through to the auth fixture mock (returns [])
    return route.fulfill({ json: [] })
  })
  return calls
}

test.describe('QuickCapture', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/dashboard')
  })

  test.describe('capture and close (existing behavior)', () => {
    test('Enter submits and closes modal', async ({ page }) => {
      const calls = mockDeferredPost(page)

      await openQuickCapture(page)
      await page.getByPlaceholder('What\'s on your mind?').fill('Buy groceries')
      await page.keyboard.press('Enter')

      // Should show confirmation then close
      await expect(page.getByText('Captured.')).toBeAttached()
      await expect(page.getByRole('dialog')).not.toBeAttached({ timeout: 3000 })
      expect(calls).toEqual(['Buy groceries'])
    })

    test('Capture button submits and closes modal', async ({ page }) => {
      const calls = mockDeferredPost(page)

      await openQuickCapture(page)
      await page.getByPlaceholder('What\'s on your mind?').fill('Call dentist')
      await page.getByRole('button', { name: 'Capture', exact: true }).click()

      await expect(page.getByText('Captured.')).toBeAttached()
      await expect(page.getByRole('dialog')).not.toBeAttached({ timeout: 3000 })
      expect(calls).toEqual(['Call dentist'])
    })
  })

  test.describe('brain-dump mode (capture and continue)', () => {
    test('Ctrl+Enter submits and keeps modal open', async ({ page }) => {
      const calls = mockDeferredPost(page)

      await openQuickCapture(page)
      const textarea = page.getByPlaceholder('What\'s on your mind?')
      await textarea.fill('First thought')
      await page.keyboard.press('Control+Enter')

      // Modal should stay open
      await expect(page.getByRole('dialog')).toBeAttached()

      // Textarea should be cleared after flash
      await expect(textarea).toHaveValue('', { timeout: 2000 })

      // API was called
      expect(calls).toEqual(['First thought'])
    })

    test('Keep capturing button submits and keeps modal open', async ({ page }) => {
      const calls = mockDeferredPost(page)

      await openQuickCapture(page)
      await page.getByPlaceholder('What\'s on your mind?').fill('Another thought')
      await page.getByRole('button', { name: 'Keep capturing' }).click()

      await expect(page.getByRole('dialog')).toBeAttached()
      await expect(page.getByPlaceholder('What\'s on your mind?')).toHaveValue('', { timeout: 2000 })
      expect(calls).toEqual(['Another thought'])
    })

    test('multiple rapid captures in sequence', async ({ page }) => {
      const calls = mockDeferredPost(page)

      await openQuickCapture(page)
      const textarea = page.getByPlaceholder('What\'s on your mind?')

      // Capture 1
      await textarea.fill('Idea one')
      await page.keyboard.press('Control+Enter')
      await expect(textarea).toHaveValue('', { timeout: 2000 })

      // Capture 2
      await textarea.fill('Idea two')
      await page.keyboard.press('Control+Enter')
      await expect(textarea).toHaveValue('', { timeout: 2000 })

      // Capture 3
      await textarea.fill('Idea three')
      await page.keyboard.press('Control+Enter')
      await expect(textarea).toHaveValue('', { timeout: 2000 })

      // All three should have been sent
      expect(calls).toEqual(['Idea one', 'Idea two', 'Idea three'])

      // Modal should still be open
      await expect(page.getByRole('dialog')).toBeAttached()
    })
  })
})
```

- [ ] **Step 2: Run the tests**

Make sure the dev server is running (`node dev.js start` from project root), note the port, then:

Run: `cd e2e && BASE_URL=http://localhost:<port> npx playwright test tests/quick-capture.spec.ts --reporter=list`
Expected: 5 tests pass

- [ ] **Step 3: Commit**

```bash
git add e2e/tests/quick-capture.spec.ts
git commit -m "test(e2e): add QuickCapture brain-dump mode tests

Covers both submit paths (capture-and-close, capture-and-continue)
and multi-capture sequence."
```
