# Phase 8A-CV: Computer Vision Architecture

## Flow Architecture

```
DISTRACTING APP LAUNCH (e.g. Instagram)
               ↓
AccessibilityService / UsageStatsInterception
               ↓
PolicyEngine (Parent BLOCK > Parent DELAY > SELF POLICY)
               ↓
InterventionEngine
               ↓
InterventionSelector (5-Factor Scoring, in-memory filtering)
               ↓
InterventionSession CREATED (Authoritative sessionId)
               ↓
ValidationMethodSelector
   ┌───────────┴───────────┐
   ▼                       ▼
Camera Pose Validator    Motion / Timer Validator (Fallback)
   ↓                       ↓
ImageAnalysis (30 FPS)   SensorManager / Clock
   ↓
ML Kit Pose Detection
   ↓
ExercisePoseClassifier
   ↓
ValidationResult.Completed(session)
               ↓
InterventionEngine.onSessionCompleted(session)
               ↓
EarnedTimeWalletService.earnTime(idempotencyKey = "earn_intervention_${sessionId}")
               ↓
Adaptive Outcome Recording (InterventionAdaptiveStore / Room V9)
               ↓
Optional Helpfulness Feedback Dialog
```

## Absolute Invariants Preserved
1. **Parent Policy Precedence**: `Parent BLOCK` > `Parent DELAY` > `Self Policy` > `Intervention Selection`.
2. **Single Wallet Authority**: `EarnedTimeWalletService` is the sole entity allowed to credit balances, keyed idempotently by `earn_intervention_${sessionId}`.
3. **Single Authoritative Session Identity**: `sessionId` is created at session preparation and flows through validation, completion, wallet crediting, and adaptive learning without mutation.
