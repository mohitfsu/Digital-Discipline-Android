# Digital Discipline — Android Feasibility Test Plan

**Product Concept**: Digital Discipline (Parental-Control & Screen-Time Intervention)  
**Target Platform**: Android 14 (API 34), Android 15 (API 35), Android 16 (API 36 Preview)  
**Objective**: Empirically evaluate detection latency, overlay intervention reliability, temporary unlock state machines, device policy management mechanisms, tamper boundaries, and Google Play Policy viability.

---

## 1. Test Architecture & Modular Strategy

To ensure zero coupling and allow clean architectural evolution, all components implement well-defined Kotlin interfaces:

```
                  ┌───────────────────────────────┐
                  │       AppLaunchDetector       │
                  └───────────────┬───────────────┘
                                  │
                 ┌────────────────┴────────────────┐
                 ▼                                 ▼
    ┌─────────────────────────┐       ┌─────────────────────────┐
    │ UsageStatsLaunchDetector│       │AccessibilityLaunchDetect│
    └────────────┬────────────┘       └────────────┬────────────┘
                 │                                 │
                 └────────────────┬────────────────┘
                                  ▼
                   ┌──────────────────────────────┐
                   │         PolicyEngine         │
                   └──────────────┬───────────────┘
                                  │
                 ┌────────────────┴────────────────┐
                 ▼                                 ▼
    ┌─────────────────────────┐       ┌─────────────────────────┐
    │OverlayEnforcementStrateg│       │DevicePolicyEnforcementSt│
    └─────────────────────────┘       └─────────────────────────┘
```

### Core Interfaces

1. `AppLaunchDetector`:
   - `startMonitoring(callback: (AppLaunchEvent) -> Unit)`
   - `stopMonitoring()`
   - `isPermissionGranted(): Boolean`
   - `getDetectorType(): DetectorType` (`USAGE_STATS` | `ACCESSIBILITY`)

2. `AppEnforcementStrategy`:
   - `enforceRestriction(packageName: String, rule: RestrictionRule)`
   - `liftRestriction(packageName: String)`
   - `getStrategyType(): EnforcementType` (`OVERLAY` | `DEVICE_POLICY_SUSPENSION`)

3. `InterventionStrategy`:
   - `presentIntervention(context: Context, targetPackage: String, onComplete: () -> Unit, onDismiss: () -> Unit)`

---

## 2. Test Matrix & Experiments

### Experiment A: Foreground App Detection (UsageStats vs Accessibility)

| Metric / Scenario | Experiment A1: `UsageStatsManager` | Experiment A2: `AccessibilityService` |
| :--- | :--- | :--- |
| **API Utilized** | `UsageStatsManager.queryEvents(beginTime, endTime)` | `AccessibilityService.onAccessibilityEvent` |
| **Event Types Analyzed** | `UsageEvents.Event.ACTIVITY_RESUMED` (1), `ACTIVITY_PAUSED` (2) | `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED` (32) |
| **Execution Model** | Periodic Background Polling (`ScheduledExecutorService` / Coroutine) | Event-Driven Push Callback directly from Android OS Window Manager |
| **Tested Polling Rates** | 250ms, 500ms, 1000ms, 2000ms | Real-time push (0ms polling) |
| **Detection Latency Formula** | `Latency = DetectionTimestamp - UsageEvent.timeStamp` | `Latency = CallbackReceivedTimestamp - Event.eventTime` |
| **Launch Vectors Tested** | 1. Launcher home screen icon<br>2. Recents app switcher<br>3. Push notification tap<br>4. URL / Deep link intent<br>5. Cross-app explicit intent | 1. Launcher home screen icon<br>2. Recents app switcher<br>3. Push notification tap<br>4. URL / Deep link intent<br>5. Cross-app explicit intent |
| **Failure / Edge Scenarios** | • Sub-second app switches missed between polling intervals<br>• Query event batching delays on OEM skins<br>• Power saving throttling background execution | • Accessibility service disabled or crashed<br>• Non-activity window changes (keyboards, dialogs, popups triggering false positives) |

---

### Experiment B: Intervention UI Display & Interactive Overlay

| Parameter | Specification |
| :--- | :--- |
| **Enforcement Mechanism** | `WindowManager.addView()` with `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` + Fallback Fullscreen Activity |
| **Required Permissions** | `android.permission.SYSTEM_ALERT_WINDOW` (`Settings.canDrawOverlays()`) |
| **Layout & Interaction Test** | • Non-dismissible full-screen intervention view.<br>• Clearly marked: *"Digital Discipline — Instagram Paused by Parent"*.<br>• Dynamic action buttons: `[ WAIT 10 SECONDS ]`, `[ SQUAT CHALLENGE (10 reps) ]`, `[ EXIT TO HOME ]`. |
| **Window Flag Configurations** | `FLAG_NOT_TOUCH_MODAL` (off), `FLAG_WATCH_OUTSIDE_TOUCH` (on), `FLAG_LAYOUT_IN_SCREEN`, `FLAG_NOT_FOCUSABLE` (handled dynamically). |
| **Child Escape Vectors Tested** | 1. Tapping System Home Button.<br>2. Tapping System Back Gesture/Button.<br>3. Tapping System Recents.<br>4. Pulling Notification Shade / Quick Settings.<br>5. Splitting screen / Floating window.<br>6. Power button press / Screen lock-unlock cycle. |

