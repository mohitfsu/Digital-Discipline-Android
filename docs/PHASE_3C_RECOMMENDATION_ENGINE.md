# Phase 3C — Intervention Recommendation Engine & Future AI Interface

## 1. Executive Summary
The `InterventionRecommendationEngine` evaluates local behavioral friction data to formulate deterministic, evidence-based intervention recommendations for parents. It acts as an **advisory subsystem** and strictly requires explicit parent confirmation before altering any device enforcement policies.

---

## 2. Core Operational Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                      LOCAL BEHAVIOURAL TELEMETRY                       │
│      Intervention Attempts & 5-Minute Rapid Reopen Frequencies         │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│            DETERMINISTIC RECOMMENDATION ENGINE (LOCAL)                 │
│   • Compares 5-minute reopen rates across Pause, Breathing, Squats     │
│   • Requires minimum statistical threshold (e.g. >= 10 trials)         │
│   • Generates structured advisory report with confidence score         │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 PARENT CONTROL CENTER PRESENTATION                     │
│   • Displayed on Parent Web Dashboard (/insights)                      │
│   • Displays supporting metric evidence & rationale                    │
│   • REQUIRES EXPLICIT PARENT APPROVAL (Zero automated policy change)   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Deterministic Recommendation Rules

1. **Physical Exercise Priority**:
   - **Condition**: Squat Challenge has $\ge 10$ completed trials **AND** 5-minute reopen rate is $\ge 20\%$ lower than Mindful Pause.
   - **Recommendation**: Recommend Squat Challenge for target application.
   - **Rationale**: Physical challenges provide somatic habit disruption with zero short-term rapid reopens.
2. **Calming Box Breathing Priority**:
   - **Condition**: Box Breathing has $\ge 10$ completed trials **AND** 5-minute reopen rate is $\ge 15\%$ lower than Mindful Pause.
   - **Recommendation**: Recommend Box Breathing for target application.
   - **Rationale**: Box breathing breaks rapid compulsion loops during evening entertainment hours.
3. **Mindful Pause Baseline**:
   - **Default**: Mindful Pause provides low-friction cognitive awareness for standard daily usage.

---

## 4. Future AI / Gemini Advisor Integration Interface
The recommendation engine interface is intentionally decoupled to allow a future cloud or on-device Gemini LLM advisor to consume aggregated summaries without interfering with the local real-time policy engine:

```
Aggregated Daily Summaries (Firestore)
               ↓
Optional Gemini Policy Advisor (Cloud / Parent Hub)
               ↓
Structured Recommendation & Parental Summary
               ↓
Parental One-Tap Approval
               ↓
CloudPolicyDto Sync to Child Device Room DB
               ↓
Deterministic Local PolicyEngine Enforcement
```

> [!IMPORTANT]
> Real-time app blocking decisions will **NEVER** depend on an LLM or network call. AI advisors operate strictly out-of-band at the parental configuration level.
