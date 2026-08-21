# Google Play Policy Compliance Checklist: Digital Discipline

**Policy Baseline**: Current Google Play Developer Program Policies  
**Target Category**: Parenting / Digital Wellbeing  
**Key Sensitive APIs**: `AccessibilityService`, `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`  

---

## 1. Accessibility API Policy Verification

- [x] **`android:isAccessibilityTool` is set to `false`**:
  - The app does NOT claim to be a tool for users with disabilities.
- [x] **Parental Control Exception**:
  - Accessibility API usage is strictly declared under the approved **Parental Control and Digital Wellbeing Exception**.
- [x] **Zero Content Sniffing**:
  - `accessibility_service_config.xml` has `android:canRetrieveWindowContent="false"`.
- [x] **Zero Automated UI Actions**:
  - Service does NOT click buttons, navigate settings, dismiss dialogs, or alter operating system state programmatically.
- [x] **Prominent In-App Disclosure Implemented**:
  - Disclosure screen is presented *before* user is directed to the Android Accessibility settings screen.
  - Explains specifically: (1) what data is accessed (active package name), (2) why it is accessed (detecting restricted app launches to display parental intervention overlays), (3) confirms that no personal messages or keystrokes are recorded.
- [x] **Explicit Affirmative Consent**:
  - Requires user to tap an explicit `[ AGREE & CONTINUE ]` button. Contains a distinct `[ DECLINE ]` button.

---

## 2. Overlay Permission (`SYSTEM_ALERT_WINDOW`) Verification

- [x] **Transparent Attribution**:
  - The overlay window prominently displays the **Digital Discipline** brand name and shield icon.
- [x] **Anti-Phishing / Anti-Deception Compliance**:
  - Overlay does NOT mimic Android system dialogs, lockscreens, or target application login screens.
- [x] **No User Trap**:
  - Overlay provides a dedicated `[ 🏠 EXIT TO HOME SCREEN ]` button allowing user to leave the app at any time without entrapment.

---

## 3. Package Visibility & Query Permissions

- [x] **Targeted Package Queries**:
  - Replace broad `QUERY_ALL_PACKAGES` permission in consumer production manifest with specific `<queries>` intent filters (`android.intent.action.MAIN` with `CATEGORY_LAUNCHER`) to comply with Google Play package visibility policies.

---

## 4. Google Play Console Declaration Deliverables

| Declaration Item | Console Section | Requirement / Artifact |
| :--- | :--- | :--- |
| **Accessibility Declaration Form** | App Content → Accessibility API | Select *"Parental Controls / Screen Time"*. Provide step-by-step video link demonstrating the in-app disclosure and overlay behavior. |
| **Data Safety Section** | App Content → Data Safety | Declare collection of: App info and performance (App interactions, Crash logs). Declare zero collection of Personal Info, Messages, Photos, Audio. |
| **Target Audience & Content** | App Content → Target Audience | Select target age groups; declare that the app is a parental supervisory tool designed for parents. |
| **Demonstration Video** | Review Attachment | 60-second video demonstrating: (1) Prominent disclosure UI, (2) Accessibility permission toggle, (3) Instagram launch interception. |
