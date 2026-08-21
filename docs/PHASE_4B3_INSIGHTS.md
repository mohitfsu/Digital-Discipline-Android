# Phase 4B-3 — Deterministic Behaviour Insights Engine

## 1. Mathematical Formulas & Rules

### A. Habit Interruption Rate (HIR)
$$\text{HIR} = \left( \frac{\text{Attempts without 5-minute rapid reopen}}{\text{Total Intervention Attempts}} \right) \times 100$$

---

### B. Feedback Rules Specification

| Rule ID | Mathematical Condition | Output Message |
| :--- | :--- | :--- |
| **RULE A** | $\text{Attempts} \ge 10 \land \text{HIR} \ge 70\%$ | *"You're successfully interrupting most distraction attempts."* |
| **RULE B** | $\text{Attempts} \ge 10 \land \text{HIR}_{\text{current}} - \text{HIR}_{\text{previous}} \ge +10\%$ | *"Your ability to interrupt the habit is improving."* |
| **RULE C** | $\text{Attempts} \ge 10 \land \text{HIR}_{\text{current}} - \text{HIR}_{\text{previous}} \le -10\%$ | *"Your distraction pattern has become harder to interrupt this week."* |
| **RULE D** | Best intervention has $\ge 10$ trials and $\ge 20\%$ lower reopen rate than second | *"Your [intervention] appears to work better for you."* |
| **RULE E** | $\text{Attempts} < 10$ (Insufficient Data) | *"Keep going. We'll show more useful patterns as you build history."* |

---

## 2. Distraction Pattern & Peak Detection
- Requires minimum $10$ intervention attempts across monitored distraction apps.
- Computes $2$-hour time buckets across the $24$-hour day.
- Returns clear natural language summary: *"You tend to open Instagram most often between 9 PM and 11 PM."*
