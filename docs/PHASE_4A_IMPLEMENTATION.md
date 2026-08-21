# Phase 4A — Behaviour & Goal Engine Implementation

## 1. Executive Summary
Phase 4A introduces the foundational Behaviour & Goal domain model to Digital Discipline. It enables the product to support two distinct modes using the **same underlying enforcement engine**:
1. **PARENT MODE**: Parent manages child app restrictions and time limits (with absolute policy precedence).
2. **SELF MODE**: An individual sets positive goals (e.g. Fitness, Study, Mindfulness), links distracting trigger apps, chooses replacement behaviors (e.g. Squats, Breathing), and earns controlled screen-time rewards.

---

## 2. Core Architectural Pipeline

```
┌────────────────────────────────────────────────────────────────────────┐
│                              USER MODE                                 │
│                     PARENT MODE  vs  SELF MODE                         │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        BEHAVIOUR DOMAIN MODEL                          │
│  • Goals (Fitness, Study, Productivity, Mindfulness, Health, Custom)   │
│  • Triggers (Target app, category, active schedule window, days)       │
│  • Replacement Behaviours (Squats, Pushups, Breathing, Focus Blocks)   │
│  • Behaviour Policies (Goal + Trigger + Behaviour + Reward)            │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     BEHAVIOUR POLICY RESOLVER                          │
│   • Evaluates incoming package name & current time window              │
│   • MANDATORY: Parent Mode rules take 100% precedence                  │
│   • Synthesizes concrete AppRuleEntity without modifying parent state  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   EXISTING LOCAL ENFORCEMENT ENGINE                    │
│   PolicyEngine ──► AccessibilityService ──► Compose OverlayManager     │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Subsystem File Map

| Subsystem | File Path | Purpose |
| :--- | :--- | :--- |
| **Goal Model** | `app/src/main/java/.../data/local/entities/GoalEntity.kt` | Room entity representing behavioral change goals |
| **Trigger Model** | `app/src/main/java/.../data/local/entities/TriggerEntity.kt` | Distraction trigger apps and schedule windows |
| **Replacement Behaviour** | `app/src/main/java/.../data/local/entities/ReplacementBehaviourEntity.kt` | Reusable positive friction activities |
| **Behaviour Policy** | `app/src/main/java/.../data/local/entities/BehaviourPolicyEntity.kt` | Linkage: Goal + Trigger + Behaviour + Reward |
| **Goal Progress** | `app/src/main/java/.../data/local/entities/GoalProgressEntity.kt` | Daily progress rollups without expensive streams |
| **Policy Resolver** | `app/src/main/java/.../behaviour/BehaviourPolicyResolver.kt` | Deterministic resolution with parent precedence |
| **Behaviour Repo** | `app/src/main/java/.../behaviour/BehaviourRepository.kt` | 100% local, fast (<3ms) Room database access |
| **Web Data Model** | `web/src/types/index.ts` | TypeScript interfaces for web control center |
| **Unit Test Suite** | `app/src/test/java/.../BehaviourEngineTest.kt` | Automated tests for resolution, precedence & regression |
