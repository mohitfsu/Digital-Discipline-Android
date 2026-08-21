# Phase 4C-2: Behaviour Recommendations & User Decision Flow

## 1. Recommendation Schema
The `BehaviourRecommendation` structure defines the actionable suggestion:
- `recommendationId`: Unique UUID.
- `type`: `KEEP_PLAN`, `CHANGE_INTERVENTION`, `REDUCE_REWARD`, `SHORTER_INTERVENTION`, `ADD_COOLDOWN`, `CHANGE_DISTRACTION_WINDOW`, `INSUFFICIENT_DATA`.
- `title`: Short headline explaining the action.
- `explanation`: Contextual reason using neutral, objective language.
- `currentConfiguration`: Current state string (e.g. `10 Bodyweight Squats`).
- `suggestedConfiguration`: Proposed state string (e.g. `5 Bodyweight Squats`).
- `confidenceLevel`: Deterministic enum (`LOW`, `MEDIUM`, `HIGH`).
- `evidence`: Statistical backing (e.g. `Exit rate is 45% across 20 trials`).
- `cooldownSeconds`: Optional cooldown buffer (e.g. `120`).

---

## 2. Dashboard Presentation & User Control
Recommendations are rendered on `SelfDashboardScreen` as a single primary card.

### Actions Available to User:
1. `[ APPLY CHANGE ]`: Commits proposed changes into `BehaviourRepository`, updates `BehaviourPolicyEntity` / `ReplacementBehaviourEntity`, and marks the adjustment status as `ACCEPTED`.
2. `[ KEEP MY PLAN ]`: Rejects the adjustment, updates status to `REJECTED`, and preserves existing configuration. Rejection prevents duplicate prompts for 7 days.
