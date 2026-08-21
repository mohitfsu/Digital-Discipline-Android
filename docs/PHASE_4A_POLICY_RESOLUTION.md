# Phase 4A — Behaviour Policy Resolution Architecture

## 1. Executive Summary
The `BehaviourPolicyResolver` serves as the deterministic bridge between high-level behavioural goals and the real-time `PolicyEngine`. It operates synchronously in local memory and indexed Room lookups ($< 3\text{ ms}$) with zero cloud or network overhead.

---

## 2. Resolution Hierarchy & Precedence

```
┌────────────────────────────────────────────────────────────────────────┐
│                        INCOMING APP LAUNCH EVENT                       │
│                        Target: packageName                             │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│               STEP 1: CHECK PARENT ENFORCEMENT RULES                   │
│   Does a parent rule exist in PolicyRepository?                        │
│   Is userMode == PARENT or is parent rule enabled?                     │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │ YES                             │ NO
                   ▼                                 ▼
┌──────────────────────────────────────┐  ┌──────────────────────────────┐
│  ParentPolicyMatch(appRule)          │  │ STEP 2: EVALUATE SELF MODE   │
│  [MANDATORY PARENT PRECEDENCE]       │  │ Find Active Triggers         │
└──────────────────────────────────────┘  └──────────────┬───────────────┘
                                                         │
                                                         ▼
                                          ┌──────────────────────────────┐
                                          │ Time Window & Day of Week?   │
                                          └──────────────┬───────────────┘
                                                         │ MATCH
                                                         ▼
                                          ┌──────────────────────────────┐
                                          │ Load Goal + Behaviour        │
                                          │ Synthesize concrete AppRule  │
                                          │ BehaviourPolicyMatch(...)    │
                                          └──────────────────────────────┘
```

---

## 3. Precedence Invariants
1. **Parent Policy Invariance**: A child or self cannot circumvent or override a parent-imposed restriction (e.g. `Instagram = BLOCK` or `School Hours = BLOCKED`).
2. **Deterministic Time Matching**: Triggers support overnight windows (e.g., 22:00 to 06:00) by splitting before/after midnight modular arithmetic.
3. **Fail-Closed Fallback**: If no behavior policy matches and no parent rule exists, the app defaults to `ALLOW` (standard operating mode).
