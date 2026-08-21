# Digital Discipline — Android Technical Architecture

**Classification**: Engineering Design & Component Architecture  
**Target Platform**: Android 14 (API 34), Android 15 (API 35), Android 16 (API 36 Preview)  
**Package Namespace**: `com.digitaldiscipline.spike`

---

## 1. High-Level Architectural Overview

Digital Discipline is engineered as a decoupled, event-driven supervisor designed to intercept compulsive app usage through immediate, non-deceptive parental interventions. The architecture uses pluggable interfaces for detection and enforcement to ensure policy agility, maintainability, and clean separation of concerns.

```
                                 ┌──────────────────────────────┐
                                 │      Android OS System       │
                                 │   (WindowManager / System)   │
                                 └──────────────┬───────────────┘
                                                │
                 ┌──────────────────────────────┴──────────────────────────────┐
                 │                                                             │
                 ▼ (Push Event)                                                ▼ (Polling Query)
  ┌─────────────────────────────┐                               ┌─────────────────────────────┐
  │   AccessibilityService      │                               │     UsageStatsManager       │
  │ (TYPE_WINDOW_STATE_CHANGED) │                               │  (ACTIVITY_RESUMED Events)  │
  └──────────────┬──────────────┘                               └──────────────┬──────────────┘
                 │                                                             │
                 ▼                                                             ▼
  ┌─────────────────────────────┐                               ┌─────────────────────────────┐
  │ AccessibilityLaunchDetector │                               │  UsageStatsLaunchDetector   │
  └──────────────┬──────────────┘                               └──────────────┬──────────────┘
                 │                                                             │
                 └──────────────────────────────┬──────────────────────────────┘
                                                │ AppLaunchEvent (pkg, timestamp, latency)
                                                ▼
                                 ┌──────────────────────────────┐
                                 │         PolicyEngine         │
                                 │    (Rules & State Machine)   │
                                 └──────────────┬───────────────┘
                                                │
                 ┌──────────────────────────────┴──────────────────────────────┐
                 │ (Consumer Mode)                                             │ (Enterprise / Kiosk Mode)
                 ▼                                                             ▼
  ┌─────────────────────────────┐                               ┌─────────────────────────────┐
  │  OverlayEnforcementStrategy │                               │ DevicePolicyEnforcementStrat│
  │ (TYPE_APPLICATION_OVERLAY)  │                               │   (setPackagesSuspended)    │
  └──────────────┬──────────────┘                               └──────────────┬──────────────┘
                 │                                                             │
                 ▼                                                             ▼
  ┌─────────────────────────────┐                               ┌─────────────────────────────┐
  │       OverlayManager        │                               │ DevicePolicyManagerWrapper  │
  │ (Interactive Challenge UI)  │                               │    (DevicePolicyManager)    │
  └─────────────────────────────┘                               └─────────────────────────────┘
```

---

## 2. Core Architectural Interfaces

### 2.1 `AppLaunchDetector`
```kotlin
interface AppLaunchDetector {
    fun startMonitoring(callback: (AppLaunchEvent) -> Unit)
    fun stopMonitoring()
    fun isPermissionGranted(): Boolean
    fun getDetectorType(): DetectorType
    fun isRunning(): Boolean
}
```
- **Implementations**:
  - `AccessibilityLaunchDetector`: Uses push callbacks from Android's WindowManager. Zero CPU overhead when idle. Latency: **35ms–85ms**.
  - `UsageStatsLaunchDetector`: Uses background coroutine polling (250ms–2000ms). Query overhead on battery. Latency: **250ms–1150ms**.

### 2.2 `AppEnforcementStrategy`
```kotlin
interface AppEnforcementStrategy {
    fun enforceRestriction(packageName: String, appDisplayName: String, unlockDurationSeconds: Int)
    fun liftRestriction(packageName: String)
    fun isEnforcing(): Boolean
    fun getStrategyName(): String
}
```
- **Implementations**:
  - `OverlayEnforcementStrategy`: Displays non-deceptive full-screen overlay window (`TYPE_APPLICATION_OVERLAY`) rendering real-time countdowns and physical challenge triggers.
  - `DevicePolicyEnforcementStrategy`: Invokes `DevicePolicyManager.setPackagesSuspended()` to disable target application at the operating system level (requires Device Owner).

