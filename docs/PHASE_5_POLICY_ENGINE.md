# PHASE 5 — POLICY RESOLUTION & PRECEDENCE
## Hierarchical Multi-Source Policy Resolution

---

## 1. Precedence Hierarchy
```
                      TARGET APP LAUNCHED
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
       Parent Rule Exists?            No Parent Rule
                │                             │
    ┌───────────┴───────────┐                 │
    ▼                       ▼                 ▼
Mode = BLOCK?           Mode = DELAY?    Evaluate Self Mode
    │                       │                 │
    ▼                       ▼                 ▼
Hard Block (No Access)  Enforce Delay   Check Triggers & Goals
                                              │
                                              ▼
                                         Intervention
```

---

## 2. Parent-Child Invariants
- Parent Hard Block (`RuleMode.BLOCK`) cannot be overridden by Self Mode.
- Parent Delay (`RuleMode.DELAY`) cannot be bypassed.
- Child device cannot disable or edit Parent policies.
