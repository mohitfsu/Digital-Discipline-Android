# Digital Discipline — Privacy Architecture & Data Minimization Document

**Classification**: Privacy by Design & Data Minimization Charter  
**Compliance Standard**: Google Play User Data Policy, COPPA (Children's Online Privacy Protection Act), GDPR Article 25  

---

## 1. Privacy Philosophy: Digital Discipline vs Spyware

Digital Discipline is strictly a **mindful habit-building and screen-time intervention platform**, **NOT a spyware or surveillance tool**. 

We empower parents to set healthy boundaries and help children overcome compulsive dopamine loops without violating their digital dignity or harvesting sensitive personal data.

---

## 2. Explicit Prohibitions (Data We NEVER Collect)

Digital Discipline **MUST NEVER** collect, inspect, record, or transmit any of the following data categories:

| Prohibited Category | Architectural Enforcement & Safeguards |
| :--- | :--- |
| **Screenshots & Screen Video** | No `MediaProjection` API, no virtual display recording. |
| **Chat & Message Content** | `AccessibilityService.canRetrieveWindowContent` is hardcoded to `false`. |
| **Keystrokes / Typing Data** | No Custom Input Method (IME), no `TYPE_VIEW_TEXT_CHANGED` accessibility masks. |
| **Notification Payloads** | No `NotificationListenerService` body extraction. |
| **Microphone & Ambient Audio** | `RECORD_AUDIO` permission is completely absent from `AndroidManifest.xml`. |
| **Camera Footage & Photos** | Future physical exercise verification uses on-device real-time pose keypoints (MediaPipe); zero video frames or photos will ever be saved to disk or transmitted to servers. |
| **Personal Contacts & Call Logs** | `READ_CONTACTS` and `READ_CALL_LOG` permissions are completely absent. |
| **Precise GPS Location** | `ACCESS_FINE_LOCATION` is completely absent. |
| **Personal Files & Documents** | `MANAGE_EXTERNAL_STORAGE` and `READ_MEDIA_*` permissions are absent. |

---

## 3. Data We Actually Collect & Process

| Data Field | Storage Location | Processing Purpose | Retention Period |
| :--- | :--- | :--- | :--- |
| **Package Name** (e.g. `com.instagram.android`) | Local Room DB | Match foreground app against parental restriction rules. | Up to 30 days locally |
| **Foreground Session Duration** (seconds) | Local Room DB | Aggregate daily screen time for child and parent graphs. | Aggregated daily |
| **Intervention Action** (`PAUSE`, `SQUATS`) | Local Room DB | Track mindfulness challenge adherence. | 30 days locally |
| **Aggregated Daily Summary** | Cloud Firestore | Display weekly trends on parent web dashboard. | 90 days in cloud |
| **Device Model & OS Version** | Cloud Firestore | Ensure compatibility and diagnostic support. | Lifetime of pairing |

---

## 4. Child Data Protection & Parental Transparency

1. **Zero Third-Party Ad Trackers**: No Google AdMob, Meta Audience Network, AppsFlyer, or data brokers.
2. **Prominent Onboarding Transparency**: The child's device UI explicitly states that the app is installed by their parent to enforce screen-time rules, eliminating deceptive surveillance.
3. **Right to Erasure**: Deleting a child's profile from the parent dashboard cascades and permanently removes all paired devices, policies, and daily summaries from Firestore.
