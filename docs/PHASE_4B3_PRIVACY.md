# Phase 4B-3 — Privacy & Data Minimization Architecture

## 1. Local-Only Guarantee for Reflections & Insights
- **Trigger Reflections**: Choices like "I'm bored" or "I'm avoiding something" are strictly ephemeral/local events. They are **never uploaded to Firestore** or synchronized across devices.
- **Zero Surveillance**: No screen captures, keystrokes, audio, or browsing histories are monitored or stored.
- **Zero Machine Learning Profiling**: All insights are calculated deterministically on-device using basic aggregation math in `BehaviourInsightsEngine.kt`.

---

## 2. Parent Mode Isolation
- Self Mode goal reflections and personal notes are isolated from Parent Mode reporting.
- Parent Mode analytics continue reporting high-level aggregate usage (attempts, blocks, total minutes) as designed in Phase 3C without personal psychological data.
