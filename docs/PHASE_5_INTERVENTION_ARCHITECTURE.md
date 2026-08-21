# PHASE 5 — INTERVENTION ARCHITECTURE
## Unified Engine & Policy Model

---

## 1. Unified Architecture Diagram
```
                     InterventionEngine
                             │
       ┌─────────────────────┼─────────────────────┐
       ▼                     ▼                     ▼
InterventionPolicy   InterventionCatalog   InterventionSession
(Self vs Parent)      (35 Definitions)    (Deterministic SM)
                                                   │
                                                   ▼
                                         InterventionValidator
                                          ├── MovementSensor
                                          ├── Timer
                                          ├── Cognitive
                                          └── ManualConfirmation
```

---

## 2. Policy Precedence Rules
1. **Parent Hard Block** (`RuleMode.BLOCK`): Short-circuits immediately. Child cannot access or intervene.
2. **Parent Delay** (`RuleMode.DELAY`): Mandatory mindful delay before access.
3. **Self Mode Active Policy**: Resolves user-configured positive friction challenge and triggers session.
4. **Unmonitored / Allow**: Instant pass-through (<58ms).
