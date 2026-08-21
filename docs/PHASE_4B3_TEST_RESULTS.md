# Phase 4B-3 — Test Execution & Regression Results

## Environment
- **Device**: Android Physical Device (`9645561501002LC`, Android 11 / API 30+)
- **Test Runner**: JUnit 4 / Kotlin Coroutines Test (`SelfModeBehaviourLoopTest.kt`, `WalletEngineTest.kt`)
- **APK Target**: `d:\Zidd\app\build\outputs\apk\debug\app-debug.apk`

---

## 1. Automated Test Suite Results (29/29 Pass)

| # | Test Scenario | Status | Description |
| :---: | :--- | :---: | :--- |
| **1** | Goal progress calculation | 🟢 **PASS** | $2/5$ completions computes $40\%$ completion rate |
| **2** | Daily progress calculation | 🟢 **PASS** | Daily goal completion incremented correctly |
| **3** | Weekly progress rollup | 🟢 **PASS** | Calculated consistency as "6 of 7 days" |
| **4** | Dashboard wallet balance | 🟢 **PASS** | Displays accurate available seconds from Room v6 |
| **5** | Wallet transaction display | 🟢 **PASS** | Ledger records `EARN` and `SPEND` transactions |
| **6** | Intervention completion $\rightarrow$ Earn | 🟢 **PASS** | Challenge completion deposits time to wallet |
| **7** | Intervention abandonment | 🟢 **PASS** | Exiting challenge without completion credits $0$ |
| **8** | Reflection optionality | 🟢 **PASS** | Intentional reflection options parsed correctly |
| **9** | HIR calculation | 🟢 **PASS** | $3/4$ uninterrupted attempts computes $75\%$ HIR |
| **10** | Best intervention calculation | 🟢 **PASS** | Squats identified as best intervention ($90\%$ HIR) |
| **11** | Behaviour pattern threshold | 🟢 **PASS** | Suppressed when $<10$ events, shown when $\ge 10$ |
| **12** | Weekly improvement (Rule B) | 🟢 **PASS** | $+30\%$ HIR jump triggers Rule B improvement feedback |
| **13** | Weekly decline (Rule C) | 🟢 **PASS** | $-30\%$ HIR drop triggers Rule C harder-to-interrupt feedback |
| **14** | Insufficient data (Rule E) | 🟢 **PASS** | $<10$ attempts returns neutral "Keep going" feedback |
| **15** | Parent BLOCK precedence | 🟢 **PASS** | Parent BLOCK unconditionally wins over Self wallet |
| **16** | Parent DELAY precedence | 🟢 **PASS** | Parent DELAY unconditionally wins over Self wallet |
| **17** | Parent ALLOW regression | 🟢 **PASS** | Parent ALLOW remains unblocked |
| **18** | Wallet cap respected | 🟢 **PASS** | Cannot exceed $3600\text{s}$ max balance |
| **19** | Session cap respected | 🟢 **PASS** | Cannot exceed $1800\text{s}$ max session |
| **20** | Idempotent reward | 🟢 **PASS** | Replay with duplicate key returns `DuplicateIgnored` |
| **21** | Process death recovery | 🟢 **PASS** | Unfinalized session finalized on process restart |
| **22** | Reboot recovery | 🟢 **PASS** | Active session safely invalidated upon reboot |
| **23** | Offline performance | 🟢 **PASS** | Operations resolve in $<5\text{ms}$ locally |
| **24** | Parent Mode regression | 🟢 **PASS** | Parent Mode resolution remains unchanged |
| **25** | Self Mode regression | 🟢 **PASS** | Self Mode policy match resolution verified |
| **26** | ALLOW mode regression | 🟢 **PASS** | ALLOW rules preserved |
| **27** | BLOCK mode regression | 🟢 **PASS** | BLOCK rules preserved |
| **28** | DELAY mode regression | 🟢 **PASS** | DELAY rules preserved |
| **29** | EARN mode regression | 🟢 **PASS** | EARN rules preserved |
