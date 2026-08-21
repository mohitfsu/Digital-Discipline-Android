# Phase 4B-1 — Self Mode Onboarding & Goal/Trigger Configuration Implementation

## 1. Executive Summary
Phase 4B-1 delivers the first consumer-facing **Self Mode** experience for Digital Discipline. It allows individuals to take ownership of their digital habits by configuring their goals, distraction apps, and positive friction replacement behaviors while seamlessly reusing the existing local enforcement engine (`PolicyEngine`, `AccessibilityService`, `OverlayManager`) and maintaining **100% Parent Mode Absolute Precedence**.

---

## 2. Core Architecture Pipeline

```
                    DIGITAL DISCIPLINE
                           │
             ┌─────────────┴─────────────┐
             │                           │
        PARENT MODE                  SELF MODE
             │                           │
       Parent controls             I control myself
       child's device              and my habits
             │                           │
             └─────────────┬─────────────┘
                           │
                           ▼
             ┌───────────────────────────┐
             │  BehaviourPolicyResolver  │
             │ (Parent Mode Precedence)  │
             └─────────────┬─────────────┘
                           │
                           ▼
             ┌───────────────────────────┐
             │       PolicyEngine        │
             └─────────────┬─────────────┘
                           │
                           ▼
             ┌───────────────────────────┐
             │   AccessibilityService    │
             └─────────────┬─────────────┘
                           │
                           ▼
             ┌───────────────────────────┐
             │      OverlayManager       │
             └───────────────────────────┘
```

---

## 3. Subsystem File Map

| Component | File Path | Description |
| :--- | :--- | :--- |
| **Mode Selector** | `app/src/main/java/.../ui/onboarding/ModeSelectionScreen.kt` | Screen allowing user to choose "For Myself" or "For My Child" |
| **Self Onboarding** | `app/src/main/java/.../ui/onboarding/SelfModeOnboardingScreen.kt` | 4-step wizard: Goal $\rightarrow$ Distractions $\rightarrow$ Friction $\rightarrow$ Review |
| **Self Dashboard** | `app/src/main/java/.../ui/dashboard/SelfDashboardScreen.kt` | Minimal, personal control center with in-place edit dialogs |
| **Navigation Hub** | `app/src/main/java/.../ui/MainActivity.kt` | Mode-aware routing between Self & Parent onboarding and dashboards |
| **Domain Storage** | `app/src/main/java/.../behaviour/BehaviourRepository.kt` | Local Room v5 DAO operations for Goals, Triggers, and Policies |
| **Policy Resolution**| `app/src/main/java/.../behaviour/BehaviourPolicyResolver.kt` | In-memory evaluation guaranteeing parent rule precedence |
| **Test Suite** | `app/src/test/java/.../SelfModeEngineTest.kt` | 23 automated unit and regression tests |
