# Phase 4E-6: Behaviour Journey Engine Architecture

## Synthesis Pipeline
1. Ingests local Room entities (`GoalEntity`, `InterventionEventEntity`, `PlanAdjustmentEntity`, `WeeklyReviewEntity`) and DataStore state.
2. Generates timeline events.
3. Deduplicates events by `eventId`.
4. Sorts chronologically (newest first).
5. Derives 1–3 evidence-backed personal pattern insights.
6. Evaluates long-term summary metrics in `<10ms`.
