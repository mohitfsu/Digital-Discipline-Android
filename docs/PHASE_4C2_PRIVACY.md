# Phase 4C-2: Privacy Guarantees & Surveillance Prevention

## 1. Zero Surveillance Commitment
Phase 4C-2 maintains strict adherence to the project's foundational privacy charter.

The adaptive engine and personalization systems:
- **DO NOT** read message contents or notifications.
- **DO NOT** record keystrokes or touch coordinates.
- **DO NOT** capture screenshots or screen recordings.
- **DO NOT** inspect URLs or web browsing history.
- **DO NOT** access the camera or microphone.
- **DO NOT** send any behavioural profile to external servers or cloud analytics.

---

## 2. Permitted Telemetry (On-Device Only)
The only data processed by `AdaptivePlanEngine` is locally aggregated behavioral metadata:
- Package name of monitored distraction application.
- Timestamp and duration of intervention.
- Completion outcome (`COMPLETED`, `EXITED`).
- Rapid reopen indicator (return within 5 minutes).
- Screen-time earned and consumed seconds in the local wallet.
