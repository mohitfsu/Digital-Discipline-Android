# Phase 4D-1: Data Sources & Persistence

## 1. Unified Local Data Architecture
The Today experience reuses existing entities, repositories, and preferences without introducing schema migrations or data duplication.

---

## 2. Source Mapping

| UI Section | Underlying Data Source | Engine / Repository |
| :--- | :--- | :--- |
| **Primary Goal** | `GoalEntity`, `GoalProgressEntity` | `BehaviourRepository` |
| **One Thing to Focus On** | `InterventionEventEntity` | `BehaviourPatternEngine`, `BehaviourWeeklyIntelligenceEngine` |
| **Earned Time Wallet** | `EarnedTimeWalletEntity`, `WalletSessionEntity` | `EarnedTimeWalletService` |
| **Today's Behaviour** | `DailyUsageEntity`, `InterventionEventEntity` | `LocalAnalyticsRepository`, `BehaviourInsightsEngine` |
| **Distraction Pattern** | `InterventionEventEntity` | `BehaviourPatternEngine` |
| **Recent Wins** | `InterventionEventEntity`, `WalletTransactionEntity` | `BehaviourInsightsEngine.getRecentWins` |
| **Daily Reflection** | `Preferences` (DataStore) | `PreferencesManager` |

---

## 3. Database Schema Status
- **Room Database Version**: Remains at **v8** (`DigitalDisciplineDatabase`).
- **No Room Migration Needed**: Daily reflection state is safely stored in encrypted DataStore preferences.
