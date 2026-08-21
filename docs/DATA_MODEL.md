# Digital Discipline — Data Model & Storage Specification

**Classification**: Local & Cloud Data Schema Specification  
**Architecture**: Local-First Room Database + Cloud Firestore Sync Schema  

---

## 1. Local Room Database Schema (On Child Android Device)

### 1.1 `app_rules`
| Field | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `packageName` | `TEXT` | `PRIMARY KEY` | Android package identifier (e.g. `com.instagram.android`). |
| `appDisplayName` | `TEXT` | `NOT NULL` | Human-readable app name (e.g. `Instagram`). |
| `mode` | `TEXT` | `NOT NULL` | `BLOCK` (strict), `DELAY` (pause), `EARN` (squats), `ALLOW`. |
| `isEnabled` | `INTEGER` | `NOT NULL` | `1` = active restriction, `0` = inactive. |
| `dailyLimitMinutes` | `INTEGER` | `DEFAULT 0` | Daily screen-time cap in minutes (`0` = no cap). |
| `unlockDurationSeconds`| `INTEGER`| `DEFAULT 600`| Duration of temporary unlock after intervention (seconds). |
| `interventionType` | `TEXT` | `DEFAULT 'PAUSE'` | Default challenge (`PAUSE`, `BREATHING`, `SQUATS`). |
| `updatedAt` | `INTEGER` | `NOT NULL` | Timestamp of last rule modification. |

### 1.2 `temporary_unlocks`
| Field | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `packageName` | `TEXT` | `PRIMARY KEY` | Target package currently unlocked. |
| `unlockGrantedElapsedRealtime` | `INTEGER` | `NOT NULL` | Hardware uptime timestamp when unlock was granted. |
| `unlockExpiryElapsedRealtime` | `INTEGER` | `NOT NULL` | Hardware uptime timestamp when unlock expires. |
| `unlockDurationMs` | `INTEGER` | `NOT NULL` | Total unlocked duration in milliseconds. |
| `reason` | `TEXT` | `NOT NULL` | Reason for unlock (`PAUSE_COMPLETED`, `PARENT_OVERRIDE`). |

### 1.3 `schedules`
| Field | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique schedule identifier. |
| `packageName` | `TEXT` | `NOT NULL` | Specific package or `'ALL_RESTRICTED'`. |
| `dayOfWeek` | `INTEGER` | `NOT NULL` | `1` (Sun) through `7` (Sat). |
| `startHour` / `startMinute` | `INTEGER` | `NOT NULL` | Schedule start time (24h format). |
| `endHour` / `endMinute` | `INTEGER` | `NOT NULL` | Schedule end time (24h format). |
| `isBlocked` | `INTEGER` | `DEFAULT 1` | `1` = restrict during window, `0` = allow during window. |

### 1.4 `daily_usage`
| Field | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Row identifier. |
| `dateString` | `TEXT` | `INDEX (dateString, packageName)` | Date format (`yyyy-MM-dd`). |
| `packageName` | `TEXT` | `NOT NULL` | Package name. |
| `appDisplayName` | `TEXT` | `NOT NULL` | Display name. |
| `totalForegroundSeconds` | `INTEGER` | `DEFAULT 0` | Total seconds in foreground today. |
| `openCount` | `INTEGER` | `DEFAULT 0` | Total launch attempts today. |
| `blockCount` | `INTEGER` | `DEFAULT 0` | Total interventions displayed today. |
| `unlockCount` | `INTEGER` | `DEFAULT 0` | Total interventions successfully completed. |

### 1.5 `intervention_events`
| Field | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Event identifier. |
| `timestamp` | `INTEGER` | `NOT NULL` | Wall clock timestamp. |
| `packageName` | `TEXT` | `NOT NULL` | Package name. |
| `interventionType` | `TEXT` | `NOT NULL` | `PAUSE`, `BREATHING`, `SQUATS`, `PARENT_OVERRIDE`. |
| `status` | `TEXT` | `NOT NULL` | `STARTED`, `COMPLETED`, `ABANDONED`. |
| `durationSeconds` | `INTEGER` | `NOT NULL` | Challenge duration in seconds. |
| `latencyMs` | `INTEGER` | `DEFAULT 0` | Detection latency in milliseconds. |

