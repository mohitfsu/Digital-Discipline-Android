# Phase 4E-5: Personal Behaviour Evolution & Goal Lifecycle — Implementation

## Mission Overview
Phase 4E-5 evolves Self Mode into a controlled personal behavior system where the user can deliberately evolve, pause, complete, replace, or restart goals without losing behavioral history.

## Architectural Components

### 1. `GoalLifecycleModel.kt`
- **Location**: `app/src/main/java/com/digitaldiscipline/spike/behaviour/lifecycle/GoalLifecycleModel.kt`
- **Role**: Defines `GoalLifecycleState` (`ACTIVE`, `PAUSED`, `COMPLETED`, `REPLACED`, `ARCHIVED`), `GoalTransitionType`, `TransitionValidationResult`, `GoalTransitionPreview`, and `HistoricalGoalSummary`.

### 2. `GoalLifecycleEngine.kt`
- **Location**: `app/src/main/java/com/digitaldiscipline/spike/behaviour/lifecycle/GoalLifecycleEngine.kt`
- **Role**: Deterministic, off-path validator and preview generator executing in `<1ms`.

### 3. `GoalLifecycleService.kt`
- **Location**: `app/src/main/java/com/digitaldiscipline/spike/behaviour/lifecycle/GoalLifecycleService.kt`
- **Role**: Single authoritative coordinator applying atomic state changes across Room entities and DataStore.

### 4. UI Components
- **`GoalLifecycleScreen.kt`**: Goal status, narrative, and lifecycle actions with confirmation modals.
- **`GoalHistoryScreen.kt` & `GoalHistoryDetailScreen.kt`**: Chronological display of past and present goal chapters with read-only evidence summaries.
- **`TodayScreen.kt`**: Dynamic dashboard reflecting `ACTIVE`, `PAUSED`, and `COMPLETED` states.
