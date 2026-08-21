# Phase 4D-1: Privacy Guarantees & Zero-Surveillance Compliance

## 1. Zero Surveillance Commitment
Digital Discipline remains strictly focused on user agency and self-mastery.

---

## 2. Forbidden Data (Zero Collection)
The application **never** accesses, stores, or transmits:
- Keystrokes, text input, or free-form journal notes
- Messages, notifications, or chat transcripts
- Screenshots, screen recordings, or window hierarchies
- Camera or microphone streams
- Browser URLs, web history, or search queries

---

## 3. Allowed Telemetry (Local-Only)
Only the following operational metadata is processed on-device:
- Application package identifiers needed for enforcement rules
- Timestamps of intervention attempts and completions
- Outcome flags (completed, exited, rapid reopen within 5m)
- Daily aggregate foreground time (via UsageStatsManager API)
- Earned time wallet balances and transaction counts
- Daily reflection mood/helper enum selections (stored locally)
