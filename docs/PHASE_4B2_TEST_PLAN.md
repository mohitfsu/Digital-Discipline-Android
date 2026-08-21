# Phase 4B-2 — Wallet & Session Economics Test Plan

This document outlines the 30 verification test scenarios implemented in `WalletEngineTest.kt`.

---

### Test Scenarios
1. **Initial balance = 0**: Verify fresh wallet initialized with 0 seconds.
2. **Earn 10 minutes**: Verify earning +600s updates available balance and ledger.
3. **Earn multiple rewards**: Verify successive earning accumulates in daily and available balances.
4. **Daily earning cap**: Verify earning cannot exceed configured daily cap ($3600\text{s}$).
5. **Max wallet balance cap**: Verify available balance cannot exceed max cap ($3600\text{s}$).
6. **Max session duration**: Verify session max allowed duration is clamped to $1800\text{s}$.
7. **Spend wallet time**: Verify session heartbeat decreases available balance monotonically.
8. **Wallet reaches zero**: Verify session expires when available balance reaches 0.
9. **Session expiration**: Verify expired status emitted to PolicyEngine.
10. **Backgrounding pauses consumption**: Verify leaving target app halts deduction.
11. **Reopening resumes consumption**: Verify returning to target app starts new session with remaining balance.
12. **Idempotency protection**: Verify identical `idempotencyKey` cannot earn duplicate time.
13. **Duplicate spend prevention**: Verify inactive sessions cannot be spent twice.
14. **Rapid double completion**: Verify fast consecutive completions are ignored.
15. **Process death during session**: Verify recovery finalizes elapsed time up to last heartbeat.
16. **Process death after earning**: Verify balance persists in Room SQLite.
17. **Reboot during session**: Verify active sessions are invalidated upon elapsedRealtime reset.
18. **Wall-clock forward attack**: Verify changing clock forward has zero impact on monotonic duration.
19. **Wall-clock backward attack**: Verify changing clock backward has zero impact on monotonic duration.
20. **Timezone change**: Verify changing timezone does not alter wallet balance.
21. **Parent BLOCK precedence**: Verify Parent BLOCK unconditionally wins over wallet balance.
22. **Parent DELAY precedence**: Verify Parent DELAY unconditionally wins over wallet balance.
23. **Parent ALLOW unaffected**: Verify unrestricted apps remain accessible.
24. **Multiple target apps share wallet**: Verify Instagram and YouTube share the single wallet pool.
25. **Offline performance**: Verify all operations execute locally in $<5\text{ms}$.
26. **Transaction consistency**: Verify Mutex synchronization across concurrent operations.
27. **Concurrent earn serialization**: Verify thread safety under rapid parallel calls.
28. **Concurrent spend serialization**: Verify thread safety under rapid parallel updates.
29. **Ledger balance reconstruction**: Verify mathematical sum of transactions matches wallet balance.
30. **Stale session recovery**: Verify corrupted or stale session entries are cleared cleanly.
