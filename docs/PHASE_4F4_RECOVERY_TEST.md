# Phase 4F-4: Failure & Recovery Testing

## Fault Injection Resilience
- **OS Process Death**: In-flight transactions complete or discard cleanly without ledger balance drift.
- **Device Reboot**: Monotonic elapsed-time resets active session immediately (fail-closed security).
- **Service Disablement**: Re-opening app displays truthful "PROTECTION OFF" banner and guidance.
