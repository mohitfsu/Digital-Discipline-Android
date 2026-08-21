# Phase 4C-2: Room Database Schema & Data Models

## 1. Schema Version
- **Current Version**: `7`
- **Migration**: `MIGRATION_6_7` (non-destructive table and index creation).

---

## 2. New Database Tables

### `plan_adjustments` Table
```sql
CREATE TABLE IF NOT EXISTS plan_adjustments (
    adjustmentId TEXT PRIMARY KEY NOT NULL,
    goalId TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    reason TEXT NOT NULL,
    recommendationType TEXT NOT NULL,
    currentConfiguration TEXT NOT NULL,
    suggestedConfiguration TEXT NOT NULL,
    status TEXT NOT NULL,
    appliedAt INTEGER NOT NULL,
    rejectedAt INTEGER NOT NULL,
    cooldownSeconds INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS index_plan_adjustments_goalId ON plan_adjustments(goalId);
CREATE INDEX IF NOT EXISTS index_plan_adjustments_status ON plan_adjustments(status);
```

### `personalization_profiles` Table
```sql
CREATE TABLE IF NOT EXISTS personalization_profiles (
    profileId TEXT PRIMARY KEY NOT NULL,
    preferredIntervention TEXT NOT NULL,
    peakStartHour INTEGER NOT NULL,
    peakEndHour INTEGER NOT NULL,
    challengeCompletionRate REAL NOT NULL,
    rapidReopenRate REAL NOT NULL,
    averageSessionDurationSeconds INTEGER NOT NULL,
    rewardEffectiveness TEXT NOT NULL,
    consistencyScore REAL NOT NULL,
    currentPlanHealth TEXT NOT NULL,
    lastCalculatedAt INTEGER NOT NULL
);
```

### `weekly_reviews` Table
```sql
CREATE TABLE IF NOT EXISTS weekly_reviews (
    reviewId TEXT PRIMARY KEY NOT NULL,
    goalId TEXT NOT NULL,
    weekStart INTEGER NOT NULL,
    weekEnd INTEGER NOT NULL,
    attempts INTEGER NOT NULL,
    completed INTEGER NOT NULL,
    earnedSeconds INTEGER NOT NULL,
    consumedSeconds INTEGER NOT NULL,
    habitInterruptionRate REAL NOT NULL,
    rapidReopenRate REAL NOT NULL,
    bestIntervention TEXT NOT NULL,
    planHealth TEXT NOT NULL,
    biggestWin TEXT NOT NULL,
    suggestedNextStep TEXT NOT NULL,
    generatedAt INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS index_weekly_reviews_goalId ON weekly_reviews(goalId);
CREATE INDEX IF NOT EXISTS index_weekly_reviews_weekStart ON weekly_reviews(weekStart);
```
