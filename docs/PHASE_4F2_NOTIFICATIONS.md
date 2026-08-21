# Phase 4F-2: Notification Channels & Compatibility

## Notification Governance
- Explicit channels created on Android 8.0+ (API 26+).
- Handles Android 13+ (API 33+) runtime `POST_NOTIFICATIONS` permission gracefully.
- Frequency Governor ensures no more than 2 notifications are delivered in a 24-hour cycle.
