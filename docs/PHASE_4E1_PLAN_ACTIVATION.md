# Phase 4E-1: Plan Activation & Atomic Commit

## The Invariant of Atomic Activation
A user must never end up in a half-configured state where triggers exist without a goal, or a goal exists without replacement policies.

```
DRAFT
  ↓ (validate selections: 1-5 apps, valid friction, valid reward)
VALIDATED DRAFT
  ↓ (mutex lock: acquire single-flight execution)
PERSIST ENTITIES ATOMICALLY (Goal -> Replacement -> Triggers -> Policies -> Wallet -> DataStore)
  ↓
COMMIT / SUCCESS
```

If any step fails, the coordinator catches the error, logs it to `EventLogger`, leaves the database uncorrupted, and reports failure to the UI without crashing.
