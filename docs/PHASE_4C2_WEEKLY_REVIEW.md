# Phase 4C-2: Weekly Review System

## 1. Overview
The Weekly Review aggregates behavioral outcomes over the previous 7 days into a persisted, reproducible summary (`WeeklyReviewEntity`).

---

## 2. Metrics & Content
- **Distraction Attempts**: Total interventions encountered across all monitored applications.
- **Interventions Completed**: Number of challenges successfully completed.
- **Habit Interruption Rate**: Percentage of attempts that prevented a rapid 5-minute return.
- **Screen Time Economics**:
  - Total screen time minutes earned.
  - Total screen time minutes consumed.
  - Net saved minutes.
- **Biggest Win**: Dynamic celebratory statement based on performance (e.g. `Instagram reopen attempts were successfully interrupted 75% of the time`).
- **Suggested Focus**: Neutral, actionable focus area for the upcoming week.

---

## 3. UI Implementation
- Screen: `SelfWeeklyReviewScreen.kt`.
- Accessible via the `[ Weekly Review ]` button on the top header of `SelfDashboardScreen`.
- User can dismiss with `[ KEEP MY PLAN ]` or navigate to edit with `[ ADJUST MY PLAN ]`.
