# Phase 4E-6: Test Plan & Scenario Matrix

## 35-Scenario Unit Test Plan (`BehaviourJourneyEngineTest.kt`)
1. Empty journey produces baseline snapshot
2. First Win event appears when achieved
3. Goal started event appears
4. Goal paused event appears when paused
5. Goal completed event appears for past inactive goal
6. Plan refinement event appears when adjustment applied
7. Momentum milestone appears when habit momentum is strong
8. Recovery event appears when recovery detected
9. Weekly review event appears
10. Pattern discovery appears only with sufficient data
11. Insufficient data produces calm baseline learning
12. Timeline is sorted newest first chronologically
13. Duplicate events are removed deterministically
14. Same input produces identical deterministic output
15. Historical goal data remains unchanged
16. Wallet ledger remains unchanged
17. Wallet balance remains unchanged
18. Parent BLOCK precedence unchanged
19. Parent DELAY precedence unchanged
20. Self Mode continues to work offline
21. No network dependency in journey engine
22. No surveillance data introduced
23. Current goal correctly identified
24. Current week correctly identified
25. Current plan health correctly surfaced
26. Journey summary calculations correct
27. Goal history correctly linked
28. Plan continuity correctly linked
29. Journey remains bounded for large telemetry history
30. Performance invariant executes under 10 milliseconds
31. Process recreation preserves correct journey snapshot state
32. Direction narrative adapts for paused state
33. Direction narrative adapts for completed state
34. Multiple goal chapters handled cleanly in summary
35. Room v8 schema preserved without migration
