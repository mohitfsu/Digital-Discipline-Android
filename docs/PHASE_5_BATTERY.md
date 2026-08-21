# PHASE 5 — BATTERY & SENSOR HYGIENE
## Zero Background Sensor Drain Guarantee

---

## 1. Battery Invariants
- **No Background Sensor Listeners**: Accelerometer & gyroscope are strictly dormant when no intervention is active.
- **Immediate Unregistration**: Listeners are unregistered immediately upon session completion, cancellation, failure, or window detachment.
- **No Wakelocks**: Interventions run in normal interactive window lifecycle.
