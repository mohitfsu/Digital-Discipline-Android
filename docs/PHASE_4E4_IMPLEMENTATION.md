# Phase 4E-4: Personal Habit Plan Refinement & Long-Term Continuity — Implementation

## Mission Overview
Phase 4E-4 transitions a Self Mode user from their initial 7-day habit-formation loop into a sustainable ongoing personal behavior routine. It answers: *"Now that I've tried this for a week, what should I continue, change, or simplify?"* while keeping the user in full control.

## Key Architectural Components

### 1. `PlanContinuityModel.kt`
- **Location**: `app/src/main/java/com/digitaldiscipline/spike/behaviour/continuity/PlanContinuityModel.kt`
- **Role**: Defines data models for plan continuity lifecycle (`PlanContinuityState`), evidence summaries (`PlanEvidenceSummary`), before/after change preview (`PlanChangePreview`), and full snapshots (`PlanContinuitySnapshot`).

### 2. `PlanContinuityEngine.kt`
- **Location**: `app/src/main/java/com/digitaldiscipline/spike/behaviour/continuity/PlanContinuityEngine.kt`
- **Role**: Deterministic orchestration engine that evaluates week progression ($1, 2, 3, \dots$), checks plan health, surfaces `AdaptivePlanEngine` recommendations, and builds concrete before/after diffs for user review.

### 3. `SelfPlanContinuityScreen.kt`
- **Location**: `app/src/main/java/com/digitaldiscipline/spike/ui/dashboard/SelfPlanContinuityScreen.kt`
- **Role**: Dedicated plan review & refinement screen providing What Worked / What Didn't evidence, recommendation previews, and explicit user decision actions (`[ KEEP MY PLAN ]`, `[ APPLY REFINEMENT ]`, `[ CHANGE GOAL ]`, `[ START FRESH ]`).

### 4. `TodayScreen.kt` Integration
- **Location**: `app/src/main/java/com/digitaldiscipline/spike/ui/dashboard/TodayScreen.kt`
- **Role**: Displays a compact "YOUR PLAN" card indicating current week cycle, plan health, and a direct review action.

### 5. `MainActivity.kt` Navigation
- **Location**: `app/src/main/java/com/digitaldiscipline/spike/ui/MainActivity.kt`
- **Role**: Routes `"PLAN_CONTINUITY"` sub-screen to render `SelfPlanContinuityScreen`.
