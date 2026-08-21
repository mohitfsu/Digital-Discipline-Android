# PHASE 6C — PERSONALIZED INTERVENTION LEARNING
## Implementation & Hierarchy Specification Report

---

## 1. Executive Summary
Phase 6C introduces personalized intervention learning into the `InterventionSelector` and `InterventionAdaptiveStore` through a hierarchical evidence model with confidence weighting and graceful fallback for sparse data.

### The 3-Level Learning Hierarchy:
1. **Level 1 — User / Global**: What generally works for this user across all applications and contexts?
2. **Level 2 — Trigger-Specific**: What works for this user when attempting a specific app (e.g., Instagram vs. YouTube)?
3. **Level 3 — Context-Specific**: What works for this user under specific contextual conditions (e.g., Instagram + Night)?
4. **Category-Level Fallback**: When individual intervention evidence is sparse, category-level trends support candidate ranking without assuming every item in the category is equally effective.

---

## 2. Confidence Model & Sparse Data Protection
Confidence $C \in [0.0, 1.0]$ scales deterministically based on observation volume:
\[
C = \min\left(1.0, \frac{\text{totalFeedbackCount} + \lfloor\text{completedCount} / 2\rfloor}{10}\right)
\]

### Mathematical Blending:
- **Category Baseline**:
  \[
  B_{cat} = (C_{cat} \cdot R_{cat}) + ((1 - C_{cat}) \cdot 0.50)
  \]
- **User-Level Estimate**:
  \[
  E_{user} = (C_{item} \cdot R_{item}) + ((1 - C_{item}) \cdot B_{cat})
  \]
- **Trigger-Level Estimate**:
  \[
  E_{trig} = (C_{trig} \cdot R_{trig}) + ((1 - C_{trig}) \cdot E_{user})
  \]
- **Context-Level Estimate**:
  \[
  E_{final} = (C_{ctx} \cdot R_{ctx}) + ((1 - C_{ctx}) \cdot E_{trig})
  \]

### Guarantees:
- **No Overfitting**: A single feedback event yields $C \approx 0.1$, creating a gentle adjustment (+0.05) rather than dominating the selector.
- **Deterministic Cold-Start**: Zero observations yield $E_{final} = 0.50$, perfectly matching baseline policy.
- **Sparse Trigger/Context Fallback**: When trigger or context data is missing, $C_{trig} = 0$ or $C_{ctx} = 0$, seamlessly falling back to the higher evidence level.

---

## 3. Preserved Invariants
- **Parent Precedence**: Absolute precedence preserved (`PARENT BLOCK > PARENT DELAY > SELF POLICY > INTERVENTION SELECTION`).
- **Single Wallet Authority**: `EarnedTimeWalletService` remains the sole wallet authority. Personalization never directly grants time.
- **Zero Surveillance**: 0 keystrokes, screenshots, camera, microphone, URLs, contacts, or message tracking.
- **Zero ML / Zero Cloud**: 100% deterministic, offline-first execution.
- **Room Database Version 8**: 0 migrations required; adaptive learning operates in transient thread-safe memory.
