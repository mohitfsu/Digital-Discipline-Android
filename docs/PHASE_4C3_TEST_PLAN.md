# Phase 4C-3: Test Plan

## 1. Scope
The test suite for Phase 4C-3 tests the behavior intelligence algorithms, momentum and integrity scoring engines, weekly summarizer, experiment lifecycle, database migration, and Parent Mode precedence.

---

## 2. Test Matrix

| Category | Target Scenarios | Test Method |
| :--- | :--- | :--- |
| **Pattern Analysis** | Insufficient data, peak hour, 2-hour window, weekday vs weekend, app rankings, intervention rankings | Unit tests in [`BehaviourIntelligenceTest.kt`](file:///d:/Zidd/app/src/test/java/com/digitaldiscipline/spike/BehaviourIntelligenceTest.kt) |
| **Momentum Engine** | 7-factor weighted formula, state boundaries (90, 75, 50, 25) | Automated parameter sweeps |
| **Goal Integrity** | Consistency, HIR, challenge completion, reopen control | Automated assertions |
| **Weekly Intelligence** | Strongest day, top distraction, best intervention, win message | Retrospective aggregation tests |
| **Experiments** | Creation (DRAFT), Start (ACTIVE), Completion (COMPLETED), Cancellation (CANCELLED), Expiration | Lifecycle unit tests |
| **System Invariants** | No automatic mutation, Parent Mode precedence (BLOCK/DELAY/ALLOW/EARN), zero cloud calls | Invariant unit tests |
| **Performance** | Scoring $<1\text{ms}$, Pattern analysis $<10\text{ms}$ | High-iteration nano-time benchmarks |
