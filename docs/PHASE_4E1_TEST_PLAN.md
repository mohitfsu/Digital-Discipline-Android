# Phase 4E-1: Test Plan & Verification Matrix

## Automated Test Coverage

### 1. `SelfModeActivationTest.kt` (10 Tests)
- Valid selections pass validation (Test 1).
- Null template fails validation (Test 2).
- Zero distraction apps fails validation (Test 3).
- Exactly 1 distraction app is valid (Test 4).
- Exactly 5 distraction apps is valid (Test 5).
- Sixth distraction app is blocked / invalid (Test 6).
- Null replacement behaviour fails validation (Test 7).
- Null reward preset fails validation (Test 8).
- Draft plan entity mapping and trigger/policy counts (Test 9).
- Custom goal parameters (title, description, target, unit) preservation (Test 10).

### 2. `SelfModeFirstRunTest.kt` (10 Tests)
- Goal template repository contains all required starting categories (Test 1).
- Fitness template defaults to bodyweight squats (Test 2).
- Study template defaults to study block (Test 3).
- Mindfulness template defaults to box breathing (Test 4).
- Distraction recommendations include top social/video apps (Test 5).
- Reward preset durations map to 5m, 10m, 15m (Test 6).
- Reward preset caps are positive and safe (Test 7).
- UserMode enum contains SELF and PARENT (Test 8).
- Standard preset default is 10 minutes (Test 9).
- All template titles, icons, and descriptions are populated (Test 10).

### 3. `SelfModePermissionFlowTest.kt` (10 Tests)
- Protection healthy when both Accessibility and Overlay granted (Test 1).
- Unhealthy when Accessibility missing (Test 2).
- Unhealthy when Overlay missing (Test 3).
- Unhealthy when both missing (Test 4).
- Permission denial handled gracefully without crash (Test 5).
- Permission explanation contains zero surveillance claims (Test 6).
- Accessibility action string correctness (Test 7).
- Overlay action string correctness (Test 8).
- Continuing without protection maintains "PLAN READY — PROTECTION OFF" status (Test 9).
- Permission status dynamically updates when enabled upon return (Test 10).

### 4. `SelfModeResumeTest.kt` (10 Tests)
- Onboarding state constants: NOT_STARTED (Test 1).
- IN_PROGRESS constant (Test 2).
- COMPLETED constant (Test 3).
- READY constant (Test 4).
- Existing completed user bypasses onboarding to Dashboard (Test 5).
- Fresh user routes to Mode Selection (Test 6).
- Step transition increments deterministically (Test 7).
- Back navigation decrements step deterministically (Test 8).
- Min distraction apps constant is 1 (Test 9).
- Max distraction apps constant is 5 (Test 10).
