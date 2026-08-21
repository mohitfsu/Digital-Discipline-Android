# Phase 8A-CV: Camera Lifecycle & Resource Management

## Lifecycle State Diagram

```
                 [ CAMERA OFF ]
                       │
       User explicitly taps "Start Camera"
                       │
                       ▼
                 [ CAMERA ON ]
          CameraX Provider bound
          ImageAnalysis listening at 30 FPS
          PoseDetector active in memory
                       │
     ┌─────────────────┴─────────────────┐
     │ Shutdown Trigger:                 │
     │ 1. Intervention completed         │
     │ 2. User presses close ("X")       │
     │ 3. Activity/Overlay onPause/onStop│
     │ 4. Screen locked / backgrounded   │
     │ 5. Session timeout / expired      │
     │ 6. Permission revoked             │
     └─────────────────┬─────────────────┘
                       │
                       ▼
            [ SHUTDOWN SEQUENCE ]
          1. cameraProvider.unbindAll()
          2. analyzer.close()
          3. poseDetector.close()
          4. cameraExecutor.shutdown()
          5. validator.stopValidation()
                       │
                       ▼
                 [ CAMERA OFF ]
```

## Verified Shutdown Conditions
- **Lifecycle Event Observer**: In `CameraPoseWorkoutScreen.kt`, an observer listens to `Lifecycle.Event.ON_PAUSE` and `Lifecycle.Event.ON_STOP`, immediately executing `cameraProvider.unbindAll()` and `analyzer.close()`.
- **Compose Disposal**: `DisposableEffect` guarantees that when the Composable leaves the composition tree, all CameraX bindings are released.
- **Validator Termination**: `CameraPoseValidator.stopValidation()` terminates the pose pipeline and drops any lingering callbacks.
