# Phase 4E-6: Journey Event Model & Structure

## Event Schema
- `eventId`: Unique deterministic string ID.
- `eventType`: `FIRST_WIN`, `GOAL_STARTED`, `GOAL_COMPLETED`, `GOAL_PAUSED`, `PLAN_REFINED`, `WEEKLY_REVIEW`, `HABIT_MOMENTUM`, `RECOVERY_DETECTED`.
- `timestamp`: Epoch milliseconds.
- `dateFormatted`: "MMM d, yyyy".
- `title`: Short milestone title.
- `shortDescription`: Context narrative.
- `supportingMetric`: Optional quantitative evidence (e.g., "+10m", "5/7").
- `importance`: `LOW`, `MEDIUM`, `HIGH`, `MILESTONE`.
