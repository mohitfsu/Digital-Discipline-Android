# Phase 4E-1: Architectural Decision Records (ADRs)

## ADR-1: Coordinator Pattern for Zero-Friction Onboarding
- **Context**: Onboarding involves coordinating validation, in-memory drafts, Room persistence, DataStore preferences, and wallet initialization without destabilizing existing engines.
- **Decision**: Introduce `SelfModeActivationCoordinator` as an orchestration layer using a coroutine `Mutex` for atomic single-flight execution.
- **Consequences**: Existing repositories and engines are reused cleanly without modifications to their core domain logic.

---

## ADR-2: Pre-Permission Explanatory Modals
- **Context**: Prompting users directly for Android Accessibility access causes anxiety, high drop-off, and confusion.
- **Decision**: Display an educational explanation modal before opening Android system settings, detailing exactly why access is needed, what is accessed (foreground app events only), and what is never collected (messages, keystrokes, URLs).
- **Consequences**: Increases user trust and ensures transparency while maintaining zero surveillance.

---

## ADR-3: Onboarding State in DataStore (Room v8 Unchanged)
- **Context**: Onboarding progress and recovery states must persist across process death without requiring a Room schema migration.
- **Decision**: Store onboarding lifecycle states (`SELF_ONBOARDING_STATE`, `SELF_ONBOARDING_STEP`) directly in `PreferencesManager` DataStore.
- **Consequences**: Room database remains at version 8, avoiding migration risks and deployment churn.
