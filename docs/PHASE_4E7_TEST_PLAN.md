# Phase 4E-7: Test Plan & Verification Coverage

## 20-Scenario Hardening Test Plan (`SelfModeE2EReliabilityTest.kt`)
1. Complete Self Mode E2E journey from fresh install to long-term journey
2. Process death during challenge preserves uncorrupted state
3. Process death during active wallet session survives via monotonic timestamp
4. Process death does not duplicate First Win or wallet rewards
5. Device reboot immediately terminates active wallet session fail-closed
6. Wall-clock forward or backward shift cannot forge session time
7. UI truthfully reflects PROTECTION OFF when accessibility is revoked
8. UI truthfully reflects PROTECTION ON only when accessibility is active
9. Target app launch is intercepted regardless of entry point
10. Rapid app switching maintains policy enforcement
11. Wallet balance cannot become negative
12. Wallet respects maximum balance ceiling
13. Double tap prevention on spend action
14. Parent BLOCK strictly overrides Self Mode active wallet session
15. Parent DELAY strictly overrides Self Mode active wallet session
16. Zero network dependency and 100% offline operation
17. Zero surveillance data stored in Room entities
18. Room database remains strictly at Version 8 without migration
19. Performance benchmark executes under 10ms target
20. Calm non-accusatory failure recovery for missing permissions
