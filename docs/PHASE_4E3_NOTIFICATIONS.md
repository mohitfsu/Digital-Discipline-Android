# Phase 4E-3: Smart Notification Integration

## Notification Rules
- Reuses `SmartNotificationEngine` and `NotificationFrequencyGovernor`.
- Gentle recovery reminders are permitted when a user has been quiet for 1–2 days:
  - "Want to do one small thing for your goal today?"
  - "You're back. One small action is enough."
- Strictly avoids urgency ("Don't lose your streak!").
- Suppressed when Parent Mode is active.
