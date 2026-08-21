# Phase 4F-2: Cross-Device Compatibility Test Plan

## 15-Scenario Compatibility Matrix (`AndroidCompatibilityTest.kt`)
1. Android 14 API 34 target compatibility verified
2. Android 15 API 35 foreground service and notification changes handled
3. Android 16 API 36 predictive back and window insets compatibility
4. AccessibilityService handles rapid window state changes without memory spikes
5. TYPE_APPLICATION_OVERLAY configuration is valid for Android 14+
6. Multi-window and split-screen mode triggers window detection
7. Font scaling up to 200% preserves UI component readability
8. Display density scaling from low to xxxhdpi does not crash layouts
9. Completely offline execution produces full journey snapshot
10. Zero network calls guaranteed across all core engines
11. Parent Mode BLOCK rule has absolute precedence over all device states
12. Wallet authority remains single and tamper-proof across device configurations
13. Monotonic clock protection remains fail-closed across reboots
14. Device latency benchmark executes under 10ms target
15. Room database strictly preserved at Version 8
