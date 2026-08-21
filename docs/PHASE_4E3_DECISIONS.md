# Phase 4E-3: Architectural Decision Records (ADRs)

## ADR-1: Habit Momentum vs Binary Streaks
- **Context**: Habit tracking apps frequently use streak counters with harsh resets, inducing anxiety and app abandonment when a day is missed.
- **Decision**: Implement a bounded 0–100 Habit Momentum score with gentle missed-day attenuation (-4 pts) and explicit recovery bonuses (+8 pts).
- **Consequences**: Fosters resilient, sustainable habit formation without punitive pressure.

---

## ADR-2: Derived 7-Day Window (Room v8 Preserved)
- **Context**: Displaying rolling 7-day calendars could invite creating a new database table.
- **Decision**: Dynamically derive the 7-day window from existing `InterventionEventEntity`, `GoalProgressEntity`, and `WalletTransactionEntity` tables.
- **Consequences**: Room database remains at v8 without migration risks, and calculations execute in `<1ms`.

---

## ADR-3: Dedicated 7-Day Habit Formation Screen
- **Context**: Users need a clear visual view of their 7-day formation cycle without cluttering the primary daily action hierarchy on `TodayScreen`.
- **Decision**: Keep `TodayScreen` compact with a small Habit Momentum card and provide full retrospectives in `HabitMomentumScreen`.
- **Consequences**: Clean visual hierarchy and fast daily task execution.
