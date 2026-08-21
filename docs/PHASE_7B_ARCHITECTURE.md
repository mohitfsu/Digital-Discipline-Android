# PHASE 7B — PERSISTENT ADAPTIVE ARCHITECTURE
## In-Memory Authority & Asynchronous Persistence

---

## 1. System Topology & Separation of Concerns
```
Enforcement Fast Path (<58ms Budget)
======================================================
Distraction Trigger ──> InterventionSelector.select()
                           │ (Reads In-Memory Cache)
                           ▼
                    <1ms Selection
                           │
                           ▼
                 Render Intervention Overlay

Authoritative Outcome & Asynchronous Write Path
======================================================
Intervention Validated
          │
          ▼
Authoritative Wallet Deposit (earn_intervention_<sessionId>)
          │
          ▼
Update In-Memory Store Immediately (0ms wait)
          │
          ▼
Enqueue Asynchronous Write (Dispatchers.IO Coroutine)
          │
          ▼
Room Database Upsert (intervention_adaptive_aggregates)
```

---

## 2. Cold-Start Loading Sequence
1. `DigitalDisciplineApp` initializes Room `DigitalDisciplineDatabase` (Version 9).
2. `InterventionAdaptiveStore` is instantiated with `InterventionAdaptiveAggregateDao`.
3. Background coroutine on `Dispatchers.IO` loads all aggregate rows via `dao.getAllAggregates()`.
4. In-memory `globalStatsMap`, `categoryStatsMap`, `triggerStatsMap`, and `contextStatsMap` are hydrated.
5. If an intervention trigger fires before the database read finishes, the selector deterministically uses the default cold-start baseline (0.50) without blocking the user.
