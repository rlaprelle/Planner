# Echel Planner

*Planning that works with your brain, not against it.*

Echel Planner is a daily work management tool designed for people with ADHD.

ADHD is fundamentally a disorder of self-regulation, not attention. The ADHD brain can often attend when sufficiently engaged — the deficit is in the capacity to *start*, *stop*, *sustain*, and *shift* behavior based on internal goals. This cascades into impairments across executive function: working memory overflows, time becomes invisible, motivation depends on urgency or novelty rather than importance, and the emotional weight of unfinished work compounds into paralysis.

As Russell Barkley puts it, ADHD "is not a problem of knowing what to do — it is a problem of doing what you know." Echel Planner is built on that principle. The app provides **structured rituals**: short, guided workflows that externalize the planning your brain struggles to do internally. They help you decide what matters, stay focused on one thing at a time, capture stray thoughts without losing momentum, and close out the day with a sense of accomplishment rather than anxiety about what you didn't finish.

## Core Concepts

### Rituals

The app is organized around **rituals** — guided ceremonies that bookend your day, week, and month.

| Ritual | When | What it does |
|--------|------|-------------|
| **Start Day** | Morning, 5-10 min | Review your active tasks, choose what to work on today, drag them onto a visual schedule |
| **End Day** | Evening, 10-15 min | Triage every active task (keep, defer, or cancel), process your notes from the day, reflect on the day |
| **Start Week** | Monday morning | Schedule tasks for the week ahead |
| **End Week** | Friday evening | Everything in End Day, plus weekly reflection |
| **Start Month** | First of the month | Longer-horizon planning and prioritization |
| **End Month** | End of the month | Everything in End Week, plus monthly reflection |

Start rituals help you plan. End rituals are the real engine — they create gentle pressure to keep your task list small by forcing you to look at every active task and consciously decide: *is this still worth doing?*

### Quick Capture

A persistent escape hatch for stray thoughts. A request to review a document, a deadline you just learned about, a meeting you need to schedule with a customer — press a shortcut or tap a button, type it, hit Enter. The note is saved and you're back to what you were doing.

The point is to let go. Once it's captured, it's safe. You'll process it during your next End Day ritual, not now.

Quick Capture also supports a **brain dump** mode: open-ended rapid capture for when you need to empty your head completely.

### Task Triage

The core mechanic of every End ritual. The app presents your active tasks one at a time and asks: *what do you want to do with this?*

- **Keep** — it stays on your active list
- **Defer** — push it to tomorrow, next week, or next month (it disappears until then)
- **Cancel** — you've decided it's not worth doing (a victory, not a failure)

This one-at-a-time approach prevents the overwhelm of staring at a long list. And because deferred tasks come back automatically, nothing falls through the cracks.

### Active Sessions

When it's time to work, you start an **active session** on a task. The app enters a focused mode: task title front and center, a gentle countdown timer, and an interactive checklist of subtasks. When time's up, a soft chime plays and you choose: *complete*, *extend*, or *done for now*.

No guilt. No red timers. Just a calm workspace that helps you stay on one thing.

### Points, Not Time

Tasks are estimated in **points** (1-5), not hours. Points are deliberately vague — they represent rough complexity, not precise duration. This removes the stress of time estimation while still giving you a sense of how much you're taking on.

## A Typical Day

**Morning.** You open the app and start your day ritual. A calm planning view shows your active tasks grouped by project. You pick 3-4 to focus on, drag them onto your day's schedule, and confirm. Five minutes, and you know exactly what you're doing today.

**Mid-morning.** You're deep in a task when a thought intrudes — *"I need to email Dave about that invoice."* You hit the Quick Capture shortcut, type it, hit Enter. A soft confirmation appears. The thought is gone from your head and safely in your inbox. Back to work.

**Afternoon.** You start an active session on your next task. The timer runs quietly. You check off subtasks as you go. When time's up, a chime plays. You mark it complete and feel the small satisfaction of progress.

