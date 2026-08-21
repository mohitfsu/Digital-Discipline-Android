# Phase 8D — Onboarding Test Validation

## Test File

`app/src/test/java/com/digitaldiscipline/spike/SelfModeOnboarding8DTest.kt`

## Test Count: 20

| # | Test | What It Validates |
|---|------|------------------|
| 01 | Five behaviour patterns defined | Correct count, no duplicates |
| 02 | Time estimates → year projections | All projections non-blank |
| 03 | Onboarding categories in catalog | All 6 categories present in InterventionCatalog |
| 04 | Reward minutes → RewardPreset mapping | 5→LIGHT, 10→STANDARD, 15→STRONG |
| 05 | Coordinator state constants | STATE_NOT_STARTED, IN_PROGRESS, READY, COMPLETED |
| 06 | Productivity template for default | GoalTemplateRepository has PRODUCTIVITY |
| 07 | Distraction app list has key apps | Instagram, YouTube present |
| 08 | Catalog interventions for default categories | MOVEMENT, BREATHING, COGNITIVE non-empty |
| 09 | Min distraction app count = 1 | Coordinator.MIN_DISTRACTION_APPS |
| 10 | Max distraction app count = 5 | Coordinator.MAX_DISTRACTION_APPS |
| 11 | Reward presets positive reward seconds | dailyCap >= rewardSeconds > 0 |
| 12 | Progress fractions monotonically increasing | 0f → 1.0f across 11 steps |
| 13 | Completion only at step 10 | Architecture contract |
| 14 | Coordinator has no parent override methods | Parent precedence isolation |
| 15 | Enabled interventions built from categories | Set is non-empty, all IDs valid |
| 16 | Resume step bounded to 1–10 | Step 0 never persisted |
| 17 | Micro-intervention target = 3 | Breath count target |
| 18 | Reward options cover all three tiers | 5, 10, 15 minutes |
| 19 | Catalog has ≥ 6 distinct categories | Intervention variety guarantee |
| 20 | No new Room entities | DataStore-only architecture contract |

## Run Command

```bash
# Unit tests only (fast, no device needed)
./gradlew testReleaseUnitTest

# Specific test class
./gradlew testReleaseUnitTest --tests "com.digitaldiscipline.spike.SelfModeOnboarding8DTest"

# All unit tests + build
./gradlew testReleaseUnitTest assembleRelease
```

## Existing Tests Preserved

All 27+ existing tests in `app/src/test/` continue to pass. Phase 8D does not modify any
existing test files or the classes they test.

## What Is Not Unit-Tested

The following require instrumentation tests (Compose UI tests) which are not in scope:
- Screen transitions and animations
- CTA enable/disable state on screen
- Back button behavior
- Permission card appearance

These are validated manually on device.
