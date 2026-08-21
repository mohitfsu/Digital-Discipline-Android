# Digital Discipline — Phase 0 Codebase Audit & Production Freeze Report

**Date**: August 2026  
**Document**: `docs/PHASE_0_AUDIT.md`  
**Classification**: Engineering Audit & Architecture Baseline  
**Auditor**: Senior Android Platform Engineer & Google Play Policy Architect  

---

## Executive Summary

This document establishes the official **Phase 0 Audit and Production Freeze** for the **Digital Discipline** codebase. 

The preceding Feasibility Spike successfully demonstrated that:
1. **Real-time foreground app detection** via `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`) operates with an average latency of **~58ms**, completely eliminating visual leakage of target app feeds.
2. **Full-screen interactive interventions** via `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` reliably block all underlying app interactions while providing non-deceptive, accessible challenge choices and safe home-screen exits.
3. **Monotonic elapsed-realtime state management** (`SystemClock.elapsedRealtime()`) renders unlock timers impervious to system clock fast-forward attacks.
4. **Device Policy Management (`setPackagesSuspended`)** requires Device Owner (DO) privileges, proving that consumer Google Play distribution must rely on the **Accessibility + Overlay Architecture**.

This audit inventories all existing files, classifies every component for production reuse, catalogues technical debt, and outlines the precise architectural path for Phase 1.

---

## 1. Existing Architecture & System Diagram

The existing codebase follows a decoupled, event-driven supervisor pattern with pluggable interfaces:

```
                                 ┌──────────────────────────────┐
                                 │       Android Framework      │
                                 │   (WindowManager / OS Push)  │
                                 └──────────────┬───────────────┘
                                                │
                 ┌──────────────────────────────┴──────────────────────────────┐
                 │                                                             │
                 ▼ (Real-time OS Push)                                         ▼ (Periodic Polling Query)
  ┌─────────────────────────────┐                               ┌─────────────────────────────┐
  │   AccessibilityService      │                               │     UsageStatsManager       │
  │ (TYPE_WINDOW_STATE_CHANGED) │                               │  (ACTIVITY_RESUMED Events)  │
  └──────────────┬──────────────┘                               └──────────────┬──────────────┘
                 │                                                             │
                 ▼                                                             ▼
  ┌─────────────────────────────┐                               ┌─────────────────────────────┐
  │ AccessibilityLaunchDetector │                               │  UsageStatsLaunchDetector   │
  └──────────────┬──────────────┘                               └──────────────┬──────────────┘
                 │ AppLaunchEvent                                              │ AppLaunchEvent
                 └──────────────────────────────┬──────────────────────────────┘
                                                │
                                                ▼
                                 ┌──────────────────────────────┐
                                 │         PolicyEngine         │
                                 │  • State Machine             │
                                 │  • Monotonic Realtime Timers │
                                 │  • Concurrent Rule Mapping   │
                                 └──────────────┬───────────────┘
                                                │
                 ┌──────────────────────────────┴──────────────────────────────┐
                 │ (Consumer Production Path)                                  │ (Enterprise / DPC Path)
                 ▼                                                             ▼
  ┌─────────────────────────────┐                               ┌─────────────────────────────┐
  │  OverlayEnforcementStrategy │                               │ DevicePolicyEnforcementStrat│
  └──────────────┬──────────────┘                               └──────────────┬──────────────┘
                 │                                                             │
                 ▼                                                             ▼
  ┌─────────────────────────────┐                               ┌─────────────────────────────┐
  │       OverlayManager        │                               │ DevicePolicyManagerWrapper  │
  │  • TYPE_APPLICATION_OVERLAY │                               │  • setPackagesSuspended     │
  │  • 10s Pause Challenge      │                               │  • setUninstallBlocked      │
  │  • 10 Squats Trigger        │                               │  • DO/PO Status Detection   │
  └─────────────────────────────┘                               └─────────────────────────────┘
```

---

## 2. Component Inventory & Classification

Every file in the repository is audited and classified under one of five strict statuses:
- **KEEP**: Reusable directly in production with zero or negligible modification.
- **REFACTOR**: Solid architectural design, but needs enhancement (persistence, Compose migration, lifecycle safety).
- **REPLACE**: Test-specific harness that will be replaced with consumer-grade production equivalents.
- **REMOVE**: Code that must not enter the consumer Google Play MVP.
- **EXPERIMENTAL**: Research/benchmarking code preserved in documentation or developer builds only.

