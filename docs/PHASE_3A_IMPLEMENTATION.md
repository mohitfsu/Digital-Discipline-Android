# Phase 3A — Production Reliability & Android/OEM Hardening Implementation

## 1. System Architecture & Core Tenets
Phase 3A hardens Digital Discipline for real-world Android deployments across diverse OEM skins (Google Pixel, Samsung One UI, OnePlus OxygenOS, Xiaomi MIUI/HyperOS, Oppo ColorOS, Realme UI) without compromising **Rule #1** (Preserving the core `AccessibilityService` + `TYPE_APPLICATION_OVERLAY` + `PolicyEngine` + `Room` + Monotonic Time enforcement architecture).

---

## 2. Implemented Subsystems & Hardening

### A. System Reboot & Package Update Recovery (`BootCompletedReceiver.kt`)
- **Intent Filters**: `ACTION_BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `ACTION_MY_PACKAGE_REPLACED`.
- **Integrity Check**: Verifies Room database accessibility, counts active rules, and asserts database integrity immediately upon system initialization.
- **Heartbeat & Sync Restoration**: Re-enqueues periodic policy sync and analytics WorkManager jobs and refreshes the local `ProtectionStateEntity`.

### B. Dual-Timestamp Monotonic Unlock Security (`TemporaryUnlockEntity.kt`)
- **Monotonic Clock**: Uses `SystemClock.elapsedRealtime()` which is immune to device wall-clock tampering.
- **Reboot Detection**: If `currentElapsedRealtime < unlockGrantedElapsedRealtime`, the device was restarted since the unlock was issued. The unlock is immediately revoked (fail-closed).
- **Wall-Clock Bounding**: Ensures unlocks are bounded by wall-clock time (`createdAt + unlockDurationMs + 10s`) to prevent sleep/hibernation exploitation.

### C. Zero-Upload Structured Diagnostic Logging (`DiagnosticLogger.kt`)
- **Strict On-Device Logging**: Local Room table `diagnostic_events` capturing:
  - `SERVICE_STARTED` / `SERVICE_STOPPED`
  - `OVERLAY_PERMISSION_MISSING` / `ACCESSIBILITY_DISABLED`
  - `POLICY_LOADED` / `POLICY_SYNC_STARTED` / `POLICY_SYNC_SUCCEEDED` / `POLICY_SYNC_FAILED` / `POLICY_VERSION_CHANGED`
  - `UNLOCK_CREATED` / `UNLOCK_EXPIRED`
  - `BOOT_COMPLETED` / `BOOT_INTEGRITY_VERIFIED`
- **Rolling Buffer**: Automatically prunes records beyond 200 items to preserve storage.
- **Privacy Enforcement**: Diagnostic logs are 100% on-device and never transmitted to external analytics or cloud.

### D. Cross-OEM Battery Optimization Navigation (`OemBatteryHelper.kt`)
- **Optimization Detection**: Checks `PowerManager.isIgnoringBatteryOptimizations()`.
- **OEM Safe Intent Dispatch**: Resolves native auto-start and background activity managers on Xiaomi (`com.miui.securitycenter`), Samsung (`com.samsung.android.sm`), and OnePlus/Oppo/Realme (`com.coloros.safecenter`) with graceful fallback to standard Android application details.

### E. Fail-Closed Offline Resilience
- **Offline Integrity**: During network loss or Firebase outage, the local PolicyEngine continues enforcing known-good Room policies without fallback to unrestricted access.
- **Atomic Transactions**: Policy synchronization commits rules and schedules in atomic Room transactions (`transactionalUpdatePolicy`), rolling back completely if any individual entity fails.

---

## 3. Subsystem File Map
| Subsystem | File Path | Purpose |
| :--- | :--- | :--- |
| **Diagnostics Entity** | `app/src/main/java/.../data/local/entities/DiagnosticEventEntity.kt` | Room entity for structured local events |
| **Diagnostics DAO** | `app/src/main/java/.../data/local/dao/DiagnosticEventDao.kt` | Pruning & Flow queries |
| **Diagnostics Logger** | `app/src/main/java/.../logging/DiagnosticLogger.kt` | Async non-blocking event dispatch |
| **Boot Receiver** | `app/src/main/java/.../detection/BootCompletedReceiver.kt` | Restart resilience & DB assertion |
| **OEM Helper** | `app/src/main/java/.../detection/OemBatteryHelper.kt` | Cross-OEM battery manager intents |
| **Unlock Model** | `app/src/main/java/.../data/local/entities/TemporaryUnlockEntity.kt` | Dual-timestamp tamper-proof unlock |
| **Sync Manager** | `app/src/main/java/.../sync/SyncManager.kt` | Atomic cloud-to-Room sync with logging |
| **Dashboard UI** | `app/src/main/java/.../ui/dashboard/ParentDashboardScreen.kt` | On-device diagnostics & OEM health UI |
