# Phase 4E-3: Data Model & Room v8 Preservation

## Zero Schema Churn
- Room database remains strictly at **v8**.
- All 7-day rolling window calculations, recovery events, momentum scores, and milestone snapshots are computed on-demand via `HabitMomentumEngine`.
- Eliminates migration overhead and data corruption risks.
