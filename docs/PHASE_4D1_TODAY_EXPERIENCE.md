# Phase 4D-1: Today Experience Architecture

## 1. Component Breakdown

### Header Section
- Determines greeting using local device hour:
  - 05:00–11:59: "Good morning"
  - 12:00–16:59: "Good afternoon"
  - 17:00–21:59: "Good evening"
  - 22:00–04:59: "Good night"
- Sub-navigation pills provide instant access to Insights, Plan, and Parent mode.

### Primary Goal Card
- Displays category icon (💪 Fitness, 📚 Study, 🎯 Productivity, 🧘 Mindfulness, 📖 Reading, 😴 Sleep).
- Visual progress bar and percentage completion.
- Weekly active days count.

### One Thing to Focus On
- Dynamically derives coaching guidance from `BehaviourPatternEngine` and `BehaviourWeeklyIntelligenceEngine`.
- If $\ge 10$ telemetry points: Identifies the user's primary distraction window and recommends a specific reset behavior.
- If insufficient data: Displays an encouraging baseline message.

### Earned Time Wallet
- Displays current balance and active session countdown (if running).
- Prompts user to complete challenges when balance is exhausted.

### Daily Reflection & Summary
- Appears once per day to collect mood (`🙂 Good`, `😐 Okay`, `😓 Difficult`) and helpful factors.
- Displays celebratory daily completion metrics upon submission.
