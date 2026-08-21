# Phase 3A — Production Reliability Test Specifications

This document outlines the test specifications for the 14 hardening scenarios required for Phase 3A production readiness.

---

### Test 1: Process Death
- **Procedure**: Force-stop application via `adb shell am force-stop com.digitaldiscipline.spike` while rules are active, then launch target app.
- **Expected Result**: Accessibility service detects launch event immediately; Room database loads active rules; overlay intervention displays without state corruption.

### Test 2: Device Reboot
- **Procedure**: Restart device / receive `BOOT_COMPLETED` broadcast.
- **Expected Result**: `BootCompletedReceiver` executes; Room database integrity is verified; WorkManager periodic sync is re-enqueued; Parent PIN and pairing state remain intact.

### Test 3: Network Loss (Offline Enforcement)
- **Procedure**: Put device in Airplane Mode (disable Wi-Fi & Cellular Data) and attempt to launch restricted apps.
- **Expected Result**: Rules continue enforcing 100% locally from Room cache; zero unrestricted default fallbacks occur.

### Test 4: Network Restoration & Sync
- **Procedure**: Update policy version in Parent Web Control Center while device is offline; re-enable Wi-Fi.
- **Expected Result**: WorkManager or manual 1-tap sync downloads latest policy; commits atomic transaction to Room; PolicyEngine enforces updated rules immediately.

### Test 5: Firebase / Cloud Outage
- **Procedure**: Simulate Firestore timeout / network failure during policy fetch.
- **Expected Result**: No crash; exception caught gracefully; local database and current policy remain active.

### Test 6: Accessibility Service Revocation
- **Procedure**: Manually disable Digital Discipline in Android Accessibility Settings.
- **Expected Result**: TamperDetector and AccessibilityService detect unbinding; local protection state flags `isAccessibilityActive = false`; Parent Dashboard shows `PROTECTION DISABLED`.

### Test 7: Overlay Permission Revocation
- **Procedure**: Revoke "Display over other apps" permission in Android Settings.
- **Expected Result**: `canDrawOverlays()` returns `false`; diagnostic log records `OVERLAY_PERMISSION_MISSING`; no window token crash occurs.

### Test 8: Battery Optimization & Doze
- **Procedure**: Place device in Battery Saver / Doze mode.
- **Expected Result**: AccessibilityService remains active as system-bound service; `OemBatteryHelper` warns parent if optimizations are enabled.

### Test 9: Target App Launch Vectors
- **Procedure**: Launch restricted target app via: (1) Launcher, (2) Recent Apps carousel, (3) Push Notification tap, (4) Browser deep link, (5) Share Sheet.
- **Expected Result**: Accessibility `TYPE_WINDOW_STATE_CHANGED` fires for all 5 vectors uniformly; overlay intervenes before interaction.

### Test 10: Temporary Unlock & Clock Tamper Resistance
- **Procedure**: Grant temporary unlock (10s); alter device system time forward/backward; reboot device.
- **Expected Result**: Dual-timestamp check (`elapsedRealtime` + wall-clock) detects expiration/reboot; access terminates cleanly without extension.

### Test 11: Policy Update Transitions (ALLOW / BLOCK / DELAY / EARN)
- **Procedure**: Transition a single app between all 4 modes on Web Control Center and push policy.
- **Expected Result**: Policy updates cleanly at version vX+1; PolicyEngine immediately applies updated mode.

### Test 12: Multi-Child Isolation
- **Procedure**: Create Child A and Child B in same Family; assign different app rules.
- **Expected Result**: Device paired with Child A receives only Child A policy; no cross-child policy bleed.

### Test 13: Pairing Failure Modes
- **Procedure**: Attempt pairing with: (1) Valid code, (2) Invalid code, (3) Expired code (>15m TTL), (4) Reused code, (5) Wrong Family ID.
- **Expected Result**: Valid code pairs successfully; all 4 invalid cases fail safely with human-readable error messages.

### Test 14: Data Migration & Schema Preservation
- **Procedure**: Upgrade APK from previous build to Phase 3A build.
- **Expected Result**: SQLite Room database survives; existing pairing credentials, rules, metrics, and Parent PIN remain intact.
