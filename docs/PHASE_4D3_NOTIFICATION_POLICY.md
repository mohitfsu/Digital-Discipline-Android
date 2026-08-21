# Phase 4D-3: Notification Policy & Decision Engine

## Overview
Phase 4D-3 defines a deterministic, non-manipulative notification policy framework for Self Mode.

## Notification Types & Policies

### 1. MORNING_INTENTION
- **Purpose**: Set positive daily focus before potential distraction sessions.
- **Trigger Window**: 06:00 – 10:00 local time.
- **Suppression Conditions**: Goal already completed, already sent today, outside morning window.
- **Limits**: Max 1/day.

### 2. NEXT_ACTION
- **Purpose**: Actionable reminder for the immediate next micro-habit.
- **Trigger Window**: 08:00 – 20:00 local time.
- **Suppression Conditions**: Goal completed, no pending actions, already sent today, outside time window.
- **Limits**: Max 1/day, subject to daily behaviour reminder cap.

### 3. DISTRACTION_PREEMPTION
- **Purpose**: Preemptively intervene before historical peak distraction hours.
- **Trigger Window**: 1 hour preceding peak distraction hour.
- **Data Threshold**: Minimum 3 recorded distraction data points in past 14 days.
- **Suppression Conditions**: Insufficient historical data (<3), peak hour absent, goal already completed, already sent today.
- **Limits**: Max 1/day.

### 4. MISSED_ACTION
- **Purpose**: Supportive late-day nudge for incomplete habits (never shame-based).
- **Trigger Window**: 17:00 – 21:00 local time.
- **Suppression Conditions**: Early in day (<17:00), goal completed, already sent today.
- **Limits**: Max 1/day.

### 5. SUCCESS
- **Purpose**: Positive reinforcement when daily goal is reached.
- **Trigger Window**: Upon daily goal target completion.
- **Suppression Conditions**: Goal not complete, already sent today.
- **Limits**: Max 1/day.

### 6. EVENING_REFLECTION
- **Purpose**: Prompt daily review/reflection in TodayScreen.
- **Trigger Window**: 19:00 – 23:00 local time.
- **Suppression Conditions**: Reflection already recorded for today, outside evening window, already sent today.
- **Limits**: Max 1/day.

### 7. WEEKLY_REVIEW
- **Purpose**: Notify when weekly review snapshot is available.
- **Trigger Window**: Once every 7 days when review is due.
- **Suppression Conditions**: Review generated within last 7 days, already sent this week.
- **Limits**: Max 1/week.
