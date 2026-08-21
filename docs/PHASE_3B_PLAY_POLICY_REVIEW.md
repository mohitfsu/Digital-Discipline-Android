# Phase 3B — Google Play Store Policy & Compliance Review

## 1. Overview
Digital Discipline is positioned as a **transparent, privacy-preserving parental control tool**. To guarantee long-term Play Store compliance, this review evaluates Google Play Developer Program Policies regarding Accessibility Services, Device Admin, User Data, and Uninstallation.

---

## 2. Policy Evaluation Matrix

| Android API / Feature | Google Play Policy Section | Allowed Use Case? | Compliance Requirements & Guidelines | Digital Discipline Status |
| :--- | :--- | :---: | :--- | :---: |
| **AccessibilityService** | *Accessibility API Policy* | 🟢 **YES** | 1. Must declare prominent disclosure before requesting permission.<br>2. Must explain that it detects foreground package name to apply parental discipline.<br>3. Must NOT read keystrokes, messages, or screen content (`canRetrieveWindowContent=false`).<br>4. Must NOT claim to be a disability-accessibility tool (`isAccessibilityTool=false`). | 🟢 **100% Compliant** (`canRetrieveWindowContent=false` in XML; Prominent Disclosure dialog active) |
| **SYSTEM_ALERT_WINDOW (Overlay)** | *Malware / Overlay Policy* | 🟢 **YES** | 1. Must NOT use cloaking or tapjacking.<br>2. Fullscreen intervention UI must provide clear dismiss / home navigation.<br>3. Must NOT obscure critical system dialogues (permissions, emergency calls). | 🟢 **100% Compliant** (Contains clear "Exit to Home" and "Parent PIN" buttons) |
| **Device Admin Receiver** | *Device Administration Policy* | 🟢 **YES (Optional)** | 1. Permitted for parental control & remote policy applications.<br>2. Must clearly explain what administration capabilities are used.<br>3. Must NOT block standard device uninstallation through malicious tricks. | 🟢 **Compliant** (Optional component, no anti-uninstall locking) |
| **Settings / Anti-Uninstall Blocking** | *Device & Network Abuse Policy* | 🔴 **PROHIBITED** | 1. Applications are strictly forbidden from preventing users from accessing Android Settings or uninstalling the app.<br>2. Automatically clicking "Cancel" or overlaying over the system uninstall dialog triggers **immediate Play Store removal / account termination**. | 🟢 **Strictly Prohibited & Excluded** |
| **User Data & Analytics** | *User Data Policy* | 🟢 **YES** | 1. Non-surveillance data minimization.<br>2. Zero transmission of keystrokes, SMS, browsing history, audio, or camera feeds.<br>3. Daily analytics aggregated strictly into rollups (total minutes, block counts). | 🟢 **100% Compliant** (Aggregated rollups only, zero PII collection) |

---

## 3. Mandatory Play Console Declarations

### A. Prominent Disclosure Declaration
When submitting to the Google Play Store, the app must present a prominent in-app disclosure modal *before* navigating the parent to the Accessibility settings screen:

> *"Digital Discipline uses the Android AccessibilityService API solely to detect when a parent-restricted application (e.g. YouTube, Instagram) is opened on this device in order to display the chosen mindfulness pause or physical challenge. Digital Discipline DOES NOT record screen contents, keystrokes, messages, passwords, or personal data."*

### B. App Content & Data Safety Section
- **Location**: None collected.
- **Personal Info**: Parent email only (for Firebase Authentication).
- **Photos / Videos / Audio / Files**: None collected.
- **App Activity**: App launch counts and aggregated screen-time duration (Stored with Firebase encryption in transit and at rest).
- **Ephemeral Identifiers**: Randomly generated 6-digit pairing code (15-minute TTL).
