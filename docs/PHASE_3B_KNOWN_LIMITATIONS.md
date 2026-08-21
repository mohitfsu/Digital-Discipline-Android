# Phase 3B — Known Android Security & Platform Boundaries

## 1. Executive Summary
Digital Discipline is architected for consumer Android devices without requiring enterprise Device Owner (MDM) provisioning, root exploits, or prohibited anti-uninstall techniques. This document records the fundamental security boundaries inherent to the consumer Android operating system.

---

## 2. Inherent Android Platform Boundaries

### A. Non-Blockable Application Uninstallation
- **Platform Reality**: Android is designed to give device owners complete agency over installed third-party applications. Non-system applications cannot prevent their own uninstallation.
- **Consumer Posture**: Digital Discipline treats uninstallation not as a failure of code, but as a detected lifecycle state. The cloud control plane tracks device heartbeats; when a device stops reporting, the Parent Web Dashboard alerts the parent.

### B. Android Settings & Permission Revocation
- **Platform Reality**: The user can open Android Settings at any time to disable Accessibility Services or revoke Overlay permissions.
- **Consumer Posture**: Attempting to block `com.android.settings` using aggressive overlays or automated back-button gestures violates Google Play policies and creates high risks of malware flagging. Digital Discipline detects revocations in real time ($< 5\text{ ms}$) and updates the parent dashboard with `PROTECTION DISABLED`.

### C. Multi-User & Guest Profile Sandboxing
- **Platform Reality**: Android supports secondary users and guest profiles with distinct application sandboxes (`/data/user/10/`). An application installed under User 0 is not active under User 10 unless specifically provisioned.
- **Consumer Posture**: Parents should disable Guest Mode in Android Settings ($\text{Settings} \rightarrow \text{System} \rightarrow \text{Multiple users} \rightarrow \text{OFF}$) on the child's device during initial setup.

### D. Mobile Web Browser Domain Access
- **Platform Reality**: Accessibility service rules keyed to Android package names (e.g. `com.instagram.android`) restrict the native app, but do not block web traffic when the child navigates to `https://www.instagram.com` inside a web browser.
- **Consumer Posture**: Documented as an audit finding. Full web blocking requires a future dedicated DNS / `VpnService` engine (Phase 4 Roadmap).

### E. Rooted / Compromised Devices (Level 3 Attacker)
- **Platform Reality**: A user with root execution (Magisk, KernelSU) can directly modify SQLite database files, alter memory, or hook system APIs via Frida/Xposed.
- **Consumer Posture**: Explicitly out of scope for the consumer threat model.
