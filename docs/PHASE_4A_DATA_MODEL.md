# Phase 4A — Behaviour & Goal Engine Data Model Specification

## 1. Local Database Schema (Room v5)

### A. `goals` Table
Represents high-level behavioral goals.

```sql
CREATE TABLE IF NOT EXISTS goals (
    goalId TEXT PRIMARY KEY NOT NULL,
    ownerId TEXT NOT NULL,
    mode TEXT NOT NULL,                  -- 'PARENT' or 'SELF'
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL,              -- FITNESS, STUDY, PRODUCTIVITY, SLEEP, MINDFULNESS, READING, HEALTH, FINANCE, CUSTOM
    active INTEGER NOT NULL,             -- 1 = true, 0 = false
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    startDate INTEGER NOT NULL,
    targetDate INTEGER,
    dailyTarget INTEGER NOT NULL,
    weeklyTarget INTEGER NOT NULL,
    progress INTEGER NOT NULL,
    unit TEXT NOT NULL,                  -- 'reps', 'minutes', 'pages', 'sessions'
    priority INTEGER NOT NULL
);
```

### B. `triggers` Table
Represents distracting trigger applications and schedule windows.

```sql
CREATE TABLE IF NOT EXISTS triggers (
    triggerId TEXT PRIMARY KEY NOT NULL,
    ownerId TEXT NOT NULL,
    goalId TEXT NOT NULL,
    packageName TEXT NOT NULL,
    appDisplayName TEXT NOT NULL,
    category TEXT NOT NULL,              -- SOCIAL_MEDIA, VIDEO_STREAMING, GAMING, SHOPPING, FOOD_DELIVERY, CUSTOM
    active INTEGER NOT NULL,
    startHour INTEGER NOT NULL,          -- 0..23
    startMinute INTEGER NOT NULL,        -- 0..59
    endHour INTEGER NOT NULL,            -- 0..23
    endMinute INTEGER NOT NULL,          -- 0..59
    daysOfWeek TEXT NOT NULL,            -- Comma-separated: '1,2,3,4,5,6,7'
    priority INTEGER NOT NULL,
    createdAt INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS index_triggers_packageName ON triggers (packageName);
CREATE INDEX IF NOT EXISTS index_triggers_goalId ON triggers (goalId);
```

### C. `replacement_behaviours` Table
Represents reusable positive friction alternative activities.

```sql
CREATE TABLE IF NOT EXISTS replacement_behaviours (
    behaviourId TEXT PRIMARY KEY NOT NULL,
    category TEXT NOT NULL,              -- PHYSICAL, MINDFUL, STUDY, HEALTH, PRODUCTIVITY, CUSTOM
    type TEXT NOT NULL,                  -- SQUATS, PUSHUPS, BOX_BREATHING, MINDFUL_PAUSE, STUDY_TIMER, READ_PAGES, DRINK_WATER, etc.
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    targetCount INTEGER NOT NULL,
    durationSeconds INTEGER NOT NULL,
    unit TEXT NOT NULL,
    configJson TEXT NOT NULL
);
```

### D. `behaviour_policies` Table
Connects Goal + Trigger + Replacement Behaviour + Reward.

```sql
CREATE TABLE IF NOT EXISTS behaviour_policies (
    policyId TEXT PRIMARY KEY NOT NULL,
    ownerId TEXT NOT NULL,
    goalId TEXT NOT NULL,
    triggerId TEXT NOT NULL,
    replacementBehaviourId TEXT NOT NULL,
    interventionMode TEXT NOT NULL,       -- EARN, DELAY, BLOCK, ALLOW
    rewardType TEXT NOT NULL,             -- EARNED_SCREEN_TIME, NO_REWARD, COMPLETION_ONLY
    earnedSeconds INTEGER NOT NULL,       -- e.g. 600 seconds (10 min)
    maximumDailySeconds INTEGER NOT NULL,
    maximumSessionSeconds INTEGER NOT NULL,
    enabled INTEGER NOT NULL,
    priority INTEGER NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS index_behaviour_policies_goalId ON behaviour_policies (goalId);
CREATE INDEX IF NOT EXISTS index_behaviour_policies_triggerId ON behaviour_policies (triggerId);
CREATE INDEX IF NOT EXISTS index_behaviour_policies_replacementBehaviourId ON behaviour_policies (replacementBehaviourId);
```

### E. `goal_progress` Table
Stores daily progress counts and completion percentages.

```sql
CREATE TABLE IF NOT EXISTS goal_progress (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    goalId TEXT NOT NULL,
    dateString TEXT NOT NULL,             -- 'yyyy-MM-dd'
    completedCount INTEGER NOT NULL,
    targetCount INTEGER NOT NULL,
    completedDurationSeconds INTEGER NOT NULL,
    targetDurationSeconds INTEGER NOT NULL,
    completionPercentage REAL NOT NULL,
    lastUpdated INTEGER NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS index_goal_progress_goalId_dateString ON goal_progress (goalId, dateString);
```