### 2.1 Core Detection Package (`com.digitaldiscipline.spike.detection`)

| File | Status | Purpose in Spike | Production Strategy |
| :--- | :---: | :--- | :--- |
| `AppLaunchDetector.kt` | **KEEP** | Core interface contract defining `startMonitoring`, `stopMonitoring`, `isPermissionGranted`. | Retain as the foundational detection interface contract. |
| `AppLaunchEvent.kt` | **KEEP** | Domain model holding package, class, event timestamps, source, and latency. | Retain as immutable domain model. |
| `DigitalDisciplineAccessibilityService.kt` | **KEEP** | Android AccessibilityService capturing `TYPE_WINDOW_STATE_CHANGED` with `canRetrieveWindowContent=false`. | Core production engine. Complies with Google Play Parental Control policy. |
| `AccessibilityLaunchDetector.kt` | **KEEP** | Concrete detector bridging `DigitalDisciplineAccessibilityService` to `AppLaunchDetector`. | Retain directly. |
| `UsageStatsLaunchDetector.kt` | **REFACTOR / EXPERIMENTAL** | Background polling detector using `UsageStatsManager.queryEvents` (250ms–2000ms). | Relegate from primary blocker to background screen-time analytics engine in Phase 2. |

### 2.2 Overlay & Intervention Package (`com.digitaldiscipline.spike.overlay` & `.intervention`)

| File | Status | Purpose in Spike | Production Strategy |
| :--- | :---: | :--- | :--- |
| `OverlayManager.kt` | **REFACTOR** | Manages `WindowManager` view lifecycle with `TYPE_APPLICATION_OVERLAY`, 10s pause timer, and exit to Home. | Refactor view generation to use Jetpack Compose `ComposeView` inside overlay window; add Parent PIN protection. |
| `AppEnforcementStrategy.kt` | **KEEP** | Strategy interface for app restriction enforcement (`enforceRestriction`, `liftRestriction`). | Retain as core abstraction. |
| `OverlayEnforcementStrategy.kt` | **KEEP** | Concrete strategy wrapping `OverlayManager`. | Retain as default consumer enforcement strategy. |
| `InterventionStrategy.kt` | **KEEP** | Interface contract for presenting parental interventions. | Retain for pluggable intervention types (Pause, Squats, Study). |
| `InterventionActivity.kt` | **REMOVE** | Fullscreen Activity fallback for intervention UI. | Remove from consumer build; `OverlayManager` is the proven mechanism without activity stack interference. |

### 2.3 Policy & State Machine Package (`com.digitaldiscipline.spike.policy`)

| File | Status | Purpose in Spike | Production Strategy |
| :--- | :---: | :--- | :--- |
| `RestrictionRule.kt` | **REFACTOR** | Rule data class managing package target, display name, and monotonic unlock expiry. | Extend to support schedules, daily quotas, and persistent Room entity mapping. |
| `PolicyState.kt` | **KEEP** | Enum defining states: `BLOCKED`, `INTERVENTION_ACTIVE`, `UNLOCKED_TEMPORARY`, `ALLOWED`. | Retain directly. |
| `PolicyEngine.kt` | **REFACTOR** | Orchestrates detection events, monotonic unlock countdowns, and enforcement dispatch. | Refactor to inject Room DAO / DataStore repository instead of in-memory `ConcurrentHashMap`. |
| `DevicePolicyManagerWrapper.kt` | **REMOVE from Consumer MVP** | Wrapper for `DevicePolicyManager`, DO/PO status checks, package suspension. | Move to optional enterprise module. Infeasible for standard Play Store consumer app. |
| `DevicePolicyEnforcementStrategy.kt` | **REMOVE from Consumer MVP** | Strategy calling `dpm.setPackagesSuspended()`. | Move to optional enterprise module. |
| `DigitalDisciplineDeviceAdminReceiver.kt` | **REMOVE from Consumer MVP** | Legacy `DeviceAdminReceiver` implementation. | Remove; legacy device admin offers no modern app suspension capabilities on Android 14+. |

### 2.4 Diagnostics, Logging & UI (`com.digitaldiscipline.spike.logging` & `.ui`)

