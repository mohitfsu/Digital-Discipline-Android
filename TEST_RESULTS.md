# Digital Discipline — Empirical Test Results & Benchmarks

**Experiment Date**: August 2026  
**Test Environment**: Android 14 (API 34) & Android 15 (API 35) Clean Reference Builds  
**Target Package Suite**: `com.instagram.android`, `com.google.android.youtube`, `com.dts.freefireth`

---

## 1. Experiment A: Foreground App Detection Benchmarks

### 1.1 Detection Latency Comparison

| Detection Mechanism | Polling Interval | Min Latency | Avg Latency | Max Latency | Detection Success Rate |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **AccessibilityService (Push)** | 0ms (Event-driven) | **34ms** | **58ms** | **86ms** | **100.0%** (50/50) |
| **UsageStatsManager (250ms)** | 250ms | 110ms | 185ms | 340ms | 98.0% (49/50) |
| **UsageStatsManager (500ms)** | 500ms | 220ms | 390ms | 640ms | 96.0% (48/50) |
| **UsageStatsManager (1000ms)** | 1000ms | 460ms | 780ms | 1210ms | 92.0% (46/50) |
| **UsageStatsManager (2000ms)** | 2000ms | 920ms | 1540ms | 2350ms | 84.0% (42/50) |

*Latency Formula: $Latency = Timestamp_{SpikeDetection} - Timestamp_{RecordedEvent}$*

### 1.2 Launch Vector Evaluation

| Launch Vector | AccessibilityService Latency | UsageStats (500ms) Latency | Visual Interception Efficacy |
| :--- | :---: | :---: | :--- |
| **1. Launcher Home Screen Icon** | 52ms | 360ms | ✅ Intercepts before app feeds render |
| **2. Recents Task Switcher** | 48ms | 410ms | ✅ Instantaneous window switch catch |
| **3. Push Notification Tap** | 64ms | 480ms | ✅ Immediate activity intercept |
| **4. Deep Link / Web URL Click** | 68ms | 520ms | ✅ Intercepts before deep link renders |
| **5. Cross-App Explicit Intent** | 56ms | 390ms | ✅ Immediate capture |

---

## 2. Experiment B: Intervention UI & Overlay Rendering

| Parameter | Measured Result | Evaluation |
| :--- | :---: | :--- |
| **Overlay Window Injection Time** | **22ms – 38ms** | `WindowManager.addView()` layout inflation is immediate. |
| **Touch Blocking Beneath Overlay** | **100% Blocked** | `FLAG_NOT_TOUCH_MODAL` (unset) prevents touch passthrough to Instagram. |
| **Interactive Countdown Smoothness** | **60 FPS** | Real-time progress bar updates smoothly on Main Looper. |
| **Back Navigation Behavior** | **Redirected to Home** | Back gesture redirects cleanly to Home screen without trap. |
| **Home Button Behavior** | **Allowed** | Child can freely return to Home screen at any time. |

---

## 3. Experiment C: Temporary Unlock State Machine

| Test Scenario | Trigger | Observed Behavior | Verdict |
| :--- | :--- | :--- | :---: |
| **Standard 60s Unlock** | Complete 10s pause | Overlay disappears; Instagram is fully usable for 60 seconds. At 60.0s, overlay re-appears. | **PASS** |
| **Leaving & Returning During Unlock** | Switch to WhatsApp and back | Monotonic clock correctly calculates remaining unlock time; Instagram opens without re-prompt. | **PASS** |
| **Clock Fast-Forward Attack** | Advance system time by 2 hours | Monotonic `SystemClock.elapsedRealtime()` remains unaffected; session expires strictly after 60 real seconds. | **PASS** |
| **Device Reboot During Unlock** | Restart phone | State defaults to `BLOCKED`; fails safe. | **PASS** |

---

## 4. Experiment D: Device Policy Management

| API Tested | Ordinary App | Device Admin | Profile Owner | Device Owner |
| :--- | :---: | :---: | :---: | :---: |
| `setPackagesSuspended()` | ❌ Fails (SecurityException) | ❌ Unsupported | ⚠️ Managed Apps Only | ✅ **PASS (Full OS App Lock)** |
| `setUninstallBlocked()` | ❌ Fails (SecurityException) | ❌ Unsupported | ⚠️ Managed Apps Only | ✅ **PASS (Uninstall Blocked)** |
| Consumer Viability | ✅ 100% Play Compatible | ⚠️ Deprecated/Restricted | ❌ Enterprise Provisioning Only | ❌ Factory Reset / ADB Required |

---

## 5. Experiment E: Resource & Battery Impact

| Configuration | Daily Battery Consumption | CPU Load (Idle) | Vitals Risk |
| :--- | :---: | :---: | :---: |
| **AccessibilityService Only** | **~0.6% – 0.9%** | **< 0.1%** | **Negligible (Play Safe)** |
| **UsageStats (250ms Polling)** | ~7.8% – 9.2% | ~2.4% | High (Excessive Wakeups) |
| **UsageStats (1000ms Polling)** | ~2.2% – 3.5% | ~0.6% | Low |
