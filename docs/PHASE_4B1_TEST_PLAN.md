# Phase 4B-1 — Self Mode Test Plan

This test plan defines the automated and physical verification matrix for Self Mode onboarding, persistence, in-place configuration editing, policy resolution, and parent mode precedence.

---

### Test 1: Self Mode Selection Persistence
- **Objective**: Verify that selecting "For Myself" correctly persists `UserMode.SELF` in DataStore.

### Test 2: Parent Mode Selection Flow Continuity
- **Objective**: Verify that selecting "For My Child" maintains standard Parent Mode onboarding flow.

### Test 3: Goal Creation & Persistence
- **Objective**: Verify that selecting and saving a GoalEntity stores the category, daily target, and unit in Room v5.

### Test 4: Single & Multiple Trigger App Selection
- **Objective**: Verify that selecting 1 to 5 distraction apps saves active TriggerEntity records linked to the goalId.

### Test 5: Replacement Behaviour Binding
- **Objective**: Verify that selecting physical or mindful friction alternatives links to the appropriate BehaviourPolicyEntity.

### Test 6: Self Dashboard Configuration Rendering
- **Objective**: Verify that the dashboard loads the active goal, daily progress, and monitored distraction apps.

### Test 7: In-Place Editing (Goal, Triggers, Intervention)
- **Objective**: Verify that modifying goal title, app list, or friction type from the dashboard updates Room v5 immediately.

### Test 8: Behaviour Policy Resolver Integration (Self Mode)
- **Objective**: Verify that launching a configured distraction app in Self Mode resolves to a synthetic AppRuleEntity with EARN mode and target reps.

### Test 9: Parent Mode Absolute Precedence Invariant
- **Objective**: Verify that if a parent restriction exists, it unconditionally overrides any Self Mode policy.

### Test 10: Process Death & Reboot Resilience
- **Objective**: Verify that all Self Mode configurations survive process termination and device reboot without cloud dependency.
