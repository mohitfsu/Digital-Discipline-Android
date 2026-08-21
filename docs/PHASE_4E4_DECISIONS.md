# Phase 4E-4: Architectural Decision Records (ADRs)

## ADR-1: Orchestration over New Intelligence
- **Context**: Long-term plan refinement could lead to building an "AdaptivePlanEngineV2".
- **Decision**: Reuse `AdaptivePlanEngine` and `PersonalizationRepository` directly, treating Phase 4E-4 as an orchestration and UX continuity layer.
- **Consequences**: Zero duplicate recommendation logic, lower footprint, and rock-solid architectural simplicity.

---

## ADR-2: Mandatory Before vs After Change Previews
- **Context**: Automatic plan adjustments cause confusion and erode trust.
- **Decision**: Render concrete diff previews for every proposed modification (`current` vs `suggested`) requiring explicit user confirmation.
- **Consequences**: User feels fully in control of their habit routine.

---

## ADR-3: Non-Destructive Plan Continuity
- **Context**: Switching goals or starting fresh could inadvertently wipe historical logs.
- **Decision**: Ensure that goal switches and "Start Fresh" only update the active plan pointers while strictly preserving historical analytics, wallet ledgers, and past weekly reviews.
- **Consequences**: Data integrity guaranteed across all plan iterations.
