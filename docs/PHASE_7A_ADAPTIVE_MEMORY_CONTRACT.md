# PHASE 7A — ADAPTIVE MEMORY CONTRACT
## Privacy-First Persistent Adaptive Memory Architecture & Specification

---

> **CRITICAL PRINCIPLE**  
> Digital Discipline must remember only what it needs to answer:  
> *"What intervention is most likely to help this person regain control right now?"*  
> It must never become a historical surveillance system. The product should remember less, not more.  
> **Status**: DESIGN ONLY. No database modifications, migrations, or code changes are performed in Phase 7A.

---

## 1. Current Adaptive State Inventory

The in-memory adaptive system introduced across Phases 6A, 6B, and 6C maintains the following transient runtime structures in `InterventionAdaptiveStore`:

| Structure | Key Format | Contents | Current Lifetime |
|---|---|---|---|
| `outcomesHistory` | Sequence index | Bounded `CopyOnWriteArrayList<InterventionOutcome>` (max 200 items). Contains `sessionId`, `interventionId`, `targetPackage`, `startedAtRealtimeMs`, `completedAtRealtimeMs`, `status`, `rewardSeconds`, `helpfulness`, `timestampMs`. | In-memory process lifetime |
| `globalStatsMap` | `interventionId` | `InterventionStats`: `startedCount`, `completedCount`, `helpedCount`, `didNotHelpCount`, `totalFeedbackCount`, `lastUsedTimestampMs`. | In-memory process lifetime |
| `categoryStatsMap` | `category` (enum) | Aggregated `InterventionStats` across all interventions in that category. | In-memory process lifetime |
| `triggerStatsMap` | `"$targetPackage:$interventionId"` | Target package-specific `InterventionStats`. | In-memory process lifetime |
| `contextStatsMap` | `"$targetPackage:${timeBucket}:$interventionId"` | Contextual `InterventionStats` (package + morning/afternoon/evening/night). | In-memory process lifetime |
| `sessionCounter` | Atomic counter | Integer for feedback sampling rate calculations (e.g. 20% rate). | In-memory process lifetime |

---

## 2. Proposed Persisted Fields

To survive Android process termination and device reboots without persisting sensitive behavioral logs, the system should persist **aggregated statistical summaries**, rather than raw timestamped event logs.

### Aggregate Table: `intervention_adaptive_aggregates`

| Field | Type | Why Required? | Selection Impact | Sensitivity | Persistence Required? | Retention | Derived? |
|---|---|---|---|---|---|---|---|
| `aggregateKey` | `TEXT` (PK) | Unique composite key (`GLOBAL:id`, `CAT:category`, `TRIG:pkg:id`, `CTX:pkg:bucket:id`) | Indexes the evidence level in O(1) time | Low | Yes | 90 days with decay | No |
| `evidenceLevel` | `TEXT` | Identifies hierarchy layer (`GLOBAL`, `CATEGORY`, `TRIGGER`, `CONTEXT`) | Controls confidence weighting & fallback | None | Yes | 90 days | No |
| `interventionId` | `TEXT` | ID of the intervention definition | Direct candidate mapping | None | Yes | 90 days | No |
| `targetPackage` | `TEXT?` | Target distraction app (null for GLOBAL/CAT) | Enables Level 2 & 3 trigger personalization | Low (standard Android app ID) | Yes | 90 days | No |
| `timeBucket` | `TEXT?` | Coarse time bucket (`MORNING`, `AFTERNOON`, `EVENING`, `NIGHT`) | Enables Level 3 contextual suitability | Low | Yes | 90 days | No |
| `startedCount` | `INTEGER` | Total intervention starts | Calculates completion rate (25% weight) | None | Yes | 90 days | No |
| `completedCount` | `INTEGER` | Validated completions | Calculates completion rate (25% weight) & confidence | None | Yes | 90 days | No |
| `helpedCount` | `INTEGER` | Count of `HELPED` responses | Numerator for helpfulness rate (40% weight) | None | Yes | 90 days | No |
| `didNotHelpCount` | `INTEGER` | Count of `DID_NOT_HELP` responses | Denominator for helpfulness rate | None | Yes | 90 days | No |
| `totalFeedbackCount`| `INTEGER` | Total user responses | Determines helpfulness denominator & confidence | None | Yes | 90 days | No |
| `lastUpdatedTimestampMs`| `INTEGER` | Epoch millis of last update | Drives exponential decay calculation | Low | Yes | 90 days | No |

