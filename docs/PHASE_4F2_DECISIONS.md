# Phase 4F-2: Architectural Decision Records (ADRs)

## ADR-1: Standard Platform API Compatibility
- **Context**: Avoid fragile OEM-specific private API hooks or root-level circumventions.
- **Decision**: Restrict all background and enforcement operations strictly to official Android SDK APIs (`AccessibilityService`, `WindowManager`, `WorkManager`).
- **Consequences**: Safe, reliable Google Play Store compliance across all OEM families.
