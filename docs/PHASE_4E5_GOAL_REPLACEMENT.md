# Phase 4E-5: Goal Replacement & Transition Previews

## Replacement Workflow
1. User selects `[ CHANGE GOAL ]` and chooses a new goal template.
2. `GoalLifecycleEngine` generates a clear before/after diff:
   - **What Changes**: Goal title, positive friction challenge, reward preset, distraction triggers.
   - **What Stays**: Previous goal history, wallet balance, transaction ledger, past milestones, and weekly reviews.
3. User confirms change via `[ CONFIRM NEW GOAL ]`.
4. `GoalLifecycleService` atomically archives the old goal and saves the new plan draft.
