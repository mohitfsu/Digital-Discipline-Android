# Phase 4C-1 — Goal Templates & Smart Plan Creation Implementation

## 1. Executive Summary
Phase 4C-1 introduces the **Consumer Product Intelligence Layer** into Digital Discipline. It translates high-level user intent (e.g. *"I want to get fitter"*, *"I want to study more consistently"*) into a sensible, ready-to-use behavioral starting plan without requiring users to configure complex policy mechanics, triggers, replacement behaviors, reward economics, or session caps.

---

## 2. Architecture & Flow

```
                      [ USER GOAL INTENT ]
                     ("I want to get fitter")
                                │
                                ▼
                       [ GOAL TEMPLATE ]
                       (FITNESS Template)
                                │
                                ▼
                      [ DISTRACTION APPS ]
                     (Instagram, YouTube)
                                │
                                ▼
                    [ REPLACEMENT BEHAVIOUR ]
                          (10 Squats)
                                │
                                ▼
                     [ REWARD ECONOMICS ]
                     (10m Earn / 30m Cap)
                                │
                                ▼
                    [ BEHAVIOUR PLAN DRAFT ]
                 (In-Memory Preview Structure)
                                │
                                ▼
                     [ PLAN REVIEW SCREEN ]
                    ([ START THIS PLAN ])
                                │
                                ▼
                  [ ATOMIC ROOM PERSISTENCE ]
             (Goal, Trigger, Behaviour, Policy)
```

---

## 3. Subsystem File Map

| Subsystem | File | Description |
| :--- | :--- | :--- |
| **Domain Models & Presets** | `app/.../behaviour/templates/GoalTemplate.kt` | `GoalTemplate`, `RewardPreset` (LIGHT, STANDARD, STRONG), and app recommendation models. |
| **Template Repository** | `app/.../behaviour/templates/GoalTemplateRepository.kt` | Deterministic presets for FITNESS, STUDY, PRODUCTIVITY, MINDFULNESS, READING, SLEEP, HEALTH, CUSTOM. |
| **Smart Plan Creator** | `app/.../behaviour/templates/BehaviourPlanCreator.kt` | Orchestration layer generating pure drafts and handling atomic confirmation persistence. |
| **Plan Review Screen** | `app/.../ui/onboarding/SelfPlanReviewScreen.kt` | Review screen presenting clear plan terms before activation. |
| **Fast Onboarding UI** | `app/.../ui/onboarding/SelfModeOnboardingScreen.kt` | Fast 5-step template flow (<60s completion time). |
| **Test Suite** | `app/.../GoalTemplateEngineTest.kt` | 32 comprehensive tests. |
