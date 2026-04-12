# Session Retrospective Skill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a personal Claude Code skill that runs collaborative session retrospectives, producing CLAUDE.md entries and follow-up actions through structured discussion.

**Architecture:** Single SKILL.md file at `~/.claude/skills/session-retrospective/SKILL.md`. The skill is a structured prompt — no code, no scripts, no supporting files. All behavior is encoded as instructions in the SKILL.md itself.

**Tech Stack:** Markdown (SKILL.md format with YAML frontmatter)

---

### Task 1: Create SKILL.md with Frontmatter and Overview

**Goal:** Create the skill file with metadata, hard rules, and the overview section.

**Files:**
- Create: `~/.claude/skills/session-retrospective/SKILL.md`

**Acceptance Criteria:**
- [ ] YAML frontmatter has `name: session-retrospective` and a description matching the spec's trigger section
- [ ] Hard rules section encodes all 5 rules from the spec (no automatic changes, on-demand only, root causes over symptoms, generalize over specifics, collaborative not compliant)
- [ ] Overview explains the skill's purpose in 2-3 sentences

**Verify:** `cat ~/.claude/skills/session-retrospective/SKILL.md | head -30` → shows valid frontmatter and overview

**Steps:**

- [ ] **Step 1: Create the skill directory**

```bash
mkdir -p ~/.claude/skills/session-retrospective
```

- [ ] **Step 2: Write the frontmatter and overview**

```markdown
---
name: session-retrospective
description: "Use when a session is complete and the user wants to capture learnings. Triggered by /retro or when user says 'let's do a retro', 'what did we learn', 'session review', or 'capture learnings'. On-demand only — never suggest proactively."
---

# Session Retrospective

A collaborative retrospective that captures workflow learnings as CLAUDE.md entries. Claude and the user review the session together, identify friction points, agree on what to encode, and write the results — with explicit user approval at every step.

## Hard Rules

1. **No automatic changes.** Every CLAUDE.md addition, modification, or deletion requires explicit user approval. Claude proposes, the user decides. No exceptions.
2. **On-demand only.** Never suggest a retro proactively. The user initiates.
3. **Root causes over symptoms.** If a workaround was needed, the friction point is why the workaround was needed, not the workaround itself.
4. **Generalize over specifics.** Extract the broader principle from the specific incident.
5. **Collaborative, not compliant.** Push back when you disagree, cite session evidence, and challenge assumptions. A retro where everyone agrees on everything is a failed retro.
```

- [ ] **Step 3: Commit**

```bash
git add ~/.claude/skills/session-retrospective/SKILL.md
git commit -m "feat: session-retrospective skill — frontmatter and overview"
```

---

### Task 2: Write Phase 1 (Analyze) and Phase 2 (Solicit)

**Goal:** Add the analysis and solicitation phases that produce the friction point list.

**Files:**
- Modify: `~/.claude/skills/session-retrospective/SKILL.md`

**Acceptance Criteria:**
- [ ] Phase 1 instructs Claude to review the full session and produce a numbered list
- [ ] Each friction point format includes: what happened, why it was friction, suggested target file
- [ ] Phase 2 explicitly asks the user for additions
- [ ] The announce-at-start instruction is included

**Verify:** `grep -c "Phase" ~/.claude/skills/session-retrospective/SKILL.md` → at least 2

**Steps:**

- [ ] **Step 1: Add the phases section**

Append to SKILL.md after the Hard Rules section:

```markdown
## Flow

**Announce at start:** "I'm using the session-retrospective skill to run a collaborative retro on this session."

### Phase 1: Analyze

Review the full session and produce a numbered list of friction points. For each:

1. **What happened** — the observable friction, grounded in a specific moment in the session (reference message numbers or quotes where possible)
2. **Why it was friction** — the underlying cause, not the surface symptom
3. **Suggested target** — `local` (project CLAUDE.md), `global` (~/.claude/CLAUDE.md), or `discuss` if unclear

Focus on friction that is likely to recur. Skip issues that were caught and resolved immediately within the session. Apply the root-cause and generalization rules — if the specific incident points to a broader pattern, frame it that way.

### Phase 2: Solicit

After presenting the list, ask: **"Anything I missed? Any friction you felt that I might not have noticed?"**

Append the user's additions to the list. If the user adds something that seems like a one-off or is already covered by an existing item, say so — but add it if they insist.
```

- [ ] **Step 2: Commit**

```bash
git add ~/.claude/skills/session-retrospective/SKILL.md
git commit -m "feat: session-retrospective — analyze and solicit phases"
```

