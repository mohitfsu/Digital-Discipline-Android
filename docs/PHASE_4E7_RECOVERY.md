# Phase 4E-7: Process Death, Reboot & Clock Tampering Recovery

## Monotonic Clock Protection
- Uses `SystemClock.elapsedRealtime()` exclusively for in-flight sessions and unlocks.
- Shifting device wall-clock forward or backward cannot artificially extend or grant session time.
- Device reboot resets uptime, causing active sessions to immediately expire (fail-closed security).

## Process Death Resilience
- Room entity transactions commit synchronously before UI dismissal.
- OS task termination during a challenge leaves in-flight events incomplete without awarding unwarranted wallet minutes.
- Deduplication prevents re-awarding First Win or milestones on application relaunch.
