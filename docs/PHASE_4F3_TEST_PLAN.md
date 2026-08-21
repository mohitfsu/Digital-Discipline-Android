# Phase 4F-3: Security & Privacy Test Plan

## 30-Scenario Security Test Plan (`ProductionSecurityTest.kt`)
1. Internal services and receivers are not exported
2. PendingIntents enforce FLAG_IMMUTABLE on Android 12+
3. Deep link cannot bypass onboarding or activate plans unauthorized
4. EarnedTimeWalletService remains the sole wallet authority
5. Wallet transaction idempotency prevents duplicate rewards
6. Wallet balance cannot become negative
7. Wallet balance ceiling of 120 minutes is strictly enforced
8. Concurrent double-tap spend is prevented by mutex serialisation
9. Session expiration relies on monotonic elapsedRealtime not wall clock
10. Wall clock tampering forward or backward cannot extend active session
11. Device reboot immediately terminates active sessions fail-closed
12. Parent Mode BLOCK rule strictly overrides Self Mode wallet unlock
13. Parent Mode DELAY rule strictly overrides Self Mode allow
14. Zero keystroke collection guaranteed
15. Zero screenshot or screen recording capture guaranteed
16. Zero microphone or audio capture guaranteed
17. Zero camera or visual capture guaranteed
18. Zero URL or browser history tracking guaranteed
19. AccessibilityService content retrieval is disabled
20. Zero remote network calls required for core Self Mode operation
21. Room database schema strictly preserved at Version 8
22. Historical archived goals remain immutable
23. One-primary-active-goal invariant enforced
24. Plan modification requires explicit user confirmation
25. Security policy evaluation latency under 1ms
26. Notification CTA cannot execute privileged action directly
27. Overlay lifecycle cleans up cleanly without lingering window leak
28. Local user data reset completely clears sensitive state
29. Release build logging excludes private user telemetry
30. Deterministic security invariants hold true across execution
