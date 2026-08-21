# Phase 4C-3: Behaviour Intelligence & Goal Coaching — Implementation Guide

## 1. Executive Summary
Phase 4C-3 elevates Digital Discipline from an adaptive screen-time intervention tool into a comprehensive personal behaviour intelligence and coaching system. It computes explainable, deterministic metrics (Behaviour Momentum, Goal Integrity, Time/App/Intervention patterns, and Weekly Intelligence) and introduces an explicit user-driven Behaviour Experimentation framework.

---

## 2. Architecture & Path Isolation Invariant

### A. Real-Time Enforcement Path (Untouched & Local)
$$\text{AccessibilityService} \longrightarrow \text{BehaviourPolicyResolver} \longrightarrow \text{PolicyEngine} \longrightarrow \text{OverlayManager}$$
- **Zero Overhead**: Does not call intelligence engines or aggregation routines.
- Policy resolution remains $<1\text{ms}$.

### B. Behaviour Intelligence Path (Off-Path / Asynchronous)
$$\text{Local Telemetry} \longrightarrow \text{Pattern \& Momentum Engines} \longrightarrow \text{Goal Integrity} \longrightarrow \text{Weekly Review / Experiments} \longrightarrow \text{User Decision}$$
- Runs entirely on-device during dashboard visits or background workers.

---

## 3. Core Engine Components
1. [`BehaviourPatternEngine.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/behaviour/intelligence/BehaviourPatternEngine.kt):
   - Computes Time Patterns (peak hour, 2-hour window, weekday vs weekend, time-of-day buckets).
   - Computes Monitored App Rankings (attempts, completions, exits, reopens, HIR %).
   - Computes Intervention Rankings (interruption rate by replacement behavior).
   - Computes Goal & Wallet Patterns (consistency, session averages, rapid consumption).
2. [`BehaviourMomentumEngine.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/behaviour/intelligence/BehaviourMomentumEngine.kt):
   - Deterministic 0–100 score across 7 normalized components.
   - States: `STRONG_MOMENTUM` (90–100), `BUILDING_MOMENTUM` (75–89), `INCONSISTENT` (50–74), `NEEDS_RESET` (25–49), `NEEDS_ATTENTION` (0–24), `INSUFFICIENT_DATA` (<10 attempts).
3. [`GoalIntegrityEngine.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/behaviour/intelligence/GoalIntegrityEngine.kt):
   - Answers: "How closely is my digital behaviour aligned with the goal I chose?"
   - Evaluates Target Consistency (35%), Interruption Rate (35%), Challenge Completion (15%), and Rapid Reopen Control (15%).
   - Generates Goal $\rightarrow$ Distraction $\rightarrow$ Intervention relationship mapping without claiming causality.
4. [`BehaviourWeeklyIntelligenceEngine.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/behaviour/intelligence/BehaviourWeeklyIntelligenceEngine.kt):
   - Generates 7-day deep summaries: Strongest day, biggest distraction share, what worked, vulnerable window, biggest win, and next-week focus.
5. [`ExperimentRepository.kt`](file:///d:/Zidd/app/src/main/java/com/digitaldiscipline/spike/behaviour/intelligence/ExperimentRepository.kt):
   - Provides lifecycle management for structured 7-day behaviour experiments (`DRAFT`, `ACTIVE`, `COMPLETED`, `CANCELLED`).
