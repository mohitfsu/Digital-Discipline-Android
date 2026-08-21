# Phase 4E-5: Architectural Decision Records (ADRs)

## ADR-1: Single Primary Active Goal Invariant
- **Context**: Allowing multiple independent active goals simultaneously can cause conflicting triggers, multiple competing interventions, and ambiguous wallet economics.
- **Decision**: Enforce exactly ONE primary active Self Mode goal while retaining past/paused/completed goals in Goal History.
- **Consequences**: Zero friction conflicts, clean mental model for the user, and robust wallet economics.

---

## ADR-2: Atomic Goal Lifecycle Service
- **Context**: Goal transitions involve policy toggling, goal updates, and DataStore state updates.
- **Decision**: Centralize all mutations in `GoalLifecycleService` to ensure atomic state consistency.
- **Consequences**: No orphaned or desynchronized policy states.

---

## ADR-3: Read-Only Historical Goal Archive
- **Context**: Users reviewing completed or replaced goals might attempt to edit past metrics.
- **Decision**: Historical goal detail screens are strictly read-only evidence summaries.
- **Consequences**: Preserves immutable history and psychological closure.
