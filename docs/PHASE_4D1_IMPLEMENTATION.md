# Phase 4D-1: Daily Habit Experience — Implementation Guide

## 1. Executive Summary
Phase 4D-1 establishes the Daily Habit Experience (`TodayScreen.kt`) as the primary landing page for Self Mode. It provides immediate, calm, and motivating feedback answering what the user's goal is today, how they are progressing, their available earned screen time, distraction patterns, recent wins, and a lightweight end-of-day reflection.

---

## 2. Architecture & Path Isolation Invariant

### A. Real-Time Enforcement Path (Unchanged)
$$\text{AccessibilityService} \longrightarrow \text{PolicyEngine} \longrightarrow \text{BehaviourPolicyResolver} \longrightarrow \text{OverlayManager}$$
- **Zero Overhead**: Does not call intelligence engines or aggregation routines.
- Policy resolution remains $<1\text{ms}$.

### B. Daily Habit Experience Path (Off-Path / Compose Layer)
$$\text{DataStore / Room v8} \longrightarrow \text{TodayScreen} \longrightarrow \text{User Interaction} \longrightarrow \text{Daily Reflection}$$
- Local-first rendering with $<100\text{ms}$ aggregation time.
- No network requests on screen display.

---

## 3. Component Hierarchy
1. [`TodayScreen.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/ui/dashboard/TodayScreen.kt):
   - **Header**: Time-based greeting, date, top navigation pills (Insights, Plan, Parent).
   - **Primary Goal Card**: Category icon, daily progress bar, weekly consistency indicator.
   - **One Thing to Focus On**: Deterministic primary coaching card.
   - **Earned Time Wallet Card**: Available screen time, active session indicator, earn trigger.
   - **Today's Behaviour Card**: Interrupted count, completion count, HIR %.
   - **Distraction Pattern**: Peak 2-hour window and top distraction app.
   - **Recent Wins**: Live victory history.
   - **Daily Reflection & Summary**: 3-option mood assessment, helper choice, and daily victory summary.
2. [`PreferencesManager.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/data/preferences/PreferencesManager.kt):
   - Persists `KEY_LAST_REFLECTION_DATE`, `KEY_LAST_REFLECTION_MOOD`, `KEY_LAST_REFLECTION_HELPED` via DataStore.
3. [`MainActivity.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/ui/MainActivity.kt):
   - Routes `UserMode.SELF` directly to `TodayScreen`, providing sub-navigation to Plan, Insights, and Weekly Review.
