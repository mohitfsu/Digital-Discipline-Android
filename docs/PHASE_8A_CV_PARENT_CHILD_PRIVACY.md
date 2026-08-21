# Phase 8A-CV: Parent / Child Privacy & Boundary Audit

## Parent Policy Authority
- `Parent BLOCK` and `Parent DELAY` execute strictly before intervention selection or camera activation.
- Camera validation cannot be used to bypass a parent boundary or unlock a blocked app.
- If a parent disables an intervention category (e.g. `MOVEMENT`), camera validation for that category is entirely inaccessible.

## Absolute Privacy from Parent Surveillance
- Parents configuring rules do **NOT** receive:
  - Video streams or photos
  - Pose tracking landmarks or body geometries
  - Exercise performance grades or form scores
- Device processing is strictly local to the child device.
