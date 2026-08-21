# Phase 4E-7: Permission Resilience & Truthful Communication

## Permission State Machine
- **Accessibility Service**: Required for foreground app detection and overlay push.
  - If revoked: App displays unambiguous **"PROTECTION OFF"** warning and deep-links to system accessibility settings.
  - If active: App displays **"PROTECTION ON"**.
- **Notification Permission**: Optional on Android 13+ (TIRAMISU).
  - If denied: Disables local scheduled notifications without degrading real-time enforcement or wallet operations.
