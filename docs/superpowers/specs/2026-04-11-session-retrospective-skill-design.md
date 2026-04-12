# Session Retrospective Skill Design

A Claude Code skill for collaborative session retrospectives that captures workflow learnings as CLAUDE.md entries.

## Problem

Sessions accumulate friction — miscommunications, wrong assumptions, workflow inefficiencies — but these learnings evaporate when the session ends. The existing `revise-claude-md` command focuses on codebase knowledge (commands, patterns, environment quirks). This skill focuses on the collaboration process: what went wrong, why, and how to prevent it next time.

## Hard Rules

1. **No automatic changes.** Every CLAUDE.md addition, modification, or deletion requires explicit user approval. Claude proposes, the user decides.
2. **On-demand only.** Never suggest a retro proactively. Triggered by `/retro` or natural language ("let's do a retro", "session review", "what did we learn").
3. **Root causes over symptoms.** If a workaround was needed, the friction point is why the workaround was needed, not the workaround itself.
4. **Generalize over specifics.** "Always check DB column types match Java field types" not "PostgreSQL SMALLINT must use Java short."
5. **Collaborative, not compliant.** Claude should push back when it disagrees, cite session evidence, and challenge the user's assumptions — same energy as a good peer retro.

## Location

`~/.claude/skills/session-retrospective/SKILL.md` — personal skill, available across all projects.

## Trigger

```yaml
name: session-retrospective
description: >
  Use when a session is complete and the user wants to capture learnings.
  Triggered by /retro or when user says "let's do a retro", "what did we learn",
  "session review", or "capture learnings". On-demand only.
```

## Flow

### Phase 1: Analyze

Claude reviews the full session and produces a numbered list of friction points. Each entry includes:

- **What happened** — the observable friction, grounded in a specific moment
- **Why it was friction** — the underlying cause (not the symptom)
- **Suggested target** — local CLAUDE.md, global CLAUDE.md, or "discuss" if unclear

### Phase 2: Solicit

Claude asks the user: "Anything I missed?" User's additions are appended to the list.

### Phase 3: Discuss

Walk through each friction point one at a time. Each point has two stages:

**Stage A — Intent:** Do we agree this is a real problem worth encoding?
- Claude explains its reasoning for flagging this point
- If the user wants to reject, Claude can push back if it believes the friction was real and likely to recur, citing session evidence
- If the user wants to add something Claude thinks is a one-off or already covered, Claude should say so
- Outcome: encode it, or drop it

**Stage B — Wording:** What exactly do we write?
- Only reached if Stage A results in "encode it"
- Claude proposes specific text and target file (local or global CLAUDE.md)
- User can edit the wording; Claude can push back on wording that is too vague to be actionable or too verbose for prompt space
- Outcome: final text + target file

### Phase 4: Review

Claude launches a single agent that receives all accepted entries and the full content of both CLAUDE.md files (local: `./CLAUDE.md` in the working directory, global: `~/.claude/CLAUDE.md`). The agent checks:

1. **Consistency** — Does any new entry contradict or duplicate an existing entry within the same file? Near-duplicates should be flagged for merging.
   - Local CLAUDE.md contradicting global CLAUDE.md is valid (it's a project-specific override), but should be flagged as a possible flaw in the global rule. The global rule may be too broad.
2. **Quality** — Is each entry concise, actionable, and falsifiable? Would a future Claude session know exactly what to do differently?
3. **Scope** — Is each entry in the right file? Project-specific guidance belongs in local CLAUDE.md; cross-project behavioral rules belong in global CLAUDE.md.

The agent returns a structured report. Claude walks through flagged items with the user one at a time, same collaborative style as Phase 3. No auto-resolution — every change from the review requires user approval.

### Phase 5: Write

Accepted entries (with any post-review adjustments) are written to their target files. Each write requires explicit user confirmation.

### Phase 6: Follow-ups

Non-CLAUDE.md actions that surfaced during discussion are presented as a list for the user's reference. Examples: tools to install, environment variables to set, bugs to file. No enforcement — just a list.

## Patterns

- **Ground friction points in specific session moments.**
  - Bad: "Sometimes assumptions are made about the tech stack"
  - Good: "At message 12, I assumed the frontend used Redux when it actually uses TanStack Query"

- **Propose entries that are falsifiable and actionable.**
  - Bad: "Be more careful with code quality"
  - Good: "Always run lint before committing"

- **Distinguish project-specific (local) from cross-project (global) learnings.**

- **Keep entries to one line when possible.** CLAUDE.md is prompt space.

- **When pushing back on the user, cite evidence from the session.**

- **Prefer updating an existing CLAUDE.md section over adding a new one.**

- **Focus on root causes, not symptoms.**
  - Bad: "When Hibernate throws a schema validation error, run `docker compose down -v` to reset the database"
  - Good: "Ensure Flyway migration column types match entity field types — schema validation errors mean the migration is wrong, not that the DB needs resetting"

- **Generalize from the specific instance to the broader principle.**
  - Bad: "PostgreSQL `SMALLINT` columns must use Java `short`, not `int`"
  - Good: "Always check that DB column types match Java field types before writing entity classes"

## Anti-Patterns

- **Don't surface friction that was already resolved mid-session and won't recur.** A typo caught and fixed immediately is not a friction point.

- **Don't propose entries that restate what's obvious from the code or git history.**

- **Don't be a pushover.** Agreeing with everything the user says defeats the purpose of a collaborative retro.

- **Don't propose entries about one-off environmental issues.**
  - Bad: "npm install was slow today — try clearing the cache"

- **Don't let entries accumulate without checking against existing CLAUDE.md content.**

- **Don't write entries that are judgments about the user rather than actionable guidance for Claude.**
  - Bad: "User tends to give vague requirements"
  - Good: "When requirements are ambiguous, ask for acceptance criteria before starting implementation"

- **Don't propose entries that encode workarounds — fix the underlying problem instead.**
  - Bad: "When the dev server won't start, kill the orphaned node process on port 5173"
  - Good: Surface "dev server doesn't clean up on crash" as a follow-up action to fix

- **Don't propose entries for low-recurrence technical trivia unlikely to come up again.**

- **Don't auto-resolve review findings — every change requires user discussion and approval.**