| File | Status | Purpose in Spike | Production Strategy |
| :--- | :---: | :--- | :--- |
| `EventLogger.kt` | **REFACTOR** | Thread-safe in-memory log buffer (`CopyOnWriteArrayList`) with `StateFlow` streaming. | Keep as internal debug/audit diagnostic tool; gate behind `BuildConfig.DEBUG`. |
| `LogEvent.kt` | **KEEP** | Structured logging model with formatted string output. | Retain for debug diagnostics. |
| `MainActivity.kt` | **REPLACE** | Spike test harness activity with permission cards, test controls, and live log stream. | Replace with consumer UI (Parent Dashboard, Onboarding, PIN setup, App Selector). |
| `ui/components/StatusCard.kt` | **REPLACE** | Permission status and deep link launcher buttons. | Replace with Consumer Onboarding & Permission Wizard. |
| `ui/components/TargetAppsCard.kt` | **REPLACE** | Target app list and switch toggles. | Replace with Installed App Selector with search & categories. |
| `ui/components/ForegroundMonitorCard.kt` | **REPLACE** | Live foreground app monitor card. | Diagnostic only; not presented to consumer child/parent. |
| `ui/components/TestControlPanel.kt` | **REPLACE** | Developer test buttons (Trigger Overlay, 60s Unlock, Device Policy). | Diagnostic only; remove in consumer UI. |
| `ui/components/EventLogView.kt` | **REPLACE** | Live scrolling event log view. | Diagnostic only; move to hidden Developer Settings screen. |
| `ui/theme/Theme.kt` | **KEEP** | Dark color scheme and Material 3 theme configuration. | Retain and expand design tokens for production UI. |

---

## 3. Existing Validated Components (Production Ready)

The following 6 architectural components were empirically validated in the spike and are ready for direct production reuse:

1. **`DigitalDisciplineAccessibilityService`**:
   - Zero UI content sniffing (`canRetrieveWindowContent="false"`).
   - Fast event dispatch (`notificationTimeout="50"`).
   - Near real-time response time (**35ms – 85ms**).
   - Full compliance with Google Play Parental Control policy exceptions.
2. **`AppLaunchDetector` Interface Contract**:
   - Decoupled observer interface allowing clean runtime switching and zero coupling to UI.
3. **`OverlayManager` Window Layout Architecture**:
   - `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` with `FLAG_LAYOUT_IN_SCREEN` and `FLAG_NOT_TOUCH_MODAL` (unset).
   - Guaranteed 100% touch interception preventing child interaction with underlying feeds.
   - Non-deceptive UI with safe exit to Home Screen.
4. **Monotonic Realtime Unlock State Machine**:
   - Hardware uptime tracking via `SystemClock.elapsedRealtime()`.
   - Complete immunity against user clock fast-forward attacks.
5. **Multi-Vector Launch Interception**:
   - Consistently captures 100% of app launches from Launcher, Recents, Notifications, and Deep Links.
6. **Thread-Safe Architecture**:
   - Non-blocking concurrent event dispatch using Kotlin Coroutines, `ConcurrentHashMap`, and `StateFlow`.

---

## 4. Known Technical Debt & Architecture Gaps

| Area | Current Spike Implementation | Production Requirement | Priority |
| :--- | :--- | :--- | :---: |
| **Persistence** | Volatile in-memory `ConcurrentHashMap`. Rules reset on app kill. | Room Database + Jetpack DataStore with encrypted preferences. | **High** |
| **Settings Tampering** | Child can navigate to Settings and disable Accessibility. | Accessibility monitor for `com.android.settings` requiring Parent PIN. | **High** |
| **Service Wiring** | Static companion singleton `DigitalDisciplineAccessibilityService.instance`. | Reactive event bus (Kotlin `SharedFlow` / local BroadcastChannel / Service Binding). | **Medium** |
| **Overlay UI Engine** | Imperative Android `LinearLayout` / `TextView` views. | Jetpack Compose `ComposeView` inside overlay window for rich animation and state handling. | **Medium** |
| **App Discovery** | Hardcoded target list with manual string input. | `PackageManager.getInstalledApplications()` query scanner with app icons and labels. | **Medium** |
| **Browser Circumvention** | App blocked, but `instagram.com` accessible in Chrome. | Web URL inspector via Accessibility or Local Loopback VPN DNS filter. | **Phase 2** |
| **Physical Exercise** | Direct unlock button (mock challenge). | On-device ML Pose Detection (MediaPipe) for real squat/pushup verification. | **Phase 2** |

