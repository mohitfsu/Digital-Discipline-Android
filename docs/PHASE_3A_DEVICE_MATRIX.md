# Phase 3A — Android OEM Compatibility & Test Matrix

## 1. Overview
Digital Discipline maintains a **single, unified Android codebase** using standard Android APIs (`AccessibilityService`, `Settings.canDrawOverlays`, `PowerManager`, `WorkManager`, `Room`). No OEM-specific SDK forks are used.

---

## 2. OEM Compatibility Matrix

| OEM & OS Layer | Accessibility Stability | Overlay Permission Flow | Background Process Retention | Recommended Configuration Steps |
| :--- | :--- | :--- | :--- | :--- |
| **Google Pixel** (Stock Android 12–15) | 🟢 **Excellent** (Service survives background idle) | 🟢 Standard Android dialog | 🟢 Stock Doze mode handles WorkManager cleanly | Set Battery Usage $\rightarrow$ "Unrestricted" |
| **Samsung Galaxy** (One UI 4–6) | 🟢 **High** (Keeps service active if excluded from deep sleep) | 🟢 Standard Android Settings redirection | 🟡 "Never sleeping apps" whitelist required | 1. Add to "Never sleeping apps"<br>2. Disable "Put unused apps to sleep" |
| **OnePlus / Oppo / Realme** (OxygenOS / ColorOS / Realme UI) | 🟡 **Moderate** (Aggressive task killer kills unbound processes) | 🟢 Standard system alert window | 🟡 Auto-launch toggle must be enabled | 1. Enable "Allow background activity"<br>2. Enable "Auto-launch"<br>3. Lock app in Recents screen |
| **Xiaomi / Redmi / Poco** (MIUI 13–14 / HyperOS) | 🟡 **Moderate** (MIUI Security kills background services on low memory) | 🟡 Extra confirmation prompt required | 🔴 "No restrictions" battery mode mandatory | 1. Autostart $\rightarrow$ Enabled<br>2. Battery Saver $\rightarrow$ "No restrictions"<br>3. Lock in Recents |

---

## 3. Generic Android API Strategy

1. **Battery Exemption**: `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` allows users to whitelist Digital Discipline from aggressive OS task killing with a single system prompt.
2. **Settings Component Fallback**: `OemBatteryHelper` iterates through known OEM autostart intents (`com.miui.securitycenter`, `com.samsung.android.sm`, `com.coloros.safecenter`) and safely falls back to standard `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` on unlisted devices.
3. **Accessibility Survival**: Since the app binds to the Android system as an active `AccessibilityService`, the Android framework gives it high process priority (`PROCESS_STATE_PERSISTENT_UI` / `FOREGROUND_SERVICE` tier), preventing arbitrary low-memory kills during normal device usage.
