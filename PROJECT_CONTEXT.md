# PROJECT CONTEXT

> **PURPOSE**
>
> This file is the persistent memory, source of truth, and handoff document for this project.
>
> Any AI assistant, developer, or future version of Antigravity working on this project MUST read this file before making significant changes.
>
> **Do not assume previous AI conversation history is available.**
>
> The project must remain understandable and recoverable even if all previous conversations are lost.
>
> Keep this document updated as the project evolves.

---

# 1. PROJECT IDENTITY

## Project Name

`Digital Discipline (Zidd)`

## Short Description

Digital Discipline (Zidd) is an Android behavioral psychology and screen-time regulation application that introduces intentional friction and earned screen time before opening distraction apps. Instead of passive tracking or easily bypassable app blockers, it uses active micro-interventions—including on-device real-time AI computer vision pose workouts, mindful resets, and cognitive challenges—to restore conscious agency over phone usage.

## Project Type

`Mobile App (Android Native — Kotlin & Jetpack Compose)`

## Core Problem

Digital dopamine loops and compulsive phone opening occur automatically without friction (the moment between impulse and action is lost). Existing app blockers are either trivially bypassed, punitive, or rely on surveillance. Users need a system that builds mindful friction, replaces mindless scrolling with positive micro-habits, and enforces earned access across Self, Family, and Corporate environments.

## Target Users

### Primary Users

1. **Self Mode (Individual Adults)**: Individuals seeking to reclaim focus, break doom-scrolling habits, and replace compulsive app opening with fitness, breathing, and cognitive resets.
2. **Family Mode (Parents & Children)**: Parents managing up to 3 children's devices with QR-pairing, PIN overrides, earned screen time via physical/learning challenges, and bedtime schedules.
3. **Corporate Mode (Enterprise & Teams)**: Organizations with teams (minimum 5 employees) looking to enforce shared deep work hours and reduce workplace digital distractions.

### Secondary Users

- Educators, students, and fitness enthusiasts wanting a gamified, health-positive screen-time management tool.

## Core Value Proposition

"You don't have to quit your apps. You just need a better interruption."
Digital Discipline sits between impulse and action, requiring users to earn screen time through healthy physical movement (validated by on-device AI camera tracking), calming breathing exercises, or cognitive puzzles.

## Current Status

**Status:** `Active Development / Pre-Production Beta`

**Version:** `v1.0.0-beta (Phase 8D & Vision Studio Enhanced)`

**Current Phase:** `Phase 8D — Onboarding Redesign & Vision Pose Studio Integration`

**Last Updated:** `2026-08-31`

---

# 2. PRODUCT VISION

## Long-Term Vision

To become the global standard for conscious device usage across personal, family, and enterprise ecosystems—empowering users to replace 1,000+ hours of annual mindless screen time with physical health, mental clarity, and intentional productivity.

## MVP Goal

Deliver a rock-solid, tamper-resistant Android application that intercepts target distraction apps with high-performance Jetpack Compose overlays, validates physical/cognitive micro-interventions with on-device ML Kit pose detection, and tracks earned time in a local DataStore/Room wallet.

## What This Product IS

* **An Active Interruption Engine**: Inserts intentional pauses, physical exercises, and cognitive challenges before distraction apps open.
* **An On-Device AI Vision Studio**: Evaluates real-time body mechanics (Push-ups, Squats, Sit-ups, Wall Sit, Plank, Calf Raises) completely on-device without sending video or images to the cloud.
* **An Earned Time Wallet**: A secure bank of minutes earned through healthy behaviors to unlock controlled distraction time (e.g. 5m, 10m, 15m).
* **A Multi-Mode Ecosystem**: Seamless operation across Self, Family (max 3 children), and Corporate (min 5 employees) modes.

## What This Product IS NOT

* **A Spyware / Surveillance Tool**: Does not log keystrokes, capture screen recordings, or spy on user content.
* **A Simple Timer Blocker**: Does not just shut down apps arbitrarily without giving users healthy alternatives.
* **A Cloud-Dependent Computer Vision Tool**: Does not stream camera feeds to external servers; all ML Kit pose estimation is 100% on-device.

---

# 3. CORE PRODUCT PRINCIPLES

1. **On-Device First & Zero Surveillance**: All pose estimation, friction logic, and personal metrics stay on the device.
2. **Earned Agency Over Deprivation**: Frame screen time as an earned reward rather than a forbidden punishment.
3. **Biomechanical & Kinetic Integrity**: Camera vision challenges must accurately validate form (e.g., full range of motion, horizontal plank for push-ups, $90^\circ$ wall sit, adaptive heel elevation for calf raises) and never award reps falsely.
4. **Resilient Interception & Anti-Bypass**: Prevent circumvention via split-screen, recent apps switcher, or quick navigation while maintaining Google Play policy compliance.
5. **Fluid & Cinematic UX**: Use dark-themed, high-contrast visual cues (Neon Green `#22C55E` for good form, Bright Red `#EF4444` for paused/broken posture, animated phone placement vectors).

