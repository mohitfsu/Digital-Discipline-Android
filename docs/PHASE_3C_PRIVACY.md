# Phase 3C — Privacy Architecture & Non-Surveillance Guarantees

## 1. Non-Surveillance Architectural Mandate
Digital Discipline is engineered as a **habit cultivation and parental mindfulness tool**, explicitly rejecting spyware, tracking, and surveillance paradigms.

---

## 2. Strictly Prohibited Data Categories
Under no circumstances does Digital Discipline collect, store, transmit, or process:

1. ❌ **Keystrokes / Keylogging**
2. ❌ **Private Messages / Chat Transcripts**
3. ❌ **Screen Recordings or Screenshots**
4. ❌ **Browser History or Full URLs**
5. ❌ **Notification Text / Personal Alerts**
6. ❌ **Microphone / Audio Recordings**
7. ❌ **Camera / Photo Capture**
8. ❌ **Real-Time GPS Location**
9. ❌ **Contact Lists or Address Books**

---

## 3. Allowed Behavioral Metadata
The only data captured by the system consists of anonymous application package identifiers and intervention lifecycle timings:

| Metadata Field | Description | Privacy Impact |
| :--- | :--- | :--- |
| `packageName` | Android application ID (e.g. `com.google.android.youtube`) | Identifies targeted app category only |
| `timestamp` | Ephemeral wall-clock timestamp | Grouping hourly and daily intervals |
| `interventionType` | `PAUSE`, `BREATHING`, `SQUATS`, or `PARENT_OVERRIDE` | Categorizes friction modality |
| `status` / `outcome` | `STARTED`, `COMPLETED`, `ABANDONED`, `EXITED` | Measures behavioral friction response |
| `earnedSeconds` | Duration of temporary unlock granted | Tracks controlled screen-time budget |
| `reopenWithin5Minutes` | Boolean flag (true/false) | Determines Habit Interruption Rate |

---

## 4. Local-First Data Retention & Aggregation
- **Granular Event Retention**: Raw `intervention_events` are stored strictly within the local Room SQLite database on the child's physical device and pruned on a rolling schedule.
- **Single Daily Rollup**: Only aggregated totals (total minutes, block count, habit interruption percentage) are synchronized to Cloud Firestore, limited to **one single write per device per day**.
- **Tenant Isolation**: Firestore rules ensure each family's aggregated summaries are strictly accessible only by the authenticated parent account owning that family.
