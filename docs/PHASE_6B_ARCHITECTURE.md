# PHASE 6B — ADAPTIVE FEEDBACK ARCHITECTURE
## Component Interaction & Data Flow

---

## 1. System Topology
```
[ Compose Intervention Overlay ]
          │ (onSessionCompleted)
          ▼
[ InterventionEngine ] ──────────────► [ EarnedTimeWalletService ] (Authoritative)
          │                                  │
          ├──────────────────────────────────┤
          │                                  ▼
          │                            [ Ledger Entry ]
          ▼
[ shouldSampleFeedback()? ]
  ├── false ──> [ Direct Continuation ]
  └── true  ──> [ Render Feedback Chips ]
                     │ (YES / A LITTLE / NOT REALLY)
                     ▼
             [ recordFeedback(sessionId, feedback) ]
                     │
                     ▼
        [ InterventionAdaptiveStore ]
                     │
                     ▼
        [ Historical Helpfulness Rate ]
                     │
                     ▼
        [ InterventionSelector Next Loop ]
```

---

## 2. Feedback Mapping Matrix
| UI Action | Domain Value | Effect on Next Selection |
|---|---|---|
| Tap `[ YES ]` | `HelpfulnessFeedback.HELPED` | Increases historical helpfulness score (40% weight) |
| Tap `[ A LITTLE ]` | `HelpfulnessFeedback.NEUTRAL` | Neutral signal recorded; does not artificially boost/penalize |
| Tap `[ NOT REALLY ]` | `HelpfulnessFeedback.DID_NOT_HELP` | Decreases historical helpfulness score |
| Tap `[ CONTINUE ]` or Dismiss | `HelpfulnessFeedback.NOT_ASKED` | No change to helpfulness rate (prevents false negative) |
