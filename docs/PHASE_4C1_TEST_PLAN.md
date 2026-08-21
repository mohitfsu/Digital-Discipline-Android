# Phase 4C-1 — Test Plan: Goal Templates & Smart Plan Creator

This document outlines the 32 verification and regression test scenarios implemented in `GoalTemplateEngineTest.kt`.

---

### Test Scenarios
1. **Fitness Template Creation**: Verify target (5 actions), unit, icon ("💪"), and category.
2. **Study Template Creation**: Verify target (4 blocks), unit, icon ("📚"), and category.
3. **Productivity Template Creation**: Verify target (4 tasks), unit, icon ("💼"), and category.
4. **Mindfulness Template Creation**: Verify target (3 sessions), LIGHT reward preset, icon ("🧘").
5. **Reading Template Creation**: Verify target (2 sessions), unit, icon ("📖"), and category.
6. **Sleep Template Creation**: Verify target (1 session), icon ("😴"), and category.
7. **Health Template Creation**: Verify target (3 actions), icon ("❤️"), and category.
8. **Custom Goal Creation**: Verify custom title, description, target, and unit propagation.
9. **Fitness Intervention Recommendations**: Verify squats and pushups recommended.
10. **Study Intervention Recommendations**: Verify 5-min and 10-min study blocks recommended.
11. **Productivity Intervention Recommendations**: Verify task sprints recommended.
12. **Mindfulness Recommendations**: Verify box breathing recommended.
13. **Trigger Category Recommendation**: Verify categorization of Social, Video, and Gaming packages.
14. **Reward Preset LIGHT**: Verify 300s reward, 1200s daily cap, 900s session cap.
15. **Reward Preset STANDARD**: Verify 600s reward, 1800s daily cap, 900s session cap.
16. **Reward Preset STRONG**: Verify 600s reward, 1800s daily cap, 600s session cap.
17. **BehaviourPlanDraft Generation**: Verify draft correctly maps template into unpersisted entities.
18. **Draft Non-Persistence**: Verify draft generation does NOT save to database before user confirmation.
19. **Confirmed Draft Persistence**: Verify confirmed draft atomically commits to Room and DataStore.
20. **Existing Self Mode Plan Preserved**: Verify existing plans are not overwritten on initialization.
21. **Existing Parent Mode Unaffected**: Verify Parent Mode rules function normally.
22. **Parent BLOCK Precedence**: Verify Parent BLOCK strictly overrides Self templates.
23. **Parent DELAY Precedence**: Verify Parent DELAY strictly overrides Self templates.
24. **Parent ALLOW Regression**: Verify Parent ALLOW remains unblocked.
25. **Offline Template Operation**: Verify plan generation executes locally in $<10\text{ms}$.
26. **Process Death Persistence**: Verify confirmed draft entities survive process recreation.
27. **Room Migration Safety**: Verify Room v6 persistence and wallet integrity.
28. **Wallet Configuration Respected**: Verify wallet daily caps updated per draft configuration.
29. **Existing SelfModeEngine Regression**: Verify policy resolver match behavior.
30. **Existing WalletEngine Regression**: Verify transaction ledger accounting.
31. **Existing BehaviourEngine Regression**: Verify HIR calculation.
32. **Existing Parent Mode Regression**: Verify Parent Mode rule resolution.
