# Phase 4D-3: Architectural Decision Records (ADRs)

## ADR-1: Off-Enforcement-Path Notification Subsystem
- **Context**: Real-time enforcement (`AccessibilityService` -> `PolicyEngine` -> `OverlayManager`) requires sub-15ms deterministic execution and zero blocking operations.
- **Decision**: Notification intelligence and scheduling are strictly isolated from the enforcement path. `SmartNotificationEngine` never runs synchronously inside `AccessibilityService` or `PolicyEngine`.
- **Consequences**: Zero risk of introducing enforcement latency or regressions.

---

## ADR-2: Notification History in Encrypted DataStore (No Room Migration)
- **Context**: Phase 4D-3 requires recording recent notification history (timestamps, types, dismissals, interaction outcomes) for suppression and cooldown calculations.
- **Decision**: Store the rolling history window (up to 100 entries) as a lightweight JSON document in DataStore preferences rather than adding a new Room table.
- **Consequences**: Room database remains at version 8, eliminating migration failure risks and schema churn.

---

## ADR-3: Battery-Conscious Periodic WorkManager Scheduling
- **Context**: Notifications must trigger at appropriate times of day without running a continuous background service.
- **Decision**: Register two periodic WorkManager jobs: `DailyNotificationWorker` (24-hour interval) and `ActionReminderWorker` (12-hour interval) using `ExistingPeriodicWorkPolicy.KEEP`.
- **Consequences**: Zero foreground service notifications, negligible CPU/battery drain, survives device reboots and process death.
