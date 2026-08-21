# Phase 4E-4: Plan Refinement Types & Preview

## Supported Refinement Types from `AdaptivePlanEngine`
1. `KEEP_PLAN`: Current challenge is highly effective; continue without modification.
2. `CHANGE_INTERVENTION`: Switch to alternative positive friction (e.g. Squats $\rightarrow$ Box Breathing).
3. `REDUCE_REWARD`: Screen time reward is excessive, tightening to +5m.
4. `SHORTER_INTERVENTION`: Reps or duration reduced for easier compliance.
5. `ADD_COOLDOWN`: Adds intentional cooldown (e.g. 120s) to prevent bingeing.
6. `CHANGE_DISTRACTION_WINDOW`: Adjusts active distraction schedule.
7. `INSUFFICIENT_DATA`: Supportive notice to continue baseline learning.

## Before vs After Diff Preview
Before applying any change, `PlanChangePreview` computes exact field differences (Current Title/Reward vs Suggested Title/Reward) so the user is completely aware of what changes.
