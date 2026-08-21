# Digital Discipline — Android Enforcement Feasibility Spike

**Digital Discipline** is a parental-control & screen-time intervention prototype designed to help parents reduce compulsive app usage (e.g. Instagram, YouTube, gaming apps) through interactive physical challenges and mindful pauses.

This repository contains the complete technical feasibility spike codebase, empirical test harness, and Google Play policy feasibility documentation.

---

## Deliverables Index

1. [`FEASIBILITY_REPORT.md`](FEASIBILITY_REPORT.md) — Comprehensive Feasibility Report & Final Verdict.
2. [`PLAY_POLICY_FEASIBILITY.md`](PLAY_POLICY_FEASIBILITY.md) — Deep-dive Google Play Policy & Accessibility API Analysis with official references.
3. [`ANDROID_ARCHITECTURE.md`](ANDROID_ARCHITECTURE.md) — Detailed Technical Architecture, Sequence Flows, and Interfaces.
4. [`TEST_PLAN.md`](TEST_PLAN.md) — Experimental Test Matrix, Latency Formulas, and Test Scenarios.
5. [`TEST_RESULTS.md`](TEST_RESULTS.md) — Empirical Test Results, Latency Benchmarks, and Battery Metrics.
6. [`BYPASS_RESULTS.md`](BYPASS_RESULTS.md) — Security Boundary & Child Bypass Vulnerability Analysis.

---

## 🛠️ Prerequisites & Build Instructions

### Requirements
- **JDK**: Java 17 or Java 21 (OpenJDK / Android Studio Bundled JBR)
- **Android SDK**: Compile SDK 35 / Min SDK 26
- **Device / Emulator**: Android 14 (API 34), Android 15 (API 35), or Android 16 (API 36 Preview)

### Building the Debug APK
```powershell
# Set Java and Android SDK environment
$env:JAVA_HOME = "D:\Android Studio\jbr"
$env:ANDROID_HOME = "D:\AndroidStudio\sdk"

# Build Debug APK
.\gradlew.bat assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📲 Installation & Device Setup

### 1. Install via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Grant Permissions via ADB (Fast Automated Setup)
```bash
# 1. Grant Usage Statistics Access
adb shell appops set com.digitaldiscipline.spike GET_USAGE_STATS allow

# 2. Grant Draw Over Other Apps (Overlay)
adb shell appops set com.digitaldiscipline.spike SYSTEM_ALERT_WINDOW allow

# 3. Enable Digital Discipline Accessibility Service
adb shell settings put secure enabled_accessibility_services com.digitaldiscipline.spike/com.digitaldiscipline.spike.detection.DigitalDisciplineAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

*(Alternatively, you can tap the action buttons directly inside the app to open the respective Android Settings pages).*

---

## 🧪 Step-by-Step Testing Guide

### Test 1: Accessibility-Driven App Detection & Overlay
1. Ensure **Accessibility** and **Overlay** permissions are marked `ENABLED` on the dashboard.
2. Ensure **Instagram** (`com.instagram.android`) is toggled **ON** under *Target Applications*.
3. Launch Instagram from your home screen or recents.
4. **Expected Result**: Within **35ms–85ms**, the *Digital Discipline* full-screen intervention UI appears:
   ```
   DIGITAL DISCIPLINE
   Instagram Paused
   Your parent has enabled a screen-time rule.
   [ WAIT 10 SECONDS ]  or  [ COMPLETE 10 SQUATS ]
   ```
5. Check the live event log in Digital Discipline to observe the recorded latency in milliseconds.

### Test 2: Mindful Pause & 60-Second Temporary Unlock
1. When the intervention overlay appears, tap `[ ⏳ WAIT 10 SECONDS ]`.
2. Observe the animated progress bar and real-time countdown.
3. Upon reaching 0s, the overlay automatically dismisses.
4. **Expected Result**: Instagram becomes fully interactable.
5. After exactly **60 seconds**, the overlay automatically re-appears to restrict Instagram again.

### Test 3: Monotonic Clock Fast-Forward Tamper Test
1. Complete an intervention to unlock Instagram for 60 seconds.
2. Immediately go to **Android Settings → System → Date & Time** and advance the system clock forward by 2 hours.
3. Return to Instagram.
4. **Expected Result**: The unlock remains active until the 60 actual hardware seconds elapse. The monotonic hardware uptime clock (`SystemClock.elapsedRealtime()`) prevents clock manipulation bypasses.

### Test 4: UsageStats vs Accessibility Comparison
1. In the app dashboard under *Test Controls*, switch detection engine to **UsageStats (Poll)**.
2. Select **500ms** polling rate.
3. Launch Instagram and observe the increased detection latency (**~390ms** avg).
4. Notice how Accessibility provides instant (~58ms) push response without polling battery drain.

### Test 5: Device Policy & Enterprise App Suspension (Test Device Only)
1. On a dedicated test device or emulator provisioned as Device Owner:
   ```bash
   adb shell dpm set-device-owner com.digitaldiscipline.spike/.policy.DigitalDisciplineDeviceAdminReceiver
   ```
2. Tap **[ Test Device Policy ]** on the dashboard.
3. **Expected Result**: Instagram icon is grayed out on the launcher. Launching Instagram shows the Android OS system dialog: *"Instagram is paused by your administrator"*.

---

## 🏛️ Architecture & Package Structure

```
app/src/main/java/com/digitaldiscipline/spike/
├── DigitalDisciplineApp.kt               # Application Singleton & DI
├── detection/
│   ├── AppLaunchDetector.kt              # Core Detection Interface
│   ├── AppLaunchEvent.kt                 # Event Model & Latency
│   ├── UsageStatsLaunchDetector.kt       # UsageStats Polling Engine (250-2000ms)
│   ├── AccessibilityLaunchDetector.kt    # Accessibility Bridge
│   └── DigitalDisciplineAccessibilityService.kt # Window Transition Listener
├── intervention/
│   ├── InterventionStrategy.kt           # Intervention Interface
│   ├── InterventionActivity.kt           # Fallback Fullscreen Activity
├── overlay/
│   └── OverlayManager.kt                 # TYPE_APPLICATION_OVERLAY Window Manager
├── policy/
│   ├── AppEnforcementStrategy.kt         # Enforcement Interface
│   ├── OverlayEnforcementStrategy.kt     # Overlay Strategy Impl
│   ├── DevicePolicyEnforcementStrategy.kt# OS Suspension Strategy Impl
│   ├── DevicePolicyManagerWrapper.kt     # DPM & Enterprise Authority Checks
│   ├── DigitalDisciplineDeviceAdminReceiver.kt # Admin Receiver
│   ├── PolicyEngine.kt                   # Monotonic State Machine & Scheduler
│   └── RestrictionRule.kt                # Rule & Unlock Session Model
├── logging/
│   ├── LogEvent.kt                       # Structured Log Entry
│   └── EventLogger.kt                    # Thread-Safe In-Memory & StateFlow Stream
└── ui/
    ├── MainActivity.kt                   # Developer Dashboard Activity
    ├── theme/                            # Jetpack Compose Theme & Colors
    └── components/
        ├── StatusCard.kt                 # Permission Toggles & Status
        ├── TargetAppsCard.kt             # Target App Selector & Switch
        ├── ForegroundMonitorCard.kt      # Live Foreground Package & Latency
        ├── TestControlPanel.kt           # Interactive Experiment Triggers
        └── EventLogView.kt               # Live Log Viewer & Clear Action
```
