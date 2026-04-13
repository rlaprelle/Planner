# Internationalization (i18n)

> **Audience:** Engineers and coding agents implementing translations.
> **Out of scope:** General architecture or non-i18n conventions.

---

## Approach

**Library**: `react-i18next` (wrapper around `i18next`). This is the dominant i18n solution in the React ecosystem. It provides:
- Namespace-based translation file organization
- ICU message format for plurals and interpolation
- Lazy-loading of translation bundles per namespace
- Built-in language detection (browser settings, localStorage)
- React hooks (`useTranslation`) and components (`<Trans>`) for JSX integration

**Translation file structure**:
```
frontend/src/locales/
  en/
    common.json       # Shared UI: nav, buttons, loading states, errors
    auth.json         # Login, register
    dashboard.json    # Dashboard cards, greeting, streaks
    ritual.json       # Triage, inbox, reflection, completion phases
    tasks.json        # Task/project CRUD, deferral, scheduling
    deferred.json     # Quick capture, inbox, defer actions
    timeBlocking.json # Start Day calendar, time block grid
    admin.json        # Admin CRUD tables, forms
  es/
    common.json
    ...
```

---

## Scope Assessment

| Category | Count | Notes |
|----------|-------|-------|
| Files with hardcoded strings | ~57 | Pages, components, layouts, auth |
| Distinct UI strings | ~200+ | Buttons, labels, placeholders, headings, errors, microcopy |
| Date/time formatting sites | ~9 files | `toLocaleDateString`, `formatDistanceToNow`, manual formatting |
| Problematic string concatenation | ~15 instances | Template literals building sentences, inline ternary plurals |
| Aria-labels with text | ~40+ | Some static, some with interpolated variables |
| Suggested namespaces | 8 | See translation file structure above |

### ADHD-Friendly Microcopy

The app's tone is a core design value, not a cosmetic detail. Strings like "Done for now", "Is this still on your radar?", and "You showed up" are intentional choices that reinforce the app's supportive personality. Translation must preserve this tone, not just the literal meaning.

**Translation guideline**: Every locale needs a tone guide explaining the design principles (supportive, non-judgmental, encouraging — see CLAUDE.md "Design Principles"). Translators should be briefed that "Done for now" is deliberately softer than "Skip" and the translation should carry the same warmth.

### Patterns That Need Refactoring

**String concatenation** — Template literals that build sentences will break in languages with different word order:
```jsx
// Breaks in i18n — word order varies by language
`${count} tasks completed`
`Deferred ${deferralCount} times`
`${skipped} task${skipped > 1 ? 's' : ''} didn't fit`

// i18n-safe — let the translation handle word order
t('tasksCompleted', { count })         // en: "{{count}} tasks completed"
t('deferralCount', { count })          // en: "Deferred {{count}} times"
t('tasksDontFit', { count: skipped })  // en: "{{count}} tasks didn't fit"
```

**Inline plural ternaries** — Replace with ICU plural rules or i18next's built-in plural support:
```jsx
// Before
`${points} ${points === 1 ? 'point' : 'points'}`

// After (i18next handles plural forms per locale)
t('points', { count: points })
// en: { "points_one": "{{count}} point", "points_other": "{{count}} points" }
```

**Date formatting** — Replace hardcoded `'en-US'` locale strings with the user's active locale:
```jsx
// Before
new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })

// After
new Date().toLocaleDateString(i18n.language, { weekday: 'long', month: 'long', day: 'numeric' })
```

---

## Phased Implementation Plan

### Phase 0: Audit & String Catalog

Walk every frontend source file and produce a comprehensive catalog of all user-facing strings, organized by file and namespace. This catalog becomes the reference artifact for Phase 1 extraction.

**Tasks**:
1. Create `docs/planning/user_design/I18N_AUDIT.md`
2. For each file in `frontend/src/` that contains user-facing text, record:
   - File path
   - Every hardcoded string (buttons, headings, labels, placeholders, error messages, aria-labels, toast notifications)
   - Suggested namespace assignment (common, auth, dashboard, ritual, tasks, deferred, timeBlocking, admin)
   - Flag strings that need special handling: interpolation, plurals, or ADHD-tone microcopy
3. Catalog all date/time formatting call sites with file path, line, and current format pattern
4. Catalog all string concatenation patterns that build user-visible sentences, with the current code and a suggested `t()` replacement
5. Note any strings that appear in multiple files (candidates for the `common` namespace)

**Outcome**: A single reference document that Phase 1 can work through mechanically. Decisions about namespace assignment, interpolation keys, and plural forms are made here, not during extraction.

**Effort**: Medium — read-only pass through ~57 files. No code changes.

### Phase 1: Infrastructure & Extraction

Install the library, configure it, and extract all hardcoded strings into English translation files. No new languages yet — the app still looks and behaves identically, but all UI text flows through `react-i18next`.

**Tasks**:
1. Install `react-i18next`, `i18next`, and `i18next-browser-languagedetector`
2. Create `frontend/src/i18n.ts` — initialize i18next with:
   - English as the default and fallback language
   - Namespace-based loading from `frontend/src/locales/en/*.json`
   - Browser language detection (for future use; English-only at this stage)
3. Wrap `<App>` in the i18next `<I18nextProvider>`
4. Extract all hardcoded strings into the 8 English JSON namespace files
5. Replace every hardcoded string in JSX with `t()` calls, file by file:
   - Pages (~15 files)
   - Components (~20 files)
   - Layouts and auth (~4 files)
   - Admin pages (~10 files)
6. Refactor string concatenation patterns into ICU-compatible `t()` calls with interpolation and plurals
7. Replace hardcoded `'en-US'` locale in date/number formatting with `i18n.language`
8. Update all `aria-label` attributes to use `t()` calls
9. Verify: full E2E test suite passes with no visible regressions (all text should render identically since only English is loaded)

**Outcome**: Every user-facing string is externalized. The English translation files serve as the authoritative source of all UI copy. Adding a new language is now just adding a new folder of JSON files.

**Effort**: Large — touches ~57 files. Mostly mechanical but requires care with interpolation patterns.

### Phase 2: Locale-Aware Formatting

Ensure dates, times, numbers, and relative timestamps respect the active locale.

**Tasks**:
1. Audit all `toLocaleDateString`, `toLocaleTimeString`, `Intl.DateTimeFormat`, and `formatDistanceToNow` calls
2. Create a shared `useLocale()` hook (or use `i18n.language` directly) so formatting functions pick up the active language
3. Configure `date-fns` locale support — `formatDistanceToNow` needs an explicit `locale` option per call, or wrap it in a helper that injects the current locale
4. Replace any manual date formatting (e.g., `${d.getMonth() + 1}/${d.getDate()}`) with `Intl.DateTimeFormat` using the active locale
5. Verify: dates, times, and relative timestamps render correctly for `en` and at least one test locale (e.g., `es` or `fr`) by temporarily switching the browser language

**Outcome**: All formatting is locale-aware. When a second language is added, dates and numbers automatically adapt.

**Effort**: Medium — ~9 files with date formatting, plus creating the shared utility.

### Phase 3: Translation Tooling & First Additional Language

Set up the workflow for adding and managing translations, then ship the first non-English locale.

**Tasks**:
1. Choose a translation management approach:
   - **Option A**: Manual JSON editing (fine for 2-3 languages)
   - **Option B**: Integration with a translation platform (Crowdin, Lokalise, Phrase) for community or professional translation
2. Write a **Tone & Translation Guide** documenting the ADHD-friendly voice, with annotated examples of microcopy and the intent behind each choice (e.g., "Done for now" means "this is a valid stopping point, not a failure")
3. Add the first non-English locale (likely Spanish — large user base, good test of plural rules and word order)
4. Implement namespace lazy-loading so non-English bundles are only fetched when needed
5. Add a simple language switcher (temporary, for testing — will be replaced by the user setting in Phase 5)
6. Full QA pass: verify every page renders correctly in both English and the new locale, with special attention to:
   - Text overflow / truncation in buttons and badges (translations are often longer)
   - Pluralization correctness
   - Date/time formatting
   - Microcopy tone fidelity

**Outcome**: The app is fully bilingual. The translation pipeline is proven and documented.

**Effort**: Large — translation work is the bulk. Tooling setup is medium.

### Phase 4: RTL & Layout Resilience (if applicable)

Only needed if supporting a right-to-left language (Arabic, Hebrew, Farsi). Can be skipped or deferred if the language roadmap doesn't include RTL locales.

**Tasks**:
1. Audit CSS for directional assumptions — replace `margin-left`/`padding-right` with logical properties (`margin-inline-start`, `padding-inline-end`)
2. Add `dir="rtl"` support to the HTML root, toggled by the active locale
3. Review layout components (sidebar, nav, time block grid) for RTL correctness
4. Test with an RTL locale

**Outcome**: The app renders correctly in RTL languages.

**Effort**: Medium — Tailwind CSS supports logical properties via plugins, but the time block grid and drag-and-drop interactions may need special attention.

### Phase 5: User Language Preference Setting

Add a per-user language preference stored in the backend, so users can explicitly choose their language rather than relying on browser detection.

**Tasks**:
1. **Backend**: Add a `preferred_locale` column to the `users` table (nullable VARCHAR, e.g., `'en'`, `'es'`). Flyway migration.
2. **Backend**: Expose `preferred_locale` in the user profile API (`GET /api/v1/auth/me`) and allow updates (`PATCH /api/v1/users/me/preferences` or similar)
3. **Frontend**: Add a "Language" dropdown to the Settings/Preferences view (see DEFERRED_WORK.md — this depends on that view existing)
4. **Frontend**: On app load, resolve the active language with this priority:
   1. User's `preferred_locale` from the backend (if logged in and set)
   2. Browser's `navigator.language` (via `i18next-browser-languagedetector`)
   3. English (fallback)
5. **Frontend**: When the user changes language in settings, update the backend preference and switch `i18n.language` immediately (no reload needed — `react-i18next` re-renders reactively)
6. **UX**: Show the language selector on the login/register page as well, using only browser detection + localStorage (no backend preference available pre-login)

**Outcome**: Users have full control over their language. The setting persists across devices via the backend.

**Effort**: Low-Medium — straightforward once the Settings view exists. The backend change is a single column + endpoint.

---

## Dependencies & Risks

| Item | Detail |
|------|--------|
| **Settings/Preferences view** | Phase 5 depends on this existing (tracked in DEFERRED_WORK.md). The language selector needs a home. |
| **Translation quality** | ADHD-friendly tone is hard to translate. Professional translators with context will produce better results than machine translation. Budget for review. |
| **Bundle size** | Each locale adds ~10-30 KB of JSON. Namespace lazy-loading (Phase 3) keeps initial load small. |
| **Text overflow** | German and French translations are typically 20-30% longer than English. UI must accommodate without clipping. Phase 3 QA should catch these. |
| **E2E tests** | Tests use English text for assertions. After Phase 1, tests should continue to pass since English is the default. If tests ever run against a non-English locale, selectors will need to use `data-testid` attributes instead of text matching. |

---

## Non-Goals

- **Backend i18n** — API error messages remain in English. The frontend maps error codes to localized strings.
- **User-generated content translation** — Task names, project descriptions, and reflection notes are stored as-is. No auto-translation.
- **Locale-specific features** — No currency formatting, address formats, or locale-specific workflows. This is a personal productivity tool, not a commerce platform.
