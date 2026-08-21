# Phase 4E-5: Pause and Resume Mechanics

## Pause Mechanics
- **User Intention**: User chooses to take a break without deleting progress or failing.
- **UX Guarantee**: *"Your progress won't be deleted. Your history stays here. Your plan simply stops asking you to complete challenges."*
- **Technical Action**:
  1. Sets policy `enabled = false` in `BehaviourRepository`.
  2. Updates `KEY_PRIMARY_GOAL_LIFECYCLE_STATE = "PAUSED"`.
  3. Parent Mode enforcement remains unconditionally active.
  4. Wallet balances and transaction ledgers remain intact.

## Resume Mechanics
- **User Action**: Taps `[ RESUME GOAL ]`.
- **Technical Action**: Re-enables policies (`enabled = true`), restores active friction without resetting days or momentum.
