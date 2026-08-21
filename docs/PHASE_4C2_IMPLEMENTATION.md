# Phase 4C-2: Adaptive Behaviour & Personalization — Implementation Architecture

## 1. Executive Summary
Phase 4C-2 introduces on-device adaptive evaluation and personalization for Self Mode. The engine evaluates local behavioral signals (Habit Interruption Rate, completion vs exit rates, session duration, and rapid reopen chains) to generate deterministic, explainable recommendations.

---

## 2. Core Architectural Invariant
$$\text{OBSERVATION} \longrightarrow \text{DETERMINISTIC ANALYSIS} \longrightarrow \text{RECOMMENDATION} \longrightarrow \text{USER DECISION} \longrightarrow \text{OPTIONAL PLAN CHANGE}$$
**No automatic plan modification or escalation is ever executed.** The system only presents suggestions, and the user must explicitly tap `[ APPLY CHANGE ]` to adopt an adjustment or `[ KEEP MY PLAN ]` to reject it.

---

## 3. Component Architecture

```
                                +---------------------------+
                                |  Historical Behavioral    |
                                |  Telemetry (Room v7)      |
                                +-------------+-------------+
                                              |
                                              v
+------------------------+      +---------------------------+
| Real-time Blocking     |      |    AdaptivePlanEngine     | (Off-Path Evaluation)
| Enforcement Path       |      |   (Deterministic Logic)   |
| (NO ADAPTIVE CALLS)    |      +-------------+-------------+
+------------------------+                    |
                                              v
                                +---------------------------+
                                | PersonalizationRepository |
                                +-------------+-------------+
                                              |
                                              v
                                +---------------------------+
                                |  SelfDashboardScreen &    |
                                |  SelfWeeklyReviewScreen   |
                                +-------------+-------------+
                                              | [ USER EXPLICIT CHOICE ]
                                              v
                                +---------------------------+
                                | BehaviourRepository & DB  |
                                +---------------------------+
```

---

## 4. Key Classes & Responsibilities

| Class | Path | Primary Responsibility |
| :--- | :--- | :--- |
| `AdaptivePlanEngine` | `behaviour.adaptive.AdaptivePlanEngine` | Deterministic plan health, intervention ranking, reward loop detection, and recommendation generation in $<1\text{ms}$. |
| `PersonalizationRepository` | `behaviour.adaptive.PersonalizationRepository` | Coordinates background calculation of profile, persistence of pending recommendations, and atomic adjustment apply/reject logic. |
| `SelfDashboardScreen` | `ui.dashboard.SelfDashboardScreen` | Renders Plan Health badge, single primary recommendation card, and entry point to Weekly Review. |
| `SelfWeeklyReviewScreen` | `ui.dashboard.SelfWeeklyReviewScreen` | Visual 7-day performance breakdown, biggest win summary, and next-week focus. |
| `DigitalDisciplineDatabase` | `data.local.DigitalDisciplineDatabase` | Room database v7 with non-destructive `MIGRATION_6_7`. |
