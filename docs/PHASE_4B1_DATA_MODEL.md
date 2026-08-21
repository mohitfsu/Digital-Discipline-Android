# Phase 4B-1 — Self Mode Data Model & Persistence Specification

## 1. Schema Reuse (Room v5)
Phase 4B-1 directly utilizes the Room v5 tables created in Phase 4A without requiring destructive migrations:

### A. `goals` Table
- `goalId`: UUID primary key (e.g. `goal_self_...`)
- `ownerId`: `"self"`
- `mode`: `"SELF"`
- `title`: User goal name (e.g. `"Fitness"`, `"Study"`)
- `category`: `GoalCategory` (`FITNESS`, `STUDY`, `PRODUCTIVITY`, `MINDFULNESS`, `READING`, `SLEEP`, `CUSTOM`)
- `dailyTarget`: Integer target count (e.g. 2 sessions)
- `unit`: String unit (e.g. `"sessions"`, `"blocks"`, `"tasks"`)
- `active`: 1 (true)

### B. `triggers` Table
- `triggerId`: UUID primary key
- `ownerId`: `"self"`
- `goalId`: Reference to parent `goalId`
- `packageName`: Target app identifier (e.g. `com.instagram.android`)
- `appDisplayName`: User-friendly app name
- `category`: `TriggerCategory` (`SOCIAL_MEDIA`, `VIDEO_STREAMING`, `GAMING`, `CUSTOM`)
- `startHour` / `startMinute` / `endHour` / `endMinute`: 0:00 to 23:59 (all-day default)
- `daysOfWeek`: `"1,2,3,4,5,6,7"` (all days default)

### C. `replacement_behaviours` Table
- `behaviourId`: Identifier (e.g. `beh_squats_10`, `beh_pause_10s`, `beh_breathing_30s`, `beh_study_timer_25m`)
- `category`: `PHYSICAL`, `MINDFUL`, `STUDY`, `CUSTOM`
- `type`: `SQUATS`, `MINDFUL_PAUSE`, `BOX_BREATHING`, `STUDY_TIMER`, `CUSTOM`
- `targetCount`: Quantity (e.g. 10 reps)
- `durationSeconds`: Seconds (e.g. 60s)

### D. `behaviour_policies` Table
- `policyId`: UUID primary key
- `goalId`: Reference to `GoalEntity`
- `triggerId`: Reference to `TriggerEntity`
- `replacementBehaviourId`: Reference to `ReplacementBehaviourEntity`
- `interventionMode`: `EARN` or `BLOCK`
- `rewardType`: `EARNED_SCREEN_TIME` or `NO_REWARD`
- `earnedSeconds`: 600 (10 minutes)
- `enabled`: 1 (true)

---

## 2. Preference Keys (`DataStore`)
- `KEY_USER_MODE`: Stores active mode (`"SELF"` vs `"PARENT"`).
- `KEY_ONBOARDING_COMPLETED`: Flag indicating completion of onboarding.
