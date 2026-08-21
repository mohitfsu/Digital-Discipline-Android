# Phase 4B-2 — Wallet Security & Anti-Circumvention

## 1. Attack Vectors & Mitigations

| Attack Vector | Vulnerability Description | Mitigation Strategy |
| :--- | :--- | :--- |
| **Wall-Clock Forward/Backward Shift** | User alters system date/time to skip duration or restore balance | All session duration is evaluated using hardware monotonic `SystemClock.elapsedRealtime()`. |
| **Double-Tap / Replay Exploit** | Rapid repeated clicks on challenge completion | `idempotencyKey` check in `EarnedTimeWalletService` ensures each completion is credited exactly once. |
| **Process Kill during Active Session** | Force-stopping the app to prevent time deduction | `recoverAfterCrashOrReboot()` finalizes elapsed time up to last heartbeat upon service restart. |
| **Device Reboot Attack** | Rebooting to reset timer or double-dip | Detected via `nowElapsed < startedElapsedRealtime` $\rightarrow$ automatically invalidates session. |
| **Parent Policy Override Attempt** | Child attempts to use Self Mode wallet to bypass parent block | `PolicyEngine` checks Parent Mode rules first; Parent `BLOCK`/`DELAY` unconditionally overrides wallet balance. |