---

# 4. MVP DEFINITION

## MUST HAVE — Current MVP

* **Multi-Mode Support**: Self Mode, Family Mode (Parent + Child with QR pairing & PIN), Corporate Mode.
* **Intervention Engine**: 51 cataloged interventions across Movement, Breathing, Meditation, Yoga/Mobility, Physical Reset, and Cognitive challenges.
* **Vision Studio (Camera AI)**:
  - 1-2-3-GO sequence starting only upon valid stance detection.
  - Real-time skeletal overlay with dynamic neon-green/red form feedback.
  - Immediate pause/resume on broken form.
  - Target joint halos and live angle measurement.
  - Animated vector illustrations for camera setup and correct posture.
  - Mid-workout challenge switching.
* **App Interception**: `SpikeAccessibilityService` + Overlay Window detecting and blocking configured distraction packages.
* **Time Wallet**: Real-time balance tracking, session countdowns, and automatic re-locking upon balance exhaustion.

## NOT PART OF MVP

* iOS companion app (architecture planned, Android primary).
* Cloud ML model retraining.
* Third-party wearable heart rate sync.

## MVP Success Criteria

* Zero false-rep counting when user is merely facing camera or out of position.
* 100% reliable interception of selected distraction apps.
* Flawless challenge switching and countdown sequences in overlay and demo modes.
* 730+ unit tests passing consistently.

---

# 5. CORE USER FLOWS

## Primary User Journey

### Step 1 — Entry & Onboarding
User installs app, completes 11-step cinematic onboarding, selects distraction apps (e.g., Instagram, YouTube, TikTok), chooses intervention style, and grants Accessibility and Overlay permissions.

### Step 2 — Distraction App Open
User taps a blocked app on their home screen. `SpikeAccessibilityService` detects the launch event within 50ms.

### Step 3 — Interception Overlay
Full-screen Compose overlay appears, presenting an intentional pause, camera workout, cognitive reset (e.g., Mindful Hangman / Picture Puzzle), or breathing challenge.

### Step 4 — Challenge Completion & Unlock
User completes the exercise (e.g. 10 squats or 30s breathing). The wallet credits unlocked time (e.g., 10 minutes). The overlay dismisses and the distraction app opens.

---

# 6. PRODUCT RULES & NON-NEGOTIABLES

## Non-Negotiable Rules

1. **Family Mode Constraint**: Limited to a maximum of 3 children per parent account.
2. **Corporate Mode Constraint**: Limited to a minimum of 5 employees per corporate workspace.
3. **Mindful Hangman Catalog**: Contains a rich vocabulary of everyday mindful words (44+ words across Focus, Calm, Health, Energy, and Habits).
4. **Camera Vision Strictness**:
   - Push-ups MUST validate horizontal floor plank (not vertical upright).
   - Squats MUST validate full range ($>155^\circ \rightarrow <118^\circ \rightarrow >155^\circ$).
   - Reps/timers MUST ONLY count during `WorkoutStage.ACTIVE` (after "GO!").
   - Posture loss MUST immediately pause counters.

---

# 7. TECHNOLOGY STACK

## Platforms
- Android (API Level 26+ / Android 8.0 to Android 15+)

## Architecture & Frameworks
- **Language**: Kotlin 1.9+ / Kotlin Coroutines & Flow
- **UI Toolkit**: Jetpack Compose (Material3, Compose Navigation, Canvas)
- **Computer Vision**: Google ML Kit Pose Detection (Accurate Model) + CameraX (TextureView COMPATIBLE mode for overlay support)
- **Local Persistence**: Room Database (`AppDatabase`), AndroidX DataStore (`PreferencesManager`)
- **Background & Interception**: Android `AccessibilityService`, `WindowManager` Overlay System, WorkManager
- **Backend / Cloud**: Firebase Auth, Firebase Firestore, Cloud Functions
- **QR & Utilities**: ZXing Embedded (for Family pairing)

---

# 8. HIGH-LEVEL ARCHITECTURE

```text
               ┌───────────────────────────────┐
               │   Distraction App Trigger     │
               └───────────────┬───────────────┘
                               │ (Foreground App Event)
                               ▼
               ┌───────────────────────────────┐
               │   SpikeAccessibilityService   │
               └───────────────┬───────────────┘
                               │ (Check Distraction & Wallet Balance)
                               ▼
               ┌───────────────────────────────┐
               │  WindowManager Floating View  │
               │ (InterventionOverlayCompose)  │
               └───────────────┬───────────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         ▼                     ▼                     ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Camera Vision   │  │    Cognitive     │  │  Breathing /     │
│  Pose Studio     │  │ (Hangman/Puzzle) │  │  Physical Reset  │
│ (ML Kit Tracker) │  └──────────────────┘  └──────────────────┘
└────────┬─────────┘
         │ (Validated Completion)
         ▼
┌───────────────────────────────┐
│   EarnedTimeWalletService     │
│   (DataStore / Room Credit)   │
└───────────────────────────────┘
```

