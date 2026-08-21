# Phase 8D — Onboarding Implementation Guide

## Overview

Phase 8D completely replaces the 6-step `SelfModeOnboardingScreen.kt` with a premium 11-screen
cinematic onboarding flow. All existing enforcement, wallet, parent precedence, adaptive learning,
Room v9 persistence, and accessibility functionality is preserved and unmodified.

## File Location

- **Primary**: `app/src/main/java/com/digitaldiscipline/spike/ui/onboarding/SelfModeOnboardingScreen.kt`
- **Preferences**: `app/src/main/java/com/digitaldiscipline/spike/data/preferences/PreferencesManager.kt`
- **Tests**: `app/src/test/java/com/digitaldiscipline/spike/SelfModeOnboarding8DTest.kt`

## Screen Inventory

| Screen | Step | Content | Validation |
|--------|------|---------|------------|
| `Ob0Opening` | 0 | Cinematic 3-phase animated opening | None (auto-advance on tap) |
| `Ob1BehaviourPattern` | 1 | 5-option single-select | Must select |
| `Ob2AppPicker` | 2 | LazyColumn app multi-select | ≥ 1 app |
| `Ob3TimeEstimate` | 3 | 5-option single-select | Must select |
| `Ob4AhaMoment` | 4 | Animated lifetime projection | None |
| `Ob5Reframe` | 5 | Cinematic 3-phase IMPULSE→PAUSE→CHOOSE flow | None |
| `Ob6InterventionStyle` | 6 | Category multi-select (6 categories) | ≥ 1 category |
| `Ob7MicroIntervention` | 7 | Interactive 3-breath exercise | 3 breaths OR skip |
| `Ob8EarnedAccess` | 8 | 3-option reward minute selector | Always valid (default 10) |
| `Ob9Permissions` | 9 | A11y + Overlay permission cards | Both must be granted |
| `Ob10Ready` | 10 | Summary card + atomic activation button | CTA disabled during activation |

## Design System

All screens use the Phase 8D premium dark palette:

```kotlin
private val Bg0 = Color(0xFF070B12)          // deepest background
private val BgCard = Color(0xFF0F172A)        // card surface
private val AccentBlue = Color(0xFF38BDF8)    // primary CTA, highlights, progress
private val AccentGreen = Color(0xFF10B981)   // success states
private val TextPrimary = Color(0xFFF8FAFC)   // headlines
private val TextSecondary = Color(0xFF94A3B8) // body copy
private val TextMuted = Color(0xFF475569)     // hints, captions
private val BorderDefault = Color(0xFF1E293B) // unselected borders
private val BorderSelected = Color(0xFF38BDF8)// selected borders
```

## Key Design Decisions

1. **Single composable, manual step state machine** — Follows existing app navigation pattern.
   No Jetpack Navigation library introduced.

2. **Existing activation path preserved** — `Ob10Ready.onActivate` calls
   `SelfModeActivationCoordinator.createDraft()` → `activatePlan()` → `setEnabledCategories()` →
   `setEnabledInterventions()` → `setOnboardingCompleted(true)` — identical call sequence as before.

3. **Screen 2 uses LazyColumn** — Wraps it in a `Box`+`Column` layout (not inside ObScaffold's
   verticalScroll) to avoid nested scroll conflicts.

4. **Screen 0 has no progress bar** — Step 0 is the immersive opening; progress only shows from
   step 1 onward.

5. **Resume** — `savedStep = preferencesManager.selfOnboardingStepFlow.collectAsState()`. If > 0,
   jumps directly to that step on composition.

## Running the Tests

```bash
./gradlew testReleaseUnitTest --tests "com.digitaldiscipline.spike.SelfModeOnboarding8DTest"
```
