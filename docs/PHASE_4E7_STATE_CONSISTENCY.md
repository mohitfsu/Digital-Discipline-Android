# Phase 4E-7: State Consistency & Single Authority Audit

## Authoritative State Mapping
- **Wallet State**: `EarnedTimeWalletEntity` (Room) is authoritative; UI state is derived reactively via StateFlow.
- **Goals & Policies**: `GoalEntity`, `BehaviourPolicyEntity` (Room) are authoritative.
- **First Win / Preferences**: Encrypted PreferencesManager (DataStore) is authoritative.
- **Journey Timeline**: `BehaviourJourneyEngine` synthesizes events purely on-the-fly; zero duplicate cache rows.
