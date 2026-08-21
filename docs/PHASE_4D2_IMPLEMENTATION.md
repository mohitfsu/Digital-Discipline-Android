# Phase 4D-2: Daily Habit Actions & Progress — Implementation Guide

## 1. Executive Summary
Phase 4D-2 transforms Self Mode's `TodayScreen` into an **action-oriented daily habit system**. It introduces the deterministic `DailyActionPlanner` and interactive `DailyActionScreen`, establishing the self-regulation loop:
$$\text{GOAL} \longrightarrow \text{SMALL ACTION} \longrightarrow \text{COMPLETION} \longrightarrow \text{REWARD} \longrightarrow \text{PROGRESS} \longrightarrow \text{CONSISTENCY}$$

---

## 2. Architecture & Component Inventory

### A. Core Components
1. [`DailyActionPlanner.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/behaviour/planner/DailyActionPlanner.kt):
   - Deterministically calculates action segments and the primary `nextAction` CTA.
   - Handles multiple goal categories (Fitness, Study, Reading, Mindfulness) and units.
   - Pure function, offline-first, execution latency $<5\text{ms}$.
2. [`DailyActionScreen.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/ui/dashboard/DailyActionScreen.kt):
   - Interactive execution view with rep counters, breathing animation, and focus timers.
   - Completion dialog providing explicit user choice: `[ USE NOW ]` (starts wallet session) or `[ SAVE FOR LATER ]` (returns to TodayScreen).
3. [`TodayScreen.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/ui/dashboard/TodayScreen.kt):
   - Redesigned with prominent **NEXT ACTION** card as primary CTA.
   - Displays real-time goal progress bar, 7-day consistency metric, today's actions timeline, and end-of-day reflection.

---

## 3. Enforcement Path Isolation
The real-time app blocking path remains completely isolated:
$$\text{AccessibilityService} \longrightarrow \text{PolicyEngine} \longrightarrow \text{BehaviourPolicyResolver} \longrightarrow \text{OverlayManager}$$
- App interception latency remains $<1\text{ms}$.
- Parent Mode rules (`BLOCK`, `DELAY`, `ALLOW`, schedules) strictly supersede all Self Mode daily actions and wallet sessions.
