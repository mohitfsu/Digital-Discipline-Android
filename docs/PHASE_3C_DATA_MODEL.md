# Phase 3C — Behaviour Analytics & Data Model Specification

## 1. Local Database Schema (Room Database v4)

### A. `intervention_events` Table
Tracks granular behavioral interaction points for habit analysis.

```sql
CREATE TABLE IF NOT EXISTS intervention_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    eventId TEXT NOT NULL,
    deviceId TEXT NOT NULL,
    childId TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    packageName TEXT NOT NULL,
    appDisplayName TEXT NOT NULL,
    interventionType TEXT NOT NULL,       -- PAUSE, BREATHING, SQUATS, PARENT_OVERRIDE
    status TEXT NOT NULL,                 -- STARTED, COMPLETED, ABANDONED, EXITED
    outcome TEXT NOT NULL,                -- COMPLETED, ABANDONED, EXITED, PARENT_OVERRIDE, EARNED_ACCESS, AUTO_EXPIRED, RAPID_REOPEN
    durationSeconds INTEGER NOT NULL,
    earnedSeconds INTEGER NOT NULL,
    unlockStartedAt INTEGER NOT NULL,
    unlockExpiredAt INTEGER NOT NULL,
    reopenWithin1Minute INTEGER NOT NULL,  -- 0 (false) or 1 (true)
    reopenWithin5Minutes INTEGER NOT NULL, -- 0 (false) or 1 (true)
    reopenWithin15Minutes INTEGER NOT NULL,-- 0 (false) or 1 (true)
    hourOfDay INTEGER NOT NULL,           -- 0..23
    dayOfWeek INTEGER NOT NULL,           -- 1..7 (Calendar.DAY_OF_WEEK)
    latencyMs INTEGER NOT NULL
);
```

### B. `daily_usage` Table
Stores daily aggregated rollups and precomputed Habit Interruption Rates per app.

```sql
CREATE TABLE IF NOT EXISTS daily_usage (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    dateString TEXT NOT NULL,
    packageName TEXT NOT NULL,
    appDisplayName TEXT NOT NULL,
    totalForegroundSeconds INTEGER NOT NULL,
    openCount INTEGER NOT NULL,
    blockCount INTEGER NOT NULL,
    unlockCount INTEGER NOT NULL,
    attempts INTEGER NOT NULL,
    completed INTEGER NOT NULL,
    abandoned INTEGER NOT NULL,
    earnedAccess INTEGER NOT NULL,
    parentOverrides INTEGER NOT NULL,
    rapidReopens INTEGER NOT NULL,
    habitInterruptionRate REAL NOT NULL,
    earnedMinutes INTEGER NOT NULL,
    pauseCount INTEGER NOT NULL,
    breathingCount INTEGER NOT NULL,
    squatsCount INTEGER NOT NULL,
    lastUpdated INTEGER NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS index_daily_usage_dateString_packageName ON daily_usage (dateString, packageName);
```

---

## 2. Cloud Firestore Schema

### Collection: `/families/{familyId}/daily_summaries/{summaryId}`
- **Document ID Format**: `${childId}_${dateString}` (e.g. `child_alex_2026-08-16`)
- **Fields**:
  - `summaryId` (string)
  - `familyId` (string)
  - `childId` (string)
  - `deviceId` (string)
  - `dateString` (string, `yyyy-MM-dd`)
  - `totalScreenTimeMinutes` (number)
  - `totalInterventionsCompleted` (number)
  - `totalBlocksTriggered` (number)
  - `totalAttempts` (number)
  - `totalEarnedMinutes` (number)
  - `habitInterruptionRate` (number, float percentage)
  - `pauseCount` (number)
  - `breathingCount` (number)
  - `squatsCount` (number)
  - `topApps` (array of `DailyAppUsageDto`):
    - `packageName` (string)
    - `appDisplayName` (string)
    - `usageMinutes` (number)
    - `openCount` (number)
    - `blockCount` (number)
    - `unlockCount` (number)
    - `attempts` (number)
    - `earnedMinutes` (number)
    - `habitInterruptionRate` (number)
  - `uploadedAt` (server timestamp)

---

## 3. Write Budget Audit
- **Daily Writes per Device**: Exactly **1 single document write** per 24 hours.
- **Monthly Firestore Writes (1,000 devices)**: $1,000 \times 30 = 30,000\text{ writes/month}$ (Well within Firebase Spark free tier limit of $600,000\text{ writes/month}$).
