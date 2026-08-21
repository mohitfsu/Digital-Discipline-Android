# Phase 4E-3: Rolling 7-Day Habit Formation Model

## Daily Status States
1. `COMPLETED`: $\ge 1$ meaningful intervention completed or positive goal progress recorded.
2. `STRONG`: $\ge 2$ meaningful interventions or daily target reached.
3. `PARTIAL`: Distraction encounter or reflection paused without completing friction challenge.
4. `ACTIVE`: Current day awaiting activity.
5. `MISSED`: Past day with zero meaningful activity.
6. `FUTURE`: Future day in formation cycle.

## Deterministic Day Mapping
The model evaluates a rolling window of 7 days ($T-6$ to $T_0$) using local `InterventionEventEntity`, `GoalProgressEntity`, and `WalletTransactionEntity` timestamps.
