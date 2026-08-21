# Phase 4E-1: Activation Architecture & Engine Isolation

## Architecture Pipeline

```
FirstRunExperience (UI Compose)
        ↓
SelfModeActivationCoordinator (Validation, Mutex, Atomic Scope)
        ↓
BehaviourPlanCreator (Draft -> Plan Entities)
        ↓
BehaviourRepository (Room Goal, Trigger, Policy entities)
        ↓
EarnedTimeWalletService (Wallet Initialisation & Authoritative Caps)
        ↓
PreferencesManager (DataStore Mode & Onboarding State)
        ↓
TodayScreen (Primary Dashboard Destination)
```

## Non-Interference with Real-Time Enforcement Path

The real-time enforcement loop:
```
AccessibilityService
        ↓
PolicyEngine
        ↓
BehaviourPolicyResolver
        ↓
OverlayManager
```

**Invariant**: The activation coordinator runs strictly on worker/UI dispatchers during initial setup. It does NOT execute inside the window state detection or blocking path. The enforcement path remains completely untouched.
