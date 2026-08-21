# Phase 8A-CV: Audit Findings & Remediations

## Pre-Implementation Audit Findings & Remediations

| Finding ID | Severity | Description | Remediation |
| :--- | :--- | :--- | :--- |
| **FINDING-01** | **P1 (High)** | Direct completion callback in workout screen bypassed `InterventionValidator` and `InterventionEngine`. | Refactored `CameraPoseWorkoutScreen` to bind directly to `CameraPoseValidator` and the authoritative `InterventionSession`. |
| **FINDING-02** | **P1 (High)** | Lack of lifecycle shutdown observer in Camera preview could lead to CameraX / ML Kit holding hardware resources on app background. | Added `LifecycleEventObserver` listening to `ON_PAUSE` / `ON_STOP` to immediately invoke `cameraProvider.unbindAll()` and `analyzer.close()`. |
| **FINDING-03** | **P2 (Medium)** | Multi-capability fallback structure was implicit rather than formal. | Introduced `ValidationCapability` enum, `supportedCapabilities` on `InterventionDefinition`, and `ValidationMethodSelector`. |
| **FINDING-04** | **P3 (Low)** | Landmark confidence was not explicitly gated before calculating 3D angles. | Added `PoseAngleCalculator.isConfident` checking `inFrameLikelihood >= 0.5f`. |
