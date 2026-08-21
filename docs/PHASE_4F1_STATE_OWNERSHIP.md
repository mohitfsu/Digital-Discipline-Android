# Phase 4F-1: State Ownership & Source of Truth Map

| State Domain | Authoritative Store | Writer Component | Readers | Concurrency Protection | Recovery Behavior |
|---|---|---|---|---|---|
| **User Mode** | Encrypted DataStore (`PreferencesManager`) | `PreferencesManager` | `MainActivity`, `PolicyEngine` | Coroutine IO Dispatcher | Restores from DataStore on process restart |
| **Parent Rules & PIN** | Room (`app_rules`, `schedules`) + Keystore | `PolicyRepository` | `PolicyEngine`, `ParentDashboard` | Database transaction | Synchronous Room reload on boot |
| **Self Goals** | Room (`goals`) | `GoalRepository`, `GoalLifecycleService` | `TodayScreen`, `SelfDashboard` | Database transaction | Synchronous Room reload on restart |
| **Behaviour Policies** | Room (`behaviour_policies`) | `PolicyRepository` | `PolicyEngine`, `TodayScreen` | Room DAO | Restores active policies |
| **Wallet Balance & Ledger** | Room (`wallet_ledger`, `wallet_sessions`) | `EarnedTimeWalletService` (Sole Authority) | `TodayScreen`, `Overlay` | `Mutex` in `EarnedTimeWalletService` | Deterministic reconstruction from ledger |
| **First Win State** | Encrypted DataStore | `FirstWinStateManager` | `TodayScreen`, `SelfDashboard` | DataStore atomic write | Restores state on process restart |
| **Habit Momentum** | Derived on-the-fly | `HabitMomentumEngine` | `TodayScreen`, `SelfJourney` | Pure function (read-only) | Instant re-evaluation from telemetry |
| **Journey Timeline** | Derived on-the-fly | `BehaviourJourneyEngine` | `SelfJourneyScreen` | Pure function (read-only) | Instant re-evaluation ($< 0.5\text{ms}$) |
| **Plan Continuity** | Derived + Room (`plan_adjustments`) | `PlanContinuityEngine` | `SelfPlanContinuityScreen` | Room DAO | Restores applied adjustments |
