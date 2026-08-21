# Phase 4E-2: First-Win Deterministic State Machine

## State Machine Graph

```
[ NOT_STARTED ]
       ↓ (Plan Activation)
[ PLAN_ACTIVE ]
       ↓ (First Trigger Encounter)
[ FIRST_TRIGGER_SEEN ]
       ↓ (Intentional Pause)
[ REFLECTION_COMPLETED ]
       ↓ (Start Replacement Friction)
[ INTERVENTION_STARTED ]
       ↓ (Challenge Finished)
[ INTERVENTION_COMPLETED ]
       ↓ (Deposit to Wallet)
[ TIME_EARNED ]
      /           \
 (USE TIME)    (SAVE TIME)
    ↓               ↓
[ TIME_USED ]   [ TIME_SAVED ]
      \           /
       \         /
  [ FIRST_WIN_COMPLETED ]
```

## State Invariants
- **Plan Scoping**: State is associated with `planId` to ensure new plans can form their own first wins.
- **Idempotency**: Mutex-protected transitions prevent double-triggering or double-earning.
- **Process Recovery**: All states are persisted atomically to DataStore.
