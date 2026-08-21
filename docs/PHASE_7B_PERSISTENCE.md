# PHASE 7B — PERSISTENCE & DATA CONTRACT

---

## 1. Aggregate Keys
- `GLOBAL:<interventionId>` — User-level overall effectiveness.
- `CATEGORY:<category>` — Family-level trends.
- `TRIGGER:<targetPackage>:<interventionId>` — App-specific effectiveness.
- `CONTEXT:<targetPackage>:<timeBucket>:<interventionId>` — Coarse temporal condition.

---

## 2. In-Memory Authority & Concurrency
- `InterventionAdaptiveStore` uses `ConcurrentHashMap` for all internal statistical structures.
- Writes are serialized asynchronously via Kotlin coroutines running on `Dispatchers.IO` with `SupervisorJob`.
- Room disk operations are completely non-blocking to the caller.
