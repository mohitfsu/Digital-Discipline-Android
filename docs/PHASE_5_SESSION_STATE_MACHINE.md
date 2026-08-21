# PHASE 5 — SESSION STATE MACHINE
## Deterministic Lifecycle Specification

---

## 1. State Diagram
```
  [ CREATED ]
       │
       ▼ (prepare)
   [ READY ]
       │
       ▼ (start)
  [ RUNNING ] ──────────┬────────────────────────┐
       │                │ (cancel)               │ (timeout)
       ▼ (markCompleting)▼                        ▼
 [ COMPLETING ]   [ CANCELLED ]              [ EXPIRED ]
       │
       ▼ (complete)
 [ COMPLETED ] (Idempotent)
```

## 2. Invariants
- Invalid state jumps throw `false` and reject transition.
- Completion is idempotent and cannot double-credit wallet balance.
- Monotonic elapsed time calculation via `SystemClock.elapsedRealtime()`.
