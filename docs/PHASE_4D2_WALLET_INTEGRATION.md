# Phase 4D-2: Wallet Integration & User Agency

## 1. Single Authoritative Service
Daily actions interact with the wallet exclusively via [`EarnedTimeWalletService.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/wallet/EarnedTimeWalletService.kt). No duplicate wallet or ledger logic is introduced.

---

## 2. Explicit User Choice Post-Completion
Upon finishing a daily action, the user receives their reward with two explicit choices:
1. **`[ USE NOW ]`**: Immediately initiates an active session (`startOrResumeSession`) on the target distraction application.
2. **`[ SAVE FOR LATER ]`**: Credits the wallet ledger and returns to `TodayScreen` without consuming wallet balance.

---

## 3. Safeguards & Limits Enforced
- **Daily Earning Cap**: Defaults to 60 minutes (`dailyEarnCapSeconds = 3600`).
- **Max Balance Cap**: Defaults to 60 minutes (`maxBalanceCapSeconds = 3600`).
- **Monotonic Clocks**: Elapsed realtime clocks prevent device clock manipulation.
- **Parent Precedence**: Parent Mode blocks/delays strictly supersede unlocked wallet time.
