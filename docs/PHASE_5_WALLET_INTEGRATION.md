# PHASE 5 — WALLET INTEGRATION
## Single Authoritative Ledger Preservation

---

## 1. Authoritative Flow
```
        Intervention Completed
                  ↓
       Idempotency Key Created
   (earn_intervention_<sessionId>)
                  ↓
    EarnedTimeWalletService.earnTime(...)
                  ↓
          Ledger Entry Appended
                  ↓
     Temporary Unlock Granted
```

## 2. Invariants
- Zero duplicate wallets.
- Zero direct balance manipulation.
- Idempotent: replaying a completed session produces 0 additional credit.
