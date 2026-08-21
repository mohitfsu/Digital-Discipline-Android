# Phase 4E-7: Self Mode MVP Hardening & End-to-End Reliability — Implementation Plan

## 1. Executive Summary & Objective
Phase 4E-7 is a dedicated **hardening and reliability verification phase** for Self Mode in Digital Discipline. Its mission is to prove and ensure that a user can operate Self Mode for days, weeks, and months without state corruption, wallet desynchronization, permission ambiguity, process death data loss, circumvention loopholes, or crashes.

---

## 2. Architectural Invariants (Non-Negotiables)
1. **Real-Time Enforcement Path Untouched**:
   `DigitalDisciplineAccessibilityService` $\rightarrow$ `PolicyEngine` $\rightarrow$ `OverlayManager` remains unchanged.
2. **Absolute Parent Precedence**:
   Parent `BLOCK` and `DELAY` rules strictly override any Self Mode wallet unlocks or configurations.
3. **Sole Wallet Authority**:
   `EarnedTimeWalletService` remains the single source of truth for balances, ledger transactions, session states, and monotonic timer enforcement.
4. **Zero Room Schema Migration**:
   Room database schema strictly preserved at **Version 8**.
5. **Zero Cloud / AI / LLM Dependencies**:
   100% on-device local computation with fail-closed offline security.
6. **Zero Surveillance / Data Minimization**:
   No collection of keystrokes, screenshots, audio, camera, messages, URLs, or browser contents.
7. **Anti-Gamification Integrity**:
   No streaks, XP, level badges, or coercive psychological triggers.

---

## 3. End-to-End Test Harness & Scenarios

### Complete Lifecycle Verification Matrix (`SelfModeE2EReliabilityTest.kt`):
1. **Fresh Install & Activation**: Initial setup $\rightarrow$ Goal creation $\rightarrow$ Policy binding $\rightarrow$ Permission verification $\rightarrow$ Active protection.
2. **Intervention & Positive Friction**: App launch trigger $\rightarrow$ Mindful pause $\rightarrow$ Challenge execution $\rightarrow$ Earned time deposit.
3. **Wallet Consumption & Monotonic Clock**: "Use My Time" activation $\rightarrow$ Session creation $\rightarrow$ App backgrounding $\rightarrow$ Resume $\rightarrow$ Monotonic expiration $\rightarrow$ Lock re-enforcement.
4. **"Save for Later" Flow**: Impulses diverted $\rightarrow$ Banked time $\rightarrow$ Today screen state $\rightarrow$ First Win completion $\rightarrow$ Habit Momentum tracking.
5. **Multi-Day Continuity**: Weekly Review $\rightarrow$ Plan Continuity evaluation $\rightarrow$ Plan Refinement $\rightarrow$ Goal completion $\rightarrow$ History archive $\rightarrow$ My Journey chronological timeline synthesis.
6. **Process Death & Crash Recovery**: State persistence across OS process termination at every lifecycle juncture (during challenge, active wallet session, pending review).
7. **Reboot & Monotonic Integrity**: Fail-closed session termination upon device reboot; wall-clock shifts (forward/backward/timezone) cannot forge time.
8. **Permission Resilience**: Dynamic revocation/granting of Accessibility and Notification permissions with accurate "PROTECTION ON / OFF" state messaging.
9. **Circumvention & Bypass Resistance**: Rapid task switching, Recent Apps, Back button, notification trampolines, split screen; 100% interception preserved.
10. **Parent Mode Invariance**: Absolute precedence of Parent BLOCK/DELAY over active Self Mode wallet sessions.
11. **State Consistency Audit**: Single source of truth verification between Room, DataStore, and in-memory caches.

---

## 4. Documentation Deliverables
- `docs/PHASE_4E7_IMPLEMENTATION.md`
- `docs/PHASE_4E7_E2E_TEST_PLAN.md`
- `docs/PHASE_4E7_RECOVERY.md`
- `docs/PHASE_4E7_PERMISSION_RESILIENCE.md`
- `docs/PHASE_4E7_BYPASS_TESTING.md`
- `docs/PHASE_4E7_WALLET_SECURITY.md`
- `docs/PHASE_4E7_STATE_CONSISTENCY.md`
- `docs/PHASE_4E7_DEVICE_MATRIX.md`
- `docs/PHASE_4E7_PERFORMANCE.md`
- `docs/PHASE_4E7_PRIVACY.md`
- `docs/PHASE_4E7_UX_FAILURE_STATES.md`
- `docs/PHASE_4E7_TEST_PLAN.md`
- `docs/PHASE_4E7_TEST_RESULTS.md`
- `docs/PHASE_4E7_DECISIONS.md`