**Evening.** You start your End Day ritual. The app walks you through every active task — you keep two, defer one to next week, and cancel one you've been avoiding for a month (it feels great). Then you process your inbox: that email to Dave becomes a real task in the right project. Finally, a brief reflection: how was your energy today? Your mood? A few words about what went well. Day closed.

## Design Principles

### UX Philosophy

- Be supportive, not judgemental. Never question the user's decisions. Ask "Is this still important?" not "Why haven't you done this?"
- The goal is to encourage the user to make informed decisions about how to spend their time, not to steer them toward a particular decision.
- Deciding NOT to do something is a victory. Keep task lists small and focused.
- Prefer soft deletes over hard deletes. Nothing should be permanently destroyed.
- The workflow is a suggestion, not a restriction.

### Visual Design

1. **Calm over clever** — The interface should feel quiet and grounding. Avoid visual noise: competing colors, dense layouts, animated distractions. When in doubt, remove rather than add.
2. **Soft shapes, soft colors** — Rounded corners (12-16px for containers, 8-12px for inline elements like badges and inputs). Muted tones from a soft lavender palette — dusty purples, warm grays, gentle off-whites. Avoid harsh borders, high-contrast outlines, and saturated colors except for intentional emphasis.
3. **One thing at a time** — Favor progressive disclosure over showing everything at once. Surface the current step or task prominently; let secondary information recede or be available on demand. Reduce decision fatigue by narrowing what's visible.
4. **Generous breathing room** — Relaxed whitespace and padding throughout. Elements should not feel crowded. Give each item space to be read without competing with its neighbors. Comfortable touch targets, spacious row heights.
5. **Warm, not clinical** — The palette and tone should feel personal and inviting, not sterile or corporate. Slight warmth in backgrounds (tinted off-whites, not pure white), soft shadows over hard borders. The app should feel like opening a journal, not a spreadsheet.
6. **Guide gently** — Use subtle visual hierarchy (tinted backgrounds, font weight, muted color shifts) to guide the eye toward what matters now, without demanding attention. Nothing should shout.
7. **Evoke physical artifacts** — UI elements should feel like tangible objects: notebooks, index cards, sticky notes. Rounded corners, soft shadows, and subtle depth cues create a tactile quality that makes the digital feel personal and approachable.

## Running Echel Planner

### Prerequisites

- Java 21
- Maven 3.8+
- Node.js 20+
- Docker Desktop

### Quick Start

**Git Bash / WSL:**
```bash
./start.sh
```

**PowerShell:**
```powershell
.\start.ps1
```

Both scripts start PostgreSQL (via Docker), the Spring Boot backend, and the Vite frontend. Press **Ctrl+C** to stop everything.

Once running, open **http://localhost:5173**.

### For Contributors

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed development setup, testing, project structure, and environment configuration.

## Documentation

### Research
- [`docs/research/adhd_research_v1.2.md`](docs/research/adhd_research_v1.2.md) — The neuroscience of ADHD as it relates to task management, time management, and executive function. This research informed the design of the tool.

### Architecture & Implementation
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — Data model, API endpoints, tech stack
- [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md) — Design decisions and implementation rationale
- [`docs/INTERNATIONALIZATION.md`](docs/INTERNATIONALIZATION.md) — i18n approach and phased plan
- [`docs/DEFERRED_WORK.md`](docs/DEFERRED_WORK.md) — Roadmap of deferred features and future work

### Original Design Documents
These documents capture the early vision and design rationale for the project. They are not living documents and may differ significantly from the current implementation.
- [`docs/planning/user_design/`](docs/planning/user_design/) — User wishes, use cases, core workflows, information architecture, wireframes

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Flyway |
| Frontend | React 18, Vite, Tailwind CSS, Radix UI, dnd-kit, TanStack Query |
| Database | PostgreSQL 16 |
| Infrastructure | Docker Compose |
