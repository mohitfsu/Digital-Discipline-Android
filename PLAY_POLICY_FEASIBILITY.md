# Google Play Policy Feasibility Analysis: Digital Discipline

**Prepared By**: Senior Android Platform Engineer & Google Play Policy Architect  
**Classification**: Technical & Regulatory Feasibility Assessment  
**Reference Date**: Current Policy Baseline (Google Play Developer Program Policies)

---

## Executive Policy Verdict: Categorized Assessment

| Category | Policy Classification | Feasibility | Risk Level |
| :--- | :--- | :---: | :---: |
| **A. Technically Possible & Fully Compliant** | Core foreground UI, Monotonic timer state machine, Notification alerts | ✅ Feasible | **Low** |
| **B. Technically Possible Only with Special Authority** | `DevicePolicyManager.setPackagesSuspended`, `setUninstallBlocked` | ⚠️ Enterprise/DPC Only | **High** (Not viable for consumer Play Store install) |
| **C. Technically Possible but Subject to Strict Review** | `AccessibilityService`, `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS` | ⚖️ Conditionally Viable | **Medium - High** (Requires declaration compliance) |
| **D. Not Recommended / Forbidden** | Auto-clicking settings, `isAccessibilityTool=true` misuse, bypassing Android setup | ❌ Strictly Prohibited | **Critical Policy Strike** |

---

## 1. Accessibility API Policy Analysis

