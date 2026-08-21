# Phase 4B-2 — Earned Time Wallet & Session Economics Implementation

## 1. Executive Summary
Phase 4B-2 introduces the **Earned Time Wallet & Session Economics Engine** for Self Mode in Digital Discipline. Rather than granting unmetered access or simple timer overrides, completed physical or mindful challenges (e.g. 10 squats) bank finite minutes into an auditable ledger. Time is consumed strictly on a monotonic clock basis while target apps are foregrounded, bounded by anti-binge limits and daily caps, while maintaining **100% Parent Mode Absolute Precedence**.

---

## 2. Architecture & Data Flow

```
                     Target App Launched
                              │
                              ▼
                   BehaviourPolicyResolver
                   ├── 1. Parent Mode Rule Active?
                   │      └── If yes: Parent Mode Wins (BLOCK/DELAY)
                   └── 2. Self Mode Match?
                              │
                              ▼
                   EarnedTimeWalletService
                   ├── Check available balance
                   ├── Start/Resume WalletSessionEntity
                   └── If balance == 0: Show Intervention Overlay
                              │
                              ▼
                     Intervention Screen
                   ├── Complete Challenge (e.g. 10 Squats)
                   ├── earnTime(+600s, idempotencyKey)
                   └── Deposit to WalletTransactionEntity (EARN)
                              │
                              ▼
                     Monotonic Consumption
                   ├── SystemClock.elapsedRealtime() Heartbeat
                   ├── Deduct consumed seconds only while foregrounded
                   └── When balance hits 0 -> Enforce Restriction
```

---

## 3. Subsystem File Map

| Component | File Path | Description |
| :--- | :--- | :--- |
| **Wallet Entity** | `app/src/main/java/.../data/local/entities/EarnedTimeWalletEntity.kt` | Room v6 entity for balance and anti-binge caps |
| **Transaction Entity** | `app/src/main/java/.../data/local/entities/WalletTransactionEntity.kt` | Auditable ledger entity for EARN, SPEND, EXPIRE |
| **Session Entity** | `app/src/main/java/.../data/local/entities/WalletSessionEntity.kt` | Monotonic session entity tracking foreground consumption |
| **Wallet DAOs** | `app/src/main/java/.../data/local/dao/EarnedTimeWalletDao.kt` etc. | Room v6 DAOs with migration `MIGRATION_5_6` |
| **Wallet Service** | `app/src/main/java/.../wallet/EarnedTimeWalletService.kt` | Centralized, transactional wallet manager |
| **Policy Engine** | `app/src/main/java/.../policy/PolicyEngine.kt` | Integrated monotonic session heartbeat & overlay trigger |
| **Self Dashboard** | `app/src/main/java/.../ui/dashboard/SelfDashboardScreen.kt` | Wallet balance card & recent activity history |
| **Test Suite** | `app/src/test/java/.../WalletEngineTest.kt` | 30 automated unit & regression tests |
