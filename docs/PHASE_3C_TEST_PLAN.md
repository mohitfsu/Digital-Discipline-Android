# Phase 3C — Behaviour Analytics & Intelligence Test Plan

This document details the test scenarios for verifying the local event model, Habit Interruption Rate calculation, rapid reopen detection, offline aggregation, idempotent single-write cloud sync, and deterministic recommendation engine.

---

### Test 1: Intervention Start Event
- **Procedure**: Launch restricted target application (e.g. `com.google.android.youtube`).
- **Pass Criteria**: `InterventionEventEntity` recorded with `status = STARTED`, `outcome = STARTED`, current timestamp, and `hourOfDay`.

### Test 2: Intervention Completion
- **Procedure**: Wait 10 seconds on Mindful Pause until timer completes.
- **Pass Criteria**: `InterventionEventEntity` updated with `status = COMPLETED`, `outcome = EARNED_ACCESS`, and `earnedSeconds = 600`.

### Test 3: Intervention Abandonment & Exit
- **Procedure**: Tap "Exit to Home" during an active intervention overlay.
- **Pass Criteria**: `InterventionEventEntity` recorded with `status = EXITED`, `outcome = EXITED`; overlay dismissed cleanly.

### Test 4: Parent PIN Override
- **Procedure**: Tap "Parent PIN", enter valid PIN, and submit.
- **Pass Criteria**: `InterventionEventEntity` recorded with `outcome = PARENT_OVERRIDE`; immediate unlock granted.

### Test 5: Earned Unlock Session
- **Procedure**: Complete Squat Challenge (10 reps).
- **Pass Criteria**: Temporary unlock active in Room `temporary_unlocks`; `DailyUsageEntity` increments `unlockCount` and `earnedMinutes`.

### Test 6: Unlock Expiry
- **Procedure**: Wait until unlock duration expires.
- **Pass Criteria**: Target app restricted again; `TEMPORARY_UNLOCK_EXPIRED` logged in `DiagnosticLogger`.

### Test 7: Rapid Reopen (<5 Minutes)
- **Procedure**: Reopen target app within 2 minutes of unlock expiration.
- **Pass Criteria**: `reopenWithin5Minutes` set to `true` on previous event; `outcome` updated to `RAPID_REOPEN`; `daily_usage.rapidReopens` incremented.

### Test 8: No Reopen (Successful Habit Interruption)
- **Procedure**: Do not open target app for >5 minutes post-intervention.
- **Pass Criteria**: `reopenWithin5Minutes` remains `false`; Habit Interruption Rate remains high.

### Test 9: Multiple Interventions Across Apps
- **Procedure**: Trigger interventions sequentially on YouTube, Instagram, and Free Fire.
- **Pass Criteria**: Discrete event entities created per package name with appropriate `pauseCount`, `breathingCount`, and `squatsCount`.

### Test 10: Device Reboot Resilience
- **Procedure**: Force restart device.
- **Pass Criteria**: SQLite Room v4 database preserves all historical events and daily rollups; `BootCompletedReceiver` validates DB integrity.

### Test 11: Process Death Resilience
- **Procedure**: `am force-stop com.digitaldiscipline.spike`.
- **Pass Criteria**: Relaunching app immediately retrieves historical analytics without corrupted state.

### Test 12: Offline Aggregation
- **Procedure**: Place device in Airplane Mode; trigger and complete 5 interventions.
- **Pass Criteria**: All 5 events and daily summary updated 100% locally in SQLite Room DB.

### Test 13: Retry After Network Restoration
- **Procedure**: Re-enable network after offline period.
- **Pass Criteria**: `DailyAnalyticsUploadWorker` runs, reads local daily rollups, and uploads to Firestore.

### Test 14: Duplicate Upload Prevention (Idempotency)
- **Procedure**: Trigger `DailyAnalyticsUploadWorker` twice consecutively.
- **Pass Criteria**: Document `${childId}_${dateString}` is updated via idempotent upsert; exactly 1 document exists in `/families/{familyId}/daily_summaries/`.

### Test 15: Multi-Child Isolation
- **Procedure**: Log daily rollups for Child A and Child B.
- **Pass Criteria**: Firestore document path includes distinct `childId`; parent dashboard loads Child A data without mixing Child B data.

### Test 16: Firestore Security Rules Verification
- **Procedure**: Attempt to query daily summaries from an unauthenticated Firebase client.
- **Pass Criteria**: Request denied by security rule `isParentOfFamily(familyId)`.
