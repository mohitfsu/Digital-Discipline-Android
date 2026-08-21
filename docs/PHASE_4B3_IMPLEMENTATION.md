# Phase 4B-3 — Self Mode Behaviour Loop & Experience Implementation

## 1. Executive Summary
Phase 4B-3 elevates **Self Mode** into an empowering, goal-first personal behaviour-change and digital wellness product. Replacing punitive parental framing with supportive positive friction, Self Mode introduces an intentional pause reflection experience, deterministic behavioral insights (Habit Interruption Rate, Best Intervention, Distraction Pattern, Rules A–E), and a goal-first dashboard, while maintaining **100% Parent Mode Absolute Precedence** and zero cloud dependencies in the real-time enforcement path.

---

## 2. The Central Behaviour Loop

```
                        [ GOAL ]
                   (e.g., Get Fit, Study)
                           │
                           ▼
                 [ DISTRACTION TRIGGER ]
                 (e.g., Instagram Launched)
                           │
                           ▼
                  [ BEHAVIOURAL PAUSE ]
              ("What do you want right now?"
            [I want to use it / Bored / Habit])
                           │
                           ▼
                   [ POSITIVE ACTION ]
             (e.g., Complete 10 Squats Challenge)
                           │
                           ▼
                     [ EARNED TIME ]
              (+10 mins banked into Wallet)
                           │
                           ▼
                   [ INTENTIONAL USE ]
         (Use Now vs Done For Now [Save Time])
                           │
                           ▼
                 [ BEHAVIOURAL FEEDBACK ]
              (HIR, Pattern, Best Intervention)
                           │
                           ▼
                    [ IMPROVEMENT ]
             ("You're building consistency")
```

---

## 3. Subsystem File Map

| Subsystem | File | Description |
| :--- | :--- | :--- |
| **Deterministic Insights Engine** | `app/.../behaviour/BehaviourInsightsEngine.kt` | Deterministic calculations for HIR, Best Intervention, Pattern Detection, Feedback Rules A–E, and Consistency. |
| **Interruption Experience** | `app/.../overlay/InterventionOverlayCompose.kt` | Intentional pause reflection chips, challenge completion celebration, and `USE TIME` / `DONE FOR NOW` actions. |
| **Goal-First Dashboard** | `app/.../ui/dashboard/SelfDashboardScreen.kt` | Goal-first header, wallet balance & breakdown, today's metrics, what worked, your pattern, feedback, recent wins, and my plan. |
| **Test Suite** | `app/.../SelfModeBehaviourLoopTest.kt` | 29 unit and regression test cases. |
