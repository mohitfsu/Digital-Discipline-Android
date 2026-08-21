# Phase 8D — Onboarding Architecture

## Data Flow

```
MainActivity.setContent
  └─ isOnboardingCompleted = preferencesManager.onboardingCompletedFlow (DataStore)
  └─ userDisplayName = preferencesManager.userDisplayNameFlow (DataStore)
  └─ if (!isOnboardingCompleted && mode == SELF)
       └─ SelfModeOnboardingScreen(...)
            ├── Screen 0..10 (AnimatedContent step state machine)
            ├── On step 10 "activate":
            │     SelfModeActivationCoordinator.createDraft(template, apps, replacement, preset)
            │     SelfModeActivationCoordinator.activatePlan(draft, repo, wallet, prefs)
            │       ├── BehaviourPlanCreator.confirmAndPersistPlan() → Room v9
            │       ├── preferencesManager.setUserMode(SELF)
            │       └── walletService.configureEarnedTimePolicy(...)
            │     preferencesManager.setEnabledCategories(Set<String>)
            │     preferencesManager.setEnabledInterventions(Set<String>)
            │     preferencesManager.setOnboardingCompleted(true)
            │     onComplete() → back to MainActivity dashboard
            └── At each step: preferencesManager.setSelfOnboardingStep(step) [resume support]
```

## Reused Systems (Unchanged)

| System | Role | Location |
|--------|------|----------|
| `SelfModeActivationCoordinator` | Atomic plan commit | `behaviour/activation/` |
| `BehaviourPlanCreator` | Goal + policy persistence | `behaviour/templates/` |
| `BehaviourRepository` | Room DAO access | `behaviour/` |
| `EarnedTimeWalletService` | Wallet policy configuration | `wallet/` |
| `InterventionCatalog` | Category → intervention ID mapping | `intervention/catalog/` |
| `PreferencesManager` | DataStore persistence | `data/preferences/` |
| `PolicyEngine` | Enforcement (untouched) | `enforcement/` |
| Room v9 | Entity persistence | `data/local/` |

## New DataStore Keys (Phase 8D additions only)

| Key | Type | Purpose |
|-----|------|---------|
| `user_display_name` | String | Shown as greeting in TodayScreen header |
| `onboarding_behaviour_pattern` | String | Persists selected behaviour pattern from Screen 1 |
| `onboarding_screen_time_estimate` | String | Persists screen time estimate from Screen 3 |

Existing keys reused: `self_onboarding_step`, `self_onboarding_state`, `enabled_categories`,
`enabled_interventions`, `onboarding_completed`, `user_mode`.

## Component Tree

```
SelfModeOnboardingScreen
├── ObScaffold (steps 1,3,4,5,6,7,8,9,10)
│   ├── ObTopBar (← Back button + step counter + progress bar)
│   ├── Column (verticalScroll) — content area
│   └── ObCta (bottom fixed CTA button)
├── Ob2AppPicker (custom Box — uses LazyColumn, no nested scroll)
├── Ob0Opening (full-screen Box, no scaffold)
└── Shared atom components:
    ├── ObQuestion
    ├── ObSingleCard (radio-style selection)
    ├── ObTwoLineCard (title + subtitle)
    ├── ObCategoryCard (icon + checkbox)
    ├── ObPermissionCard
    └── ObSummaryRow
```
