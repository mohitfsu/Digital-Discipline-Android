# Phase 4F-3: Android Component Security

## Component Declaration Review
- **`MainActivity`**: Exported, protected launcher entry point.
- **`DigitalDisciplineAccessibilityService`**: Unexported, bound by system only.
- **`OverlayActivity`**: Unexported, internal view context.
- **Deep Links**: `digitaldiscipline://today` navigates only to safe, authenticated UI states and cannot mutate goals, plans, or wallet balances directly.
