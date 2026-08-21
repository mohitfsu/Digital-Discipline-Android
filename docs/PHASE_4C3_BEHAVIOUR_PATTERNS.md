# Phase 4C-3: Behaviour Patterns Engine

## 1. Overview
The [`BehaviourPatternEngine`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/behaviour/intelligence/BehaviourPatternEngine.kt) analyzes on-device telemetry to detect deterministic habit patterns across time, monitored apps, interventions, and earned time consumption.

---

## 2. Pattern Analysis Dimensions

### A. Time Patterns
- **Peak Hour Detection**: Hourly frequency histogram over past 7–14 days.
- **2-Hour Distraction Window**: 12 sliding 2-hour buckets ($00\text{--}02, 02\text{--}04, \dots, 22\text{--}24$) to pinpoint primary habit vulnerability.
- **Day of Week Comparison**: Weekday (Mon–Fri) vs Weekend (Sat–Sun) attempt distributions.
- **Time-of-Day Buckets**:
  - Morning: 06:00 – 11:59
  - Afternoon: 12:00 – 17:59
  - Evening: 18:00 – 21:59
  - Night: 22:00 – 05:59

### B. Monitored App Rankings
Ranked by:
1. Total intervention attempts
2. Successfully interrupted habit loops (completed challenge without 5-minute rapid reopen)
3. Exits & abandoned sessions
4. Habit Interruption Rate ($\text{HIR} = \frac{\text{attempts} - \text{rapid reopens}}{\text{attempts}} \times 100$)
5. Total earned screen-time generated

### C. Intervention Performance Comparison
- Evaluates individual replacement behaviors (Squats, Box Breathing, Mindful Pause, Study Blocks).
- Ranks interventions by Habit Interruption Rate (HIR) once a minimum threshold of $\ge 10$ trials is reached.

### D. Wallet Consumption Patterns
- Tracks total earned seconds, consumed seconds, average session length, and reward-to-consumption ratio.
- Flags rapid session chaining when $\ge 85\%$ of earned time is consumed immediately across $\ge 5$ sessions.
