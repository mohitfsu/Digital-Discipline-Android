# PHASE 7B — FAILURE RECOVERY & ISOLATION

---

## 1. Failure Boundaries
- **Database Write Exception**: Caught in background coroutine; logged as non-fatal; memory store remains active.
- **Database Read Failure at Startup**: In-memory store falls back to deterministic cold-start defaults ($0.50$ baseline).
- **Corrupted Adaptive Table**: Can be wiped via `resetAdaptiveMemory()` without affecting wallet or rule enforcement tables.
