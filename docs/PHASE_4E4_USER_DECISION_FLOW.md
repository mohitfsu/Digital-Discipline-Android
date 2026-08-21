# Phase 4E-4: Mandatory User Decision Flow & Atomic Commits

## Strict Decision Flow
$$\text{Observe Telemetry} \longrightarrow \text{Analyze Off-Path} \longrightarrow \text{Surface Recommendation} \longrightarrow \text{User Decision} \longrightarrow \text{Atomic Commit}$$

## Atomic Update Guarantee
- All changes are applied atomically via `PersonalizationRepository` and `BehaviourRepository`.
- If an update fails, no partial or corrupted policies remain active.
- Unapproved recommendations remain purely ephemeral and never alter active rules.
