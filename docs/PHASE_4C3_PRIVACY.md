# Phase 4C-3: Privacy & Zero-Surveillance Guarantees

## 1. Absolute Privacy Commitment
Digital Discipline remains strictly focused on user agency and self-mastery. It never acts as surveillance software.

---

## 2. Prohibited Data (Zero Collection)
The application **never** accesses, stores, or transmits:
- Keystrokes or text input
- Messages, notifications, or chat transcripts
- Screenshots, screen recordings, or window hierarchies
- Camera or microphone streams
- Browser URLs, web history, or search queries
- Private file contents or photos

---

## 3. Allowed Telemetry (Local-Only)
Only the following operational metadata is processed on-device:
- Application package identifiers needed for enforcement rules
- Timestamps of intervention attempts and completions
- Outcome flags (completed, exited, rapid reopen within 5m)
- Daily aggregate foreground time (via UsageStatsManager API)
- Earned time wallet balances and transaction counts
