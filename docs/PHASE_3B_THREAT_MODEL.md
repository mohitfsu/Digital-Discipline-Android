# Phase 3B — Threat Model & Attacker Personas

## 1. Overview
Digital Discipline is a consumer parental-control and screen-time discipline application for Android. It operates within the boundaries of standard Android framework permissions and consumer sandboxing without requiring Device Owner (MDM), ADB provisioning, or device rooting.

---

## 2. Attacker Classification

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           THREAT SPECTRUM                               │
├───────────────────┬────────────────────────────┬────────────────────────┤
│      LEVEL 1      │          LEVEL 2           │        LEVEL 3         │
│   Normal Child    │  Technically Savvy Teen    │   Advanced / Rooted    │
│  (Ages 6 – 12)    │      (Ages 13 – 17)        │                        │
├───────────────────┼────────────────────────────┼────────────────────────┤
│ • Native UI       │ • Android Settings         │ • Magisk / KernelSU    │
│ • Intuitive apps  │ • Safe Mode / Force Stop   │ • ADB / Fastboot       │
│ • Accidental taps │ • Clear Storage / Cache    │ • Custom ROMs          │
│ • Clock tampering │ • Dual Apps / Clones       │ • Frida / Hooking      │
│ • Launch vectors  │ • Web browser alternatives │ • SQLite / DB edit     │
├───────────────────┼────────────────────────────┼────────────────────────┤
│   IN SCOPE (MVP)  │    IN SCOPE (PARTIAL)      │     OUT OF SCOPE       │
│  100% Prevention  │ Detection + Heartbeat Alert│ Platform Compromise    │
└───────────────────┴────────────────────────────┴────────────────────────┘
```

### Level 1: Normal Child (Ages 6 – 12)
- **Capabilities**: Navigates launchers, app stores, home screens, and notifications. May attempt to change the device clock or launch apps through indirect shortcuts (Recents, Google Assistant, share sheets).
- **Security Goal**: **100% Prevention & Enforcement**. Interventions (Mindful Pause, Breathing, Squats, PIN) must block target apps seamlessly across all standard launch vectors. Monotonic clocks must prevent time tampering.

### Level 2: Technically Capable Teenager (Ages 13 – 17)
- **Capabilities**: Explores Android Settings, revokes Accessibility or Overlay permissions, attempts App Force Stop, uses Clear Data, launches mobile browsers (e.g. Chrome, Firefox) to access web versions of blocked apps, or uses OEM App Cloners / Dual Messenger.
- **Security Goal**: **Fail-Closed Protection + Instant Parent Telemetry**. While consumer Android allows users to revoke permissions in Settings, Digital Discipline must instantly detect unbinding, update the cloud heartbeat, and alert the parent dashboard (`PROTECTION DISABLED`).

### Level 3: Rooted / Developer / Advanced Attacker
- **Capabilities**: Unlocks bootloader, installs Magisk/KernelSU, modifies SQLite database via root shell, injects hooks via Frida/Xposed, or issues ADB package disabling commands.
- **Security Goal**: **Explicit Non-Goal for Consumer MVP**. No consumer non-MDM app can defend against an attacker with root kernel execution. Digital Discipline documents this as an out-of-scope platform boundary.

---

## 3. Threat Assessment Matrix

| Threat ID | Vector | Target Level | Consumer Feasibility | System Impact | Security Posture |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **TH-01** | Clock Tampering | Level 1 | High | Bypass unlock duration | **Prevented** (Monotonic clock) |
| **TH-02** | Alternative Launch Vectors | Level 1 | High | Bypass launcher hook | **Prevented** (Accessibility window state) |
| **TH-03** | Permission Revocation (A11y/Overlay) | Level 2 | High | Disables enforcement | **Detected & Alerted** (Heartbeat telemetry) |
| **TH-04** | Web Browser Bypass | Level 2 | High | Access via `m.instagram.com` | **Audited** (Documented limitation) |
| **TH-05** | Force Stop / Task Kill | Level 2 | Moderate | Halts service process | **Detected** (Heartbeat timeout) |
| **TH-06** | Clear App Data | Level 2 | Moderate | Resets local DB | **Mitigated** (Cloud policy restoration) |
| **TH-07** | OEM App Cloning | Level 2 | Moderate | Cloned package names | **Detected & Rule Extensible** |
| **TH-08** | Root / Kernel Injection | Level 3 | Low | Total compromise | **Out of Scope** |
