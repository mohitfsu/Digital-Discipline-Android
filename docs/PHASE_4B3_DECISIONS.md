# Phase 4B-3 — Architectural Decisions & Trade-Offs

## ADR 1: Goal-First Dashboard Hierarchy vs App-First Monitoring
- **Context**: App-first dashboards reinforce compulsive focus on forbidden apps.
- **Decision**: Put the user's primary positive goal (e.g. Get Fit, Study) and progress bar at the very top of the dashboard.
- **Rationale**: Keeps the user's attention anchored on their aspirational identity and habits rather than distraction apps.

---

## ADR 2: Deterministic Rule-Based Feedback vs LLM/AI Inference
- **Context**: Generating user feedback for habit interruption.
- **Decision**: Implement deterministic Rules A–E in `BehaviourInsightsEngine.kt`.
- **Rationale**: 100% predictable, testable, fast ($<1\text{ms}$), zero battery drain, zero privacy risk, and zero cloud API failure modes.

---

## ADR 3: Optional Ephemeral Reflection vs Mandatory Journaling
- **Context**: Helping users recognize trigger states (boredom, avoidance, habit).
- **Decision**: Provide optional 1-tap reflection chips without blocking the intervention or requiring text input.
- **Rationale**: Zero cognitive burden, zero friction fatigue, and 100% privacy preservation.
