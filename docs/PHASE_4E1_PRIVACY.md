# Phase 4E-1: Privacy & Zero Surveillance Guarantees

## Onboarding Privacy Boundaries
- **Zero Cloud Requirement**: The entire onboarding and plan creation process runs 100% locally on the device.
- **No Keystroke / Text Logging**: Text entered in custom goal dialogs is saved only to the local SQLite database.
- **No App Usage Scanning for Marketing**: Only selected distraction apps are added to `TriggerEntity`. No background app cataloging or cloud telemetry is performed during onboarding.
- **No Surveillance Permissions**: Camera, microphone, location, contacts, and browsing history are never requested.
