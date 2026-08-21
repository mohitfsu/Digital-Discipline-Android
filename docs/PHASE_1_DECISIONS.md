# Digital Discipline — Phase 1 Architecture Decisions & Tradeoffs

**Document**: `docs/PHASE_1_DECISIONS.md`  
**Phase**: Phase 1 — Production Foundation  
**Status**: Approved & Baseline Established  

---

## 1. Summary of Decisions

| # | Architectural Decision | Choice Made | Key Rationale | Alternatives Considered & Rejected |
| :---: | :--- | :--- | :--- | :--- |
| **D01** | **Persistence Engine** | **Room Database (SQLite)** | Acid-compliant, type-safe, supports Flow streams, reliable survival across process death and reboots. | SharedPreferences (unstructured), Realm (heavy bloat), Cloud Firestore (requires network). |
| **D02** | **Preferences Storage** | **Jetpack DataStore (Preferences)** | Modern asynchronous replacement for SharedPreferences; prevents UI thread deadlocks. | Legacy SharedPreferences (deprecated, prone to ANRs). |
| **D03** | **Parent PIN Security** | **PBKDF2WithHmacSHA256 + Android Keystore** | High cryptographic work factor (12,000 iterations), salt randomization, encrypted storage via hardware Keystore. | Plaintext PIN (insecure), MD5/SHA-256 without salt (vulnerable to rainbow tables). |
| **D04** | **Overlay View Engine** | **`ComposeView` on `TYPE_APPLICATION_OVERLAY`** | Declarative state handling, smooth Compose animations (breathing pulse), clean UI maintenance. | Imperative Android `LinearLayout` (cumbersome, brittle UI state management). |
| **D05** | **Temporary Unlock Timing** | **`SystemClock.elapsedRealtime()`** | Monotonic hardware uptime clock completely prevents clock fast-forward bypasses. | `System.currentTimeMillis()` (vulnerable to system time manipulation). |
| **D06** | **Cloud Synchronization** | **Daily Summary Aggregation Only** | Reduces cloud writes by 99.6%, keeps monthly cloud costs below \$75 at 100k users. | Real-time event streaming to Firestore (astronomical cloud bill, battery drain). |
| **D07** | **AI Integration Strategy** | **Advisory-Only Interface (`InterventionRecommendationEngine`)** | Real-time blocking must execute in ~58ms on device; AI must never be a point of failure. | Real-time Cloud LLM blocking calls (unacceptable 800ms+ latency, breaks offline). |

---

## 2. Deferred Features & Out-of-Scope Items for Phase 1

1. **Firebase Authentication & Cloud Sync Engine**: Deferred to Phase 2.
2. **Parent Next.js Web Dashboard**: Deferred to Phase 2.
3. **On-Device MediaPipe Pose Verification**: Deferred to Phase 3.
4. **Cloud Gemini Recommendation Engine**: Deferred to Phase 3/4.
5. **Subscription & Payment Processing**: Deferred to commercial launch phase.
