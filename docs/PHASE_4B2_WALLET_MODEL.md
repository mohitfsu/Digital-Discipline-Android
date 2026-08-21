# Phase 4B-2 — Earned Time Wallet Data & Ledger Specification

## 1. Room v6 Schema

### A. `earned_time_wallets` Table
```sql
CREATE TABLE IF NOT EXISTS earned_time_wallets (
    walletId TEXT PRIMARY KEY NOT NULL,
    ownerId TEXT NOT NULL,
    mode TEXT NOT NULL,
    availableSeconds INTEGER NOT NULL,
    lifetimeEarnedSeconds INTEGER NOT NULL,
    lifetimeConsumedSeconds INTEGER NOT NULL,
    dailyEarnedSeconds INTEGER NOT NULL,
    dailyConsumedSeconds INTEGER NOT NULL,
    dailyEarnCapSeconds INTEGER NOT NULL,
    maxBalanceCapSeconds INTEGER NOT NULL,
    maxSessionSeconds INTEGER NOT NULL,
    lastDateString TEXT NOT NULL,
    lastUpdatedElapsedRealtime INTEGER NOT NULL,
    lastUpdatedWallClock INTEGER NOT NULL,
    walletVersion INTEGER NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);
```

### B. `wallet_transactions` Table (Auditable Ledger)
```sql
CREATE TABLE IF NOT EXISTS wallet_transactions (
    transactionId TEXT PRIMARY KEY NOT NULL,
    walletId TEXT NOT NULL,
    type TEXT NOT NULL, -- EARN, SPEND, EXPIRE, ADJUSTMENT, RESET
    amountSeconds INTEGER NOT NULL,
    balanceAfterSeconds INTEGER NOT NULL,
    source TEXT NOT NULL,
    triggerPackage TEXT,
    idempotencyKey TEXT,
    sessionId TEXT,
    goalId TEXT,
    timestampWallClock INTEGER NOT NULL,
    elapsedRealtime INTEGER NOT NULL,
    createdAt INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS index_wallet_transactions_walletId ON wallet_transactions(walletId);
CREATE UNIQUE INDEX IF NOT EXISTS index_wallet_transactions_idempotencyKey ON wallet_transactions(idempotencyKey);
```

### C. `wallet_sessions` Table
```sql
CREATE TABLE IF NOT EXISTS wallet_sessions (
    sessionId TEXT PRIMARY KEY NOT NULL,
    walletId TEXT NOT NULL,
    triggerPackage TEXT NOT NULL,
    startedElapsedRealtime INTEGER NOT NULL,
    lastHeartbeatElapsedRealtime INTEGER NOT NULL,
    startedWallClock INTEGER NOT NULL,
    initialWalletSeconds INTEGER NOT NULL,
    consumedSeconds INTEGER NOT NULL,
    maxAllowedSeconds INTEGER NOT NULL,
    status TEXT NOT NULL, -- ACTIVE, EXPIRED, ENDED, INTERRUPTED, INVALIDATED
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS index_wallet_sessions_walletId ON wallet_sessions(walletId);
CREATE INDEX IF NOT EXISTS index_wallet_sessions_status ON wallet_sessions(status);
```
