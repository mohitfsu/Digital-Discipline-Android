# Phase 4E-4: Plan Continuity Lifecycle & State Machine

## Plan Continuity States

| State | Description | Transition Trigger |
|---|---|---|
| `LEARNING` | First 7 days in progress (< 7 days or < 5 active days) | Plan activation |
| `FIRST_WEEK_REVIEW` | First 7-day cycle completed, awaiting user review | First week complete |
| `PLAN_CONFIRMED` | User explicitly confirmed current plan for ongoing week | User taps `[ KEEP MY PLAN ]` |
| `PLAN_NEEDS_REVIEW` | `AdaptivePlanEngine` suggests adjustment or review is due | Recommendation generated |
| `PLAN_REFINED` | User recently approved and applied an adjustment | User taps `[ APPLY CHANGE ]` |
| `INSUFFICIENT_DATA` | Not enough baseline telemetry to evaluate | < 3 recorded events |

## Invariant Guarantees
- Unapproved recommendations never alter database configuration.
- Continuity state transitions occur only on explicit user actions.
