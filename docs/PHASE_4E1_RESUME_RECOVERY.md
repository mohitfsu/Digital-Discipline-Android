# Phase 4E-1: Resume & Process Recovery

## Onboarding State Lifecycle

| State | Meaning |
|---|---|
| `NOT_STARTED` | User has not selected a mode or started onboarding. Routes to `ModeSelectionScreen`. |
| `IN_PROGRESS` | User has selected Self Mode and is progressing through steps 1–4. |
| `READY` | User has reached Step 5 (Plan Preview) with a complete in-memory draft. |
| `COMPLETED` | Plan has been atomically committed. Routes directly to `TodayScreen`. |

## Process Death Invariants
- If process death occurs during steps 1–4, the user safely resumes without corrupting Room entities.
- If existing active goals/policies are present, onboarding is bypassed automatically.
- Parent Mode users are never routed into Self Mode onboarding.
