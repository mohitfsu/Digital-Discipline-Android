# Phase 4E-2: Test Plan & Verification Matrix

## Automated Test Coverage (`SelfModeFirstWinTest.kt`)
1. New Self Mode user starts with NOT_STARTED (Test 1).
2. Active plan enters FIRST_TRIGGER_SEEN (Test 2).
3. Reflection completion transitions correctly (Test 3).
4. Intervention start transitions correctly (Test 4).
5. Intervention completion transitions correctly with earned seconds (Test 5).
6. Earned time deposited exactly once (Test 6).
7. Double completion does not double-earn (Test 7).
8. USE MY TIME sets state and session flags (Test 8).
9. SAVE FOR LATER records saved seconds without starting session (Test 9).
10. First Win marked completed after SAVE (Test 10).
11. First Win marked completed after USE (Test 11).
12. First Win survives process death (Test 12).
13. First Win survives Activity recreation (Test 13).
14. First Win recovers safely after reboot (Test 14).
15. Wallet idempotency format remains intact (Test 15).
16. Parent BLOCK overrides First-Win earned time (Test 16).
17. Parent DELAY overrides First-Win earned time (Test 17).
18. Parent ALLOW remains unaffected (Test 18).
19. First-win notification stops after completion (Test 19).
20. Notification eligible for active uncompleted plan in Self Mode (Test 20).
21. Notification suppressed if Parent Mode active (Test 21).
22. DailyActionPlanner works seamlessly with First Win state (Test 22).
23. Goal template repository returns valid starter goals (Test 23).
24. SmartNotificationEngine remains functional (Test 24).
25. First Win state scoped to planId (Test 25).
26. No duplicate analytics events (Test 26).
27. Offline operation works (Test 27).
28. UserMode enum supports SELF and PARENT (Test 28).
29. First Win completion timestamp recorded (Test 29).
30. First Win state enum has 10 deterministic states (Test 30).
