# Phase 8A-CV: Camera Privacy Contract & Audit

## Official Privacy Principle
> "Digital Discipline never activates the camera for monitoring or surveillance. Camera access occurs only when the user explicitly starts a camera-validated intervention. Camera frames are processed transiently on-device and are never stored, recorded, uploaded, or transmitted."

---

## Technical Privacy Guarantees

### 1. Zero Background Processing
- The camera is NEVER activated by `AccessibilityService` or when a distracting app is launched.
- The camera starts ONLY when the user explicitly taps **"START CAMERA POSE TRACKING"** or chooses the camera mode.

### 2. Transient Memory-Only Execution
- Frames received by CameraX (`ImageAnalysis.Analyzer`) are converted to in-memory `InputImage` objects.
- Frames are analyzed by Google ML Kit Pose Detector to extract 33 dimensionless geometric landmarks.
- Upon completion of analysis, `imageProxy.close()` is invoked immediately, recycling the memory buffer.

### 3. Absolute Persistence Prohibition
- 0 image files written to disk.
- 0 video files written to disk.
- 0 raw pixel arrays cached in memory.
- 0 network requests containing camera or pose metadata.
- 0 biometric profiles or facial recognition data created.

### 4. Parent Privacy Boundary
- Parents configuring boundaries NEVER receive camera video, screenshots, or pose landmarks from the child device.
- All computer vision processing is isolated to the child's local device hardware.
