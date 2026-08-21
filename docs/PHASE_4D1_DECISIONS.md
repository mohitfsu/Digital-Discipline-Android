# Phase 4D-1: Architectural Decision Records (ADRs)

## ADR-1: Primary Landing Experience as `TodayScreen`
- **Context**: Self Mode users need immediate (<5s) clarity on daily progress and actionable focus without wading through dense dashboards.
- **Decision**: Route `UserMode.SELF` directly to `TodayScreen.kt` in `MainActivity.kt`, while retaining sub-navigation to Plan, Insights, and Weekly Review.
- **Consequences**: Fast daily feedback loop, zero visual clutter, seamless backwards compatibility.

---

## ADR-2: Persistence of Daily Reflection via DataStore
- **Context**: Daily reflection requires storing the last reflection date, mood, and helper choice once per day.
- **Decision**: Store reflection properties in `PreferencesManager` (DataStore) instead of creating a new Room table.
- **Consequences**: Avoids unnecessary Room database migrations (database remains stable at v8), guarantees fast access, and maintains offline-first safety.

---

## ADR-3: Isolation of TodayScreen from Enforcement Path
- **Context**: Real-time app blocking must never experience latency regression.
- **Decision**: `TodayScreen.kt` operates purely as a presentation and local aggregation layer; it never alters or hooks into `AccessibilityService` or `PolicyEngine`.
- **Consequences**: Strict performance isolation; app interception latency remains $<1\text{ms}$.
