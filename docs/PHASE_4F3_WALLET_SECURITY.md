# Phase 4F-3: Wallet Security & Financial Ledger Integrity

## Financial Ledger Controls
- `EarnedTimeWalletService` is the single mutation authority.
- Transaction idempotency prevents replay attacks and duplicate rewards.
- Balance boundaries (0 to 120 minutes) strictly enforced.
- Concurrency serialized with Mutex lock to prevent double-spending.
- Monotonic elapsed time ensures tamper-proof session expiration.
