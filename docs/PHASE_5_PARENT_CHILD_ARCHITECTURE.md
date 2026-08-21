# PHASE 5 — PARENT / CHILD ARCHITECTURE
## Unified Intervention Integration for Parent Mode

---

## 1. Architectural Model
```
  PARENT CONTROL PLANE                 CHILD ENFORCEMENT PLANE
┌───────────────────────┐            ┌─────────────────────────┐
│ Parent Policy Author  │ ─────────> │   InterventionEngine    │
│ (BLOCK / DELAY / EARN)│            │  (Single Unified Engine)│
└───────────────────────┘            └─────────────────────────┘
```

- **Unified Execution**: Interventions required by Parent Mode run through the exact same `InterventionEngine` and `InterventionValidator` components as Self Mode.
- **Strict Authority**: The Child device executes interventions to earn temporary access where permitted by Parent policy, but cannot modify rules or bypass hard blocks.
