# Design Principles

> **Purpose:** UX philosophy, visual design language, animation conventions.
> **Audience:** Both humans and agents doing UX work.
> **Out of scope:** Architecture, data model, dev setup.

---

## UX Philosophy

- Be supportive, not judgemental. Never question the user's decisions. Ask "Is this still important?" not "Why haven't you done this?"
- The goal is to encourage the user to make informed decisions about how to spend their time, not to steer them toward a particular decision.
- Deciding NOT to do something is a victory. Keep task lists small and focused.
- Prefer soft deletes over hard deletes. Nothing should be permanently destroyed.
- The workflow is a suggestion, not a restriction.

## Visual Design

1. **Calm over clever** — The interface should feel quiet and grounding. Avoid visual noise: competing colors, dense layouts, animated distractions. When in doubt, remove rather than add.
2. **Soft shapes, soft colors** — Rounded corners (12-16px for containers, 8-12px for inline elements like badges and inputs). Muted tones from a soft lavender palette — dusty purples, warm grays, gentle off-whites. Avoid harsh borders, high-contrast outlines, and saturated colors except for intentional emphasis.
3. **One thing at a time** — Favor progressive disclosure over showing everything at once. Surface the current step or task prominently; let secondary information recede or be available on demand. Reduce decision fatigue by narrowing what's visible.
4. **Generous breathing room** — Relaxed whitespace and padding throughout. Elements should not feel crowded. Give each item space to be read without competing with its neighbors. Comfortable touch targets, spacious row heights.
5. **Warm, not clinical** — The palette and tone should feel personal and inviting, not sterile or corporate. Slight warmth in backgrounds (tinted off-whites, not pure white), soft shadows over hard borders. The app should feel like opening a journal, not a spreadsheet.
6. **Guide gently** — Use subtle visual hierarchy (tinted backgrounds, font weight, muted color shifts) to guide the eye toward what matters now, without demanding attention. Nothing should shout.
7. **Evoke physical artifacts** — UI elements should feel like tangible objects: notebooks, index cards, sticky notes. Rounded corners, soft shadows, and subtle depth cues create a tactile quality that makes the digital feel personal and approachable.

## CSS & Animation

- **CSS `transform` doesn't compose** — Keyframe animations that set `transform` will overwrite Tailwind transform utilities (`-translate-x-1/2`, `scale-50`, etc.) on the same element. Use nested elements to separate transforms that need to coexist.
- **Smooth arcs via split-axis animation** — Mid-journey keyframes stutter because CSS interpolates linearly between stops, creating visible direction changes. Instead, nest two elements and animate each axis independently with different timing functions (e.g., linear X + ease-out Y produces a natural arc).
