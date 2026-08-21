# Phase 4A — Behaviour & Goal Engine Test Plan

This document details the test specifications for verifying the Behaviour & Goal domain model, policy resolution, time matching, and parent policy precedence.

---

### Test 1: Goal Creation & Validation
- **Objective**: Verify persistent creation and retrieval of Goal entities across categories.
- **Pass Criteria**: `GoalEntity` created with title, category, priority, and daily target; fields retrieved with 100% fidelity.

### Test 2: Goal Activation & Deactivation
- **Objective**: Verify that toggling `active = false` immediately prevents the Goal from being matched in policy resolution.
- **Pass Criteria**: `getActiveGoals()` excludes deactivated goals; resolver ignores deactivated goal triggers.

### Test 3: Trigger Creation & Package Indexing
- **Objective**: Verify Trigger entity creation and package name indexing.
- **Pass Criteria**: Triggers retrieved by `packageName` via indexed query in $<3\text{ ms}$.

### Test 4: Time-Window & Day-of-Week Matching
- **Objective**: Verify trigger schedule matching across standard and overnight time windows.
- **Pass Criteria**:
  - Matches inside time window (e.g. 18:00..22:00 at 19:30).
  - Rejects outside time window (e.g. at 10:00).
  - Matches overnight window (e.g. 22:00..06:00 at 23:30 and 04:00).
  - Respects day-of-week comma-separated filtering (`1,2,3,4,5,6,7`).

### Test 5: Replacement Behaviour Parameterization
- **Objective**: Verify configuration of physical (Squats/Pushups) and mindful (Breathing/Pause) replacement behaviors.
- **Pass Criteria**: `targetCount`, `durationSeconds`, and `unit` correctly passed to synthetic `AppRuleEntity`.

### Test 6: Behaviour Policy Resolution (Self Mode)
- **Objective**: Verify end-to-end policy resolution in `UserMode.SELF`.
- **Pass Criteria**: Resolver returns `BehaviourPolicyMatch` with mapped `AppRuleEntity` (mode `EARN`, `squatsTargetCount = 10`, `unlockDurationSeconds = 600`).

### Test 7: Parent Policy Precedence Invariant
- **Objective**: Verify that Parent Mode rules take 100% precedence over any user-defined or self-mode policy.
- **Pass Criteria**:
  - In `UserMode.PARENT`, parent `AppRuleEntity` is returned directly.
  - In `UserMode.SELF`, if parent rule exists and is enabled, parent rule overrides Self Mode.

### Test 8: Offline Policy Resolution
- **Objective**: Verify policy resolution operates completely offline without network calls.
- **Pass Criteria**: Resolution completes in $<3\text{ ms}$ using local SQLite Room queries.

### Test 9: Goal Progress Rollup
- **Objective**: Verify recording goal completion updates daily progress rollups and completion percentages accurately.
- **Pass Criteria**: `completedCount` and `completionPercentage` updated monotonically.

### Test 10: Regression Verification of Existing Modes
- **Objective**: Verify existing ALLOW, BLOCK, DELAY, and EARN modes continue functioning without regression.
- **Pass Criteria**: All 16 Phase 3C tests and standard PolicyEngine rules pass without regression.
