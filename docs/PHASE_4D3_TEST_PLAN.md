# Phase 4D-3: Test Plan & Verification Matrix

## Automated Test Coverage

### 1. `SmartNotificationEngineTest.kt` (30 Tests)
- Parent Mode suppression of all notification types (Tests 1–2).
- Inactive plan suppression (Test 3).
- Morning intention generation, suppression outside window, and suppression on completion (Tests 4–6).
- Next action generation and completion suppression (Tests 7–9).
- Distraction window preemption data thresholds and peak hour verification (Tests 10–11).
- Missed action late-day window constraints and suppression (Tests 12–13).
- Success notification triggers and suppression (Tests 14–15).
- Evening reflection daily completion suppression and window bounds (Tests 16–17).
- Weekly review frequency and due status evaluation (Tests 18–19).
- Single per-type per-day cap enforcement (Test 20).
- User preference toggles for individual categories (Tests 21–22).
- Determinism and idempotency of evaluation (Test 23).
- Wallet and policy state isolation (Test 24).
- User dismissal penalty and cooldown (Test 25).
- Data threshold constant validation (Tests 26–27).
- Weekly review 7-day cooldown (Test 28).
- Success copy with earned time balance (Test 29).
- Non-Self mode suppression (Test 30).

### 2. `NotificationFrequencyGovernorTest.kt` (18 Tests)
- Initial state verification (Test 1).
- Total daily notification limits across Minimal, Balanced, and Helpful modes (Tests 2–5).
- Minimum time gap enforcement (Tests 6–7).
- Per-type daily limits (Test 8).
- Behaviour reminder caps (Test 9).
- Dedicated caps for Success, Preemptive, and Missed actions (Tests 10–12).
- Automatic daily midnight reset (Test 13).
- Mode-specific min gap configurations (Tests 14–16).
- Independent evaluation of Weekly Review (Test 17).
- Zero timestamp edge-case handling (Test 18).

### 3. `NotificationSchedulerTest.kt` (17 Tests)
- Worker tag constants (Tests 1–3).
- Channel configuration and importance levels (Tests 4–7).
- Type-to-channel mapping (Test 8).
- Default user preferences (Tests 9–10).
- Deep link parser safety (Tests 11–17).

### 4. `NotificationDeepLinkTest.kt` (17 Tests)
- Valid URI parsing: Today, Action, Weekly Review (Tests 1–4).
- Malformed / null / empty / cross-scheme URI handling (Tests 5–11).
- Candidate deep link propagation (Tests 12–14).
- Architectural safety invariants (Tests 15–17).
