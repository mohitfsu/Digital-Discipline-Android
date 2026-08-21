# Phase 4F-1: Privacy & Data Minimization Audit

## Data Inventory & Privacy Boundaries

| Data Field | Source | Storage Location | Purpose | Can Leave Device? | Can Be Deleted? |
|---|---|---|---|---|---|
| **App Package Name** | Accessibility Event | Room (`intervention_events`) | Identify configured target apps | **NO (Local-only)** | YES |
| **Intervention Type** | User Selection | Room (`behaviour_policies`) | Challenge rendering | **NO (Local-only)** | YES |
| **Wallet Balance** | Earned / Spent Actions | Room (`wallet_ledger`) | Session economics | **NO (Local-only)** | YES |
| **Goal Title & Category** | User Input | Room (`goals`) | Goal coaching & timeline | **NO (Local-only)** | YES |
| **Keystrokes / Typing** | N/A | **NEVER COLLECTED** | N/A | **N/A** | N/A |
| **Screenshots / Visuals** | N/A | **NEVER COLLECTED** | N/A | **N/A** | N/A |
| **Audio / Microphone** | N/A | **NEVER COLLECTED** | N/A | **N/A** | N/A |
| **URLs / Browsing** | N/A | **NEVER COLLECTED** | N/A | **N/A** | N/A |

### Privacy Certification
The application strictly enforces **Zero Surveillance**. No personally identifying behavioral data is collected, stored outside Room v8, or transmitted to any cloud servers.
