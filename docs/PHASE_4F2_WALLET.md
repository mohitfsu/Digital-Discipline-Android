# Phase 4F-2: Cross-Device Wallet Lifecycle & Ledger Integrity

## Financial Invariants Across Devices
- Balance is non-negative and capped at 120 minutes.
- `EarnedTimeWalletService` is the single writer across all API levels and device form-factors.
- Re-entrant clicks on spend are debounced via Mutex.
