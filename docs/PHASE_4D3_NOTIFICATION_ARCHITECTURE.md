# Phase 4D-3: Notification Architecture

## Architectural Boundary

Smart Notifications are a strictly **off-path** subsystem.

```
Behaviour Data (Room / DataStore)
        ↓
SmartNotificationEngine          ← pure object, no coroutines, <10ms
        ↓
NotificationFrequencyGovernor    ← DataStore-backed, <1ms decision
        ↓
NotificationDecision (Show / Suppress / Defer / Reschedule)
        ↓
NotificationChannelManager       ← posts via NotificationManagerCompat
        ↓
Android NotificationManager
        ↓
User Device
```

### The Enforcement Path (UNCHANGED)
```
UsageStatsDetector / AccessibilityService
        ↓
PolicyEngine
        ↓
BehaviourPolicyResolver
        ↓
OverlayManager
```

These two paths **NEVER intersect**. `SmartNotificationEngine` is never called from `AccessibilityService`, `PolicyEngine`, or `OverlayManager`.

## WorkManager Scheduling

| Worker | Repeat Interval | Flex Window | Types Evaluated |
|--------|----------------|-------------|-----------------|
| `DailyNotificationWorker` | 24 hours | ±2 hours | `MORNING_INTENTION`, `EVENING_REFLECTION`, `WEEKLY_REVIEW` |
| `ActionReminderWorker` | 12 hours | ±1 hour | `NEXT_ACTION`, `DISTRACTION_PREEMPTION`, `MISSED_ACTION` |

Both workers use `ExistingPeriodicWorkPolicy.KEEP` to avoid duplicate scheduling. No network constraints. No foreground service. No continuous polling.

## Parent Mode Safety

The very first operation in `SmartNotificationEngine.evaluate()`:

```kotlin
if (ctx.isParentMode || !ctx.isSelfMode) {
    return NotificationDecision.Suppress("Parent Mode active — Self Mode notifications suppressed")
}
```

This gate runs before **any** scoring or notification type evaluation.

## State Storage

| Store | What | Why DataStore (not Room) |
|-------|------|--------------------------|
| `notification_governor` | Daily counters | Small key/value; no relational queries needed |
| `notification_history` | Rolling JSON log | Max 100 records; no migration risk |
| `digital_discipline_prefs` | User preferences | Extends existing DataStore |
