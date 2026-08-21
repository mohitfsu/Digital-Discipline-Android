# Phase 4C-3: Architectural Decision Records (ADRs)

## ADR-1: Separation of Real-Time Enforcement Path from Behaviour Intelligence
- **Context**: Real-time blocking requires $<1\text{ms}$ resolution latency and cannot tolerate aggregation queries.
- **Decision**: Keep the enforcement loop (`AccessibilityService` $\rightarrow$ `PolicyEngine` $\rightarrow$ `OverlayManager`) strictly isolated from all intelligence calculations.
- **Consequences**: Zero latency regression on app interception; intelligence calculations execute asynchronously.

---

## ADR-2: Explicit User Decision for Behaviour Experiments
- **Context**: Behavioural insights should empower user agency without unexpected policy shifts.
- **Decision**: All experiments are created in `DRAFT` state and only activated when the user explicitly taps `[ START EXPERIMENT ]`. Active experiments can be cancelled at any time.
- **Consequences**: Guarantees predictability and prevents user frustration.

---

## ADR-3: Supportive Coaching Language & Non-Causal Framing
- **Context**: Traditional screen-time applications use shaming, punitive terminology.
- **Decision**: Implement supportive, calm, and non-judgmental language across all dashboards and engine outputs. State correlations as associations rather than definitive causes.
- **Consequences**: Fosters positive intrinsic motivation and self-efficacy.