---

# 9. PROJECT STRUCTURE

```text
d:\Zidd\
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/digitaldiscipline/spike/
│   │   │   │   ├── behaviour/           # Plan templates, activation coordinators
│   │   │   │   ├── cloud/               # Firebase auth & Firestore managers
│   │   │   │   ├── data/                # Room entities, DAOs, PreferencesManager
│   │   │   │   ├── intervention/
│   │   │   │   │   ├── catalog/         # 51 Intervention definitions & categories
│   │   │   │   │   ├── model/           # InterventionCategory, Difficulty, ValidationType
│   │   │   │   │   ├── session/         # Authoritative InterventionSession state machine
│   │   │   │   │   ├── validation/      # CameraPoseValidator, TimerValidator
│   │   │   │   │   └── vision/          # ML Kit Pose Classifier, Canvas, PoseAngleCalculator
│   │   │   │   ├── overlay/             # WindowManager Compose overlays & accessibility UI
│   │   │   │   ├── service/             # SpikeAccessibilityService, NotificationListener
│   │   │   │   ├── ui/
│   │   │   │   │   ├── auth/            # Mode selection, Login, Register
│   │   │   │   │   ├── dashboard/       # TodayScreen, ChildDashboard, Picker, ParentDashboard
│   │   │   │   │   ├── onboarding/      # SelfModeOnboarding, ChildOnboarding
│   │   │   │   │   ├── theme/           # Dark theme design tokens
│   │   │   │   │   └── vision/          # CameraPoseWorkoutScreen, AnimatedExerciseGuideCanvas
│   │   │   │   └── wallet/              # EarnedTimeWalletService
│   │   │   └── res/                     # Drawables, layouts, values
│   │   └── test/                        # 736 unit tests for catalog, classifiers, wallet
├── docs/                                # Phase audit & technical decisions
├── PROJECT_CONTEXT.md                   # Source of truth & persistent memory
└── README.md                            # High-level overview
```

---

# 10. CURRENT PROJECT STATE & RECENT WORK

## Last Completed Work
1. **Pose Classifier Overhaul**: Added strict horizontal floor plank for Push-ups, full standing-to-deep-squat range of motion for Squats, dedicated Core Sit-ups classifier, and adaptive baseline for Calf Raises.
2. **1-2-3-GO Sequence & Form Pause**: Uninterrupted countdown that triggers only on stable starting stance; instant pause with bright red wireframe whenever posture breaks.
3. **Animated Setup & Form Illustration Engine**: Added `AnimatedExerciseGuideCanvas.kt` with live vector animations for phone placement (floor 45° angle vs table waist height) and looping skeletal form demonstrations.
4. **Running Challenge Switcher**: Seamless switching between camera workouts, Mindful Hangman, 3x3 Puzzle, Box Breathing, and Step Away.
5. **Test Suite Status**: All 736 unit tests passing cleanly.

## What Is Working
- Distraction app interception via `SpikeAccessibilityService`.
- On-device ML Kit pose detection & skeletal wireframe rendering.
- Complete 1-2-3-GO countdown and neon-green/red dynamic feedback.
- Challenge switcher across overlay and demo picker studios.
- Clean installation and launch on connected Android device (`5d4d404c`).

---

# 11. EXACT NEXT TASK

## Task Name
`Phase 8E — Verification, Edge-Case Hardening & Cross-Mode Challenge Polish`

## Objective
1. Conduct user-testing on connected physical device across all 51 challenges in catalog.
2. Verify edge cases in low-light environments and portrait/landscape rotations.
3. Ensure Family Mode PIN override and remote sync gracefully respect the updated challenge switcher.

## Relevant Files
- `d:\Zidd\app\src\main\java\com\digitaldiscipline\spike\ui\vision\CameraPoseWorkoutScreen.kt`
- `d:\Zidd\app\src\main\java\com\digitaldiscipline\spike\ui\vision\AnimatedExerciseGuideCanvas.kt`
- `d:\Zidd\app\src\main\java\com\digitaldiscipline\spike\intervention\vision\ExercisePoseClassifier.kt`
- `d:\Zidd\app\src\main\java\com\digitaldiscipline\spike\overlay\InterventionOverlayCompose.kt`
- `d:\Zidd\app\src\main\java\com\digitaldiscipline\spike\ui\dashboard\InterventionCatalogPickerScreen.kt`

---

# 12. SOURCE OF TRUTH HIERARCHY

```text
1. Explicit current user instruction
          ↓
2. PROJECT_CONTEXT.md
          ↓
3. Confirmed architecture / decision documents (docs/)
          ↓
4. Current working code
          ↓
5. AI assumptions (lowest priority)
```

---
