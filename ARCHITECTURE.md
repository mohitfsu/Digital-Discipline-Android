# Digital Discipline — System Architecture Master Document

**Product**: Digital Discipline (Local-First Parental Control & Screen-Time Intervention Platform)  
**Architecture Baseline**: Phase 1 Production Foundation  
**Tech Stack**: Android (Kotlin, Jetpack Compose, Room, DataStore, AccessibilityService) | Web (Next.js, TypeScript) | Backend (Firebase Auth, Cloud Firestore)  

---

## 1. System Topology

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                   CHILD ANDROID DEVICE                                  │
│                                                                                         │
│  ┌────────────────────────┐         ┌────────────────────────┐                          │
│  │   Android OS Window    │         │  AccessibilityService  │                          │
│  │   Manager Event Push   │────────►│  (TYPE_WINDOW_STATE)   │                          │
│  └────────────────────────┘         └───────────┬────────────┘                          │
│                                                 │ AppLaunchEvent (~58ms)                │
│                                                 ▼                                       │
│                                     ┌────────────────────────┐                          │
│                                     │      PolicyEngine      │                          │
│                                     └───────────┬────────────┘                          │
│                                                 │                                       │
│                    ┌────────────────────────────┴───────────────────────────┐           │
│                    │                                                        │           │
│                    ▼                                                        ▼           │
│       ┌─────────────────────────┐                              ┌─────────────────────────┐
│       │    PolicyRepository     │                              │     OverlayManager      │
│       │  (Room Local Database)  │                              │  (ComposeView Overlay)  │
│       └────────────┬────────────┘                              └─────────────────────────┘
│                    │                                                                    │
│                    ├─► AppRuleEntity (BLOCK / DELAY / EARN / ALLOW)                     │
│                    ├─► TemporaryUnlockEntity (Monotonic SystemClock.elapsedRealtime)    │
│                    ├─► ScheduleEntity (Time & Day Rules)                                │
│                    └─► DailyUsageEntity (On-device Aggregation)                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 │ WorkManager Nightly Sync
                                                 ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                               FIREBASE CLOUD INFRASTRUCTURE                             │
│   • Firebase Authentication (Parent Accounts)   • Cloud Firestore (Summaries & Policy)  │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Core Subsystems

1. **Detection Engine**: Real-time push events via `DigitalDisciplineAccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`, `canRetrieveWindowContent=false`).
2. **Overlay & Intervention**: `WindowManager` (`TYPE_APPLICATION_OVERLAY`) rendering Jetpack Compose `ComposeView` with Mindful Pause (10s), Box Breathing (16s), Squat Challenge, and Parent PIN override.
3. **Local Database & Behavioural Telemetry**: Room database v5 storing `app_rules`, `temporary_unlocks`, `schedules`, `daily_usage`, `intervention_events`, `protection_state`, `diagnostic_events`, `goals`, `triggers`, `replacement_behaviours`, `behaviour_policies`, and `goal_progress`.
4. **Security & Cryptography**: PBKDF2WithHmacSHA256 hashed Parent PIN with EncryptedSharedPreferences (Android Keystore AES-256 GCM) and rate-limiting lockout.
5. **Monotonic Realtime Protection**: `SystemClock.elapsedRealtime()` eliminates clock manipulation attacks.
6. **Behaviour & Goal Engine**: Dual-mode (`PARENT` vs `SELF`) domain architecture with deterministic `BehaviourPolicyResolver` executing with 100% Parent Mode precedence.

