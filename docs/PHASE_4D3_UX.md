# Phase 4D-3: Notification UX & Copywriting Guidelines

## Core Philosophy
Notifications must feel like a personal coach quietly offering support at key moments—never like a high-pressure, manipulative marketing hook.

## Tone & Phrasing Principles
1. **Calm & Non-Shaming**: Never use words like "failed", "wasted time", "behind", or urgent exclamation marks.
2. **Action-Oriented**: Focus on the next micro-step that can be completed in minutes.
3. **Transparent**: Clear about what the user chose and earned.

## Copy Matrix

| Type | Title | Body Example | Primary CTA |
|---|---|---|---|
| `MORNING_INTENTION` | Today's focus | "Today's focus: 10 Squats before your first distraction." | [ VIEW TODAY ] |
| `NEXT_ACTION` | Your action is still waiting | "Your next action: 10 Squats. It takes just a few minutes." | [ DO IT NOW ] |
| `DISTRACTION_PREEMPTION` | A heads-up | "You usually get pulled into distractions around now. Want to do your 10 Squats first?" | [ DO IT NOW ] |
| `MISSED_ACTION` | Still time today | "Your Get Fit action isn't done yet. There's still time." | [ DO IT NOW ] |
| `SUCCESS` | Today's action: done | "You completed today's Get Fit action. +10 min earned." | [ VIEW TODAY ] |
| `EVENING_REFLECTION` | Quick check-in | "How did today's plan work for you?" | [ VIEW TODAY ] |
| `WEEKLY_REVIEW` | Your weekly review is ready | "See how your plan performed this week." | [ VIEW REVIEW ] |

## Safe Deep Links
- `digitaldiscipline://today` → Opens `TodayScreen`
- `digitaldiscipline://action/{actionId}` → Opens `DailyActionScreen`
- `digitaldiscipline://weekly-review` → Opens `SelfWeeklyReviewScreen`
