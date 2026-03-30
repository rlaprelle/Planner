# Phase 1: User Wishes & Aspirations

**Purpose**: Understand the full vision for what you want to accomplish and how this tool should make you feel. No constraints here — just brainstorm freely.

---

## What Does a "Successful Day" Feel Like?

*Describe what using this tool for a full day would feel like if everything went perfectly:*
- How do you feel when you open the app in the morning?
- What happens throughout the day?
- How do you feel at the end of the day?
- What accomplishment feels good?

**Your answer:**

A successful day is one where:
- **Morning**: Open the app to a guided 5-10 minute morning planning ritual (step-by-step prompts, but feeling natural and conversational, not rigid)
- **Throughout the day**: Stay focused on one thing at a time, with the ability to jot unstructured notes whenever something random comes up (knowing it will be handled later, not cluttering my head)
- **End of day**: See tangible accomplishment that reflects the true significance of what was done (accounting for effort, impact, and difficulty), and feel a sense of closure and peace
- **Emotional tone**: The interface is calm, simple, and uncluttered — it feels like a supportive presence, not a demanding manager

---

## Biggest ADHD-Related Obstacles

*What are the top obstacles you face with task management and work planning?*
- What makes you procrastinate?
- When do you get overwhelmed?
- What causes you to lose track of what you're working on?
- Where do tasks "disappear" into a black hole?
- What's hard about starting a task?
- What's hard about finishing a task?

**Your answer:**

**Paralysis**, especially at two critical moments:
1. **At the very beginning** — Getting started on a big task is hard
2. **When hitting an early roadblock** — Momentum breaks when something doesn't work immediately

**Approach to solving paralysis**:
- Start with a "Just make it smaller" paradigm — the user owns the process of breaking down their task
- But offer a non-pressuring "suggest help" button: "Do you think it would help to talk about the first few micro-steps?"
- Only show guided breakdown UI if the user accepts the suggestion
- This prevents feeling trapped while offering help when needed

**Architectural note**: Build flexible enough architecture that new "breakthrough" UIs can be easily added by developers to help with different types of freezes (possibly via microservices that suggest workflows and offer guidance).

---

## What Would Feel "Magical" or Surprising?

*If this tool did something unexpected that felt amazing, what would it be?*
- A feature you didn't know you needed?
- A workflow that feels effortless?
- A way the tool understands you?
- Something that makes you actually want to use it?

**Your answer:**

The ability to jot unstructured notes and have them intelligently handled later (deferred items). This escape hatch should be available in almost every flow so the tool never feels constraining. Once captured, I should be able to mentally let it go, trusting the system to process it at the right time.

**Future vision** (out of scope for MVP, but exciting): LLM-integrated morning ritual that flows like a conversation with a supportive coach rather than a rigid step-by-step form.

---

## Emotional Outcomes

*How should you feel using this tool day-to-day?*
- Energized or calm?
- In control or supported?
- Proud of progress or relieved of burden?
- What feeling would make you open the app voluntarily?

**Your answer:**

**Calm, grounded, and supported.** The tool should feel like:
- A mindfulness exercise that roots me in the here and now
- A supportive presence that reduces anxiety (not adds it)
- A place where I can let go of future worries because they're safely captured
- A celebration of what I actually accomplished, not a judge of what I didn't do
- A way to feel in control without feeling overwhelmed

---

## Specific Pain Points to Solve

*For each of these common ADHD challenges, describe how badly you experience it and what the ideal solution would look like:*

### Decision Fatigue / What Matters
- **Pain**: Too many choices about what to work on; feels pressured to do everything; hard to separate signal from noise
- **Your approach**:
  - Set rough, approximate priorities ahead of time (via morning ritual or planning cadence) — have intentionality about what matters
  - BUT actively encourage consciously changing the plan — don't treat it as rigid
  - When pivoting: make it easy (minimal friction), explicitly prompt to reconsider at intervals or when blocked, and show trade-offs (what are you deprioritizing?)
  - The tool should help separate what matters from what doesn't, so you can focus on what actually matters

### Context Switching / Distractions
- **Pain**: Random ideas, new requests, anything that pops up during the day breaks focus
- **Your approach**:
  - The deferred item capture (jot-a-note) is the escape hatch — anything that deserves thought but not *right now* gets captured
  - Once captured, you need to feel confident it won't be lost:
    - Brief visual/audio confirmation that it was saved
    - Ability to review your deferred inbox anytime if you get worried
    - Trust that it will be processed at regular review times (daily, weekly, etc.)
  - This lets you mentally let go of distractions and refocus without guilt or anxiety

### Time Estimation
- **Pain**: Time estimates create stress and are often inaccurate
- **Your approach**:
  - Use "points" instead of time estimates — deliberately vague and stress-free
  - Points roughly indicate complexity/scope (1 point = small, 5 points = big) without the precision anxiety of "45 minutes"
  - Over time, the tool can help you learn your own patterns (e.g., "You said 3 points, it actually took 2 hours")
  - But the initial estimate itself should be judgment-free and low-pressure

### Progress Invisibility
- **Pain**: Can't see what you've accomplished; easy to feel like you did nothing
- **Your approach**:
  - **Streak tracking**: Track consecutive days of morning planning, weekly reviews, or consistent work
  - **Weekly/monthly summaries**: Show stats like "You completed 12 tasks this week" with trends over time
  - **Intelligent celebrations**: Congratulate the user specifically on tasks that were large in terms of:
    - Hours spent
    - Complexity/difficulty (high-point tasks)
    - Level of effort required
  - Progress should correlate to actual significance, not just task count

### Task Organization
- **Pain**: Tasks sprawl across Areas/Projects; hard to separate signal from noise; don't know which tasks matter
- **Your approach**:
  - Don't enforce organization — let the user make their own organizational decisions
  - BUT give them protected time during cadenced reviews (daily, weekly, monthly, yearly) to consciously organize and prioritize
  - **Key constraint: Track deadlines prominently** — tasks with approaching deadlines naturally force prioritization
  - The tool should make deadlines visible and help the user consciously decide what matters based on due dates and effort

---

## Work Patterns & Preferences

*Help me understand how you actually work:*

- What time of day do you have the most energy?
- How long can you focus before needing a break?
- Do you prefer small frequent tasks or large deep-work blocks?
- How many different "Areas of Work" (customers, projects, teams) do you juggle?
- Do you work on recurring tasks? (daily standup, weekly planning, etc.)
- Do you prefer visual information (charts, timers) or just text?
- Do you like reminders or would they feel intrusive?

**Your answer:**

*To be filled in with guided questions*

---

## Non-Negotiables & Nice-to-Haves

*What's essential vs. optional for you?*

**Must have**:
- Feature 1
- Feature 2
- Feature 3

**Really want**:
- Feature 1
- Feature 2

**Nice to have**:
- Feature 1
- Feature 2

**Don't care about**:
- Feature 1
- Feature 2

---

## Comparison & Inspiration

*Are there other tools or systems you've tried or admired?*
- What did you like about them?
- What frustrated you?
- What would you steal from them?

**Your answer:**

---

## Success Metrics

*How would you know this tool is actually working for you?*
- What would you measure? (tasks completed, stress level, time clarity?)
- What would be the breakthrough moment that proves it works?

**Your answer:**

---

## Notes & Thoughts

*Anything else that feels important to mention?*

**Your answer:**
