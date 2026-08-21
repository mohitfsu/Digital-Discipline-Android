# PHASE 6A — ADAPTIVE ARCHITECTURE
## Data Flow & Invariants

---

## 1. System Topology & Data Flow
```
AccessibilityService (Window Event)
         ↓
PolicyEngine (<58ms fast path)
         ↓
TriggerContextBuilder (Builds InterventionContext)
         ↓
InterventionSelector (5-Factor Scoring & Anti-Repetition)
         ↓
InterventionEngine (Creates session & orchestrates validator)
         ↓
Validator (Accelerometer / Timer / Cognitive / Manual)
         ↓
OutcomeRecorder (Updates InterventionAdaptiveStore)
         ↓
EarnedTimeWalletService (Authoritative Ledger Credit)
```

---

## 2. Invariants & Policy Precedence
1. **Parent Hard Block** (`RuleMode.BLOCK`): Bypasses all selectors and immediately terminates access attempt.
2. **Parent Delay** (`RuleMode.DELAY`): Mandatory pause cannot be circumvented by Self Mode or Selector.
3. **Self Policy Bounds**: The selector operates strictly within user-defined active goal triggers and protected app boundaries.
4. **Room Database Version 8**: Maintained with 0 schema migrations.
