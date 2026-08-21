# Phase 4D-2: Comprehensive Test Plan

## 1. Scope
The Phase 4D-2 test suite verifies the deterministic behavior of `DailyActionPlanner`, interactive `DailyActionScreen` transitions, wallet integrations, goal integrity, Parent Mode precedence, offline operation, and performance.

---

## 2. Test Coverage Matrix

| Area | Scenarios Tested | Validation Approach |
| :--- | :--- | :--- |
| **Goal Planning** | Zero progress, partial progress, completed state, remaining math, chunk segmentation across Fitness, Study, Reading, Mindfulness | Unit tests in [`TodayDailyActionsTest.kt`](file:///d:/Zidd/app/src/test/java/com/digitaldiscipline/spike/TodayDailyActionsTest.kt) |
| **Action Completion** | Progress entity incrementation, partial vs full completion, idempotency, process restart recovery, offline state | Unit tests |
| **Wallet Integration** | Ledger credit, non-reward actions, max balance cap, daily earn cap, `USE NOW` vs `SAVE FOR LATER` | Unit tests |
| **Goal Integrity** | Daily target clamping, duplicate completion prevention, missed day fresh start, zero carried debt | Unit tests |
| **Parent Precedence** | Parent BLOCK, DELAY, ALLOW overriding Self wallet sessions | Invariant tests |
| **Performance** | `DailyActionPlanner.planDailyActions` executing in $<5\text{ms}$ | Microbenchmark |
