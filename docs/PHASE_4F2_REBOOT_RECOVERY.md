# Phase 4F-2: Reboot Recovery & Monotonic Security Invariants

## Fail-Closed Reboot Architecture
- **Active Sessions**: Any in-progress wallet unlock automatically terminates upon reboot due to `SystemClock.elapsedRealtime()` uptime reset.
- **Parent Rules**: Parent BLOCK and DELAY policies persist in Room and immediately enforce upon reboot.
- **Ledger Invariance**: Wallet balance matches cumulative ledger transactions precisely.
