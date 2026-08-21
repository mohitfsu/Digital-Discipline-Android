# Phase 4E-5: Test Plan & Scenario Matrix

## 45-Scenario Unit Test Plan (`GoalLifecycleEngineTest.kt`)
1. ACTIVE to PAUSED valid
2. PAUSED to ACTIVE valid
3. ACTIVE to COMPLETED valid
4. ACTIVE to REPLACED valid
5. COMPLETED to ARCHIVED valid
6. REPLACED to ARCHIVED valid
7. Invalid transitions rejected
8. Pause requires explicit confirmation
9. Completion requires explicit confirmation
10. Replacement requires explicit confirmation
11. Start fresh requires explicit confirmation
12. Historical data preserved after pause
13. Historical data preserved after completion
14. Historical data preserved after replacement
15. Wallet balance global & preserved
16. Wallet ledger immutable
17. First Win state retained
18. Habit Momentum history retained
19. Goal Progress history retained
20. Weekly Review records retained
21. Old-goal notifications suppressed when paused
22. Old-goal notifications suppressed when completed
23. Old-goal notifications suppressed when replaced
24. New goal starts with clean insufficient-data baseline
25. Previous goal telemetry isolated
26. Parent Mode BLOCK overrides Self Mode
27. Parent Mode DELAY overrides Self Mode
28. Offline operation
29. Deterministic preview generation
30. Idempotent transition application
31. Process death recovery
32. Activity recreation recovery
33. Duplicate invalid transition safely handled
34. Room v8 compatibility preserved
35. Target state calculation matches transition
36. Transition preview whatChanges populated
37. Transition preview whatStays populated
38. Transition preview confirmation narrative populated
39. Active goal screen snapshot evaluated correctly
40. Paused goal screen snapshot evaluated correctly
41. Completed goal screen snapshot evaluated correctly
42. Goal History summary formats dates cleanly
43. Historical goal detail is read-only
44. Single primary active goal invariant preserved
45. Performance invariant (<1ms steady-state)
