# Phase 3B — Circumvention & Vulnerability Matrix

## 1. Overview
This matrix documents every potential circumvention vector analyzed during Phase 3B, classifying severity, detection capabilities, safe mitigations, and Google Play policy implications.

---

## 2. Comprehensive Circumvention Matrix

| Vector ID | Attack / Circumvention Technique | Attacker Level | Severity | Can Be Detected? | Safe Mitigation Available? | Google Play Policy Risk | Target Disposition |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **BYPASS-01** | **Disabling Accessibility Service in Settings** | Level 2 | **HIGH** | 🟢 **Yes** (`onUnbind` fires in 0ms) | 🟢 **Yes** (Alert Parent Dashboard via cloud heartbeat) | 🔴 **HIGH** if blocking Settings; 🟢 **NONE** if reporting | **MVP Detection + Telemetry Alert** |
| **BYPASS-02** | **Revoking Overlay Permission** | Level 2 | **HIGH** | 🟢 **Yes** (`Settings.canDrawOverlays`) | 🟢 **Yes** (Display Warning Banner + Notify Parent) | 🟢 **NONE** | **MVP Detection + Telemetry Alert** |
| **BYPASS-03** | **App Force Stop via Settings / Task Killer** | Level 2 | **HIGH** | 🟡 **Delayed** (Heartbeat timeout) | 🟡 **Partial** (OEM background autostart guidance) | 🔴 **HIGH** if self-restarting; 🟢 **NONE** if heartbeat | **Heartbeat Timeout Alert** |
| **BYPASS-04** | **Clear Storage / App Data** | Level 2 | **HIGH** | 🟢 **Yes** (Device re-pairs/registers) | 🟢 **Yes** (Cloud policy & credentials restored) | 🟢 **NONE** | **Cloud Policy Persistence** |
| **BYPASS-05** | **App Uninstallation** | Level 2 | **HIGH** | 🟡 **Delayed** (Missed heartbeats) | 🟡 **Partial** (Parent dashboard marks device offline) | 🔴 **FATAL** if blocking uninstallation; 🟢 **NONE** if reporting | **Platform Limitation (Heartbeat)** |
| **BYPASS-06** | **Clock / Timezone Modification** | Level 1 | **LOW** | 🟢 **Prevented** | 🟢 **Enforced** (`SystemClock.elapsedRealtime()`) | 🟢 **NONE** | **MVP Enforced (Monotonic Clock)** |
| **BYPASS-07** | **Reboot during Temporary Unlock** | Level 1 | **LOW** | 🟢 **Prevented** | 🟢 **Enforced** (Dual-timestamp detects boot reset) | 🟢 **NONE** | **MVP Enforced (Fail-Closed)** |
| **BYPASS-08** | **Alternative Launch Vectors (Recents/Share)** | Level 1 | **LOW** | 🟢 **Prevented** | 🟢 **Enforced** (`TYPE_WINDOW_STATE_CHANGED`) | 🟢 **NONE** | **MVP Enforced (Window Interceptor)** |
| **BYPASS-09** | **Mobile Web Access (e.g. `m.instagram.com`)** | Level 2 | **MEDIUM** | 🟡 **Audit Only** | 🟡 **Future Phase** (DNS/VpnService or Accessibility URL) | 🟡 **MODERATE** (VpnService / Accessibility URL rules) | **Phase 4 Roadmap (Audit Only)** |
| **BYPASS-10** | **OEM App Cloner / Dual Apps** | Level 2 | **MEDIUM** | 🟢 **Yes** (Target package selection) | 🟢 **Yes** (Add custom package e.g. `.clone`) | 🟢 **NONE** | **MVP Extensible App Presets** |
| **BYPASS-11** | **Multi-User / Guest Profile Switch** | Level 2 | **MEDIUM** | 🔴 **No** (OS sandbox separation) | 🔴 **No** (Requires Device Owner / MDM) | 🔴 **N/A** (OS platform boundary) | **Acceptable Platform Boundary** |
| **BYPASS-12** | **Cross-Child Policy Tampering** | Level 2 | **CRITICAL** | 🟢 **Prevented** | 🟢 **Enforced** (Firestore auth & scoped rules) | 🟢 **NONE** | **MVP Enforced (Firestore Rules)** |
| **BYPASS-13** | **Expired / Reused Pairing Codes** | Level 2 | **MEDIUM** | 🟢 **Prevented** | 🟢 **Enforced** (Atomic 15-min TTL & `isUsed` flag) | 🟢 **NONE** | **MVP Enforced (Pairing Protocol)** |
| **BYPASS-14** | **SQLite Database Tampering (Rooted)** | Level 3 | **LOW** | 🔴 **No** | 🔴 **No** (Out of consumer scope) | 🟢 **NONE** | **Out of Scope (Rooted Device)** |

---

## 3. Severity Scoring Methodology

$$\text{Severity Score} = \text{Impact} \times \text{Likelihood} \times \text{Ease of Execution}$$

- **CRITICAL**: Threat enables cross-user data exposure, unauthorized remote policy alteration, or parent PIN compromise without device access.
- **HIGH**: Threat allows child to completely disable local protection via standard Android menus (Settings revocation, Force Stop, Data Clear). *Mitigated via fail-closed behavior and parent cloud heartbeat reporting.*
- **MEDIUM**: Alternative access methods (web browser versions, app cloners, guest profiles) that circumvent package-specific enforcement.
- **LOW**: Minor vector or fully prevented by architectural controls (clock tampering, launch vectors, reboot reset).
- **ACCEPTABLE PLATFORM LIMITATION**: Built-in Android OS mechanisms (Uninstallation, Multi-User Guest mode) that cannot be blocked without Device Owner (MDM) or root access.
