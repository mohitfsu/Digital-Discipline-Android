# Phase 4F-2: AccessibilityService Compatibility & Resilience

## Service Configuration & Invariants
- `canRetrieveWindowContent = false` (Zero surveillance, no screen scraping).
- `eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`.
- **Automatic Service Re-binding**: If Android kills the service under high memory pressure, `onServiceConnected()` synchronously re-loads policies from Room without requiring user re-activation.
