# Phase 8D — Onboarding State Machine

## State Diagram

```
NOT_STARTED
    │  (user opens app for first time in SELF mode)
    ▼
STEP 0 (opening)
    │  tap "BUILD MY PLAN"
    ▼
STEP 1..10  (each step persisted via setSelfOnboardingStep)
    │
    ├── On any back navigation: step -= 1
    │
    └── On step 10 activation success:
         setSelfOnboardingState("COMPLETED")
         setOnboardingCompleted(true)
         → post-onboarding dashboard
```

## DataStore Keys and Lifecycle

| Key | Written | Read | Reset |
|-----|---------|------|-------|
| `self_onboarding_step` | On every step advance | On cold start to resume | Not reset (for resume) |
| `self_onboarding_state` | On activation success | Not used in UI (auditing only) | Not reset |
| `onboarding_completed` | On activation success | MainActivity routing | Not reset |
| `enabled_categories` | On activation | PolicyEngine, InterventionSelector | On re-onboarding only |
| `enabled_interventions` | On activation | InterventionSelector | On re-onboarding only |
| `user_display_name` | During activation if set | TodayScreen header | Never |
| `onboarding_behaviour_pattern` | After Screen 1 selection | Not used post-onboarding | Never |
| `onboarding_screen_time_estimate` | After Screen 3 selection | Not used post-onboarding | Never |

## Resume Behavior

On app relaunch before onboarding is complete:
1. `isOnboardingCompleted = false` → onboarding still shown
2. `savedStep = selfOnboardingStepFlow.collectAsState(initial = 0)`
3. If `savedStep in 1..10 && step == 0` → jump directly to saved step
4. Step 0 is never persisted (it's always a fresh cinematic experience)

## Per-Screen Validation

| Step | `canContinue` condition |
|------|------------------------|
| 0 | Always `true` (tap CTA to advance) |
| 1 | `selectedPattern.isNotEmpty()` |
| 2 | `selectedApps.isNotEmpty()` |
| 3 | `selectedTimeEstimate.isNotEmpty()` |
| 4 | Always `true` |
| 5 | Always `true` |
| 6 | `selectedCategories.isNotEmpty()` |
| 7 | `microDone` (or can skip via "SKIP FOR NOW") |
| 8 | Always `true` (default 10 min) |
| 9 | `isAccessibilityGranted && isOverlayGranted` |
| 10 | `!isActivating` |
