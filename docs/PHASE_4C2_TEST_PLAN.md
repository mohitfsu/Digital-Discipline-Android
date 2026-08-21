# Phase 4C-2: Test Plan & Scenarios

## 1. Test Overview
`AdaptivePlanEngineTest.kt` contains 41 comprehensive automated unit tests covering all mathematical formulas, thresholds, decision paths, models, and regression invariants.

---

## 2. Test Matrix

| ID | Test Name | Target Invariant |
| :--- | :--- | :--- |
| `test01` | `test01_insufficientData_returnsInsufficientData` | $<10$ events yields `INSUFFICIENT_DATA` |
| `test02` | `test02_workingPlan_evaluatedCorrectly` | $\text{HIR} \ge 65\%$ & $\text{comp} \ge 60\%$ yields `WORKING` |
| `test03` | `test03_notWorkingPlan_evaluatedCorrectly` | Low completion & low HIR yields `NOT_WORKING` |
| `test04` | `test04_improvingPlan_feedbackEvaluated` | $\ge 10$ point HIR improvement triggers `RULE_B` |
| `test05` | `test05_stablePlan_evaluatedCorrectly` | Stable metrics yield `WORKING` |
| `test06` | `test06_interventionRanking_sortedByHIR` | Interventions sorted in descending HIR order |
| `test07` | `test07_bestInterventionThreshold_requiresMin10Trials` | Min 10 trials required before declaring best |
| `test08` | `test08_interventionComparison_requiresMin10Each` | Comparisons require min 10 trials for each candidate |
| `test09` | `test09_peakDetection_identifiesCorrect2HourWindow` | Correct 2-hour peak distraction detection |
| `test10` | `test10_peakMinimumThreshold_handlesSmallSamples` | Insufficient sample returns `hasSufficientData = false` |
| `test11` | `test11_rewardEffectiveness_baselineRequiresMin10Sessions` | Reward analysis requires min 10 sessions |
| `test12` | `test12_rewardLoopDetection_flagsRapidChain` | $\ge 35\%$ immediate reopen flags reward loop |
| `test13` | `test13_cooldownRecommendation_triggeredOnRapidSuccessiveAttempts` | Rapid successive sessions trigger `ADD_COOLDOWN` |
| `test14` | `test14_noCooldownRecommendation_whenSessionsSpaced` | Well-spaced sessions do not recommend cooldown |
| `test15` | `test15_rewardReductionRecommendation_whenExcessiveRewardDetected` | High physical challenge exit suggests `SHORTER_INTERVENTION` |
| `test16` | `test16_interventionChangeRecommendation_whenAlternativeIsSuperior` | Alternative with $\ge 20\%$ higher HIR suggests change |
| `test17` | `test17_shorterInterventionRecommendation_whenExitRateHigh` | $>40\%$ exit rate suggests shorter count (e.g. 5) |
| `test18` | `test18_recommendationExplanation_isInformativeAndNeutral` | Language is neutral and non-judgmental |
| `test19` | `test19_recommendationDeterminism_yieldsExactSameOutput` | Identical inputs produce identical outputs |
| `test20` | `test20_recommendationId_isUnique` | UUID uniqueness per recommendation |
| `test21` | `test21_planAdjustmentCreation_populatesFieldsCorrectly` | Entity properties correctly constructed |
| `test22` | `test22_planAdjustment_statusTransitions` | Status lifecycle: PENDING $\rightarrow$ ACCEPTED / REJECTED |
| `test23` | `test23_applyAdjustment_updatesBehaviour` | Updating targetCount to suggested value |
| `test24` | `test24_rejectAdjustment_leavesPlanUntouched` | Rejection preserves policy unchanged |
| `test25` | `test25_rejectedAdjustment_doesNotModifyPlan` | Verification of unchanged plan values |
| `test26` | `test26_acceptedAdjustment_modifiesPlan` | Policy values properly updated |
| `test27` | `test27_duplicateAdjustmentPrevention` | Prevents redundant pending suggestions |
| `test28` | `test28_expiredRecommendationHandling` | Marks superseded adjustments as `EXPIRED` |
| `test29` | `test29_personalizationProfileCalculation_correctValues` | Accurate profile metrics extraction |
| `test30` | `test30_profilePersistence_instantiation` | Profile instantiation |
| `test31` | `test31_weeklyReviewCalculation_aggregatesStats` | Accurate weekly metrics aggregation |
| `test32` | `test32_weeklyReviewPersistence_instantiation` | Review instantiation |
| `test33` | `test33_processDeathRecovery_stateReconstructible` | State reconstructible from Room |
| `test34` | `test34_rebootRecovery_persistedModelsRemainIntact` | State preserved across device reboots |
| `test35` | `test35_offlineOperation_noNetworkDependency` | 100% offline local computation |
| `test36` | `test36_roomMigration_entityStructureValid` | Room v7 schema compliance |
| `test37` | `test37_parentBlockPrecedence_strictlyOverridesSelfMode` | Parent BLOCK strictly authoritative |
| `test38` | `test38_parentDelayPrecedence_strictlyOverridesSelfMode` | Parent DELAY strictly authoritative |
| `test39` | `test39_parentAllowPrecedence_strictlyOverridesSelfMode` | Parent ALLOW strictly authoritative |
| `test40` | `test40_parentEarnPrecedence_remainsAuthoritative` | Parent EARN strictly authoritative |
| `test41` | `test41_performanceInvariant_evaluatesInUnder1Millisecond` | Average evaluation executes in $<1\text{ms}$ |
