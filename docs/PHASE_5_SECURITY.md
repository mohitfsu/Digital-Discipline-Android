# PHASE 5 — SECURITY SPECIFICATION
## Anti-Circumvention & Tamper Resistance

---

## 1. Anti-Circumvention Features
- **Monotonic Clocks**: `SystemClock.elapsedRealtime()` prevents bypass via phone clock changes.
- **Shake Detection Filter**: Accelerometer rate-limiter stops users from shaking device rapidly to fake reps.
- **Immutability**: Session state machine rejects illegitimate transitions and enforces single-reward idempotency.
- **No Exported Intervention Receivers**: Completion signals originate strictly within app process boundaries.
