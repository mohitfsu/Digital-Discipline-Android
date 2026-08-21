# Phase 4E-6: Long-Term Self Mode Continuity & Personal Behaviour Timeline — Implementation

## Mission Overview
Phase 4E-6 delivers the long-term continuity layer for Self Mode ("MY JOURNEY"), transforming discrete daily events into a private, local, chronological record of meaningful behaviour change.

## Architecture Components

### 1. `JourneyEventModel.kt`
- **Location**: `app/src/main/java/com/digitaldiscipline/spike/behaviour/journey/JourneyEventModel.kt`
- **Role**: Defines `JourneyEventType`, `JourneyEvent`, `PersonalPatternInsight`, `JourneyLongTermSummary`, and `BehaviourJourneySnapshot`.

### 2. `BehaviourJourneyEngine.kt`
- **Location**: `app/src/main/java/com/digitaldiscipline/spike/behaviour/journey/BehaviourJourneyEngine.kt`
- **Role**: Deterministic, off-path timeline synthesizer executing in `<10ms` (steady-state ~0.2ms).

### 3. UI Components
- **`SelfJourneyScreen.kt`**: Full journey screen with header, current chapter, long-term summary, learnings, timeline, and current direction.
- **`TodayScreen.kt`**: Compact "MY JOURNEY" entry card deep-linking to journey timeline.
- **`MainActivity.kt`**: Navigation route `"JOURNEY"`.
