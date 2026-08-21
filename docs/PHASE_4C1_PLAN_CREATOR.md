# Phase 4C-1 — Smart Plan Creator Orchestration

## 1. Plan Drafting & Non-Persistence Invariant
The `BehaviourPlanCreator` separates planning into two distinct phases:

1. **`createDraftPlan(...)`**: Generates a complete `BehaviourPlanDraft` in memory without modifying the SQLite database or preferences.
2. **`confirmAndPersistPlan(...)`**: Persists the draft to Room v5/v6 and updates preferences **only** after the user explicitly reviews and confirms on `SelfPlanReviewScreen`.

---

## 2. Invariant Rules
- **No Hidden Policies**: Users are explicitly presented with the distraction apps, challenge type, reward time, and session caps before activation.
- **Atomic Persistence**: Goal, replacement behaviour, triggers, policies, and wallet settings are saved together in a single transaction path.
