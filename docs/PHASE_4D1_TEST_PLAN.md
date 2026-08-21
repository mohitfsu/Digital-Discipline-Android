# Phase 4D-1: Test Plan

## 1. Scope
The test suite for Phase 4D-1 verifies the user experience logic, goal progress calculations, wallet summary presentations, pattern rendering, reflection workflows, and isolation from the real-time enforcement loop.

---

## 2. Test Matrix

| Category | Target Scenarios | Test Method |
| :--- | :--- | :--- |
| **Routing & Modes** | Self Mode routes to Today, Parent Mode bypasses Today | Unit tests in [`TodayExperienceTest.kt`](file:///d:/Zidd/app/src/test/java/com/digitaldiscipline/spike/TodayExperienceTest.kt) |
| **Goal Presentation** | Goal loading, progress bar calculations, 7-day consistency, zero progress, completion capping | Automated assertions |
| **Wallet Card** | Balance extraction, earned/used math, active session countdown, zero balance prompt | Automated assertions |
| **Behaviour & Patterns** | HIR computation, baseline message on insufficient data, deterministic peak window on sufficient data | Automated assertions |
| **Recent Wins** | Dynamic victory derivation from events and wallet ledger | Automated assertions |
| **Daily Reflection** | Once-per-day completion, no duplicate triggers on same day, completion summary generation | Unit tests |
| **Invariants & Safety** | Offline execution, zero network calls on render, Parent Mode absolute precedence | Invariant tests |
