# Phase 3B — Security & Circumvention Test Suite

This document defines the automated and manual verification procedures for all 13 circumvention categories.

---

### SEC-01: Accessibility Permission Revocation
- **Objective**: Verify that disabling the Accessibility Service in Android Settings is detected in real time and parent dashboard reflects `PROTECTION DISABLED`.
- **Procedure**:
  1. Pair Android device and verify `OFFLINE-FIRST PROTECTION ACTIVE`.
  2. Open Android Settings $\rightarrow$ Accessibility $\rightarrow$ Turn OFF Digital Discipline.
  3. Inspect local protection state in `ParentDashboardScreen` and cloud heartbeat in Web Dashboard.
- **Pass Criteria**: `TamperDetector` records `isAccessibilityActive = false`; Parent Dashboard immediately reflects `PROTECTION DISABLED`; diagnostic logger records `ACCESSIBILITY_DISABLED`.

### SEC-02: Overlay Permission Revocation
- **Objective**: Verify that revoking "Display over other apps" fails safely without app crash or false "Protected" claims.
- **Procedure**:
  1. Revoke overlay permission in Settings $\rightarrow$ Special App Access $\rightarrow$ Display over other apps.
  2. Launch restricted app (YouTube).
- **Pass Criteria**: `OverlayManager` checks `canDrawOverlays()`, logs `OVERLAY_PERMISSION_MISSING`, does NOT crash, and Parent Dashboard warns `PROTECTION DISABLED`.

### SEC-03: Process Force-Stop
- **Objective**: Document process death behavior and verify state reconstruction upon next launch.
- **Procedure**:
  1. Execute `adb shell am force-stop com.digitaldiscipline.spike`.
  2. Launch target app or re-open Digital Discipline.
- **Pass Criteria**: Process respawns; Room DB loads active rules; Parent PIN remains required; cloud policy survives.

### SEC-04: Clear App Storage / Data
- **Objective**: Verify security state behavior after storage wipe.
- **Procedure**:
  1. Execute `adb shell pm clear com.digitaldiscipline.spike`.
  2. Open Digital Discipline.
- **Pass Criteria**: App enters clean initialization; cannot be configured without new Parent PIN / Pairing; cloud policy is untouched in Firestore and can be re-synchronized.

### SEC-05: Device Uninstallation
- **Objective**: Verify platform behavior and cloud status tracking upon uninstallation.
- **Procedure**:
  1. Uninstall application from device.
  2. Inspect Parent Web Control Center.
- **Pass Criteria**: Cloud device heartbeat stops updating; parent dashboard identifies device as unlinked/offline after grace period.

### SEC-06: Device Reboot During Temporary Unlock
- **Objective**: Verify that hardware reboot resets monotonic clock and immediately invalidates active temporary unlocks.
- **Procedure**:
  1. Complete Mindful Pause intervention to earn 10-minute unlock on YouTube.
  2. Reboot phone via `adb reboot` (or simulate reboot).
  3. Launch YouTube immediately after unlocking phone.
- **Pass Criteria**: Dual-timestamp check (`currentElapsed < unlockGrantedElapsed`) flags reboot; unlock is invalidated; intervention screen appears immediately (fail-closed).

### SEC-07: Clock & Timezone Tampering
- **Objective**: Verify that changing system time forward/backward cannot extend temporary unlocks.
- **Procedure**:
  1. Grant 10s unlock on Instagram.
  2. Advance system time by +2 hours.
  3. Wait 10 real seconds.
- **Pass Criteria**: Unlock expires in exactly 10 real seconds regardless of wall-clock setting.

### SEC-08: Target App Launch Vectors
- **Objective**: Verify uniform enforcement across all 5 launch paths.
- **Procedure**: Launch blocked target app via: (1) Launcher icon, (2) Recent Apps switcher, (3) Push Notification, (4) Chrome web link, (5) Android Share Sheet.
- **Pass Criteria**: In all 5 cases, `TYPE_WINDOW_STATE_CHANGED` detects the target package and attaches the Compose overlay.

### SEC-09: Mobile Web Browser Bypass Audit
- **Objective**: Audit circumvention via Chrome/Firefox web versions of restricted apps.
- **Procedure**:
  1. Restrict native Instagram app.
  2. Open Google Chrome $\rightarrow$ navigate to `https://www.instagram.com`.
- **Pass Criteria**: Document that native package rule does not intercept web URLs; assess URL detection feasibility vs VPN approach.

### SEC-10: Alternative Android Profiles (Clones & Guests)
- **Objective**: Audit OEM App Cloner and Multi-User profile boundaries.
- **Procedure**: Create a cloned instance of a target app (e.g. Dual Messenger / Parallel Space).
- **Pass Criteria**: Document package name suffix behavior and add custom package preset support.

### SEC-11: Pairing Protocol & Security Isolation
- **Objective**: Verify 6-digit code TTL, replay prevention, and multi-tenant isolation.
- **Procedure**:
  1. Attempt pairing with expired code (>15 min).
  2. Attempt pairing with used code.
  3. Attempt pairing with invalid code.
- **Pass Criteria**: All 3 invalid attempts fail with clear error messages; zero unauthorized device associations.

### SEC-12: Cloud Firestore Access Rules
- **Objective**: Verify client-side Firestore rules prevent unauthorized reading or writing of another family's policy.
- **Procedure**: Attempt to read `/families/{otherFamilyId}/children/{otherChildId}/policy/current` without valid parent authentication.
- **Pass Criteria**: Firestore rejects request with `PERMISSION_DENIED`.

### SEC-13: Local SQLite Database Tampering
- **Objective**: Verify Android sandbox isolation for non-rooted devices.
- **Procedure**: Attempt to access `/data/data/com.digitaldiscipline.spike/databases/` from a standard non-root third-party file manager.
- **Pass Criteria**: Android OS sandbox denies access (`EACCES: Permission denied`).
