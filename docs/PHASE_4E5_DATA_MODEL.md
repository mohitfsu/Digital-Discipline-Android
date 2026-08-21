# Phase 4E-5: Data Model & Zero Room Migration

## Room Database Integrity
- Room database preserved strictly at **v8**.
- `GoalEntity` in Room v8 stores all goals with `active` flags, `startDate`, and `updatedAt`.
- DataStore stores active lifecycle state (`KEY_PRIMARY_GOAL_LIFECYCLE_STATE`, `KEY_PRIMARY_GOAL_PAUSED_AT`, `KEY_PRIMARY_GOAL_COMPLETED_AT`).
