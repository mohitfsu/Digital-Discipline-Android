# Phase 4D-2: Architectural Decision Records (ADRs)

## ADR-1: DailyActionPlanner as Pure Deterministic Engine
- **Context**: Daily action segmentation must be fast (<5ms), offline, and never mutate state.
- **Decision**: `DailyActionPlanner` is a pure `object` with a single `planDailyActions()` function returning an immutable `DailyActionPlan`. No coroutines, no Room, no side effects.
- **Consequences**: Trivially testable, zero risk to enforcement path, fully offline.

---

## ADR-2: Explicit USE NOW / SAVE FOR LATER — No Automatic Unlock
- **Context**: Users must retain full agency over when earned screen time is spent.
- **Decision**: After completing any daily action, the wallet is credited immediately but no session is started unless the user explicitly taps `[ USE NOW ]`. Tapping `[ SAVE FOR LATER ]` returns to TodayScreen with the balance intact.
- **Consequences**: Zero automatic unlocks, eliminates accidental session starts, preserves parental trust in the system.

---

## ADR-3: No Room Schema Migration in Phase 4D-2
- **Context**: Daily action progress can be represented entirely by existing `GoalProgressEntity` (increment completedCount) and `WalletTransactionEntity` (idempotent EARN record).
- **Decision**: Room database remains at version 8. No new entity or migration introduced.
- **Consequences**: Eliminates migration risk, preserves database stability, reduces deployment surface.
