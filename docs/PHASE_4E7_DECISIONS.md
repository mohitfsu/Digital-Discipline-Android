# Phase 4E-7: Architectural Decision Records (ADRs)

## ADR-1: Dedicated End-to-End Hardening Harness
- **Context**: Unit tests evaluate isolated classes, but full multi-day journeys require cross-engine integration testing.
- **Decision**: Introduce `SelfModeE2EReliabilityTest` validating end-to-end state consistency across all subsystems.
- **Consequences**: Guaranteed long-term system stability for production MVP release.

---

## ADR-2: Monotonic-Only Session Enforcement
- **Context**: Operating system wall-clock can be altered by users to bypass session timeouts.
- **Decision**: Enforce all active wallet unlocks strictly using `SystemClock.elapsedRealtime()`.
- **Consequences**: Fail-closed security against time manipulation and device reboots.
