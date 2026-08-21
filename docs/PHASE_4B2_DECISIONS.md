# Phase 4B-2 — Architectural Decisions & Trade-Offs

## ADR 1: Ledger-Based Transactional Accounting vs Simple Mutable Balance
- **Context**: Screen time in Self Mode is a finite, earned resource.
- **Decision**: Store all balance alterations in an auditable ledger (`WalletTransactionEntity`) with source, idempotency keys, and explicit transaction types (`EARN`, `SPEND`, `EXPIRE`, `ADJUSTMENT`, `RESET`).
- **Rationale**: An auditable ledger allows mathematical verification, prevents duplicate double-tap earning bugs, and enables post-crash state reconciliation without risk of balance loss or inflation.

---

## ADR 2: Monotonic Hardware Clock (`elapsedRealtime`) for Session Accounting
- **Context**: Users might change wall-clock dates/times or travel across timezones.
- **Decision**: Session consumption is calculated strictly using hardware monotonic `SystemClock.elapsedRealtime()`.
- **Rationale**: Hardware monotonic time increases strictly linearly and is completely unaffected by wall-clock shifts, leap seconds, or network time updates.

---

## ADR 3: Fail-Closed Reboot Handling
- **Context**: `SystemClock.elapsedRealtime()` resets to zero when Android is rebooted.
- **Decision**: If `nowElapsed < startedElapsedRealtime`, the system detects a reboot and immediately invalidates any active session rather than restoring extra time.
- **Rationale**: Prevents users from exploiting device reboots to extend screen time sessions.
