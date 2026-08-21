# Phase 4B-3 — Test Plan: Behaviour Loop & Experience

This document details the 29 verification and regression test scenarios implemented in `SelfModeBehaviourLoopTest.kt`.

---

### Test Scenarios
1. **Goal Progress Calculation**: Verify progress percentage math against daily target.
2. **Daily Progress**: Verify incrementing daily progress on challenge completion.
3. **Weekly Progress Rollup**: Verify 7-day consistency calculation ("6 of 7 days").
4. **Dashboard Wallet Balance**: Verify accurate representation of available minutes.
5. **Wallet Transaction Display**: Verify ledger records `EARN` and `SPEND` types.
6. **Intervention Completion $\rightarrow$ Earn**: Verify completing challenge credits wallet.
7. **Intervention Abandonment**: Verify exiting challenge without completion credits 0.
8. **Reflection Optionality**: Verify reflection models and display strings.
9. **HIR Display**: Verify mathematical correctness of Habit Interruption Rate.
10. **Best Intervention Calculation**: Verify highest HIR intervention identified ($\ge 10$ trials).
11. **Behaviour Pattern Threshold**: Verify pattern suppressed when $<10$ events, shown when $\ge 10$.
12. **Weekly Improvement (Rule B)**: Verify +10% HIR growth triggers improvement message.
13. **Weekly Decline (Rule C)**: Verify -10% HIR drop triggers harder-to-interrupt message.
14. **Insufficient Data (Rule E)**: Verify $<10$ events returns neutral "Keep going" message.
15. **Parent BLOCK Precedence**: Verify Parent BLOCK overrides Self Mode wallet.
16. **Parent DELAY Precedence**: Verify Parent DELAY overrides Self Mode wallet.
17. **Parent ALLOW Regression**: Verify Parent ALLOW remains unblocked.
18. **Wallet Cap Respected**: Verify balance capped at 3600s max.
19. **Session Cap Respected**: Verify single session capped at 1800s max.
20. **Idempotency Protection**: Verify duplicate transaction keys ignored.
21. **Process Death Recovery**: Verify session consumption finalized on restart.
22. **Reboot Recovery**: Verify active session safely invalidated after reboot.
23. **Offline Performance**: Verify operations execute locally in $<5\text{ms}$.
24. **Parent Mode Policy Regression**: Verify Parent Mode resolution unchanged.
25. **Self Mode Policy Resolution**: Verify Self Mode policy match resolution.
26. **ALLOW Mode Regression**: Verify ALLOW rules remain unaffected.
27. **BLOCK Mode Regression**: Verify BLOCK rules remain enforced.
28. **DELAY Mode Regression**: Verify DELAY rules remain enforced.
29. **EARN Mode Regression**: Verify EARN rules remain enforced.
