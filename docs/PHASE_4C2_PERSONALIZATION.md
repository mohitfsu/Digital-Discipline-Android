# Phase 4C-2: Personalization Profile Architecture

## 1. Overview
The `PersonalizationProfileEntity` stores a lightweight, deterministic snapshot of user behavioral trends derived strictly from on-device activity.

---

## 2. Profile Attributes

| Field | Type | Description |
| :--- | :--- | :--- |
| `profileId` | `String` | Primary key (`profile_self`). |
| `preferredIntervention` | `String` | Most effective challenge (e.g. `SQUATS`, `BOX_BREATHING`). |
| `peakStartHour` | `Int` | Start hour (24-hour format) of primary distraction window. |
| `peakEndHour` | `Int` | End hour (24-hour format) of primary distraction window. |
| `challengeCompletionRate` | `Float` | Percentage of attempts successfully completed. |
| `rapidReopenRate` | `Float` | Percentage of completed sessions followed by 5m reopen. |
| `averageSessionDurationSeconds` | `Int` | Average screen-time session length. |
| `rewardEffectiveness` | `String` | Classification: `BALANCED` vs `NEEDS_COOLDOWN`. |
| `consistencyScore` | `Float` | Percentage of active days with completed actions over the last 7 days. |
| `currentPlanHealth` | `String` | State: `WORKING`, `NEEDS_ADJUSTMENT`, `NOT_WORKING`, `INSUFFICIENT_DATA`. |
| `lastCalculatedAt` | `Long` | Wall-clock millisecond timestamp of last calculation. |

---

## 3. Computation Triggers
The personalization profile is computed **strictly off-path**:
1. When the user opens the Self Dashboard.
2. When the user views the Weekly Review screen.
3. When the user applies or rejects a plan adjustment.
4. When a daily rollup finishes.

No background looping or polling occurs.
