# Phase 4B-2 — Test Execution & Regression Results

## Environment
- **Target Device**: Physical Android Device (`9645561501002LC`, Android 11 / API 30+)
- **Unit Test Runner**: JUnit 4 / Kotlin Coroutines Test (`WalletEngineTest.kt`)
- **APK Target**: `d:\Zidd\app\build\outputs\apk\debug\app-debug.apk`

---

## 1. Automated Test Suite Results

| Test # | Test Scenario | Status | Result Summary |
| :---: | :--- | :---: | :--- |
| **1** | Initial wallet balance = 0 | 🟢 **PASS** | `availableSeconds == 0` |
| **2** | Earn 10 minutes | 🟢 **PASS** | +600s added to wallet and transaction recorded |
| **3** | Earn multiple rewards | 🟢 **PASS** | Accumulated to 1500s |
| **4** | Daily earning cap | 🟢 **PASS** | Clamped at 3600s max per day |
| **5** | Maximum wallet cap | 🟢 **PASS** | Capped at 3600s max balance |
| **6** | Maximum session duration | 🟢 **PASS** | Clamped to 1800s max per session |
| **7** | Spend wallet time | 🟢 **PASS** | Monotonic heartbeat decreased balance by 60s |
| **8** | Wallet reaches zero | 🟢 **PASS** | Emitted `SessionUpdateResult.Expired` |
| **9** | Session expires correctly | 🟢 **PASS** | Session marked EXPIRED |
| **10** | Backgrounding target app | 🟢 **PASS** | Consumption paused; total consumed finalized |
| **11** | Reopening target app | 🟢 **PASS** | Resumed with remaining 540s balance |
| **12** | Idempotency protection | 🟢 **PASS** | Duplicate key returned `DuplicateIgnored` |
| **13** | Duplicate spend prevention | 🟢 **PASS** | Double pause returned `NoActiveSession` |
| **14** | Rapid double completion | 🟢 **PASS** | Second request ignored |
| **15** | Process death during session | 🟢 **PASS** | Recovered and finalized up to heartbeat |
| **16** | Process death after earning | 🟢 **PASS** | Balance persisted cleanly |
| **17** | Reboot during session | 🟢 **PASS** | Returned `SessionUpdateResult.RebootInvalidated` |
| **18** | Wall-clock forward attack | 🟢 **PASS** | Hardware monotonic clock immune to wall shift |
| **19** | Wall-clock backward attack | 🟢 **PASS** | Zero impact on session duration |
| **20** | Timezone change | 🟢 **PASS** | Zero impact on session duration |
| **21** | Parent BLOCK precedence | 🟢 **PASS** | Parent BLOCK unconditionally overrides wallet |
| **22** | Parent DELAY precedence | 🟢 **PASS** | Parent DELAY unconditionally overrides wallet |
| **23** | Parent ALLOW unaffected | 🟢 **PASS** | Unrestricted apps remain ALLOW |
| **24** | Multiple apps share wallet | 🟢 **PASS** | Instagram and YouTube draw from shared pool |
| **25** | Offline latency | 🟢 **PASS** | Resolved in $<1\text{ms}$ locally |
| **26** | Concurrent earn serialization | 🟢 **PASS** | Mutex serialized atomic writes |
| **27** | Concurrent spend serialization | 🟢 **PASS** | Mutex serialized atomic deductions |
| **28** | Ledger balance reconstruction | 🟢 **PASS** | Mathematical sum of ledger == available balance |
| **29** | Database transaction safety | 🟢 **PASS** | Room v6 consistency preserved |
| **30** | Stale session recovery | 🟢 **PASS** | Stale sessions cleared; fresh start allowed |
