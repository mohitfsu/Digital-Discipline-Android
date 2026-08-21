# Phase 4C-2: Architectural Decision Records (ADRs)

## ADR 1: Non-Automatic Plan Modification Invariant
- **Context**: Users need adaptive guidance without the system modifying their boundaries behind their backs or creating unintended friction loops.
- **Decision**: Plans change **only** upon explicit user confirmation via `[ APPLY CHANGE ]`. Rejection via `[ KEEP MY PLAN ]` leaves all policies untouched.
- **Consequences**: User agency is fully respected, and the system never acts unpredictably.

## ADR 2: Complete Separation from Real-Time Enforcement Path
- **Context**: The real-time blocking path (`AccessibilityService` $\rightarrow$ `PolicyEngine` $\rightarrow$ `OverlayManager`) requires sub-millisecond execution without database contention.
- **Decision**: `AdaptivePlanEngine` is strictly an off-path analytics engine invoked only during dashboard loads or explicit review screens.
- **Consequences**: Zero impact on enforcement latency, 0ms overhead added to overlay triggering.

## ADR 3: Pure Deterministic Mathematics Over AI/LLM
- **Context**: Behavior suggestions must be 100% explainable, local-first, zero-cost, and testable without cloud latency or hallucinations.
- **Decision**: All evaluations use pure Kotlin deterministic algorithms with explicit numerical thresholds.
- **Consequences**: Zero cloud egress, $<1\text{ms}$ execution speed, and 100% reproducible outcomes.
