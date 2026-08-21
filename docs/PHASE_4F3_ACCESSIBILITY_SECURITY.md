# Phase 4F-3: AccessibilityService Security Audit

## Service Isolation & Boundary
- Event listening limited strictly to `TYPE_WINDOW_STATE_CHANGED`.
- No window nodes or hierarchy text inspected.
- Real-time enforcement path isolated from disk, network, and heavy allocations.
- Automatic recovery on memory pressure without security bypass.
