# Phase 4E-3: Habit Momentum Formula & Invariants

## Concept Definition
Habit Momentum represents recent behavioral consistency in interrupting distraction triggers and completing positive friction. It is fundamentally different from a binary streak:
- **Streaks**: One missed day resets progress to zero, inducing anxiety, shame, and abandonment.
- **Habit Momentum**: A missed day causes a modest attenuation (-4 pts), while subsequent recovery (+8 pts) is recognized and rewarded.

## Exact Mathematical Formula

$$\text{Score} = \text{clamp}\Big(\text{ConsistencyPts} + \text{StrongBonus} + \text{VolumeBonus} + \text{RecoveryBonus} + \text{FirstWinBonus} - \text{MissedPenalty},\; 0,\; 100\Big)$$

Where:
- $\text{ConsistencyPts} = \frac{\text{MeaningfulDays}}{7} \times 50$ (Max: 50 pts)
- $\text{StrongBonus} = \min(15,\; \text{StrongDays} \times 5)$ (Max: 15 pts)
- $\text{VolumeBonus} = \min(15,\; \text{TotalInterventions} \times 3)$ (Max: 15 pts)
- $\text{RecoveryBonus} = \min(15,\; \text{RecoveryCount} \times 8)$ (Max: 15 pts)
- $\text{FirstWinBonus} = 5\text{ if First Win is completed, else }0$
- $\text{MissedPenalty} = \text{MissedDays} \times 4$

## Momentum Tiers & Narratives

| Score Range | Tier | Narrative |
|---|---|---|
| 70–100 | `STRONG_MOMENTUM` | "You are getting better at interrupting the habit." |
| 40–69 | `BUILDING_MOMENTUM` | "You're building solid consistency day by day." |
| 15–39 | `GETTING_STARTED` | "Small interruptions become habits." |
| 0–14 | `NEEDS_ATTENTION` | "One small action today is enough to get back." |
