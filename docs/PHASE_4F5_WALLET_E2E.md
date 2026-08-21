# Phase 4F-5: Wallet End-to-End Security & Invariant Verification

## Financial Ledger Controls
- **Sole Authority**: `EarnedTimeWalletService` is the only writer across all UI layers and background threads.
- **Idempotency**: Unique challenge transaction IDs prevent duplicate awards.
- **Boundaries**: Non-negative balance constraint and 120-minute maximum ceiling enforced.
- **Double-Spend Protection**: Mutex lock serializes rapid repeated taps on unlock CTAs.
- **Tamper Immunity**: Monotonic clock protects unlock sessions against wall-clock changes.
