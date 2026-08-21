# PHASE 7B — DECAY & RESET SPECIFICATION

---

## 1. 30-Day Half-Life Decay
Decay is evaluated logically from `lastUpdatedTimestampMs` to avoid continuous database churn:
\[
\text{Decay Factor } D = 2^{-\frac{\Delta t}{30\text{ days}}}
\]
- **Fresh Evidence (0 days)**: 100% weight ($D = 1.0$)
- **30 Days Inactive**: 50% weight ($D = 0.50$)
- **60 Days Inactive**: 25% weight ($D = 0.25$)

---

## 2. Opportunistic 90-Day Purge
- Aggregates with `lastUpdatedTimestampMs < now - 90 days` are purged via `dao.deleteOlderThan(cutoff)`.

---

## 3. Reset Semantics
- `store.resetAdaptiveMemory()` atomically clears all in-memory maps and executes `dao.deleteAll()`.
- Preserves all Parent Rules, App Rules, Schedules, and Wallet Balances.
