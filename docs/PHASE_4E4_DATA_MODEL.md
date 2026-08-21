# Phase 4E-4: Data Model & Zero Room Migration

## Room Database Version
- Preserved strictly at **Room v8**.
- Plan continuity state and active week tracking are persisted safely in DataStore.
- Adjustments reuse existing `plan_adjustments` and `weekly_reviews` Room tables without schema migrations.
