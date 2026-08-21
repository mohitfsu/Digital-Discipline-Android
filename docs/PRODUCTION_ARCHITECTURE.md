# Digital Discipline — Production Architecture Document

**Classification**: Core System Architecture & Engineering Blueprint  
**Target Platform**: Android (Kotlin / Compose / Room / DataStore), Web (Next.js / TypeScript), Backend (Firebase Auth / Cloud Firestore)  
**Author**: Senior Android Platform Engineer & System Architect  

---

## 1. System Philosophy & Business Constraints

Digital Discipline is engineered to scale seamlessly from **100 users to 1,000,000+ users** while operated by an ultra-lean team (Founder + AI-assisted development, 1–3 humans).

### Primary Principle: **Local-First Enforcement**
- Real-time foreground detection, policy evaluation, and intervention display operate **100% locally on-device**.
- Zero runtime dependencies on Firebase, Internet access, Cloud Functions, Gemini, or Parent Dashboards for core blocking.
- The child's device functions autonomously even in offline mode, airplane mode, or during cloud outages.
- Cloud infrastructure is used solely for:
  1. Parent identity and multi-device authentication.
  2. Asynchronous policy distribution & synchronization.
  3. Device pairing and health telemetry.
  4. Daily aggregated analytics summaries.
  5. Subscription and account entitlement verification.

---

## 2. High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                   CHILD ANDROID DEVICE                                  │
│                                                                                         │
│  ┌────────────────────────┐         ┌────────────────────────┐                          │
│  │   Android OS Window    │         │  AccessibilityService  │                          │
│  │   Manager Event Push   │────────►│  (TYPE_WINDOW_STATE)   │                          │
│  └────────────────────────┘         └───────────┬────────────┘                          │
│                                                 │ AppLaunchEvent (~58ms)                │
│                                                 ▼                                       │
│                                     ┌────────────────────────┐                          │
│                                     │      PolicyEngine      │                          │
│                                     └───────────┬────────────┘                          │
│                                                 │                                       │
│                    ┌────────────────────────────┴───────────────────────────┐           │
│                    │                                                        │           │
│                    ▼                                                        ▼           │
│       ┌─────────────────────────┐                              ┌─────────────────────────┐
│       │    PolicyRepository     │                              │     OverlayManager      │
│       │  (Room Local Database)  │                              │  (ComposeView Overlay)  │
│       └────────────┬────────────┘                              └─────────────────────────┘
│                    │                                                                    │
│                    ├─► AppRuleEntity (BLOCK / DELAY / EARN / ALLOW)                     │
│                    ├─► TemporaryUnlockEntity (Monotonic SystemClock.elapsedRealtime)    │
│                    ├─► ScheduleEntity (Time & Day Rules)                                │
│                    └─► DailyUsageEntity (On-device Aggregation)                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 │ Background WorkManager Sync
                                                 │ (Daily summaries & policy poll)
                                                 ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                               FIREBASE CLOUD INFRASTRUCTURE                             │
│                                                                                         │
│   ┌────────────────────────┐       ┌────────────────────────┐       ┌────────────────┐  │
│   │ Firebase Authentication│       │     Cloud Firestore    │       │ Firebase App   │  │
│   │   (Parent Identity)    │       │ (Async Policy / Stats) │       │     Check      │  │
│   └────────────────────────┘       └────────────────────────┘       └────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────────┘
                                                 ▲
                                                 │ HTTPS / NextAuth
                                                 │
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                   PARENT WEB / MOBILE UI                                │
│                                                                                         │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ Next.js 15 / TypeScript / Tailwind CSS / React Server Components                 │   │
│   │ (Parent Dashboard, Remote Policy Management, Pairing Wizard, Aggregated Graphs) │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Core Android Subsystems

### 3.1 Detection Layer
- **`DigitalDisciplineAccessibilityService`**:
  - Bound to Android Accessibility framework (`android.permission.BIND_ACCESSIBILITY_SERVICE`).
  - Event mask: `TYPE_WINDOW_STATE_CHANGED`.
  - Privacy guarantee: `canRetrieveWindowContent="false"` (never inspects text, passwords, or UI trees).
  - Average latency: **35ms – 85ms**.

### 3.2 Policy & State Engine
- **Deterministic Decision Function**:
  $$\text{Decision} = f(\text{PackageName}, \text{CurrentElapsedRealtime}, \text{ScheduleEntity}, \text{AppRuleEntity}, \text{TemporaryUnlockEntity})$$
- **State Machine**:
  1. `ALLOWED`: Package is unrestricted.
  2. `BLOCKED`: Package is restricted; active intervention overlay presented.
  3. `INTERVENTION_ACTIVE`: Child is engaging with Mindful Pause (10s), Box Breathing (16s), or Squat Challenge.
  4. `UNLOCKED_TEMPORARY`: Overlay dismissed; monotonic countdown active until `unlockExpiryElapsedRealtime`.

### 3.3 Overlay & Intervention Layer
- **`OverlayManager` with Jetpack Compose**:
  - Window type: `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
  - Flags: `FLAG_LAYOUT_IN_SCREEN`, `FLAG_NOT_TOUCH_MODAL` (unset), `FLAG_WATCH_OUTSIDE_TOUCH`.
  - UI engine: `ComposeView` with `OverlayLifecycleOwner`.
  - Features:
    - **10-Second Mindful Pause**: Animated circular countdown.
    - **Box Breathing (16s)**: Expanding/contracting breathing pacer.
    - **Physical Challenge**: 10 Squats trigger.
    - **Parent PIN Override**: Cryptographic PIN verification modal.
    - **Non-trapping Safe Exit**: Direct intent to Home Screen.

### 3.4 Local Persistence (Room & DataStore)
- **Room Database (`DigitalDisciplineDatabase`)**:
  - `app_rules`: Defines mode (`BLOCK`, `DELAY`, `EARN`, `ALLOW`), unlock duration, and limits.
  - `temporary_unlocks`: Stores `unlockExpiryElapsedRealtime` (hardware monotonic time).
  - `schedules`: Time-of-day and day-of-week active windows.
  - `daily_usage`: Aggregated on-device counters (open count, block count, unlock count).
  - `intervention_events`: Historical record of challenges started and completed.
  - `protection_state`: Health and heartbeat status of system permissions.
- **Preferences DataStore (`PreferencesManager`)**:
  - Small non-relational settings: `firstRunCompleted`, `onboardingCompleted`, `parentPinConfigured`, `deviceId`, `pairedChildId`, `protectionEnabled`.

### 3.5 Security & Anti-Tamper Layer
- **`ParentPinManager`**:
  - PBKDF2WithHmacSHA256 hashing with 12,000 iterations and 16-byte random salt.
  - Storage in `EncryptedSharedPreferences` backed by Android Keystore (AES-256 GCM).
  - Rate limiting: Maximum 5 attempts, triggering a 5-minute lockout.
- **`TamperDetector`**:
  - Continuous health check verifying Accessibility and Overlay permissions.
  - Immediate state transition to `PROTECTION_DISABLED` if permissions are revoked.