### Fields Derived in Memory (NOT Persisted):
- `completionRate` = $\frac{\text{completedCount}}{\text{startedCount}}$ (Derived)
- `helpfulnessRate` = $\frac{\text{helpedCount}}{\text{totalFeedbackCount}}$ (Derived)
- `confidence` = $\min\left(1.0, \frac{\text{totalFeedbackCount} + \lfloor\text{completedCount}/2\rfloor}{10}\right)$ (Derived)
- `hierarchicalHelpfulnessEstimate` (Derived via hierarchical blending)
- `scoreBreakdown` / `totalScore` (Derived at trigger evaluation)

---

## 3. Data That Must NEVER Be Persisted

The following data streams are explicitly prohibited from collection, storage, and persistence:

```
❌ Screenshots or screen contents
❌ Keystrokes or text input
❌ URLs or web browsing history
❌ Microphone or audio recordings
❌ Camera or video feeds
❌ Precise GPS or network location
❌ Contacts, call logs, or SMS/messages
❌ Notification text or payloads
❌ Free-form user feedback notes
```

---

## 4. Persistence Granularity: Aggregates vs. Event History

| Factor | Raw Event Stream Persistence | Aggregated Statistics (Recommended) |
|---|---|---|
| **Privacy Footprint** | 🔴 High (retains detailed minute-by-minute user behavior history) | 🟢 Zero behavioral tracking (only stores counters: 9 helped, 1 not helped) |
| **Disk Growth** | 🔴 Unbounded (grows linearly with every distraction attempt) | 🟢 Fixed / Bounded ($O(N_{\text{interventions}} \times N_{\text{packages}})$, <50 KB total) |
| **Query Latency** | 🔴 Slow ($O(N)$ aggregation scan required at startup) | 🟢 Instant ($O(1)$ single-batch memory load at app start) |
| **Security Risk** | 🔴 High forensic exposure if device backup is inspected | 🟢 Completely non-sensitive aggregate numbers |

**Decision**: Persist **only aggregated counters**. Do NOT persist raw outcome histories.

---

## 5. Parent / Child Privacy Boundary

```
+-------------------------------------------------------------------------+
|                              CHILD DEVICE                               |
|                                                                         |
|  - Stores local aggregate stats (e.g. Instagram + Breathing = 9 helped) |
|  - Learns locally which interventions resolve distractions              |
|  - Adaptive learning is 100% private to the child device                |
+-------------------------------------------------------------------------+
                                     │
                 [ Boundary: Policy In, No Telemetry Out ]
                                     │
+-------------------------------------------------------------------------+
|                              PARENT DEVICE                              |
|                                                                         |
|  - Sets hard rules: Absolute Block, Delays, Allowed Hours               |
|  - Sets safety restrictions: Disabled categories, mandatory exercises   |
|  - Receives high-level compliance summaries (e.g. "Completed Challenge")|
|  - CANNOT inspect child's private adaptive ratings or feedback choices  |
|  - ZERO behavioral surveillance dashboard                               |
+-------------------------------------------------------------------------+
```

---

## 6. Retention & Exponential Time Decay

To prevent old behavioral patterns from permanently locking the user into a historical profile, a deterministic half-life decay is applied:

### Half-Life Model (30-Day Half-Life):
When loading aggregates from disk or computing stats where $\Delta t = \text{now} - \text{lastUpdatedTimestampMs} > 30\text{ days}$:
\[
\text{Decay Factor } D = 2^{-\frac{\Delta t}{30\text{ days}}}
\]
\[
\text{Effective HelpeCount} = \lfloor\text{helpedCount} \times D\rfloor
\]
\[
\text{Effective TotalFeedback} = \lfloor\text{totalFeedbackCount} \times D\rfloor
\]