- [`docs/PHASE_4E2_IMPLEMENTATION.md`](docs/PHASE_4E2_IMPLEMENTATION.md) — Phase 4E-2 Self Mode First Win & Habit Formation Loop Architecture and Implementation.
- [`docs/PHASE_4E2_FIRST_WIN_UX.md`](docs/PHASE_4E2_FIRST_WIN_UX.md) — First-Win UX principles, supportive tone, and copy matrix.
- [`docs/PHASE_4E3_IMPLEMENTATION.md`](docs/PHASE_4E3_IMPLEMENTATION.md) — Phase 4E-3 Habit Momentum & 7-Day Formation Loop Architecture and Implementation.
- [`docs/PHASE_4E4_IMPLEMENTATION.md`](docs/PHASE_4E4_IMPLEMENTATION.md) — Phase 4E-4 Personal Habit Plan Refinement & Long-Term Continuity Architecture and Implementation.
- [`docs/PHASE_4E5_IMPLEMENTATION.md`](docs/PHASE_4E5_IMPLEMENTATION.md) — Phase 4E-5 Personal Behaviour Evolution & Goal Lifecycle Architecture and Implementation.
- [`docs/PHASE_4E5_GOAL_LIFECYCLE.md`](docs/PHASE_4E5_GOAL_LIFECYCLE.md) — Goal lifecycle state machine and deterministic transition matrix.
- [`docs/PHASE_4E5_PAUSE_RESUME.md`](docs/PHASE_4E5_PAUSE_RESUME.md) — Pause and resume semantics and policy suspension.
- [`docs/PHASE_4E5_GOAL_COMPLETION.md`](docs/PHASE_4E5_GOAL_COMPLETION.md) — Intentional goal completion and history archiving.
- [`docs/PHASE_4E5_GOAL_REPLACEMENT.md`](docs/PHASE_4E5_GOAL_REPLACEMENT.md) — Goal replacement with before/after consequence previews.
- [`docs/PHASE_4E5_START_FRESH.md`](docs/PHASE_4E5_START_FRESH.md) — Start fresh baseline plan semantics.
- [`docs/PHASE_4E5_GOAL_HISTORY.md`](docs/PHASE_4E5_GOAL_HISTORY.md) — Read-only goal history timeline and detail viewer.
- [`docs/PHASE_4E5_WALLET_INTEGRATION.md`](docs/PHASE_4E5_WALLET_INTEGRATION.md) — Global wallet ledger invariants during lifecycle transitions.
- [`docs/PHASE_4E5_NOTIFICATION_INTEGRATION.md`](docs/PHASE_4E5_NOTIFICATION_INTEGRATION.md) — Goal lifecycle notification suppression rules.
- [`docs/PHASE_4E5_DATA_MODEL.md`](docs/PHASE_4E5_DATA_MODEL.md) — DataStore and Room v8 storage models.
- [`docs/PHASE_4E5_PRIVACY.md`](docs/PHASE_4E5_PRIVACY.md) — Local-first zero-surveillance privacy boundaries.
- [`docs/PHASE_4E5_UX.md`](docs/PHASE_4E5_UX.md) — Supportive calm copy principles.
- [`docs/PHASE_4E5_TEST_PLAN.md`](docs/PHASE_4E5_TEST_PLAN.md) — 45-scenario goal lifecycle test plan.
- [`docs/PHASE_4E5_TEST_RESULTS.md`](docs/PHASE_4E5_TEST_RESULTS.md) — Automated test results and execution latencies.
- [`docs/PHASE_4E5_DECISIONS.md`](docs/PHASE_4E5_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4E3_HABIT_MOMENTUM.md`](docs/PHASE_4E3_HABIT_MOMENTUM.md) — Bounded 0–100 Habit Momentum score formula, tiers, and non-streak invariants.
- [`docs/PHASE_4E3_7_DAY_MODEL.md`](docs/PHASE_4E3_7_DAY_MODEL.md) — Rolling 7-day formation model, day states, and deterministic evaluation.
- [`docs/PHASE_4E3_RECOVERY.md`](docs/PHASE_4E3_RECOVERY.md) — Recovery detection, resilience mechanics, and supportive framing.
- [`docs/PHASE_4E3_MILESTONES.md`](docs/PHASE_4E3_MILESTONES.md) — 4 calm milestones and weekly retrospective review.
- [`docs/PHASE_4E3_UX.md`](docs/PHASE_4E3_UX.md) — Anti-gamification design principles, calm UX hierarchy, and HabitMomentumScreen.
- [`docs/PHASE_4E3_NOTIFICATIONS.md`](docs/PHASE_4E3_NOTIFICATIONS.md) — Smart notification integration and quiet day reminder policy.
- [`docs/PHASE_4E3_PRIVACY.md`](docs/PHASE_4E3_PRIVACY.md) — Zero surveillance guarantees and on-device telemetry boundaries.
- [`docs/PHASE_4E3_DATA_MODEL.md`](docs/PHASE_4E3_DATA_MODEL.md) — Room v8 preservation and zero database schema churn.
- [`docs/PHASE_4E3_TEST_PLAN.md`](docs/PHASE_4E3_TEST_PLAN.md) — 40-scenario Phase 4E-3 test verification plan.
- [`docs/PHASE_4E3_TEST_RESULTS.md`](docs/PHASE_4E3_TEST_RESULTS.md) — Automated test results (40/40 passed, 448 total).
- [`docs/PHASE_4E3_DECISIONS.md`](docs/PHASE_4E3_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4E2_IMPLEMENTATION.md`](docs/PHASE_4E2_IMPLEMENTATION.md) — Phase 4E-2 Self Mode First Win & Habit Formation Loop Architecture and Implementation.
- [`docs/PHASE_4E2_FIRST_WIN_MODEL.md`](docs/PHASE_4E2_FIRST_WIN_MODEL.md) — First-Win definitions, meaningful interruption criteria, and non-gamified philosophy.
- [`docs/PHASE_4E2_STATE_MACHINE.md`](docs/PHASE_4E2_STATE_MACHINE.md) — 10-state deterministic First-Win lifecycle and transitions.
- [`docs/PHASE_4E2_WALLET_INTEGRATION.md`](docs/PHASE_4E2_WALLET_INTEGRATION.md) — USE NOW vs SAVE FOR LATER, wallet invariants, and Parent Precedence.
- [`docs/PHASE_4E2_TODAY_EXPERIENCE.md`](docs/PHASE_4E2_TODAY_EXPERIENCE.md) — TodayScreen coaching and milestone celebration card layout.
- [`docs/PHASE_4E2_NOTIFICATION_INTEGRATION.md`](docs/PHASE_4E2_NOTIFICATION_INTEGRATION.md) — First-Win notification eligibility and suppression rules.
- [`docs/PHASE_4E2_PRIVACY.md`](docs/PHASE_4E2_PRIVACY.md) — Privacy guarantees and zero-surveillance boundaries.
- [`docs/PHASE_4E2_TEST_PLAN.md`](docs/PHASE_4E2_TEST_PLAN.md) — 30-scenario Phase 4E-2 test verification plan.
- [`docs/PHASE_4E2_TEST_RESULTS.md`](docs/PHASE_4E2_TEST_RESULTS.md) — Automated test results (30/30 passed, 408 total).
- [`docs/PHASE_4E2_DECISIONS.md`](docs/PHASE_4E2_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4E1_IMPLEMENTATION.md`](docs/PHASE_4E1_IMPLEMENTATION.md) — Phase 4E-1 Self Mode First-Run Activation & Zero-Friction Onboarding Architecture and Implementation.
- [`docs/PHASE_4E1_FIRST_RUN_UX.md`](docs/PHASE_4E1_FIRST_RUN_UX.md) — 5-step setup flow, consumer-grade hierarchy, and calm non-shaming tone matrix.
- [`docs/PHASE_4E1_ACTIVATION_ARCHITECTURE.md`](docs/PHASE_4E1_ACTIVATION_ARCHITECTURE.md) — Orchestration pipeline and enforcement path non-interference invariants.
- [`docs/PHASE_4E1_PLAN_ACTIVATION.md`](docs/PHASE_4E1_PLAN_ACTIVATION.md) — Atomic plan validation, draft creation, and commit rollback guarantees.
- [`docs/PHASE_4E1_PERMISSION_FLOW.md`](docs/PHASE_4E1_PERMISSION_FLOW.md) — Pre-permission explanatory modals and graceful degradation.
- [`docs/PHASE_4E1_RESUME_RECOVERY.md`](docs/PHASE_4E1_RESUME_RECOVERY.md) — Onboarding state machine lifecycle and process death recovery.
- [`docs/PHASE_4E1_PRIVACY.md`](docs/PHASE_4E1_PRIVACY.md) — Zero surveillance commitment and local-only boundary guarantees.
- [`docs/PHASE_4E1_ACCESSIBILITY.md`](docs/PHASE_4E1_ACCESSIBILITY.md) — Android accessibility touch targets, contrast, and navigation support.
- [`docs/PHASE_4E1_TEST_PLAN.md`](docs/PHASE_4E1_TEST_PLAN.md) — 40-scenario Phase 4E-1 test verification plan.
- [`docs/PHASE_4E1_TEST_RESULTS.md`](docs/PHASE_4E1_TEST_RESULTS.md) — Automated test results (40/40 passed, 378 total).
- [`docs/PHASE_4E1_DECISIONS.md`](docs/PHASE_4E1_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4D1_IMPLEMENTATION.md`](docs/PHASE_4D1_IMPLEMENTATION.md) — Phase 4D-1 Daily Habit Experience Architecture and Implementation.
- [`docs/PHASE_4D1_UX.md`](docs/PHASE_4D1_UX.md) — Fast daily habit UX hierarchy and calm non-shaming design principles.
- [`docs/PHASE_4D1_TODAY_EXPERIENCE.md`](docs/PHASE_4D1_TODAY_EXPERIENCE.md) — TodayScreen structure, components, and primary coaching cards.
- [`docs/PHASE_4D1_DATA_SOURCES.md`](docs/PHASE_4D1_DATA_SOURCES.md) — Local persistence mapping and DataStore reflection storage.
- [`docs/PHASE_4D1_PRIVACY.md`](docs/PHASE_4D1_PRIVACY.md) — Zero surveillance commitment and local-only telemetry boundaries.
- [`docs/PHASE_4D1_TEST_PLAN.md`](docs/PHASE_4D1_TEST_PLAN.md) — 30-scenario Today experience verification test plan.
- [`docs/PHASE_4D1_TEST_RESULTS.md`](docs/PHASE_4D1_TEST_RESULTS.md) — Automated test results (30/30 passed, 220 total).
- [`docs/PHASE_4D3_IMPLEMENTATION.md`](docs/PHASE_4D3_IMPLEMENTATION.md) — Phase 4D-3 Smart Notifications & Timely Intervention Architecture and Implementation.
- [`docs/PHASE_4D3_NOTIFICATION_ARCHITECTURE.md`](docs/PHASE_4D3_NOTIFICATION_ARCHITECTURE.md) — Off-enforcement-path notification architecture, pipelines, and state isolation.
- [`docs/PHASE_4D3_NOTIFICATION_POLICY.md`](docs/PHASE_4D3_NOTIFICATION_POLICY.md) — 7 notification types, scoring thresholds, and deterministic suppression conditions.
- [`docs/PHASE_4D3_FREQUENCY_GOVERNOR.md`](docs/PHASE_4D3_FREQUENCY_GOVERNOR.md) — Hard daily frequency limits, cooldowns, and automatic midnight reset.
- [`docs/PHASE_4D3_UX.md`](docs/PHASE_4D3_UX.md) — Calm, supportive copy matrix and safe deep-link navigation.
- [`docs/PHASE_4D3_PRIVACY.md`](docs/PHASE_4D3_PRIVACY.md) — Zero surveillance guarantees and offline-first boundaries.
- [`docs/PHASE_4D3_BATTERY.md`](docs/PHASE_4D3_BATTERY.md) — Battery budgets, no continuous polling, and WorkManager batching.
- [`docs/PHASE_4D3_TEST_PLAN.md`](docs/PHASE_4D3_TEST_PLAN.md) — 82-scenario Phase 4D-3 test verification plan.
- [`docs/PHASE_4D3_TEST_RESULTS.md`](docs/PHASE_4D3_TEST_RESULTS.md) — Automated test results (82/82 passed, 338 total).
- [`docs/PHASE_4D3_DECISIONS.md`](docs/PHASE_4D3_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4D2_IMPLEMENTATION.md`](docs/PHASE_4D2_IMPLEMENTATION.md) — Phase 4D-2 Daily Habit Actions & Progress Architecture and Implementation.
- [`docs/PHASE_4D2_DAILY_ACTIONS.md`](docs/PHASE_4D2_DAILY_ACTIONS.md) — DailyActionPlanner model, segmentation rules, and deterministic planning logic.
- [`docs/PHASE_4D2_UX.md`](docs/PHASE_4D2_UX.md) — Action-oriented UX hierarchy and calm non-punitive flow.
- [`docs/PHASE_4D2_PROGRESS_MODEL.md`](docs/PHASE_4D2_PROGRESS_MODEL.md) — Goal progress state lifecycle and integrity guarantees.
- [`docs/PHASE_4D2_WALLET_INTEGRATION.md`](docs/PHASE_4D2_WALLET_INTEGRATION.md) — USE NOW vs SAVE FOR LATER, wallet safeguards, and EarnedTimeWalletService reuse.
- [`docs/PHASE_4D2_PRIVACY.md`](docs/PHASE_4D2_PRIVACY.md) — Zero surveillance commitment and local-only telemetry boundaries.
- [`docs/PHASE_4D2_TEST_PLAN.md`](docs/PHASE_4D2_TEST_PLAN.md) — 36-scenario Daily Actions test plan.
- [`docs/PHASE_4D2_TEST_RESULTS.md`](docs/PHASE_4D2_TEST_RESULTS.md) — Automated test results (36/36 passed, 256 total).
- [`docs/PHASE_4D2_DECISIONS.md`](docs/PHASE_4D2_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4C3_IMPLEMENTATION.md`](docs/PHASE_4C3_IMPLEMENTATION.md) — Phase 4C-3 Behaviour Intelligence & Goal Coaching Architecture and Implementation.
- [`docs/PHASE_4C3_BEHAVIOUR_PATTERNS.md`](docs/PHASE_4C3_BEHAVIOUR_PATTERNS.md) — Deterministic Time, App, Intervention, and Wallet pattern analysis.
- [`docs/PHASE_4C3_MOMENTUM.md`](docs/PHASE_4C3_MOMENTUM.md) — 0–100 Behaviour Momentum Score, weights, and supportive state narratives.
- [`docs/PHASE_4C3_GOAL_INTEGRITY.md`](docs/PHASE_4C3_GOAL_INTEGRITY.md) — Goal Integrity formulation and Goal <-> Distraction relationship mapping.
- [`docs/PHASE_4C3_WEEKLY_INTELLIGENCE.md`](docs/PHASE_4C3_WEEKLY_INTELLIGENCE.md) — 7-day deep retrospective, strongest days, and next-week focus.
- [`docs/PHASE_4C3_EXPERIMENTS.md`](docs/PHASE_4C3_EXPERIMENTS.md) — Behaviour Experiments Framework, lifecycle, and safety invariants.
- [`docs/PHASE_4C3_PRIVACY.md`](docs/PHASE_4C3_PRIVACY.md) — Zero surveillance guarantees and on-device privacy boundaries.
- [`docs/PHASE_4C3_DATA_MODEL.md`](docs/PHASE_4C3_DATA_MODEL.md) — Room v8 schema and `MIGRATION_7_8`.
- [`docs/PHASE_4C3_TEST_PLAN.md`](docs/PHASE_4C3_TEST_PLAN.md) — 34-scenario behaviour intelligence test plan.
- [`docs/PHASE_4C3_TEST_RESULTS.md`](docs/PHASE_4C3_TEST_RESULTS.md) — Automated test results (34/34 passed, 190 total).
- [`docs/PHASE_4C3_DECISIONS.md`](docs/PHASE_4C3_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4C2_IMPLEMENTATION.md`](docs/PHASE_4C2_IMPLEMENTATION.md) — Phase 4C-2 Adaptive Behaviour & Personalization Architecture and Implementation.
- [`docs/PHASE_4C2_ADAPTIVE_RULES.md`](docs/PHASE_4C2_ADAPTIVE_RULES.md) — Deterministic evaluation formulas, minimum sample size gates, and recommendation rules.
- [`docs/PHASE_4C2_PERSONALIZATION.md`](docs/PHASE_4C2_PERSONALIZATION.md) — Personalization profile architecture and off-path computation triggers.
- [`docs/PHASE_4C2_RECOMMENDATIONS.md`](docs/PHASE_4C2_RECOMMENDATIONS.md) — Behaviour recommendation structure and user decision workflow.
- [`docs/PHASE_4C2_WEEKLY_REVIEW.md`](docs/PHASE_4C2_WEEKLY_REVIEW.md) — 7-day performance snapshot, dynamic wins, and next-week focus.
- [`docs/PHASE_4C2_DATA_MODEL.md`](docs/PHASE_4C2_DATA_MODEL.md) — Room v7 schema and `MIGRATION_6_7`.
- [`docs/PHASE_4C2_TEST_PLAN.md`](docs/PHASE_4C2_TEST_PLAN.md) — 41-scenario adaptive evaluation test plan.
- [`docs/PHASE_4C2_TEST_RESULTS.md`](docs/PHASE_4C2_TEST_RESULTS.md) — Automated test results (41/41 passed, 156 total).
- [`docs/PHASE_4C2_DECISIONS.md`](docs/PHASE_4C2_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4C2_PRIVACY.md`](docs/PHASE_4C2_PRIVACY.md) — Zero surveillance commitment and on-device telemetry guarantees.
- [`docs/PHASE_4C1_IMPLEMENTATION.md`](docs/PHASE_4C1_IMPLEMENTATION.md) — Phase 4C-1 Goal Templates & Smart Plan Creation Architecture and Implementation.
- [`docs/PHASE_4C1_GOAL_TEMPLATES.md`](docs/PHASE_4C1_GOAL_TEMPLATES.md) — Deterministic Goal Templates Catalog and presets.
- [`docs/PHASE_4C1_PLAN_CREATOR.md`](docs/PHASE_4C1_PLAN_CREATOR.md) — Smart Plan Creator Orchestration and non-persistence invariant.
- [`docs/PHASE_4C1_RECOMMENDATIONS.md`](docs/PHASE_4C1_RECOMMENDATIONS.md) — Contextual recommendation mapping and reward presets.
- [`docs/PHASE_4C1_UX.md`](docs/PHASE_4C1_UX.md) — Fast onboarding UX (<60s) and Plan Review screen flow.
- [`docs/PHASE_4C1_PRIVACY.md`](docs/PHASE_4C1_PRIVACY.md) — Local-first privacy, non-surveillance, and zero external profiling.
- [`docs/PHASE_4C1_TEST_PLAN.md`](docs/PHASE_4C1_TEST_PLAN.md) — 32-scenario Goal Template and Plan Creator test plan.
- [`docs/PHASE_4C1_TEST_RESULTS.md`](docs/PHASE_4C1_TEST_RESULTS.md) — Automated test results (32/32 tests passed, 115 total).
- [`docs/PHASE_4C1_DECISIONS.md`](docs/PHASE_4C1_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4B3_IMPLEMENTATION.md`](docs/PHASE_4B3_IMPLEMENTATION.md) — Phase 4B-3 Self Mode Behaviour Loop & Experience Implementation.
- [`docs/PHASE_4B3_BEHAVIOUR_LOOP.md`](docs/PHASE_4B3_BEHAVIOUR_LOOP.md) — The Central Behaviour Loop, Intentional Reflection, and Positive Friction.
- [`docs/PHASE_4B3_UX.md`](docs/PHASE_4B3_UX.md) — Goal-First UX hierarchy, calm design principles, and plan summaries.
- [`docs/PHASE_4B3_INSIGHTS.md`](docs/PHASE_4B3_INSIGHTS.md) — Deterministic insights engine, HIR formulas, and Feedback Rules A–E.
- [`docs/PHASE_4B3_PRIVACY.md`](docs/PHASE_4B3_PRIVACY.md) — Local-only ephemeral reflections, non-surveillance, and zero profiling.
- [`docs/PHASE_4B3_TEST_PLAN.md`](docs/PHASE_4B3_TEST_PLAN.md) — 29-scenario behavior loop verification plan.
- [`docs/PHASE_4B3_TEST_RESULTS.md`](docs/PHASE_4B3_TEST_RESULTS.md) — Automated unit test and regression results.
- [`docs/PHASE_4B3_DECISIONS.md`](docs/PHASE_4B3_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4B2_IMPLEMENTATION.md`](docs/PHASE_4B2_IMPLEMENTATION.md) — Phase 4B-2 Earned Time Wallet & Session Economics Implementation.
- [`docs/PHASE_4B2_WALLET_MODEL.md`](docs/PHASE_4B2_WALLET_MODEL.md) — Room v6 wallet, ledger, and session persistence schemas.
- [`docs/PHASE_4B2_SESSION_ECONOMICS.md`](docs/PHASE_4B2_SESSION_ECONOMICS.md) — Monotonic time calculation, anti-binge caps, and session state machine.
- [`docs/PHASE_4E4_IMPLEMENTATION.md`](docs/PHASE_4E4_IMPLEMENTATION.md) — Personal Habit Plan Refinement & Long-Term Continuity Implementation.
- [`docs/PHASE_4E4_PLAN_CONTINUITY.md`](docs/PHASE_4E4_PLAN_CONTINUITY.md) — Plan continuity lifecycle and state machine.
- [`docs/PHASE_4E4_FIRST_WEEK_TRANSITION.md`](docs/PHASE_4E4_FIRST_WEEK_TRANSITION.md) — First-week retrospective and ongoing routine transition.
- [`docs/PHASE_4E4_PLAN_REFINEMENT.md`](docs/PHASE_4E4_PLAN_REFINEMENT.md) — Plan refinement types and before/after change preview.
- [`docs/PHASE_4E4_USER_DECISION_FLOW.md`](docs/PHASE_4E4_USER_DECISION_FLOW.md) — Mandatory user decision flow and atomic update commits.
- [`docs/PHASE_4E4_LONG_TERM_CONTINUITY.md`](docs/PHASE_4E4_LONG_TERM_CONTINUITY.md) — Long-term weekly continuity cycle without re-onboarding.
- [`docs/PHASE_4E4_GOAL_CONTINUITY.md`](docs/PHASE_4E4_GOAL_CONTINUITY.md) — Goal continuity and goal integrity anchoring.
- [`docs/PHASE_4E4_PLAN_RESET.md`](docs/PHASE_4E4_PLAN_RESET.md) — Start fresh semantics and history preservation.
- [`docs/PHASE_4E4_UX.md`](docs/PHASE_4E4_UX.md) — Calm non-shaming tone and anti-gamification refinement.
- [`docs/PHASE_4E4_PRIVACY.md`](docs/PHASE_4E4_PRIVACY.md) — Privacy guarantees and local telemetry boundaries.
- [`docs/PHASE_4E4_DATA_MODEL.md`](docs/PHASE_4E4_DATA_MODEL.md) — Data model and zero Room migration guarantee.
- [`docs/PHASE_4E4_TEST_PLAN.md`](docs/PHASE_4E4_TEST_PLAN.md) — 46-scenario unit test plan.
- [`docs/PHASE_4E4_TEST_RESULTS.md`](docs/PHASE_4E4_TEST_RESULTS.md) — Automated test suite results (494 / 494 tests passing).
- [`docs/PHASE_4E4_DECISIONS.md`](docs/PHASE_4E4_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4F7_FINAL_RELEASE_REPORT.md`](docs/PHASE_4F7_FINAL_RELEASE_REPORT.md) — Phase 4F-7 Final Production Release Report & Candidate Sign-Off.
- [`docs/PHASE_4F7_GOOGLE_PLAY_RELEASE_CHECKLIST.md`](docs/PHASE_4F7_GOOGLE_PLAY_RELEASE_CHECKLIST.md) — Google Play Release Preparation Checklist.
- [`docs/PHASE_4F7_PRODUCTION_RELEASE_RUNBOOK.md`](docs/PHASE_4F7_PRODUCTION_RELEASE_RUNBOOK.md) — Production Release Runbook & Keystore Protocol.
- [`docs/PHASE_4F7_RELEASE_ARTIFACTS.md`](docs/PHASE_4F7_RELEASE_ARTIFACTS.md) — Production Release Candidate Artifacts & Checksums.
- [`docs/PHASE_4F6_IMPLEMENTATION.md`](docs/PHASE_4F6_IMPLEMENTATION.md) — Phase 4F-6 Final Production Launch Readiness, MVP Sign-Off & Controlled Release.
- [`docs/PHASE_4F6_FINAL_RELEASE_AUDIT.md`](docs/PHASE_4F6_FINAL_RELEASE_AUDIT.md) — Final Release Configuration Audit.
- [`docs/PHASE_4F6_RELEASE_CHECKLIST.md`](docs/PHASE_4F6_RELEASE_CHECKLIST.md) — Production Release Checklist.
- [`docs/PHASE_4F6_SIGNING_RUNBOOK.md`](docs/PHASE_4F6_SIGNING_RUNBOOK.md) — Production Signing Runbook & Keystore Protocol.
- [`docs/PHASE_4F6_FIRST_RUN_VALIDATION.md`](docs/PHASE_4F6_FIRST_RUN_VALIDATION.md) — Permission & First-Run Release UX Validation.
- [`docs/PHASE_4F6_E2E_VALIDATION.md`](docs/PHASE_4F6_E2E_VALIDATION.md) — Final Complete MVP User Journey Validation.
- [`docs/PHASE_4F6_BYPASS_VALIDATION.md`](docs/PHASE_4F6_BYPASS_VALIDATION.md) — Circumvention & Bypass Resistance Validation.
- [`docs/PHASE_4F6_DATA_RECOVERY.md`](docs/PHASE_4F6_DATA_RECOVERY.md) — Data Reset, Deletion & Recovery Policy.
- [`docs/PHASE_4F6_PERFORMANCE.md`](docs/PHASE_4F6_PERFORMANCE.md) — Final Performance Regression & Budget Verification.
- [`docs/PHASE_4F6_PRIVACY.md`](docs/PHASE_4F6_PRIVACY.md) — Privacy Certification & Zero-Surveillance Compliance.
- [`docs/PHASE_4F6_SECURITY.md`](docs/PHASE_4F6_SECURITY.md) — Production Security Audit & Static Analysis.
- [`docs/PHASE_4F6_DEVICE_MATRIX.md`](docs/PHASE_4F6_DEVICE_MATRIX.md) — Production Device Compatibility Matrix.
- [`docs/PHASE_4F6_RELEASE_ARTIFACTS.md`](docs/PHASE_4F6_RELEASE_ARTIFACTS.md) — Production Release Candidate Artifacts & Checksums.
- [`docs/PHASE_4F6_FINDINGS.md`](docs/PHASE_4F6_FINDINGS.md) — Production Findings & Severity Classification.
- [`docs/PHASE_4F6_TEST_PLAN.md`](docs/PHASE_4F6_TEST_PLAN.md) — Production Launch Test Plan.
- [`docs/PHASE_4F6_TEST_RESULTS.md`](docs/PHASE_4F6_TEST_RESULTS.md) — Production Launch Test Results.
- [`docs/PHASE_4F6_DECISIONS.md`](docs/PHASE_4F6_DECISIONS.md) — Architectural Decision Records.
- [`docs/PHASE_4F6_MVP_SIGNOFF.md`](docs/PHASE_4F6_MVP_SIGNOFF.md) — Final MVP Sign-Off Decision.
- [`docs/PHASE_4F5_IMPLEMENTATION.md`](docs/PHASE_4F5_IMPLEMENTATION.md) — Phase 4F-5 Production QA, Release Candidate Validation & Real-World E2E Hardening.
- [`docs/PHASE_4F5_RELEASE_ARTIFACT_IDENTITY.md`](docs/PHASE_4F5_RELEASE_ARTIFACT_IDENTITY.md) — Release Artifact Identity & Integrity Report.
- [`docs/PHASE_4F5_CLEAN_INSTALL.md`](docs/PHASE_4F5_CLEAN_INSTALL.md) — Clean Installation Verification.
- [`docs/PHASE_4F5_SELF_MODE_E2E.md`](docs/PHASE_4F5_SELF_MODE_E2E.md) — Complete Self Mode E2E Journey Verification.
- [`docs/PHASE_4F5_WALLET_E2E.md`](docs/PHASE_4F5_WALLET_E2E.md) — Wallet End-to-End Security & Invariant Verification.
- [`docs/PHASE_4F5_PARENT_MODE.md`](docs/PHASE_4F5_PARENT_MODE.md) — Parent Mode Absolute Precedence Verification.
- [`docs/PHASE_4F5_PERMISSION_RESILIENCE.md`](docs/PHASE_4F5_PERMISSION_RESILIENCE.md) — Permission Resilience & Dynamic State Handling.
- [`docs/PHASE_4F5_RECOVERY.md`](docs/PHASE_4F5_RECOVERY.md) — Process Death, Force-Stop & Reboot Recovery.
- [`docs/PHASE_4F5_BYPASS_TESTING.md`](docs/PHASE_4F5_BYPASS_TESTING.md) — Navigation & Bypass Resistance Testing.
- [`docs/PHASE_4F5_NOTIFICATION_QA.md`](docs/PHASE_4F5_NOTIFICATION_QA.md) — Smart Notification QA & Frequency Governance.
- [`docs/PHASE_4F5_LONG_RUN.md`](docs/PHASE_4F5_LONG_RUN.md) — Long-Run Reliability & Memory Leak Testing.
- [`docs/PHASE_4F5_DEVICE_MATRIX.md`](docs/PHASE_4F5_DEVICE_MATRIX.md) — Multi-Device Verification Matrix.
- [`docs/PHASE_4F5_OFFLINE.md`](docs/PHASE_4F5_OFFLINE.md) — Offline Execution & Network Invariance.
- [`docs/PHASE_4F5_DATA_INTEGRITY.md`](docs/PHASE_4F5_DATA_INTEGRITY.md) — Data Integrity & Persistence Verification.
- [`docs/PHASE_4F5_SECURITY_SMOKE.md`](docs/PHASE_4F5_SECURITY_SMOKE.md) — Security Smoke Test & Sanity Audit.
- [`docs/PHASE_4F5_PERFORMANCE.md`](docs/PHASE_4F5_PERFORMANCE.md) — Performance Benchmark & Critical Path Latency.
- [`docs/PHASE_4F5_TEST_PLAN.md`](docs/PHASE_4F5_TEST_PLAN.md) — QA & Release Candidate Validation Test Plan.
- [`docs/PHASE_4F5_TEST_RESULTS.md`](docs/PHASE_4F5_TEST_RESULTS.md) — QA & Release Candidate Test Results.
- [`docs/PHASE_4F5_FINDINGS.md`](docs/PHASE_4F5_FINDINGS.md) — QA Findings & Severity Classification.
- [`docs/PHASE_4F5_DECISIONS.md`](docs/PHASE_4F5_DECISIONS.md) — Architectural Decision Records.
- [`docs/PHASE_4F4_IMPLEMENTATION.md`](docs/PHASE_4F4_IMPLEMENTATION.md) — Phase 4F-4 Production Release Engineering, Release Candidate & Operational Readiness.
- [`docs/PHASE_4F4_RELEASE_CONFIGURATION.md`](docs/PHASE_4F4_RELEASE_CONFIGURATION.md) — Production Release Configuration Audit.
- [`docs/PHASE_4F4_VERSIONING.md`](docs/PHASE_4F4_VERSIONING.md) — Semantic Versioning Strategy & Upgrade Policy.
- [`docs/PHASE_4F4_SIGNING.md`](docs/PHASE_4F4_SIGNING.md) — Production Signing Architecture & Keystore Security.
- [`docs/PHASE_4F4_SECRET_SCANNING.md`](docs/PHASE_4F4_SECRET_SCANNING.md) — Automated Secret Scanning & F-P3-04 Resolution.
- [`docs/PHASE_4F4_DEPENDENCY_RELEASE_AUDIT.md`](docs/PHASE_4F4_DEPENDENCY_RELEASE_AUDIT.md) — Production Dependency & License Release Audit.
- [`docs/PHASE_4F4_LOGGING.md`](docs/PHASE_4F4_LOGGING.md) — Production Logging & Diagnostic Sanitization.
- [`docs/PHASE_4F4_R8_RELEASE.md`](docs/PHASE_4F4_R8_RELEASE.md) — R8 Shrinking & Code Optimization Validation.
- [`docs/PHASE_4F4_CLEAN_INSTALL.md`](docs/PHASE_4F4_CLEAN_INSTALL.md) — Clean Installation Verification.
- [`docs/PHASE_4F4_UPGRADE_TEST.md`](docs/PHASE_4F4_UPGRADE_TEST.md) — In-Place Upgrade Verification.
- [`docs/PHASE_4F4_RECOVERY_TEST.md`](docs/PHASE_4F4_RECOVERY_TEST.md) — Failure & Recovery Testing.
- [`docs/PHASE_4F4_RELEASE_ARTIFACTS.md`](docs/PHASE_4F4_RELEASE_ARTIFACTS.md) — Release Artifacts & Checksums.
- [`docs/PHASE_4F4_PERFORMANCE.md`](docs/PHASE_4F4_PERFORMANCE.md) — Release Build Performance Verification.
- [`docs/PHASE_4F4_PRODUCTION_READINESS.md`](docs/PHASE_4F4_PRODUCTION_READINESS.md) — Final Production Readiness Matrix.
- [`docs/PHASE_4F4_TEST_PLAN.md`](docs/PHASE_4F4_TEST_PLAN.md) — Release Engineering Test Plan.
- [`docs/PHASE_4F4_TEST_RESULTS.md`](docs/PHASE_4F4_TEST_RESULTS.md) — Release Engineering Test Results.
- [`docs/PHASE_4F4_FINDINGS.md`](docs/PHASE_4F4_FINDINGS.md) — Release Engineering Findings & Severity Classification.
- [`docs/PHASE_4F4_DECISIONS.md`](docs/PHASE_4F4_DECISIONS.md) — Architectural Decision Records.
- [`docs/PHASE_4F3_IMPLEMENTATION.md`](docs/PHASE_4F3_IMPLEMENTATION.md) — Phase 4F-3 Production Security, Privacy & Data Protection Hardening.
- [`docs/PHASE_4F3_SECURITY_AUDIT.md`](docs/PHASE_4F3_SECURITY_AUDIT.md) — Production Security Audit & Static Analysis.
- [`docs/PHASE_4F3_PRIVACY_AUDIT.md`](docs/PHASE_4F3_PRIVACY_AUDIT.md) — Privacy & Zero-Surveillance Certification.
- [`docs/PHASE_4F3_SECRETS_AUDIT.md`](docs/PHASE_4F3_SECRETS_AUDIT.md) — Secrets & Credential Audit.
- [`docs/PHASE_4F3_RELEASE_CONFIGURATION.md`](docs/PHASE_4F3_RELEASE_CONFIGURATION.md) — Release Build Configuration & Variant Audit.
- [`docs/PHASE_4F3_R8_AUDIT.md`](docs/PHASE_4F3_R8_AUDIT.md) — R8 & ProGuard Shrinker Audit.
- [`docs/PHASE_4F3_COMPONENT_SECURITY.md`](docs/PHASE_4F3_COMPONENT_SECURITY.md) — Android Component Security & Export Analysis.
- [`docs/PHASE_4F3_ACCESSIBILITY_SECURITY.md`](docs/PHASE_4F3_ACCESSIBILITY_SECURITY.md) — AccessibilityService Security Audit.
- [`docs/PHASE_4F3_OVERLAY_SECURITY.md`](docs/PHASE_4F3_OVERLAY_SECURITY.md) — Overlay Window Security.
- [`docs/PHASE_4F3_WALLET_SECURITY.md`](docs/PHASE_4F3_WALLET_SECURITY.md) — Wallet Security & Financial Ledger Integrity.
- [`docs/PHASE_4F3_PARENT_MODE_SECURITY.md`](docs/PHASE_4F3_PARENT_MODE_SECURITY.md) — Parent Mode Precedence & Enforcement Hierarchy.
- [`docs/PHASE_4F3_DATA_PROTECTION.md`](docs/PHASE_4F3_DATA_PROTECTION.md) — Local Data Protection & Storage Architecture.
- [`docs/PHASE_4F3_BACKUP_RESTORE.md`](docs/PHASE_4F3_BACKUP_RESTORE.md) — Android Backup & Restore Policy.
- [`docs/PHASE_4F3_NETWORK_SECURITY.md`](docs/PHASE_4F3_NETWORK_SECURITY.md) — Network Security & Offline Invariance.
- [`docs/PHASE_4F3_LOGGING_AUDIT.md`](docs/PHASE_4F3_LOGGING_AUDIT.md) — Production Logging & Diagnostic Sanitization.
- [`docs/PHASE_4F3_DEPENDENCY_AUDIT.md`](docs/PHASE_4F3_DEPENDENCY_AUDIT.md) — Third-Party Dependency Security Audit.
- [`docs/PHASE_4F3_PRIVACY_DATA_FLOW.md`](docs/PHASE_4F3_PRIVACY_DATA_FLOW.md) — Privacy & Data-Flow Mapping.
- [`docs/PHASE_4F3_RESET_DELETION.md`](docs/PHASE_4F3_RESET_DELETION.md) — User Data Reset & Local Deletion Policy.
- [`docs/PHASE_4F3_TEST_PLAN.md`](docs/PHASE_4F3_TEST_PLAN.md) — 30-scenario Security Test Plan.
- [`docs/PHASE_4F3_TEST_RESULTS.md`](docs/PHASE_4F3_TEST_RESULTS.md) — Security & Privacy Test Results.
- [`docs/PHASE_4F3_FINDINGS.md`](docs/PHASE_4F3_FINDINGS.md) — Security Findings & Severity Classification.
- [`docs/PHASE_4F3_DECISIONS.md`](docs/PHASE_4F3_DECISIONS.md) — Architectural Decision Records.
- [`docs/PHASE_4F2_IMPLEMENTATION.md`](docs/PHASE_4F2_IMPLEMENTATION.md) — Phase 4F-2 Android Compatibility & Device Hardening.
- [`docs/PHASE_4F2_DEVICE_MATRIX.md`](docs/PHASE_4F2_DEVICE_MATRIX.md) — Comprehensive Android Version & OEM Compatibility Matrix.
- [`docs/PHASE_4F2_ANDROID14.md`](docs/PHASE_4F2_ANDROID14.md) — Android 14 (API 34) Evaluation.
- [`docs/PHASE_4F2_ANDROID15.md`](docs/PHASE_4F2_ANDROID15.md) — Android 15 (API 35) Evaluation.
- [`docs/PHASE_4F2_ANDROID16.md`](docs/PHASE_4F2_ANDROID16.md) — Android 16 (API 36) Future Compatibility.
- [`docs/PHASE_4F2_ACCESSIBILITY.md`](docs/PHASE_4F2_ACCESSIBILITY.md) — AccessibilityService Lifecycle & Resilience.
- [`docs/PHASE_4F2_OVERLAY.md`](docs/PHASE_4F2_OVERLAY.md) — Overlay Behavior & Multi-Window Management.
- [`docs/PHASE_4F2_PERMISSIONS.md`](docs/PHASE_4F2_PERMISSIONS.md) — Permission Resilience & Dynamic State Handling.
- [`docs/PHASE_4F2_BACKGROUND_EXECUTION.md`](docs/PHASE_4F2_BACKGROUND_EXECUTION.md) — Background Execution & Doze Mode Compatibility.
- [`docs/PHASE_4F2_OEM_COMPATIBILITY.md`](docs/PHASE_4F2_OEM_COMPATIBILITY.md) — OEM-Specific Battery & Service Restriction Analysis.
- [`docs/PHASE_4F2_REBOOT_RECOVERY.md`](docs/PHASE_4F2_REBOOT_RECOVERY.md) — Reboot Recovery & Monotonic Security Invariants.
- [`docs/PHASE_4F2_PROCESS_DEATH.md`](docs/PHASE_4F2_PROCESS_DEATH.md) — Process Death & Activity Recreation Resilience.
- [`docs/PHASE_4F2_WALLET.md`](docs/PHASE_4F2_WALLET.md) — Cross-Device Wallet Lifecycle & Ledger Integrity.
- [`docs/PHASE_4F2_NOTIFICATIONS.md`](docs/PHASE_4F2_NOTIFICATIONS.md) — Notification Channels & Compatibility.
- [`docs/PHASE_4F2_PERFORMANCE.md`](docs/PHASE_4F2_PERFORMANCE.md) — Cross-Device Performance & Latency Benchmarks.
- [`docs/PHASE_4F2_BATTERY.md`](docs/PHASE_4F2_BATTERY.md) — Battery Impact & Resource Utilization.
- [`docs/PHASE_4F2_SECURITY.md`](docs/PHASE_4F2_SECURITY.md) — Android Security & Platform Compatibility.
- [`docs/PHASE_4F2_PRIVACY.md`](docs/PHASE_4F2_PRIVACY.md) — Privacy & Non-Surveillance Verification.
- [`docs/PHASE_4F2_TEST_PLAN.md`](docs/PHASE_4F2_TEST_PLAN.md) — 15-scenario Compatibility Test Plan.
- [`docs/PHASE_4F2_TEST_RESULTS.md`](docs/PHASE_4F2_TEST_RESULTS.md) — Cross-Device Compatibility Test Results.
- [`docs/PHASE_4F2_FINDINGS.md`](docs/PHASE_4F2_FINDINGS.md) — Compatibility Findings & Severity Classification.
- [`docs/PHASE_4F2_DECISIONS.md`](docs/PHASE_4F2_DECISIONS.md) — Architectural Decision Records.
- [`docs/PHASE_4F1_ARCHITECTURE_AUDIT.md`](docs/PHASE_4F1_ARCHITECTURE_AUDIT.md) — Phase 4F-1 Production Architecture & Dependency Graph Audit.
- [`docs/PHASE_4F1_STATE_OWNERSHIP.md`](docs/PHASE_4F1_STATE_OWNERSHIP.md) — State Ownership & Single Authority Map.
- [`docs/PHASE_4F1_CONCURRENCY_AUDIT.md`](docs/PHASE_4F1_CONCURRENCY_AUDIT.md) — Concurrency & Race Condition Evaluation.
- [`docs/PHASE_4F1_SECURITY_AUDIT.md`](docs/PHASE_4F1_SECURITY_AUDIT.md) — Static Security & Component Export Analysis.
- [`docs/PHASE_4F1_PRIVACY_AUDIT.md`](docs/PHASE_4F1_PRIVACY_AUDIT.md) — Privacy & Data Minimization Certification.
- [`docs/PHASE_4F1_PERFORMANCE_AUDIT.md`](docs/PHASE_4F1_PERFORMANCE_AUDIT.md) — Low-End Device Latency & Resource Utilization Profile.
- [`docs/PHASE_4F1_DEPENDENCY_AUDIT.md`](docs/PHASE_4F1_DEPENDENCY_AUDIT.md) — Dependency Health & Security Review.
- [`docs/PHASE_4F1_RELEASE_AUDIT.md`](docs/PHASE_4F1_RELEASE_AUDIT.md) — Release Build Configuration & Minification Audit.
- [`docs/PHASE_4F1_TEST_GAP_ANALYSIS.md`](docs/PHASE_4F1_TEST_GAP_ANALYSIS.md) — Test Architecture & Coverage Gap Analysis.
- [`docs/PHASE_4F1_TECHNICAL_DEBT.md`](docs/PHASE_4F1_TECHNICAL_DEBT.md) — Technical Debt & Safe Deletion Candidates.
- [`docs/PHASE_4F1_FINDINGS.md`](docs/PHASE_4F1_FINDINGS.md) — Master Findings & Severity Classification Matrix.
- [`docs/PHASE_4F1_RECOMMENDATIONS.md`](docs/PHASE_4F1_RECOMMENDATIONS.md) — Production Recommendations & Release Readiness.
- [`docs/PHASE_4F1_DECISIONS.md`](docs/PHASE_4F1_DECISIONS.md) — Architectural Decision Records.
- [`docs/PHASE_4F1_TEST_RESULTS.md`](docs/PHASE_4F1_TEST_RESULTS.md) — Production Verification & Test Results Report.
- [`docs/PHASE_4E7_IMPLEMENTATION.md`](docs/PHASE_4E7_IMPLEMENTATION.md) — Phase 4E-7 Self Mode MVP Hardening & End-to-End Reliability.
- [`docs/PHASE_4E7_E2E_TEST_PLAN.md`](docs/PHASE_4E7_E2E_TEST_PLAN.md) — Comprehensive End-to-End Lifecycle Verification Matrix.
- [`docs/PHASE_4E7_RECOVERY.md`](docs/PHASE_4E7_RECOVERY.md) — Process Death, Reboot & Clock Tampering Recovery.
- [`docs/PHASE_4E7_PERMISSION_RESILIENCE.md`](docs/PHASE_4E7_PERMISSION_RESILIENCE.md) — Permission Resilience & Truthful Status Communication.
- [`docs/PHASE_4E7_BYPASS_TESTING.md`](docs/PHASE_4E7_BYPASS_TESTING.md) — Circumvention & Bypass Resistance Audit.
- [`docs/PHASE_4E7_WALLET_SECURITY.md`](docs/PHASE_4E7_WALLET_SECURITY.md) — Wallet Security & Financial Ledger Integrity.
- [`docs/PHASE_4E7_STATE_CONSISTENCY.md`](docs/PHASE_4E7_STATE_CONSISTENCY.md) — State Consistency & Single Authority Audit.
- [`docs/PHASE_4E7_DEVICE_MATRIX.md`](docs/PHASE_4E7_DEVICE_MATRIX.md) — Android Version & OEM Compatibility Matrix.
- [`docs/PHASE_4E7_PERFORMANCE.md`](docs/PHASE_4E7_PERFORMANCE.md) — Performance & Latency Benchmarks.
- [`docs/PHASE_4E7_PRIVACY.md`](docs/PHASE_4E7_PRIVACY.md) — Privacy & Non-Surveillance Audit.
- [`docs/PHASE_4E7_UX_FAILURE_STATES.md`](docs/PHASE_4E7_UX_FAILURE_STATES.md) — UX Failure States & Graceful Degradation.
- [`docs/PHASE_4E7_TEST_PLAN.md`](docs/PHASE_4E7_TEST_PLAN.md) — 20-scenario Hardening Test Plan.
- [`docs/PHASE_4E7_TEST_RESULTS.md`](docs/PHASE_4E7_TEST_RESULTS.md) — Automated test suite results (594 / 594 tests passing).
- [`docs/PHASE_4E7_DECISIONS.md`](docs/PHASE_4E7_DECISIONS.md) — Architectural Decision Records.
- [`docs/PHASE_4E6_IMPLEMENTATION.md`](docs/PHASE_4E6_IMPLEMENTATION.md) — Phase 4E-6 Long-Term Self Mode Continuity & Personal Behaviour Timeline.
- [`docs/PHASE_4E6_JOURNEY_MODEL.md`](docs/PHASE_4E6_JOURNEY_MODEL.md) — Journey Event Schema & Milestone definitions.
- [`docs/PHASE_4E6_JOURNEY_ENGINE.md`](docs/PHASE_4E6_JOURNEY_ENGINE.md) — Deterministic off-path timeline synthesis.
- [`docs/PHASE_4E6_TIMELINE.md`](docs/PHASE_4E6_TIMELINE.md) — Chronological timeline and deduplication.
- [`docs/PHASE_4E6_LONG_TERM_SUMMARY.md`](docs/PHASE_4E6_LONG_TERM_SUMMARY.md) — Long-term aggregations and metrics.
- [`docs/PHASE_4E6_PATTERN_SURFACING.md`](docs/PHASE_4E6_PATTERN_SURFACING.md) — Evidence-backed pattern learnings without fabrication.
- [`docs/PHASE_4E6_GOAL_HISTORY_INTEGRATION.md`](docs/PHASE_4E6_GOAL_HISTORY_INTEGRATION.md) — Read-only goal archives integration.
- [`docs/PHASE_4E6_PLAN_CONTINUITY_INTEGRATION.md`](docs/PHASE_4E6_PLAN_CONTINUITY_INTEGRATION.md) — Direct deep-linking to plan refinement.
- [`docs/PHASE_4E6_UX.md`](docs/PHASE_4E6_UX.md) — Calm, reflective UX standards and anti-gamification.
- [`docs/PHASE_4E6_PRIVACY.md`](docs/PHASE_4E6_PRIVACY.md) — Local-first on-device privacy guarantee.
- [`docs/PHASE_4E6_DATA_MODEL.md`](docs/PHASE_4E6_DATA_MODEL.md) — Zero Room migration guarantee (Room v8).
- [`docs/PHASE_4E6_TEST_PLAN.md`](docs/PHASE_4E6_TEST_PLAN.md) — 35-scenario verification plan.
- [`docs/PHASE_4E6_TEST_RESULTS.md`](docs/PHASE_4E6_TEST_RESULTS.md) — Automated test results (100% pass rate).
- [`docs/PHASE_4E6_DECISIONS.md`](docs/PHASE_4E6_DECISIONS.md) — Architectural Decision Records.
- [`docs/PHASE_4E5_IMPLEMENTATION.md`](docs/PHASE_4E5_IMPLEMENTATION.md) — Phase 4E-5 Goal Lifecycle & History.
- [`docs/PHASE_4E4_IMPLEMENTATION.md`](docs/PHASE_4E4_IMPLEMENTATION.md) — Phase 4E-4 Personal Habit Plan Refinement.
- [`docs/PHASE_4E3_IMPLEMENTATION.md`](docs/PHASE_4E3_IMPLEMENTATION.md) — Phase 4E-3 Habit Momentum & 7-Day Formation Loop.
- [`docs/PHASE_4E2_IMPLEMENTATION.md`](docs/PHASE_4E2_IMPLEMENTATION.md) — Phase 4E-2 Self Mode First Win & Habit Formation Loop.
- [`docs/PHASE_4E1_IMPLEMENTATION.md`](docs/PHASE_4E1_IMPLEMENTATION.md) — Phase 4E-1 Self Mode First-Run Activation & Zero-Friction Onboarding.
- [`docs/PHASE_4B2_SECURITY.md`](docs/PHASE_4B2_SECURITY.md) — Wallet security, replay protection, and Parent Precedence.
- [`docs/PHASE_4B2_TEST_PLAN.md`](docs/PHASE_4B2_TEST_PLAN.md) — 30-scenario wallet verification plan.
- [`docs/PHASE_4B2_TEST_RESULTS.md`](docs/PHASE_4B2_TEST_RESULTS.md) — Automated unit test and regression results.
- [`docs/PHASE_4B2_DECISIONS.md`](docs/PHASE_4B2_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4B1_IMPLEMENTATION.md`](docs/PHASE_4B1_IMPLEMENTATION.md) — Phase 4B-1 Self Mode Onboarding & Configuration Implementation.
- [`docs/PHASE_4B1_DATA_MODEL.md`](docs/PHASE_4B1_DATA_MODEL.md) — Room v5 persistence and DataStore mode schemas.
- [`docs/PHASE_4B1_UX.md`](docs/PHASE_4B1_UX.md) — Self Mode UX principles, emotional positioning, and 4-step user flow.
- [`docs/PHASE_4B1_TEST_PLAN.md`](docs/PHASE_4B1_TEST_PLAN.md) — Self Mode verification test plan.
- [`docs/PHASE_4B1_TEST_RESULTS.md`](docs/PHASE_4B1_TEST_RESULTS.md) — 23-scenario automated test results.
- [`docs/PHASE_4B1_DECISIONS.md`](docs/PHASE_4B1_DECISIONS.md) — Architectural Decision Records (ADRs 1–3).
- [`docs/PHASE_4A_IMPLEMENTATION.md`](docs/PHASE_4A_IMPLEMENTATION.md) — Phase 4A Behaviour & Goal Engine Architecture and Implementation.
- [`docs/PHASE_4A_DATA_MODEL.md`](docs/PHASE_4A_DATA_MODEL.md) — Goal, Trigger, Replacement Behaviour, and Behaviour Policy Room v5 Schemas.
- [`docs/PHASE_4A_POLICY_RESOLUTION.md`](docs/PHASE_4A_POLICY_RESOLUTION.md) — Policy Resolution pipeline and Parent Mode Precedence Invariants.
- [`docs/PHASE_4A_TEST_PLAN.md`](docs/PHASE_4A_TEST_PLAN.md) — 10-scenario Behaviour Engine test plan.
- [`docs/PHASE_4A_TEST_RESULTS.md`](docs/PHASE_4A_TEST_RESULTS.md) — Automated unit test and regression execution results.
- [`docs/PHASE_4A_DECISIONS.md`](docs/PHASE_4A_DECISIONS.md) — Architectural Decision Records (ADRs 1–4).
- [`docs/PHASE_3C_IMPLEMENTATION.md`](docs/PHASE_3C_IMPLEMENTATION.md) — Phase 3C Behaviour Analytics & Intervention Intelligence.
- [`docs/PHASE_3C_DATA_MODEL.md`](docs/PHASE_3C_DATA_MODEL.md) — Behavioral event schema, daily rollups, and Firestore schemas.
- [`docs/PHASE_3C_METRICS.md`](docs/PHASE_3C_METRICS.md) — Mathematical definitions of Habit Interruption Rate and behavioral catalog.
- [`docs/PHASE_3C_PRIVACY.md`](docs/PHASE_3C_PRIVACY.md) — Non-surveillance privacy architecture and neutral framing.
- [`docs/PHASE_3C_TEST_PLAN.md`](docs/PHASE_3C_TEST_PLAN.md) — 16-scenario behavioral test plan.
- [`docs/PHASE_3C_TEST_RESULTS.md`](docs/PHASE_3C_TEST_RESULTS.md) — Test execution logs and latency benchmark measurements.
- [`docs/PHASE_3C_RECOMMENDATION_ENGINE.md`](docs/PHASE_3C_RECOMMENDATION_ENGINE.md) — Recommendation logic and future AI interface.
- [`docs/PHASE_3B_THREAT_MODEL.md`](docs/PHASE_3B_THREAT_MODEL.md) — Phase 3B Threat Model & Attacker Personas (Level 1, 2, 3).
- [`docs/PHASE_3B_CIRCUMVENTION_MATRIX.md`](docs/PHASE_3B_CIRCUMVENTION_MATRIX.md) — Circumvention vectors, severity, and Google Play policy review.
- [`docs/PHASE_3B_SECURITY_TESTS.md`](docs/PHASE_3B_SECURITY_TESTS.md) — 13-category security test suite.
- [`docs/PHASE_3B_TEST_RESULTS.md`](docs/PHASE_3B_TEST_RESULTS.md) — Security & circumvention audit execution results.
- [`docs/PHASE_3B_PLAY_POLICY_REVIEW.md`](docs/PHASE_3B_PLAY_POLICY_REVIEW.md) — Google Play Store policy & compliance analysis.
- [`docs/PHASE_3B_MITIGATION_PLAN.md`](docs/PHASE_3B_MITIGATION_PLAN.md) — Circumvention mitigation plan & decision framework.
- [`docs/PHASE_3B_KNOWN_LIMITATIONS.md`](docs/PHASE_3B_KNOWN_LIMITATIONS.md) — Known Android platform boundaries & security limitations.
- [`docs/PHASE_3A_IMPLEMENTATION.md`](docs/PHASE_3A_IMPLEMENTATION.md) — Phase 3A Production Reliability & Android/OEM Hardening.
- [`docs/PHASE_3A_DEVICE_MATRIX.md`](docs/PHASE_3A_DEVICE_MATRIX.md) — Cross-OEM compatibility guide (Pixel, Samsung, OnePlus, Xiaomi, Oppo, Realme).
- [`docs/PHASE_3A_RELIABILITY_TESTS.md`](docs/PHASE_3A_RELIABILITY_TESTS.md) — 14 production resilience test specifications.
- [`docs/PHASE_3A_TEST_RESULTS.md`](docs/PHASE_3A_TEST_RESULTS.md) — Execution logs, battery benchmarks, and latency measurements.
- [`docs/PHASE_3A_KNOWN_LIMITATIONS.md`](docs/PHASE_3A_KNOWN_LIMITATIONS.md) — Android OS & OEM platform constraints and mitigations.
- [`docs/PHASE_5_IMPLEMENTATION.md`](docs/PHASE_5_IMPLEMENTATION.md) — Phase 5 Intervention-First Product Evolution & Architecture.
- [`docs/PHASE_5_PRODUCT_DIRECTION.md`](docs/PHASE_5_PRODUCT_DIRECTION.md) — Calm, positive friction product experience.
- [`docs/PHASE_5_INTERVENTION_ARCHITECTURE.md`](docs/PHASE_5_INTERVENTION_ARCHITECTURE.md) — Unified InterventionEngine and policy resolution.
- [`docs/PHASE_5_INTERVENTION_LIBRARY.md`](docs/PHASE_5_INTERVENTION_LIBRARY.md) — Complete 35-item intervention catalog across 7 categories.
- [`docs/PHASE_5_VALIDATION.md`](docs/PHASE_5_VALIDATION.md) — Sensor, timer, cognitive, and manual validation with anti-circumvention.
- [`docs/PHASE_5_SESSION_STATE_MACHINE.md`](docs/PHASE_5_SESSION_STATE_MACHINE.md) — Deterministic session state machine.
- [`docs/PHASE_5_POLICY_ENGINE.md`](docs/PHASE_5_POLICY_ENGINE.md) — Absolute Parent Precedence and Multi-source policy resolution.
- [`docs/PHASE_5_SELF_MODE.md`](docs/PHASE_5_SELF_MODE.md) — Minimal, calm Self Mode architecture.
- [`docs/PHASE_5_PARENT_CHILD_ARCHITECTURE.md`](docs/PHASE_5_PARENT_CHILD_ARCHITECTURE.md) — Parent/Child architectural preparation.
- [`docs/PHASE_5_WALLET_INTEGRATION.md`](docs/PHASE_5_WALLET_INTEGRATION.md) — Authoritative wallet integration.
- [`docs/PHASE_5_PRIVACY.md`](docs/PHASE_5_PRIVACY.md) — Zero-surveillance specification.
- [`docs/PHASE_5_SECURITY.md`](docs/PHASE_5_SECURITY.md) — Security & anti-tamper specifications.
- [`docs/PHASE_5_PERFORMANCE.md`](docs/PHASE_5_PERFORMANCE.md) — Latency benchmarks and enforcement performance.
- [`docs/PHASE_5_BATTERY.md`](docs/PHASE_5_BATTERY.md) — Zero background sensor listener guarantee.
- [`docs/PHASE_5_TEST_PLAN.md`](docs/PHASE_5_TEST_PLAN.md) — Test plan and 654 automated test verification.
- [`docs/PHASE_5_TEST_RESULTS.md`](docs/PHASE_5_TEST_RESULTS.md) — Automated and hardware test results.
- [`docs/PHASE_5_FINDINGS.md`](docs/PHASE_5_FINDINGS.md) — Zero-finding security/architecture audit and ADRs.
- [`docs/PHASE_6A_IMPLEMENTATION.md`](docs/PHASE_6A_IMPLEMENTATION.md) — Phase 6A Adaptive Intervention Loop Implementation & Selection Foundation.
- [`docs/PHASE_6A_ARCHITECTURE.md`](docs/PHASE_6A_ARCHITECTURE.md) — Adaptive data flow, 5-factor scoring model, and Parent Precedence.
- [`docs/PHASE_6A_VALIDATION.md`](docs/PHASE_6A_VALIDATION.md) — Test verification and hardware validation.
- [`docs/PHASE_6A_FINDINGS.md`](docs/PHASE_6A_FINDINGS.md) — Phase 6A findings and architectural decisions.
- [`docs/PHASE_6B_IMPLEMENTATION.md`](docs/PHASE_6B_IMPLEMENTATION.md) — Phase 6B Adaptive Feedback Loop Implementation & Post-Intervention Feedback.
- [`docs/PHASE_6B_ARCHITECTURE.md`](docs/PHASE_6B_ARCHITECTURE.md) — Feedback data flow, sampling mechanism, and adaptive store integration.
- [`docs/PHASE_6B_VALIDATION.md`](docs/PHASE_6B_VALIDATION.md) — Test verification and hardware validation.
- [`docs/PHASE_6B_FINDINGS.md`](docs/PHASE_6B_FINDINGS.md) — Phase 6B findings and architectural decisions.
- [`docs/PHASE_6C_IMPLEMENTATION.md`](docs/PHASE_6C_IMPLEMENTATION.md) — Phase 6C Personalized Intervention Learning & Hierarchy Specification.
- [`docs/PHASE_6C_ARCHITECTURE.md`](docs/PHASE_6C_ARCHITECTURE.md) — Hierarchical data flow, confidence modeling, and selection pipeline.
- [`docs/PHASE_6C_VALIDATION.md`](docs/PHASE_6C_VALIDATION.md) — Test verification and hardware validation.
- [`docs/PHASE_6C_FINDINGS.md`](docs/PHASE_6C_FINDINGS.md) — Phase 6C findings and architectural decisions.
- [`docs/PHASE_7A_ADAPTIVE_MEMORY_CONTRACT.md`](docs/PHASE_7A_ADAPTIVE_MEMORY_CONTRACT.md) — Phase 7A Adaptive Memory Contract & Privacy-First Persistence Design.
- [`docs/PHASE_7B_IMPLEMENTATION.md`](docs/PHASE_7B_IMPLEMENTATION.md) — Phase 7B Persistent Adaptive Memory Implementation & Room Migration 8 → 9.
- [`docs/PHASE_7B_ARCHITECTURE.md`](docs/PHASE_7B_ARCHITECTURE.md) — Persistent adaptive topology, async persistence, and cold start.
- [`docs/PHASE_7B_MIGRATION.md`](docs/PHASE_7B_MIGRATION.md) — Room Migration 8 → 9 DDL specification and testing.
- [`docs/PHASE_7B_PERSISTENCE.md`](docs/PHASE_7B_PERSISTENCE.md) — Persistence contract, aggregate keys, and in-memory authority.
- [`docs/PHASE_7B_DECAY.md`](docs/PHASE_7B_DECAY.md) — 30-day half-life decay model and 90-day purge semantics.
- [`docs/PHASE_7B_FAILURE_RECOVERY.md`](docs/PHASE_7B_FAILURE_RECOVERY.md) — Database failure isolation and graceful degradation.
- [`docs/PHASE_7B_PRIVACY.md`](docs/PHASE_7B_PRIVACY.md) — Zero-surveillance verification and data minimization.
- [`docs/PHASE_7B_VALIDATION.md`](docs/PHASE_7B_VALIDATION.md) — Automated and hardware validation report.
- [`docs/PHASE_7B_TEST_RESULTS.md`](docs/PHASE_7B_TEST_RESULTS.md) — Unit test suite results and latency benchmarks.
- [`docs/PHASE_7B_FINDINGS.md`](docs/PHASE_7B_FINDINGS.md) — Phase 7B audit findings and architectural decisions.
- [`docs/PHASE_2C_IMPLEMENTATION.md`](docs/PHASE_2C_IMPLEMENTATION.md) — Parent Web Control Center (Next.js 15 & Tailwind).
- [`docs/PRODUCTION_ARCHITECTURE.md`](docs/PRODUCTION_ARCHITECTURE.md) — Comprehensive technical architecture.
- [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md) — Complete Room and Firestore schemas with write audits.
- [`docs/SECURITY_ARCHITECTURE.md`](docs/SECURITY_ARCHITECTURE.md) — Threat modeling, cryptographic PIN storage, and pairing protocol.
- [`docs/PRIVACY_ARCHITECTURE.md`](docs/PRIVACY_ARCHITECTURE.md) — Data minimization and non-surveillance standards.
- [`docs/PLAY_POLICY_CHECKLIST.md`](docs/PLAY_POLICY_CHECKLIST.md) — Google Play Store compliance verification checklist.
