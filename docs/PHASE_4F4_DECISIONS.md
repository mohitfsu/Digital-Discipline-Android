# Phase 4F-4: Architectural Decision Records (ADRs)

## ADR-1: Production Release Candidate Packaging
- **Context**: Finalize release candidate build architecture and R8 ProGuard keep rules for production distribution.
- **Decision**: Package release candidate with explicit R8 keep rules for Room and Compose, keeping Room database strictly at Version 8 and retaining local-first privacy.
- **Consequences**: Deterministic, reproducible release candidate ready for deployment.
