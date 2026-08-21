# Phase 4C-3: Data Model & Database Migration

## 1. Room Database Schema Version 8
Room Database version was upgraded from **v7 to v8** with `MIGRATION_7_8`.

---

## 2. Table Definition: `behaviour_experiments`

```sql
CREATE TABLE IF NOT EXISTS behaviour_experiments (
    experimentId TEXT PRIMARY KEY NOT NULL,
    goalId TEXT NOT NULL,
    title TEXT NOT NULL,
    hypothesis TEXT NOT NULL,
    baselineStartDate INTEGER NOT NULL,
    baselineEndDate INTEGER NOT NULL,
    experimentStartDate INTEGER NOT NULL,
    experimentEndDate INTEGER NOT NULL,
    interventionConfiguration TEXT NOT NULL,
    status TEXT NOT NULL,
    baselineMetrics TEXT NOT NULL,
    experimentMetrics TEXT NOT NULL,
    conclusion TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    completedAt INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS index_behaviour_experiments_goalId ON behaviour_experiments(goalId);
CREATE INDEX IF NOT EXISTS index_behaviour_experiments_status ON behaviour_experiments(status);
CREATE INDEX IF NOT EXISTS index_behaviour_experiments_experimentStartDate ON behaviour_experiments(experimentStartDate);
```

---

## 3. Migration Safety
`MIGRATION_7_8` is completely non-destructive:
- Existing rules (`app_rules`, `schedules`, `temporary_unlocks`) remain unchanged.
- Analytics events (`daily_usage`, `intervention_events`) remain intact.
- Phase 4 entities (`goals`, `triggers`, `replacement_behaviours`, `behaviour_policies`, `goal_progress`, `earned_time_wallets`, `wallet_transactions`, `wallet_sessions`, `plan_adjustments`, `personalization_profiles`, `weekly_reviews`) are 100% preserved.
