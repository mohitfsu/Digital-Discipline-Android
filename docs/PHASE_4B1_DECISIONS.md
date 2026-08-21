# Phase 4B-1 — Architectural Decisions & Trade-Offs

## ADR 1: Dedicated Self Mode Dashboard vs Overloading Parent Dashboard
- **Context**: Parent Dashboard is centered around oversight, PIN controls, child profiles, and device pairing.
- **Decision**: Create a distinct, minimal `SelfDashboardScreen` for Self Mode focusing on individual goals, progress bars, monitored apps, and quick habit adjustments.
- **Rationale**: An individual managing their own habits has completely different psychological needs than a parent monitoring a child. The UI must feel empowering, lightweight, and personal.

---

## ADR 2: In-Place Editing Without Full Onboarding Re-run
- **Context**: Once an individual completes Self Mode onboarding, they should be able to tune their goal, add/remove distraction apps, or change the replacement exercise quickly.
- **Decision**: Provide direct `[Edit Goal]`, `[Edit Distractions]`, and `[Edit Intervention]` dialogs on the Self Dashboard that write directly to local Room v5 database tables.
- **Rationale**: Reduces friction and allows users to adapt their habits on the fly without going through the 4-step wizard again.

---

## ADR 3: Strict Parent Mode Absolute Precedence
- **Context**: Ensuring a child cannot bypass a parent block by setting a Self Mode policy.
- **Decision**: The `BehaviourPolicyResolver` evaluates Parent Mode rules first. If an active Parent rule exists for an app, it unconditionally overrides any Self Mode policy.
- **Rationale**: Non-negotiable safety invariant that preserves parental trust.
