# Phase 4D-2: Daily Actions & Planner Model

## 1. Concept: Daily Action
A Daily Action is the smallest meaningful, achievable action a user can take toward today's goal without feeling overwhelmed.

---

## 2. Category Segmentation Rules

| Goal Category | Daily Target | Segment / Chunk Size | Action Format | Estimated Duration |
| :--- | :---: | :---: | :--- | :---: |
| **Fitness** | 30 squats | 10 squats | Rep counter (`+1 REP`, `MARK DONE`) | ~60s |
| **Study** | 30 minutes | 15 minutes | Focus countdown sprint | 15m |
| **Mindfulness** | 5 sessions | 2 sessions (or 30s) | Animated Box Breathing | 30–60s |
| **Reading** | 20 pages | 5 pages | Page completion tracker | ~5m |
| **Generic** | $T$ units | $\max(1, T / 3)$ | Unit progress tracker | Scaled |

---

## 3. Deterministic Planning Logic
- `DailyActionPlanner.planDailyActions(goal, progress, behaviour, policy)`:
  - If `completed >= dailyTarget`:
    - `isGoalComplete = true`
    - `nextAction = null`
    - `completionMessage = "You've done what you planned today 🎉"`
  - If `completed < dailyTarget`:
    - `isGoalComplete = false`
    - `nextAction = DailyActionItem(...)`
    - `remaining = dailyTarget - completed`
- **Zero Mutation**: Computation is pure and leaves Room/DataStore state untouched until explicit user completion.
