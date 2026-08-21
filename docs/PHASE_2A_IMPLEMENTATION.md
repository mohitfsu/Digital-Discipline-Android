# Digital Discipline — Phase 2A Implementation Document

**Classification**: Phase 2A — Tiniest Real Android MVP Implementation  
**Status**: Completed & Verified  
**Artifact**: [`app-debug.apk`](file:///d:/Zidd/app/build/outputs/apk/debug/app-debug.apk)  

---

## 1. Overview & Architecture

Phase 2A delivers the smallest genuinely usable, local-first parental control MVP designed to run on a physical Android phone.

### Core Architectural Properties:
- **100% Offline Autonomy**: Zero dependencies on Firebase, Internet access, Cloud Functions, or remote servers.
- **~58ms Push Detection**: `DigitalDisciplineAccessibilityService` observes `TYPE_WINDOW_STATE_CHANGED` events with `canRetrieveWindowContent="false"`.
- **Full Touch Interception**: Jetpack Compose `ComposeView` on `WindowManager` (`TYPE_APPLICATION_OVERLAY`) intercepts 100% of touches before they reach the restricted application.
- **Monotonic Hardware Countdown**: `SystemClock.elapsedRealtime()` eliminates clock manipulation exploits.
- **Room SQLite Source of Truth**: Dynamic `AppRuleEntity` policies survive process death and device reboots.
- **PBKDF2 Hardware-Backed PIN**: 12,000 iterations of PBKDF2WithHmacSHA256 with 16-byte random salt stored in Android Keystore `EncryptedSharedPreferences`.

---

## 2. Modes & Default MVP Policies

Each target application is configured with one of four modes:
- **`EARN`** (*Default for MVP*): Child must complete an escalated physical or mindful challenge to earn a temporary access window.
- **`BLOCK`**: Strict restriction; intervention screen presented upon every launch.
- **`DELAY`**: Mindful pause mandatory before access.
- **`ALLOW`**: Unrestricted access.

### Default Policy Matrix (Pre-Populated into Room DB)
| Target Application | Package Name | Mode | Default Earned Access | Default Initial Challenge |
| :--- | :--- | :---: | :---: | :--- |
| **Instagram** | `com.instagram.android` | `EARN` | **10 minutes** (600s) | Mindful Pause (10s) |
| **YouTube** | `com.google.android.youtube` | `EARN` | **15 minutes** (900s) | Box Breathing (30s) |
| **Gaming App** (Free Fire) | `com.dts.freefireth` | `EARN` | **15 minutes** (900s) | Squat Challenge (10 squats) |

---

## 3. Escalated Intervention Sequence

Digital Discipline introduces a progressive behavioral friction model:

```
[Child Taps Target App]
        │
        ├─► Attempt 1 ──► ⏳ Mindful Pause (10 Seconds Countdown)
        │
        ├─► Attempt 2 ──► 🫁 Box Breathing (30 Seconds: Inhale/Hold/Exhale/Hold)
        │
        ├─► Attempt 3+ ─► 🏋️ Physical Challenge (10 Squats + "I Completed It" Button)
        │
        └─► Any Time ───► 🔑 Parent Override (Enter 4-Digit Parent PIN)
```

1. **Mindful Pause (10s)**: High-resolution countdown ring prompting self-reflection.
2. **Box Breathing (30s)**: Expanding/contracting animated guide with four 4s phases (Inhale, Hold, Exhale, Hold) across 2 full cycles.
3. **Squat Challenge**: Physical movement challenge requiring intentional tap of `[ ✓ I COMPLETED IT ]` after completing 10 squats.
4. **Parent Override**: Direct unlock via PBKDF2-verified parent PIN.
5. **Non-Trapping Safe Exit**: Dedicated `[ 🏠 EXIT TO HOME SCREEN ]` button to return to launcher safely.

---

## 4. First-Launch Parent Onboarding Flow

1. **Introduction**: Explanation of mindful screen-time habits.
2. **Prominent Disclosure & Consent**: Google Play compliant disclosure of Accessibility API usage and explicit privacy guarantees (no chat reading, no screen recording, no keystrokes, no camera/mic).
3. **Permission Activation Wizard**: Direct intents to enable Accessibility, Draw Over Apps, and Usage Access.
4. **Target Apps Confirmation**: Review of pre-configured targets.
5. **Parent PIN Creation**: Setup of 4-digit master PIN with confirmation.
6. **Parent Dashboard**: Main local management portal.

---

## 5. Tamper Handling & Health State

- **Safe Detection Only**: If Accessibility is turned off in Android Settings, `TamperDetector` transitions `ProtectionState` to `PROTECTION DISABLED`.
- **Transparent Dashboard Alert**: The parent dashboard displays an immediate red alert card with a direct button to re-enable Accessibility.
- **No Malicious OS Modification**: No automated clicking in Android Settings, no deceptive lockouts, no uninstaller blocking.
