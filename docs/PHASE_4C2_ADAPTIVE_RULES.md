# Phase 4C-2: Adaptive Rules & Thresholds

## 1. Overview
All evaluations in `AdaptivePlanEngine` are deterministic, reproducible, and mathematically defined. No probabilistic ML models, Gemini APIs, or external heuristic engines are involved.

---

## 2. Sample Size Gates

| Metric | Minimum Sample Threshold | Action When Below Threshold |
| :--- | :---: | :--- |
| **Plan Health** | $\ge 10$ intervention events | Return `PlanHealth.INSUFFICIENT_DATA` |
| **Best Intervention** | $\ge 10$ trials for candidate type | Return `null` |
| **Intervention Comparison** | $\ge 10$ trials for each compared type | Return default encouragement |
| **Distraction Peak Window** | $\ge 10$ total attempts | Return `hasSufficientData = false` |
| **Reward Loop Analysis** | $\ge 10$ completed sessions | Return `hasSufficientData = false` |
| **Primary Plan Recommendation** | $\ge 10$ attempts | Return `RecommendationType.INSUFFICIENT_DATA` |

---

## 3. Plan Health Classification Rules

$$\text{Habit Interruption Rate (HIR)} = \left(\frac{\text{Attempts without 5-minute reopen}}{\text{Total Attempts}}\right) \times 100$$
$$\text{Completion Rate} = \left(\frac{\text{Completed Interventions}}{\text{Total Attempts}}\right) \times 100$$

- **`WORKING` (🟢)**: $\text{HIR} \ge 65.0\%$ AND $\text{Completion Rate} \ge 60.0\%$.
- **`NEEDS_ADJUSTMENT` (🟡)**: $\text{HIR} \in [40.0\%, 64.99\%]$ OR $\text{Completion Rate} \in [35.0\%, 59.99\%]$.
- **`NOT_WORKING` (🔴)**: $\text{HIR} < 40.0\%$ OR $\text{Completion Rate} < 35.0\%$.
- **`INSUFFICIENT_DATA` (⚪)**: $\text{Total Attempts} < 10$.

---

## 4. Specific Deterministic Recommendation Rules

1. **High Exit Rate on Physical Challenge ($\ge 40\%$ exit rate)**:
   - *Condition*: Active behaviour is `SQUATS` or `PUSHUPS` with $\text{targetCount} > 5$, $\text{attempts} \ge 10$, and $\text{exits} / \text{attempts} \ge 0.40$.
   - *Recommendation*: `SHORTER_INTERVENTION` (suggest count = 5).
   - *Confidence*: `HIGH`.

2. **Intervention Superiority ($\ge 20\%$ higher HIR)**:
   - *Condition*: Candidate intervention has $\ge 10$ trials and its HIR exceeds current intervention by $\ge 20\%$.
   - *Recommendation*: `CHANGE_INTERVENTION`.
   - *Confidence*: `HIGH`.

3. **Rapid Session Chaining / Reopen Loop ($\ge 30\%$ immediate reopen rate)**:
   - *Condition*: Sessions $\ge 10$, and $\ge 30\%$ of sessions have another attempt within 60s of completion.
   - *Recommendation*: `ADD_COOLDOWN` (suggest 120s / 2-minute cooldown).
   - *Confidence*: `MEDIUM`.

4. **Excessive Reward Duration**:
   - *Condition*: Reopen loop detected and current policy $\text{earnedSeconds} \ge 600$.
   - *Recommendation*: `REDUCE_REWARD` (suggest 300s / 5 minutes).
   - *Confidence*: `MEDIUM`.

5. **Healthy Plan**:
   - *Condition*: Meets all working criteria without anomalies.
   - *Recommendation*: `KEEP_PLAN`.
   - *Confidence*: `HIGH`.