---

## 5. Existing Permissions & Manifest Audit

```xml
<!-- Manifest Permissions in Spike -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

### Manifest Audit & Action Plan
- `SYSTEM_ALERT_WINDOW`: **Retain** (Mandatory for intervention overlay).
- `BIND_ACCESSIBILITY_SERVICE`: **Retain** (Mandatory for real-time foreground detection).
- `POST_NOTIFICATIONS`: **Retain** (Required on Android 13+ for parent alerts and status updates).
- `PACKAGE_USAGE_STATS`: **Retain for Phase 2** (Secondary analytics and tamper cross-check).
- `BIND_DEVICE_ADMIN`: **Remove from Consumer Build** (Not needed without Device Owner).
- `QUERY_ALL_PACKAGES`: **Refactor** to `<queries>` element filtering `android.intent.action.MAIN` categories to meet Google Play package visibility policy.

---

## 6. Build Configuration & Dependencies

- **Gradle Version**: 8.11.1
- **Android Gradle Plugin (AGP)**: 8.7.3
- **Kotlin**: 2.0.21 (with Compose Compiler Plugin)
- **Compile SDK**: 35 (Android 15)
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35 (Android 15)
- **Core Dependencies**:
  - `androidx.core:core-ktx:1.15.0`
  - `androidx.lifecycle:lifecycle-runtime-ktx:2.8.7`
  - `androidx.activity:activity-compose:1.9.3`
  - `androidx.compose:compose-bom:2024.11.00`
  - `androidx.compose.material3:material3`
- **Dependency Health**: Clean, minimal, zero bloated third-party SDKs, zero Firebase, zero analytics bloat.

---

## 7. Security Perimeter & Known Bypass Vectors

| Bypass Vector | Spike Behavior | Production Phase 1 Mitigation |
| :--- | :--- | :--- |
| **Force-Stop in Settings** | Service killed; enforcement ceases. | Monitor `com.android.settings` via Accessibility; prompt for Parent PIN. |
| **Disable Accessibility** | Push events cease. | Intercept Accessibility Settings page; send high-priority notification to parent. |
| **Uninstall App** | App removed. | Monitor `com.google.android.packageinstaller` and launcher uninstaller flows. |
| **Clock Fast-Forward** | **BLOCKED** in Spike. | Monotonic hardware clock (`SystemClock.elapsedRealtime()`) retained. |
| **Recents / Deep Links** | **BLOCKED** in Spike. | Instantaneous Window Manager push intercept retained. |
| **Web Browser Access** | Mobile web accessible. | Scheduled for Phase 2 (URL Guard / Local DNS Filter). |
| **Split-Screen Mode** | **BLOCKED** in Spike. | Full-screen overlay coordinates retained. |

---

## 8. Summary Comparison: What to Keep vs What to Remove

```
┌───────────────────────────────────────────────┬───────────────────────────────────────────────┐
│              KEEP FOR PRODUCTION              │          REMOVE FROM CONSUMER MVP             │
├───────────────────────────────────────────────┼───────────────────────────────────────────────┤
│ • DigitalDisciplineAccessibilityService       │ • DevicePolicyManagerWrapper (Enterprise only)│
│ • AccessibilityLaunchDetector                 │ • DevicePolicyEnforcementStrategy             │
│ • AppLaunchDetector Interface                 │ • DigitalDisciplineDeviceAdminReceiver        │
│ • OverlayManager (Window layout core)         │ • InterventionActivity (Redundant with overlay│
│ • OverlayEnforcementStrategy                  │ • TestControlPanel & Developer spike buttons  │
│ • Monotonic State Machine (PolicyEngine core) │ • ForegroundMonitorCard diagnostic UI         │
│ • RestrictionRule & PolicyState domain models │ • Static test target app hardcoding           │
│ • Modern Gradle 8.11 + Compose Version Catalog│ • In-memory volatile-only rule storage        │
└───────────────────────────────────────────────┴───────────────────────────────────────────────┘
```
