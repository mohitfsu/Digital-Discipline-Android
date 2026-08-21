# Phase 4E-2: Wallet Integration & Economic Invariants

## Sole Wallet Authority
`EarnedTimeWalletService` remains the sole authority for wallet balances, transaction ledgers, and unlock sessions.

## Choice Semantics

### USE MY TIME
1. Records `InterventionCompleted` event.
2. Deposits earned minutes to wallet using unique idempotency key.
3. Invokes `walletService.startOrResumeSession()`.
4. Monotonic elapsed-realtime session timer starts.
5. Respects maximum session cap (`maxSessionSeconds`).

### SAVE FOR LATER
1. Records `InterventionCompleted` event.
2. Deposits earned minutes to wallet using unique idempotency key.
3. Keeps earned time in wallet balance.
4. Safely returns to Android home without starting a timer session.

## Parent Mode Absolute Precedence
Parent Mode policies (`BLOCK`, `DELAY`) override any Self Mode earned time or active sessions unconditionally.
