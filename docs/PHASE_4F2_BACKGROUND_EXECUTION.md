# Phase 4F-2: Background Execution & Doze Mode Compatibility

## Doze & App Standby Invariants
- `AccessibilityService` is classified as an active system service and is exempted from standard Doze freezing.
- Scheduled background evaluations run via WorkManager `ExistingPeriodicWorkPolicy.KEEP`.
- Zero infinite polling loops, zero wakelock acquisition, zero unnecessary battery drain.
