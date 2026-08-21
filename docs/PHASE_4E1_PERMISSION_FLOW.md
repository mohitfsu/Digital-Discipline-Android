# Phase 4E-1: Permission Flow & Graceful Degradation

## Transparent Pre-Permission Dialog
Before launching the Android Accessibility Settings screen, Digital Discipline presents a clear, anxiety-reducing explanation:

1. **Why**: "To protect the apps you choose, Digital Discipline needs Accessibility access."
2. **What is accessed**: "We use it only to detect when a selected app comes to the foreground."
3. **What is NOT collected**: "We do not read your messages, keystrokes, screen contents, or browsing history."

## Graceful Denial Handling
If the user returns without enabling Accessibility:
- The app never crashes.
- The plan remains configured in Room.
- The UI displays: `PLAN READY — PROTECTION OFF`.
- `TodayScreen` indicates that protection needs attention, with a one-tap button to re-open the setup dialog.
