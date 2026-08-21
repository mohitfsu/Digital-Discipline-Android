# PHASE 6C — PERSONALIZED INTERVENTION ARCHITECTURE
## Data Flow & Hierarchical Selection Pipeline

---

## 1. Hierarchy Data Flow
```
Distraction Trigger (Target Package, TimeBucket)
                       │
                       ▼
          [ Build TriggerContext ]
                       │
                       ▼
       [ Parent Policy Precedence Check ]
    (BLOCK > DELAY > SELF POLICY > INTERVENTION)
                       │
                       ▼
           [ Eligible Candidate Set ]
                       │
                       ▼
   [ Hierarchical Confidence Scorer ]
   ├── Category Evidence ($C_{cat}, R_{cat}$)
   ├── User Item Evidence ($C_{item}, R_{item}$)
   ├── Trigger Evidence ($C_{trig}, R_{trig}$)
   └── Context Evidence ($C_{ctx}, R_{ctx}$)
                       │
                       ▼
           [ 5-Factor Scoring Model ]
   ├── 40% Hierarchical Helpfulness
   ├── 25% Completion Rate
   ├── 15% User Preference
   ├── 10% Contextual Suitability
   ├── 10% Novelty / Freshness
   └── Anti-Repetition Penalties
                       │
                       ▼
            [ Ranked Selection ]
```

---

## 2. Component Responsibility
- **`InterventionAdaptiveStore`**: Thread-safe memory store managing global, category, trigger-specific, and context-specific statistics tables.
- **`InterventionSelector`**: Deterministic ranker executing the 10-step pipeline, calculating confidence-weighted hierarchical scores, applying repetition penalties, and producing fully explainable `ScoreBreakdown` objects.
- **`InterventionScoringModel`**: Defines scoring parameter constants and diagnostic score breakdown representations.