---

### Experiment C: Temporary Unlock State Machine

```
   [TARGET LAUNCH DETECTED]
              │
              ▼
   ┌──────────────────────┐
   │ STATE: RESTRICTED    │◄─────────────────────────────┐
   │ (Overlay Active)     │                              │
   └──────────┬───────────┘                              │
              │ Child initiates & completes              │
              │ 10s pause or physical challenge          │
              ▼                                          │
   ┌──────────────────────┐                              │
   │ STATE: UNLOCKED_TEMP │                              │
   │ (Overlay Dismissed)  │                              │
   └──────────┬───────────┘                              │
              │                                          │
              ├─► Timer Expires (e.g. 60 seconds) ───────┤
              ├─► App Switched / Backgrounded (Policy) ──┤
              └─► Device Clock Tampered / Fast-Forwarded ┘
```

#### Verification Points
1. **Clock Manipulation Resistance**: Verify whether using `SystemClock.elapsedRealtime()` prevents bypass via system time change compared to `System.currentTimeMillis()`.
2. **Session Continuity**: Verify if switching away from target app and returning within the 60s window consumes time or retains unlock state.
3. **Reboot Resilience**: Check persistence of unlock tokens across system reboot.

---

### Experiment D: Device Policy Management & Enterprise APIs

Investigate Android Enterprise & Device Policy APIs:

| Feature / API | Ordinary App Privileges | Device Admin (Legacy) | Profile Owner (Managed Profile) | Device Owner (Fully Managed) |
| :--- | :---: | :---: | :---: | :---: |
| `setPackagesSuspended()` | ❌ No | ❌ No | ✅ Yes (Managed apps) | ✅ Yes (Any system / user package) |
| `setUninstallBlocked()` | ❌ No | ❌ No | ✅ Yes (Managed apps) | ✅ Yes (Any package) |
| `addUserRestriction(DISALLOW_UNINSTALL_APPS)` | ❌ No | ❌ No | ❌ No | ✅ Yes |
| Provisioning Requirement | Play Store Install | User Settings Opt-in | Work Profile / QR Setup | Factory Reset / NFC / ADB `set-device-owner` |

**Experiment D Test Routine**:
1. Check `DevicePolicyManager.isDeviceOwnerApp()` and `isProfileOwnerApp()`.
2. Execute `setPackagesSuspended(admin, arrayOf("com.instagram.android"), true)` on provisioned test environment.
3. Measure suspension behavior when child taps app icon (OS shows built-in suspension dialog).
4. Unsuspend and measure reactivation time.

---

### Experiment E: Tamper & Bypass Attack Surface

| Test ID | Child Action / Vector | Expected Vulnerability | Spike Mitigation / Detection |
| :--- | :--- | :--- | :--- |
| **T01** | Force Stop Digital Discipline in Settings | Background service killed; overlays and polling terminate. | Accessibility Service is restarted by Android framework unless disabled. Device Owner can disable Settings/Force Stop. |
| **T02** | Revoke Accessibility / Overlay in Settings | Enforcement stops immediately. | Listen for `onServiceDisconnected` & broadcast/usage events; display persistent persistent critical alert. |
| **T03** | Uninstall App via Launcher Drag | App deleted; all rules lost. | Device Owner `setUninstallBlocked` or Accessibility detection of Settings/PackageInstaller. |
| **T04** | Fast-forward System Clock by 1 Hour | Skips temporary unlock timers. | Use monotonic `SystemClock.elapsedRealtime()`. |
| **T05** | Launch App via Recents Thumbnails | Window state change may be altered. | Accessibility `TYPE_WINDOW_STATE_CHANGED` triggers on top task switch. |
| **T06** | Launch App via Notification Quick Action | Direct activity launch bypassing launcher. | Accessibility and UsageStats both log `ACTIVITY_RESUMED`. |
| **T07** | Split-Screen / Multi-Window Mode | Target app shares screen with another app. | Overlay covers entire display coordinates (`MATCH_PARENT`). |
| **T08** | Browser Access (`instagram.com`) | App blocked, but Web active. | Spike focuses on package detection; browser URL filtering requires Accessibility URL scraping or VPN/DNS layer (documented for production roadmap). |

---

## 3. Test Harness Implementation Structure

- **Target Packages Tested**:
  1. `com.instagram.android` (Instagram)
  2. `com.google.android.youtube` (YouTube)
  3. `com.dts.freefireth` (Free Fire) / Configurable Custom Package (e.g. `com.android.chrome`, `com.google.android.apps.messaging`)
- **Developer Test Interface**:
  - Live permission toggles with direct deep-links to Android Settings
  - Active detection source indicator (`UsageStats` vs `Accessibility`)
  - Real-time event monitor and latency counter (in ms)
  - Interactive Action Panel (Trigger Test Delay, Trigger 60s Unlock, Test Suspension, Flush Logs)
- **Local Logging**:
  - Structured log entries: `[TIMESTAMP] | SOURCE=[SRC] | PKG=[PKG] | EVENT=[EVT] | LATENCY=[MS]ms`
