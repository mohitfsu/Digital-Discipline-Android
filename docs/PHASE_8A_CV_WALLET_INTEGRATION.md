# Phase 8A-CV: Single Wallet Authority & Idempotency Audit

## Wallet Invariant
`EarnedTimeWalletService` is the **SOLE authority** for modifying user screen time balances and writing ledger records.

```
CameraPoseWorkoutScreen (UI)
             ↓ emits
PoseClassificationResult
             ↓ processes
CameraPoseValidator
             ↓ emits
ValidationResult.Completed(session)
             ↓ authoritative callback
InterventionEngine.onSessionCompleted(session)
             ↓
walletService.earnTime(
    amountSeconds = session.rewardSeconds,
    source = "INTERVENTION_${session.intervention.id}",
    triggerPackage = session.targetPackage,
    idempotencyKey = "earn_intervention_${session.sessionId}"
)
```

## Idempotency Protection
- All intervention completions use the cryptographic/unique session key: `earn_intervention_${session.sessionId}`.
- If a UI event or validator triggers completion multiple times inside the same session, `EarnedTimeWalletService` detects the identical `idempotencyKey` in Room Database and rejects duplicate transactions with zero double-crediting.
