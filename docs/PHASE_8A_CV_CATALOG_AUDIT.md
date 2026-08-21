# Phase 8A-CV: Intervention Catalog Stability Audit

## Canonical Stable IDs
All 35 intervention definitions in `InterventionCatalog.kt` maintain strict ID stability across Room Version 9 adaptive aggregate persistence:

1. **Movement**: `PUSH_UPS`, `SQUATS`, `LUNGES`, `PLANK`, `JUMPING_JACKS`, `WALL_SIT`, `HIGH_KNEES`, `MOUNTAIN_CLIMBERS`, `CALF_RAISES`, `SHADOW_BOXING`.
2. **Upper Body**: `ARM_CIRCLES`, `SHOULDER_ROLLS`, `NECK_ROLLS`, `CHEST_OPENER`, `WRIST_STRETCH`.
3. **Breathing**: `BOX_BREATHING`, `FOUR_SEVEN_EIGHT`, `RESONANCE_BREATHING`, `PHYSIOLOGICAL_SIGH`, `TRIANGLE_BREATHING`.
4. **Meditation**: `ZEN_BREATH_COUNTING`, `SENSORY_GROUNDING`, `URGE_SURFACING`, `FOCUS_POINT`, `MINDFUL_PAUSE`.
5. **Yoga & Mobility**: `CAT_COW`, `CHILDS_POSE`, `FORWARD_FOLD`, `COBRA_STRETCH`, `SEATED_SPINAL_TWIST`.
6. **Physical Reset**: `COLD_WATER_SPLASH`, `WATER_HYDRATION`, `TWENTY_TWENTY_REST`, `FRESH_AIR_BREATHE`, `STEP_OUTSIDE`.
7. **Cognitive**: `MATH_CHALLENGE`, `MEMORY_SEQUENCE`, `TAP_SEQUENCE`, `REACTION_TEST`, `PATTERN_MATCH`, `QUICK_RECALL`.

---

## Zero Database Migrations
- **Room Version**: Remains **VERSION 9**.
- **Schema Compatibility**: 100% compatible with existing `adaptive_aggregates` and `intervention_outcomes` tables. 0 migrations required.
