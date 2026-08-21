# Phase 4D-3: Notification Frequency Governor

## Purpose & Core Responsibility
The `NotificationFrequencyGovernor` prevents notification spam and ensures notifications stay calm, predictable, and supportive.

## Hard Daily Frequency Limits

| Frequency Mode | Max Total / Day | Max Behaviour Reminders / Day | Min Gap Between Notifications |
|---|---|---|---|
| **MINIMAL** | 1 | 1 | 180 minutes |
| **BALANCED** (Default) | 3 | 2 | 120 minutes |
| **HELPFUL** | 5 | 3 | 60 minutes |

## Specific Per-Type Limits
- `MAX_SAME_TYPE_PER_DAY = 1`
- `MAX_PREEMPTIVE_NOTIFICATIONS_PER_DAY = 1`
- `MAX_SUCCESS_NOTIFICATIONS_PER_DAY = 1`
- `MAX_MISSED_ACTION_NOTIFICATIONS_PER_DAY = 1`
- `MAX_WEEKLY_REVIEW_PER_WEEK = 1`

## Daily Reset & Persistence
- Governor state is saved in DataStore (`notification_governor`).
- It tracks `dateString` (format `yyyy-MM-dd`), total counts, per-type counts, and last sent timestamp.
- On date change, counters automatically reset to zero for the new calendar day.
- Evaluation latency is `<1ms`.