---

## 2. Cloud Firestore Schema & Write Justification

### 2.1 Collection: `families/{familyId}`
- **Document Content**: `familyName`, `createdAt`, `subscriptionTier` (`FREE`, `FAMILY_PLUS`).
- **Write Frequency**: Once during onboarding.
- **Why Cloud?**: Multi-parent access control.

### 2.2 Collection: `families/{familyId}/parents/{parentId}`
- **Document Content**: `email`, `displayName`, `role` (`OWNER`, `GUARDIAN`), `createdAt`.
- **Write Frequency**: On account creation or guardian invite.
- **Why Cloud?**: Firebase Authentication integration.

### 2.3 Collection: `families/{familyId}/children/{childId}`
- **Document Content**: `name`, `age`, `avatarId`, `createdAt`.
- **Write Frequency**: On child profile creation.
- **Why Cloud?**: Parent management dashboard.

### 2.4 Collection: `families/{familyId}/devices/{deviceId}`
- **Document Content**: `childId`, `deviceModel`, `androidVersion`, `isProtectionActive`, `lastSeen`.
- **Write Frequency**: Once every 6–12 hours (heartbeat) or on permission status change.
- **Why Cloud?**: Allows parent to see if protection was tampered with or disabled.

### 2.5 Collection: `families/{familyId}/policies/{childId}`
- **Document Content**:
  - `rules`: Array of `{ packageName, appName, mode, isEnabled, dailyLimitMinutes, unlockDurationSeconds }`.
  - `schedules`: Array of `{ packageName, dayOfWeek, startHour, startMinute, endHour, endMinute, isBlocked }`.
  - `version`: Integer incremented on edit.
- **Write Frequency**: Only when parent explicitly changes a rule in the web dashboard (< 1–2 times per week).
- **Why Cloud?**: Cloud-to-device rule synchronization.

### 2.6 Collection: `families/{familyId}/daily_summaries/{childId_date}`
- **Document Content**:
  - `date`: `"2026-08-15"`
  - `totalScreenTimeMinutes`: Integer
  - `totalInterventionsCompleted`: Integer
  - `totalBlocksTriggered`: Integer
  - `topApps`: Array of `{ packageName, appName, usageMinutes, blockCount, unlockCount }`
- **Write Frequency**: **Once per day** via nightly WorkManager background sync.
- **Why Cloud?**: Parent insights and weekly trends dashboard.

---

## 3. "WHY? HOW OFTEN? CAN IT BE LOCAL?" Cloud Write Audit

| Proposed Cloud Write | Why is it needed? | How Often? | Can it be Local? | Final Architecture Decision |
| :--- | :--- | :--- | :---: | :--- |
| **Real-time App Open Event** | Track app launch | ~100–300 / day | **YES** | ❌ **FORBIDDEN IN CLOUD**. Store locally in Room `daily_usage`. |
| **Intervention Trigger Event** | Record child was blocked | ~30–80 / day | **YES** | ❌ **FORBIDDEN IN CLOUD**. Store locally in Room `intervention_events`. |
| **Temporary Unlock Event** | Record 10-min access granted | ~10–25 / day | **YES** | ❌ **FORBIDDEN IN CLOUD**. Handled entirely in Room `temporary_unlocks`. |
| **Daily Summary Rollup** | Display parent analytics graph | **1 / day / device** | No (Parent needs remote view) | ✅ **APPROVED FOR CLOUD SYNC**. Nightly aggregated write. |
| **Policy Update** | Change Instagram time limit | **< 2 / week** | No (Parent configures remotely) | ✅ **APPROVED FOR CLOUD SYNC**. Infrequent delta write. |
| **Protection Tamper Alert** | Notify parent if A11y disabled | **Event-only** (Rare) | No (Alerts parent phone) | ✅ **APPROVED FOR CLOUD SYNC**. Emergency push trigger only. |
