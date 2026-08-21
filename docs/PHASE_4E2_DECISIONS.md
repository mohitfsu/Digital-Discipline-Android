# Phase 4E-2: Architectural Decision Records (ADRs)

## ADR-1: Deterministic 10-State First-Win Lifecycle
- **Context**: Guiding a user to their first behavioral milestone requires tracking distinct micro-steps (trigger seen, reflection paused, challenge started, time earned, choice to use or save) without subjective scoring or AI.
- **Decision**: Define an explicit 10-state state machine in `FirstWinStateManager`.
- **Consequences**: State transitions are deterministic, testable, and 100% offline.

---

## ADR-2: Encrypted DataStore for First-Win Persistence (Room v8 Preserved)
- **Context**: First-Win state, timestamps, and metrics need persistent storage across process death and device reboots.
- **Decision**: Store First-Win keys in `PreferencesManager` DataStore rather than adding a Room entity.
- **Consequences**: Room database remains at version 8, eliminating migration failure risks and schema churn.

---

## ADR-3: Dual Choice Model: USE NOW vs SAVE FOR LATER
- **Context**: After completing a replacement challenge, some users want immediate screen time while others want to bank their earned minutes and return to focus.
- **Decision**: Provide two clear, equal-weight choices: USE NOW (starts a wallet session) vs SAVE FOR LATER (deposits time and safely returns to Android home).
- **Consequences**: Respects user intent and avoids forcing screen time consumption.
