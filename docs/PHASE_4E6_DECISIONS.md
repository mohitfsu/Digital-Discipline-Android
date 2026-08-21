# Phase 4E-6: Architectural Decision Records (ADRs)

## ADR-1: Purely Derived Journey Timeline
- **Context**: Storing duplicated timeline event rows in a separate database table causes data desynchronization and unnecessary Room migrations.
- **Decision**: Synthesize the journey timeline on-the-fly directly from existing authoritative Room entities and DataStore keys.
- **Consequences**: Zero database churn, guaranteed synchronization with underlying behavioral telemetry, and sub-10ms performance.

---

## ADR-2: Non-Fabrication of Pattern Insights
- **Context**: In early-stage usage, generating artificial behavioral insights creates false confidence or confusion.
- **Decision**: Surface pattern learnings only when backed by explicit local evidence. Otherwise, display a calm "Building Initial Baseline" statement.
- **Consequences**: Trustworthy, supportive, and non-judgmental user experience.