- **90-Day Hard Eviction**: Any aggregate with no activity for $>90\text{ days}$ is purged from storage.
- **Smooth Transition**: The decay factor scales smoothly without cliff effects or ranking instability.

---

## 7. Reset Semantics: "Reset Adaptive Learning"

A user-facing or parent-authorized reset operation must execute with the following strict contract:

```
[ RESET ADAPTIVE LEARNING ]
├── PURGES:
│   ├── All rows in intervention_adaptive_aggregates table
│   └── In-memory statsMap and outcomesHistory in InterventionAdaptiveStore
├── RESTORES:
│   ├── Deterministic cold-start baseline (0.50 score)
│   └── Zero confidence (C = 0.0) across all interventions
└── PRESERVES INTACT:
    ├── Parent Policy rules (BLOCK, DELAY, schedules)
    ├── Self Mode policy configurations
    ├── EarnedTimeWalletService ledger balance & transactions
    └── Complete 35-item InterventionCatalog
```

---

## 8. Storage Technology Evaluation

| Criteria | Option A: Extend Room (v9) | Option B: DataStore (Proto/Preferences) | Option C: Encrypted Local JSON | Option D: Hybrid |
|---|---|---|---|---|
| **Structural Fit** | 🟢 Native relational table with primary key lookups | 🟡 Requires serialization of large map structures | 🔴 Manual parse/serialize and concurrency handling | 🟡 Added architectural complexity |
| **Atomic Transactions** | 🟢 SQLite ACID transactions | 🟡 Atomic file swap only | 🔴 Risky file locking | 🟡 Two separate transaction boundaries |
| **Migration Path** | 🟢 Standard Room migration (`8 -> 9`) with test harness | 🟡 Separate storage system outside existing Room DB | 🔴 Bespoke migration scripts | 🔴 Split migration overhead |
| **Concurrency & Async** | 🟢 Flow / suspend DAO queries on Dispatchers.IO | 🟢 Coroutine Flow support | 🔴 Requires manual mutex | 🟡 Multi-layer sync |
| **Corruptions** | 🟢 SQLite WAL mode resilience | 🟢 Atomic writes | 🔴 Prone to partial writes | 🟡 Two failure domains |
| **Recommendation** | **WINNER (Option A)** | Alternative | Rejected | Rejected |

**Verdict**: **Extend Room to Version 9 in Phase 7B**. Room is the established, tested database engine in Digital Discipline. Adding a single lightweight `intervention_adaptive_aggregates` table requires 0 external dependencies.

---

## 9. Performance & Asynchronous I/O Model

```
CRITICAL ENFORCEMENT PATH (<58ms Budget)
---------------------------------------------------------
Trigger Event ──> InterventionSelector.select() ──> <1ms (Reads In-Memory Cache)
Overlay Render ──> Immediate Display

ASYNCHRONOUS PERSISTENCE PATH (Background Coroutines)
---------------------------------------------------------
Intervention Completed ──> Wallet Transaction (Authoritative)
                     ──> Update In-Memory Store
                     ──> Launch Dispatchers.IO Coroutine ──> Upsert Adaptive Aggregate (SQLite)
```

- **Cold Boot Loading**: During `Application.onCreate()` / service init, all active aggregates are loaded in a single batch query ($<5\text{ms}$) into `InterventionAdaptiveStore`.
- **Zero Blocking I/O on UI/Accessibility Thread**: All SQLite writes occur off the main thread via Kotlin coroutines.

---

## 10. Failure Isolation & Graceful Fallback

```
+-------------------------------------------------------------+
|                      FAILURE SCENARIOS                      |
+-------------------------------------------------------------+
| 1. SQLite Disk Full / Storage Unavailable                   |
|    ──> In-memory store continues functioning                |
|    ──> Enforcement & Wallet operate unobstructed            |
+-------------------------------------------------------------+
| 2. Database Corruption / Schema Incompatibility             |
|    ──> Fall back to deterministic cold-start selection      |
|    ──> Re-initialize empty in-memory store                  |
|    ──> Zero impact on parent enforcement or screen limits   |
+-------------------------------------------------------------+
| 3. Process Killed Mid-Write                                 |
|    ──> SQLite WAL journal automatically rolls back          |
|    ──> Authoritative wallet balance is unaffected           |
+-------------------------------------------------------------+
```