### Official Policy Baseline & Documentation
- **Official Policy Reference**: [Google Play Accessibility API Policy](https://support.google.com/googleplay/android-developer/answer/10964491)
- **Developer Guidelines**: [AccessibilityService Overview](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)

### Permitted Use Cases & Parental Control Exceptions
1. **The Disability Tool Designation (`isAccessibilityTool`)**:
   - Google strictly restricts `android:isAccessibilityTool="true"` to apps whose **primary purpose** is assisting users with documented disabilities (e.g., TalkBack, screen magnifiers, motor impairment tools).
   - **Verdict**: Digital Discipline **MUST NOT** set `android:isAccessibilityTool="true"`. Doing so triggers automatic review rejection and immediate policy violation under the Accessibility Misrepresentation rule.
2. **Non-Disability Permitted Exception (Parental Controls)**:
   - Google Play policy explicitly permits Accessibility API usage for legitimate **Parental Control** and **Digital Wellbeing** applications under strictly defined conditions:
     - The app must declare the exact scope of monitoring in the Google Play Console Accessibility Declaration form.
     - The app must display a **Prominent In-App Disclosure** prior to runtime permission delegation.
     - The user (parent) must give **Explicit Affirmative Consent**.
     - The app must **NOT** alter user settings without permission, prevent users from disabling the app, or work around Android built-in privacy controls.

### Explicit Prohibitions under Accessibility Policy
Based on current Google Play policies, Digital Discipline **MUST NEVER**:
- Intercept, log, or exfiltrate keystrokes, passwords, chat messages, or payment info.
- Programmatically tap or auto-click the Android UI (e.g. automatically clicking "Force Stop" or clicking through system settings).
- Prevent user uninstallation through UI hijacking or auto-dismissing the Settings uninstaller.
- Modify operating system settings or disable the notification shade programmatically without legitimate Device Owner authority.

---

## 2. Prominent In-App Disclosure & Affirmative Consent Requirements

Under Google's User Data & Accessibility Policies:
- **Location**: The disclosure must be presented inside the app before the user is directed to the Android Accessibility settings screen.
- **Content Requirement**:
  1. Clearly state that the app requests Accessibility permission.
  2. State the exact features that rely on the API (e.g., *"Detecting when restricted apps such as Instagram or YouTube are opened so that parental intervention screens can be displayed."*).
  3. Clearly explain what data is accessed (e.g., *"Package name of active foreground apps"*).
  4. Explicitly declare that no personal messages, screen content, or passwords are recorded or transmitted.
- **Affirmative Action**: Must require an explicit tap on `[ I AGREE / CONTINUE ]` and provide a clear `[ DECLINE / CANCEL ]` option. The disclosure cannot be buried in a Privacy Policy or Terms of Service document.

---

## 3. Overlay Permission (`SYSTEM_ALERT_WINDOW`) Policy

- **Official Policy Reference**: [Google Play Device & Network Abuse Policy](https://support.google.com/googleplay/android-developer/answer/9888379) & [Android 14/15 Window Management](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)
- **Overlay Behavior**:
  - Overlays must be clearly branded and attributable to "Digital Discipline".
  - Overlays must **NOT** mimic Android system dialogs, OS lockscreens, or target app UI (anti-phishing / anti-deception policy).
  - Overlays must allow the user to navigate back to the Home screen (cannot trap the OS navigation stack permanently without child exit route).
  - In Android 14+, background apps starting overlay activities or launching activities from background services are heavily restricted. An active Foreground Service or direct `WindowManager.addView()` via an active Accessibility / System Alert context is required.

---

## 4. Usage Access (`PACKAGE_USAGE_STATS`)

- **Official Reference**: [Google Play App Usage Data Policy](https://support.google.com/googleplay/android-developer/answer/10144342)
- **Requirement**:
  - Requires the `android.permission.PACKAGE_USAGE_STATS` special access token.
  - Permitted for Digital Wellbeing and Parental Control apps where calculating screen time or detecting app usage is the core user-facing functionality.
  - Polling frequency does not violate policy, but inefficient polling causing excessive battery drain triggers Android Vitals bad behavior thresholds (excessive wake locks / background execution).

---

## 5. Foreground Service & Background Execution Policy (Android 14 & 15)

- **Official Reference**: [Android 14 Foreground Service Types](https://developer.android.com/about/versions/14/changes/fgs-types-promoted)
- **Policy Requirements**:
  - For continuous operation, apps targeting API 34+ must declare specific `foregroundServiceType` attributes in `AndroidManifest.xml` (e.g., `specialUse` or `dataSync` with detailed justification in Play Console).
  - Since `AccessibilityService` runs as a framework-managed service with its own lifecycle, it does not strictly require a continuous foreground service notification to observe window transitions; however, keeping a persistent lightweight foreground service ensures process longevity on OEM battery managers.

---

## 6. Device Policy Manager, Device Owner & Profile Owner Constraints

- **Official Reference**: [Android Enterprise Overview & Play Policy](https://developer.android.com/work/dpc/build-dpc)
- **Consumer App Limitations**:
  - **Device Owner (DO)**: Cannot be provisioned via a simple Google Play Store download. It requires either:
    1. Factory reset and scanning an enterprise QR code during initial setup.
    2. Provisioning via ADB command on development/kiosk devices.
  - **Play Store Viability**: Any consumer app claiming to require Device Owner provisioning will fail consumer adoption and face severe distribution hurdles unless distributed as a B2B Enterprise / MDM product.
  - **Device Admin (Legacy)**: Legacy Device Admin (`DeviceAdminReceiver`) is allowed on Play Store for parental controls, but Google has deprecated almost all enforcement features in modern Android (cannot suspend packages or block uninstall in consumer mode).

---

## 7. Prohibited Features (Do Not Implement)

| Feature / Technique | Reason for Prohibition | Google Play Policy Penalty |
| :--- | :--- | :--- |
| Auto-clicking "Cancel" in uninstaller | Accessibility UI manipulation | Immediate app suspension |
| Fake System Lock UI | Deceptive behavior & impersonation | Policy strike & app removal |
| Hiding app icon from Launcher | Device & Network Abuse | Removal from Play Store |
| Monitoring Keystrokes or Notification Content | Privacy violation / Spyware policy | Account termination |
| Sideloaded-only APIs | Misleading user experience | Rejection in review |

---

## 8. Conclusion & Play Store Strategy

Based on current official Google Play developer policies:
1. An architecture using **AccessibilityService** + **SYSTEM_ALERT_WINDOW** + **PACKAGE_USAGE_STATS** for **Parental Controls & Digital Wellbeing** is **plausibly approved on Google Play**, provided that:
   - Full prominent disclosures are displayed before requesting permissions.
   - Declarations in the Google Play Console accurately select "Parental Control / Wellbeing".
   - `android:isAccessibilityTool` is left as `false`.
   - The UI does not manipulate third-party app views or block the device permanently without escape to Home.
2. Enterprise Device Owner features (`setPackagesSuspended`) are **not viable for general consumer Play distribution** and should only be positioned as an optional advanced add-on for dedicated managed child devices.
