# Phase 4D-3: Zero Surveillance & Privacy Guarantees

## Strict Privacy Invariants
The Smart Notification system operates entirely on local device state and introduces **zero new surveillance capabilities**.

## Explicit Exclusions
The notification system does NOT collect, inspect, or transmit:
- Screen contents or UI hierarchy
- Keystrokes or text inputs
- Messages or notifications from other apps
- Camera or microphone data
- Browser URLs or browsing history
- Location or GPS data
- Personal contacts or identity info

## Data Reused
Only existing local Room entities and DataStore keys are referenced:
- `GoalEntity` & `GoalProgressEntity` (targets and completed counts)
- `InterventionEventEntity` (timestamp, intervention type, outcome)
- `EarnedTimeWalletEntity` (available balance)
- `PreferencesManager` (local settings and reflection status)