---

## 3. End-to-End Execution Sequence

```
Child User             Android OS              Detector             PolicyEngine         OverlayManager
    │                      │                      │                      │                     │
    │── Taps Instagram ───►│                      │                      │                     │
    │                      │── Window Transition ─►│                      │                     │
    │                      │   (Push / Poll)      │── AppLaunchEvent ───►│                     │
    │                      │                      │   (com.instagram)    │                     │
    │                      │                      │                      │── Evaluates State   │
    │                      │                      │                      │   (BLOCKED)         │
    │                      │                      │                      │                     │
    │                      │                      │                      │── enforceRestriction│
    │                      │                      │                      │────────────────────►│
    │                      │                      │                      │                     │── addView() [OVERLAY]
    │◄─────────────────────┼──────────────────────┼──────────────────────┼─────────────────────│   (Latency: ~65ms)
    │  Intervention Screen │                      │                      │                     │
    │  "Instagram Paused"  │                      │                      │                     │
    │                      │                      │                      │                     │
    │── Taps [10s Pause] ─►│                      │                      │                     │
    │   (Countdown active) │                      │                      │                     │
    │                      │                      │                      │                     │
    │── 10s Elapsed ──────►│                      │                      │                     │
    │                      │                      │                      │◄── onComplete() ────│
    │                      │                      │                      │                     │── removeView()
    │                      │                      │                      │── State: UNLOCKED   │
    │                      │                      │                      │   (Expiry = Now+60s)│
    │◄─────────────────────┴──────────────────────┴──────────────────────┴─────────────────────┘
    │  Instagram Active & Usable (60 seconds)
    │
    │                      [ 60 SECONDS PASS ]
    │                      ┌─────────────────────────────────────────────┐
    │                      │ PolicyEngine Monotonic Expiry Timer Fires   │
    │                      └──────────────────────┬──────────────────────┘
    │                                             │
    │                                             │── State: BLOCKED
    │                                             │── enforceRestriction()
    │                                             │───────────────────────────────────────────►│
    │                                             │                                            │── addView() [OVERLAY]
    │◄────────────────────────────────────────────┴────────────────────────────────────────────│
    │  Intervention Screen Re-engaged
```

---

## 4. State Machine & Monotonic Clock Security

### State Diagram

```
         ┌─────────────────────────────────────────────────────────────┐
         ▼                                                             │
  ┌──────────────┐     Challenge Started     ┌──────────────────────┐  │
  │   BLOCKED    ├──────────────────────────►│ INTERVENTION_ACTIVE  │  │
  └──────────────┘                           └──────────┬───────────┘  │
         ▲                                              │              │
         │                                              │ Completed    │
         │                                              ▼              │
         │         Timer Expired (e.g. 60s)  ┌──────────────────────┐  │
         └───────────────────────────────────┤  UNLOCKED_TEMPORARY  │  │
                                             └──────────────────────┘  │
                                                        │              │
                                                        └──────────────┘
                                                    Fast-Forward Attempt
                                                    (Ignored by Monotonic Clock)
```

### Clock Tampering Mitigation
Children often attempt to bypass parental timers by advancing the device date/time in Android Settings.
- **Vulnerability**: Using `System.currentTimeMillis()` or `Date()` allows bypass.
- **Solution**: Digital Discipline strictly relies on `SystemClock.elapsedRealtime()`, which measures continuous hardware uptime including deep sleep and is completely unaffected by changes to system time zones, network time, or manual clock alterations.

---

## 5. Concurrency & Thread Safety Model

1. **`EventLogger`**: Uses `CopyOnWriteArrayList` and Kotlin `StateFlow` to guarantee non-blocking log ingestion across concurrent background detector threads and Compose UI consumers.
2. **`PolicyEngine`**: State modifications execute via thread-safe `ConcurrentHashMap` with Coroutine `SupervisorJob` managing timeout lifecycles.
3. **`OverlayManager`**: UI modifications are strictly marshaled to Android's Main Looper via `Handler(Looper.getMainLooper())` to eliminate UI thread exceptions.