---

## 11. Security Model

- **App-Private Storage**: Stored exclusively within the app's internal sandboxed directory (`/data/data/com.digitaldiscipline.spike/databases/`).
- **Android Sandboxing**: Enforced by Linux UID permissions; non-accessible to third-party apps without root.
- **Backup Exclusion**: Excluded from Android cloud auto-backups (`android:allowBackup="false"`) to prevent telemetry leakage across devices.
- **Package Name Privacy**: Uses standard reverse-domain application identifiers (e.g. `com.instagram.android`) already present in Android OS app management.

---

## 12. Proposed Room Schema Specification (Design Only for Phase 7B)

```kotlin
// PROPOSED FOR PHASE 7B (DO NOT IMPLEMENT IN 7A)
@Entity(
    tableName = "intervention_adaptive_aggregates",
    indices = [
        Index(value = ["interventionId"]),
        Index(value = ["targetPackage"]),
        Index(value = ["evidenceLevel"])
    ]
)
data class InterventionAdaptiveAggregateEntity(
    @PrimaryKey
    val aggregateKey: String, // e.g. "GLOBAL:BOX_BREATHING", "TRIG:com.instagram.android:BOX_BREATHING"
    val evidenceLevel: String, // "GLOBAL", "CATEGORY", "TRIGGER", "CONTEXT"
    val interventionId: String,
    val targetPackage: String? = null,
    val timeBucket: String? = null,
    val startedCount: Int = 0,
    val completedCount: Int = 0,
    val helpedCount: Int = 0,
    val didNotHelpCount: Int = 0,
    val totalFeedbackCount: Int = 0,
    val lastUpdatedTimestampMs: Long = System.currentTimeMillis()
)
```

---

## 13. Test Strategy for Phase 7B Implementation

1. **Persistence & Reload Verification**: Store outcomes, terminate process / reset adaptive store, reload from database, and confirm identical hierarchical score calculations.
2. **Atomic Upsert & Concurrency**: 50 concurrent background outcome writes must not lock or corrupt database records.
3. **Half-Life Decay Tests**: Verify that 30-day and 60-day simulated time jumps decay statistical weights accurately.
4. **Reset Operation Isolation**: Verify that resetting adaptive memory purges the aggregates table while leaving wallet balances, app rules, and schedules 100% intact.
5. **Room Migration `8 -> 9` Test**: Automated Room migration test verifying table creation and zero regression on existing 21 entities.
6. **Enforcement Independence**: Simulate SQLite database exception; verify that `InterventionEngine` continues to enforce policies and credit wallets without crashing.

---

## 14. Identified Risks & Mitigations

| Risk | Severity | Mitigation |
|---|---|---|
| Database write latency impacting overlay display | Medium | Decouple completely: selector reads in-memory cache; disk write runs asynchronously in background IO coroutine. |
| Over-accumulation of stale trigger aggregates | Low | Implement 90-day time-to-live (TTL) eviction sweep during weekly maintenance. |
| Inaccurate feedback aggregation after app uninstall/reinstall | Low | Process-cleared state automatically triggers deterministic cold-start defaults. |

---

## 15. Recommendation for Phase 7B

### Immediate Next Step:
Proceed with **Phase 7B (Persistent Adaptive Store Implementation & Room Migration 8 -> 9)**:
1. Define `InterventionAdaptiveAggregateEntity` and `InterventionAdaptiveAggregateDao`.
2. Write automated Room migration `MIGRATION_8_9` with validation tests.
3. Wire `InterventionAdaptiveStore` to persist aggregates asynchronously and load on initialization.
4. Maintain 100% offline privacy, zero surveillance, and <58ms enforcement latency.
