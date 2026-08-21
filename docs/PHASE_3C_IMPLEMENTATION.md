# Phase 3C — Behaviour Analytics & Intervention Intelligence Implementation

## 1. Executive Summary
Phase 3C establishes the behavioral measurement and deterministic habit intelligence layer for Digital Discipline. It captures local behavioral friction metrics (habit interruption rates, rapid reopen patterns, completion vs abandonment rates) using an asynchronous, zero-latency local pipeline (`Dispatchers.IO`) without compromising real-time AccessibilityService enforcement.

---

## 2. Core Architectural Tenets

```
┌────────────────────────────────────────────────────────────────────────┐
│                   REAL-TIME ENFORCEMENT LOOP (LOCAL & SYNC)            │
│  Accessibility Event (~14ms) ──► PolicyEngine (<3ms) ──► Overlay (<48ms)│
└────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ Non-Blocking Dispatch (Dispatchers.IO)
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   LOCAL BEHAVIOURAL TELEMETRY (ROOM v4)                │
│  • InterventionEventEntity (Raw events: 1m/5m/15m reopen flags)        │
│  • DailyUsageEntity (Local rollups: attempts, HIR, earned minutes)     │
└────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ WorkManager 24h Aggregated Sync (1 write/day)
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     FIRESTORE DAILY ROLLUP SUMMARY                     │
│  • Document: /families/{famId}/daily_summaries/{childId_yyyy-MM-dd}    │
│  • Consumed by Next.js Parent Web Dashboard (/insights)                │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Zero-Latency Guarantee**: Real-time enforcement is 100% local and deterministic. Analytics operations are executed on background coroutine dispatchers and never delay window detection or overlay attachment.
2. **Deterministic Habit Interruption**: Habit Interruption Rate (HIR) is computed strictly as the percentage of intervention attempts that do not result in reopening the target application within 5 minutes.
3. **Idempotent Single-Write Sync**: Aggregated daily rollups are uploaded once per 24 hours per device using atomic Firestore document IDs (`${childId}_${dateString}`).

---

## 3. Subsystem File Index

| Subsystem | File Path | Responsibilities |
| :--- | :--- | :--- |
| **Event Model** | `app/src/main/java/.../data/local/entities/InterventionEventEntity.kt` | Room entity storing intervention outcomes and 1m/5m/15m reopen flags |
| **Daily Usage Entity** | `app/src/main/java/.../data/local/entities/DailyUsageEntity.kt` | Daily rollup entity storing aggregated HIR, attempts, and minutes |
| **Analytics Repository** | `app/src/main/java/.../analytics/LocalAnalyticsRepository.kt` | Rapid reopen detector and HIR calculation engine |
| **Recommendation Engine**| `app/src/main/java/.../ai/InterventionRecommendationEngine.kt` | Rule-based advisory analyzer evaluating 5-minute reopen differentials |
| **Daily Upload Worker** | `app/src/main/java/.../sync/DailyAnalyticsUploadWorker.kt` | WorkManager task for idempotent daily rollup upload to Firestore |
| **Parent Web Route** | `web/src/app/insights/page.tsx` | Next.js 15 Behaviour Insights dashboard route |
| **Sidebar Navigation** | `web/src/components/Sidebar.tsx` | Linked navigation item for parent web control center |
