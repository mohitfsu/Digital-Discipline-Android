# Phase 4F-3: User Data Reset & Local Deletion Policy

## Local Data Reset Architecture
- Complete database clearing is supported cleanly via Room `clearAllTables()` and DataStore reset.
- Resetting data clears all active goals, behavioural events, wallet ledger records, and notification history.
- Restores the application to an uninitialized first-run state cleanly.
