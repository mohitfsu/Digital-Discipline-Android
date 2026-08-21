# Phase 4E-2: Smart Notification Integration

## First-Win Notification Eligibility
- **Condition**: If a user has activated a Self Mode plan but has not completed their First Win (`PLAN_ACTIVE` or `FIRST_TRIGGER_SEEN`), the system permits **ONE** supportive first-win reminder.
- **Governor Limits**: Obey's `NotificationFrequencyGovernor` daily caps and minimum time gaps.
- **Suppression**: Immediately and permanently suppressed once First Win is completed (`FIRST_WIN_COMPLETED`).
- **Parent Gate**: Unconditionally suppressed if Parent Mode is active.