---

### Task 3: Write Phase 3 (Discuss)

**Goal:** Add the discussion phase with the two-stage intent/wording flow.

**Files:**
- Modify: `~/.claude/skills/session-retrospective/SKILL.md`

**Acceptance Criteria:**
- [ ] Phase 3 walks through friction points one at a time
- [ ] Stage A (Intent) includes instructions for Claude to push back when appropriate
- [ ] Stage B (Wording) includes instructions for proposing specific text and target file
- [ ] Claude is instructed to push back on vague or verbose wording

**Verify:** `grep -c "Stage" ~/.claude/skills/session-retrospective/SKILL.md` → at least 2

**Steps:**

- [ ] **Step 1: Add Phase 3**

Append to SKILL.md after Phase 2:

```markdown
### Phase 3: Discuss

Walk through each friction point one at a time. Each point has two stages:

**Stage A — Intent: Is this worth encoding?**

Present the friction point and explain why you flagged it. Then discuss:

- If you believe it's real and recurring, make your case with session evidence
- If the user wants to reject it but you disagree, push back — "This came up N times and cost us M minutes. Are you sure it's not worth a one-liner?"
- If the user wants to encode something you think is a one-off, say so — "This feels specific to today's situation. Would it actually help in future sessions?"
- Outcome: **encode it** (proceed to Stage B) or **drop it** (move to the next point)

**Stage B — Wording: What exactly do we write?**

Only reached if Stage A results in "encode it." Propose:

1. The specific text to add or change in CLAUDE.md (keep to one line when possible)
2. The target file: `./CLAUDE.md` (local/project) or `~/.claude/CLAUDE.md` (global)
3. Where in the file it should go (which section, or a new section if none fits)

If an existing entry should be updated rather than a new one added, show the before/after.

The user can edit the wording. Push back if:
- The entry is too vague to be actionable ("be more careful with X")
- The entry is too verbose for prompt space (CLAUDE.md is loaded into every session)
- The entry encodes a workaround instead of addressing the root cause
- The entry is a judgment about the user rather than guidance for Claude

Outcome: **final text + target file**, or rework until both sides agree.
```

- [ ] **Step 2: Commit**

```bash
git add ~/.claude/skills/session-retrospective/SKILL.md
git commit -m "feat: session-retrospective — discussion phase with intent/wording stages"
```

---

### Task 4: Write Phase 4 (Review)

**Goal:** Add the review phase that dispatches an agent to check consistency, quality, and scope.

**Files:**
- Modify: `~/.claude/skills/session-retrospective/SKILL.md`

**Acceptance Criteria:**
- [ ] Phase 4 dispatches a single agent with all accepted entries and both CLAUDE.md files
- [ ] Agent checks consistency (within-file contradictions/duplicates), quality (concise/actionable/falsifiable), and scope (right file)
- [ ] Local-contradicts-global is flagged as a possible global flaw, not an error
- [ ] Findings are discussed with the user, not auto-resolved

**Verify:** `grep "agent" ~/.claude/skills/session-retrospective/SKILL.md` → appears in Phase 4 context

**Steps:**

- [ ] **Step 1: Add Phase 4**

Append to SKILL.md after Phase 3:

```markdown
### Phase 4: Review

Once all friction points have been discussed, launch a review agent using the Agent tool. The agent receives:

- All accepted entries (text + target file)
- The full content of `./CLAUDE.md` (local/project)
- The full content of `~/.claude/CLAUDE.md` (global)

**Agent prompt should instruct it to check three things:**

1. **Consistency** — Does any new entry contradict or near-duplicate an existing entry *within the same file*? Flag for merging or rewording.
   - A local entry contradicting a global entry is **valid** (project-specific override). But flag it: "This overrides the global rule '[X]'. Is the global rule too broad, or is this a legitimate project exception?"
2. **Quality** — Is each entry concise, actionable, and falsifiable? Would a future Claude session know exactly what to do differently based on this entry alone?
3. **Scope** — Is each entry in the right file? Project-specific guidance → local CLAUDE.md. Cross-project behavioral rules → global CLAUDE.md.

**The agent returns a structured report.** For each entry: "looks good" or a specific concern with a suggestion.

Walk through flagged items with the user one at a time. Same collaborative style as Phase 3 — discuss, agree, adjust. No auto-resolution. Every change requires explicit user approval.
```

- [ ] **Step 2: Commit**

```bash
git add ~/.claude/skills/session-retrospective/SKILL.md
git commit -m "feat: session-retrospective — review phase with agent dispatch"
```

