# Phase 4D-3: Battery & Performance Invariants

## Performance Budgets
- **SmartNotificationEngine evaluation latency**: `<10ms` (measured: `<2ms` typical).
- **NotificationFrequencyGovernor check latency**: `<1ms`.
- **AccessibilityService detection latency**: Unaffected (0ms impact, strictly off-path).
- **PolicyEngine evaluation latency**: Unaffected (0ms impact).
- **OverlayManager rendering latency**: Unaffected (0ms impact).

## Battery Optimization Architecture
1. **No Foreground Service**: Notifications are never posted or scheduled from a permanent foreground service.
2. **No Continuous Polling**: Scheduled via `WorkManager` using standard batch periodic windows (24h and 12h).
3. **No WakeLocks**: System handles wakeups via standard OS alarm scheduling in WorkManager without holding custom wake locks.
4. **Idempotent / Low CPU**: Each worker execution runs in under 500ms and immediately completes.
