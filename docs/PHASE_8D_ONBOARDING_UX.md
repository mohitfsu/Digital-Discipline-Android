# Phase 8D — Onboarding UX Principles

## Core Philosophy

> "The onboarding must feel emotionally intelligent and premium.
>  It should not feel like a settings wizard. The user should reach
>  the end thinking: 'I don't have to completely give up my phone.
>  I just need help creating a pause before I lose control.'"

## UX Principles Implemented

### 1. One Question Per Screen
Every screen presents exactly one question or one moment of insight. There is no multi-field
form, no scrolling through choices on the same screen as instructions, and no crowded step.

### 2. Cinematic Pacing
Three screens use `LaunchedEffect` with deliberate delays to reveal content in timed phases:
- **Screen 0**: 3-phase reveal over ~4 seconds. User cannot rush it.
- **Screen 4**: Animated lifetime projection appears after brief pause.
- **Screen 5**: IMPULSE→PAUSE→CHOOSE flow reveals in 3 stages.

### 3. Emotional Arc
The onboarding follows a deliberate emotional progression:

```
RECOGNITION (Screen 0–1): "This is me. This is what I do."
AWARENESS (Screen 2–3): "I didn't realize how much time I was giving away."
REFRAME (Screen 4–5): "It doesn't have to be this way. I don't have to quit."
AGENCY (Screen 6–8): "I choose how to be interrupted. I choose the access rules."
COMMITMENT (Screen 9–10): "I'm setting this up. I'm starting with intention."
```

### 4. Disabled CTA = No Confusion
The Continue button is visually disabled (dim, cannot tap) until the screen's selection
requirement is met. This prevents proceeding without meaningful input and avoids silent defaults.

### 5. Positive Friction as Product Demo
Screen 7 is itself an intervention — 3 taps for 3 breaths. The user experiences the
Positive Friction loop before setting it up. This is a product principle, not just onboarding.

### 6. Privacy-First Permission Screen
Screen 9 explains *why* each permission is needed with plain language, and explicitly states
what we do NOT collect. This earns trust before enforcement begins.

### 7. Closing With Intention
Screen 10 shows a summary card (not just a "you're done!" screen) and closes with:
> "The goal isn't to use Digital Discipline more. It's to need it less."

This sets the right expectation for the product's long-term value.

## Distraction → Control Product Philosophy

```
DISTRACTION   →  user opens a protected app
INTERRUPTION  →  Digital Discipline shows an overlay
POSITIVE FRICTION  →  user completes an intervention (5–30 seconds)
SMALL ACTION  →  earned access time is granted
REGAINED CONTROL  →  user makes an intentional choice to use the app
```

The onboarding instills this loop explicitly on Screen 5 and demonstrates it on Screen 7.