---

### Task 5: Write Phase 5 (Write) and Phase 6 (Follow-ups)

**Goal:** Add the final phases for writing approved entries and surfacing follow-up actions.

**Files:**
- Modify: `~/.claude/skills/session-retrospective/SKILL.md`

**Acceptance Criteria:**
- [ ] Phase 5 writes entries to target files with explicit user confirmation per file
- [ ] Phase 6 presents non-CLAUDE.md follow-up actions as a list
- [ ] Follow-ups are informational only — no enforcement

**Verify:** `grep -c "Phase" ~/.claude/skills/session-retrospective/SKILL.md` → 6

**Steps:**

- [ ] **Step 1: Add Phases 5 and 6**

Append to SKILL.md after Phase 4:

```markdown
### Phase 5: Write

Present the final list of approved entries grouped by target file:

**Changes to `./CLAUDE.md`:**
- [entry 1]
- [entry 2]

**Changes to `~/.claude/CLAUDE.md`:**
- [entry 3]

Ask: **"Ready to write these changes?"**

Wait for explicit confirmation before writing. Apply changes using the Edit tool (prefer editing existing sections over appending new ones). Show the diff after each file is modified.

### Phase 6: Follow-ups

If any non-CLAUDE.md actions surfaced during discussion (tools to install, environment changes, bugs to file, code to fix), present them as a simple list:

**Follow-up actions (for your reference):**
- [ ] [action 1]
- [ ] [action 2]

These are suggestions, not instructions. The user acts on them at their discretion. Do not take any of these actions automatically.
```

- [ ] **Step 2: Commit**

```bash
git add ~/.claude/skills/session-retrospective/SKILL.md
git commit -m "feat: session-retrospective — write and follow-up phases"
```

---

### Task 6: Write Patterns and Anti-Patterns

**Goal:** Add the patterns and anti-patterns sections with examples.

**Files:**
- Modify: `~/.claude/skills/session-retrospective/SKILL.md`

**Acceptance Criteria:**
- [ ] All 8 patterns from the spec are included with examples where specified
- [ ] All 9 anti-patterns from the spec are included with examples where specified
- [ ] Examples use Bad/Good format
- [ ] Each pattern/anti-pattern has 0 or 1 example (no more)

**Verify:** `grep -c "Bad:\|Good:" ~/.claude/skills/session-retrospective/SKILL.md` → matches spec count (8 bad + 8 good = 16 lines across both sections)

**Steps:**

- [ ] **Step 1: Add patterns and anti-patterns**

Append to SKILL.md after Phase 6. Copy the Patterns and Anti-Patterns sections exactly as they appear in the spec (`docs/superpowers/specs/2026-04-11-session-retrospective-skill-design.md`), including all examples.

- [ ] **Step 2: Commit**

```bash
git add ~/.claude/skills/session-retrospective/SKILL.md
git commit -m "feat: session-retrospective — patterns and anti-patterns with examples"
```

---

### Task 7: Final Review and Verification

**Goal:** Verify the complete skill file is well-formed and discoverable.

**Files:**
- Verify: `~/.claude/skills/session-retrospective/SKILL.md`

**Acceptance Criteria:**
- [ ] Frontmatter parses correctly (name + description)
- [ ] All 6 phases are present in order
- [ ] Hard rules section is present
- [ ] Patterns and anti-patterns sections are present
- [ ] File is under 500 lines (per skill convention)
- [ ] No TODOs, TBDs, or placeholder text

**Verify:** `wc -l ~/.claude/skills/session-retrospective/SKILL.md` → under 500; `head -5 ~/.claude/skills/session-retrospective/SKILL.md` → valid frontmatter

**Steps:**

- [ ] **Step 1: Read the complete file and verify structure**

```bash
cat ~/.claude/skills/session-retrospective/SKILL.md
```

Verify:
- Frontmatter has `name` and `description`
- Sections appear in order: Hard Rules → Flow (Phases 1-6) → Patterns → Anti-Patterns
- No placeholder text or incomplete sections
- Under 500 lines total

- [ ] **Step 2: Test discoverability**

The skill should appear when Claude Code lists available skills. Verify the directory structure:

```bash
ls -la ~/.claude/skills/session-retrospective/
```

Expected: single `SKILL.md` file.

- [ ] **Step 3: Final commit if any adjustments were made**

```bash
git add ~/.claude/skills/session-retrospective/SKILL.md
git commit -m "chore: session-retrospective skill — final review cleanup"
```
