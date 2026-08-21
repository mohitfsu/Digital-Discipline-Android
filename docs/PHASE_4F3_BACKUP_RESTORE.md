# Phase 4F-3: Android Backup & Restore Policy

## Backup Configuration
- `android:allowBackup="true"` with explicit `dataExtractionRules`.
- **Restored State Handling**: Active wallet unlock sessions are ephemeral and fail-closed upon restore; wallet ledger row totals reconstruct cleanly without duplicate awards.
