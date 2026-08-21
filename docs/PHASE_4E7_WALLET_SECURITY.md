# Phase 4E-7: Wallet Security & Financial Ledger Integrity

## Wallet Invariants
- **Sole Authority**: `EarnedTimeWalletService` is the only component authorized to read/write wallet ledger entities.
- **Non-Negative Balance**: Spend requests exceeding available balance are rejected.
- **Balance Caps**: Maximum banked minutes enforced (e.g. 120m ceiling).
- **Double-Tap Protection**: In-flight state locks prevent concurrent transaction execution.
- **Ledger Reconstructability**: Wallet balance can be deterministically audited from historical transaction records.
