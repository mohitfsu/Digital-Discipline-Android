# Phase 3C — Behavioural Metrics & Habit Interruption Rate Specification

## 1. Core Mathematical Definitions

### A. Habit Interruption Rate (HIR)
The core objective metric of Digital Discipline is the **Habit Interruption Rate**. It quantifies whether an intervention successfully broke an impulsive micro-loop by measuring whether the child refrained from reopening the target app within a 5-minute cooldown window.

$$\text{Habit Interruption Rate (HIR)} = \left( \frac{\text{Intervention Attempts with NO reopen within 5 minutes}}{\text{Total Intervention Attempts}} \right) \times 100\%$$

- **Numerator**: Count of intervention events where `reopenWithin5Minutes == false`.
- **Denominator**: Total intervention attempt events logged for the time window.
- **Scale**: $0.0\%$ to $100.0\%$.
- **Boundary Condition**: If $\text{Total Attempts} = 0$, $\text{HIR} \equiv 100.0\%$.

---

## 2. Comprehensive Behavioural Metrics Catalog

| Metric Name | Mathematical Formula | Purpose & Interpretation |
| :--- | :--- | :--- |
| **1. Intervention Attempts** | $\sum \text{events}$ | Total number of times restricted applications were launched. |
| **2. Completion Rate** | $\frac{\sum \text{events with status 'COMPLETED'}}{\sum \text{events}} \times 100\%$ | Proportion of interventions where the exercise/pause was finished. |
| **3. Abandonment Rate** | $\frac{\sum \text{events with status 'ABANDONED'}}{\sum \text{events}} \times 100\%$ | Proportion of interventions dismissed before completion. |
| **4. Earned Access Rate** | $\frac{\sum \text{events with outcome 'EARNED_ACCESS'}}{\sum \text{events}} \times 100\%$ | Frequency with which the child unlocked temporary time through effort. |
| **5. Parent Override Rate** | $\frac{\sum \text{events with outcome 'PARENT_OVERRIDE'}}{\sum \text{events}} \times 100\%$ | Frequency of parent PIN bypass entries. |
| **6. Immediate Exit Rate** | $\frac{\sum \text{events with outcome 'EXITED'}}{\sum \text{events}} \times 100\%$ | Frequency with which the child tapped "Exit to Home" when confronted with friction. |
| **7. Rapid Reopen Rate** | $\frac{\sum \text{events with reopenWithin5Minutes = true}}{\sum \text{completed unlocks}} \times 100\%$ | Frequency of immediate attempts to re-enter the app post-unlock expiration. |
| **8. Average Earned Minutes** | $\frac{\sum \text{earnedSeconds}}{60 \times \text{completed unlocks}}$ | Average duration of controlled screen-time unlocked per intervention. |
| **9. Attempts by Application** | $\text{Count}(\text{events}) \text{ grouped by } \text{packageName}$ | Identifies which applications generate the greatest friction/demand. |
| **10. Attempts by Intervention**| $\text{Count}(\text{events}) \text{ grouped by } \text{interventionType}$| Compares volume across Pause, Breathing, and Squats. |
| **11. Hourly Distribution** | $\text{Count}(\text{events}) \text{ grouped by } \text{hourOfDay} \in [0..23]$ | Visualizes temporal compulsion clusters throughout the 24-hour day. |
| **12. Weekday Distribution** | $\text{Count}(\text{events}) \text{ grouped by } \text{dayOfWeek} \in [1..7]$ | Compares school days (Mon–Fri) vs weekend usage dynamics. |
| **13. Daily Screen-Time Trend**| $\sum \text{totalForegroundSeconds} / 60 \text{ per day}$ | Tracks longitudinal screen-time reduction over 7-day and 30-day windows. |

---

## 3. Neutral Clinical Framing Principles
1. **No Psychological Stigmatization**: Never label children as "addicted", "compulsive", or "pathological".
2. **Deterministic Focus**: Report numbers as descriptive behavioral facts ("14 attempts between 8 PM and 10 PM", "80% habit interruption rate").
3. **No Automated Diagnoses**: Never correlate metrics to ADHD, depression, anxiety, or cognitive disorders.
