# Phase 4C-3: Behaviour Momentum Score

## 1. Overview
The Behaviour Momentum Score is a deterministic, explainable 0–100 score that represents a user's recent self-regulation velocity and consistency.

---

## 2. Mathematical Formulation

$$\text{Momentum} = \sum_{i=1}^{7} (\text{Component}_i \times \text{Weight}_i)$$

| Component | Weight | Calculation Basis |
| :--- | :---: | :--- |
| **Goal Consistency** | 20% | $(\text{Active Days in Last 7 Days} / 7) \times 100$ |
| **Habit Interruption Rate** | 20% | $(\text{Total Attempts} - \text{5m Rapid Reopens}) / \text{Total Attempts} \times 100$ |
| **Urge Reopen Control** | 15% | $(100 - \text{5m Rapid Reopen Rate})$ |
| **Challenge Completion** | 15% | $(\text{Completed Challenges} / \text{Total Attempts}) \times 100$ |
| **Goal Target Progress** | 15% | $(\text{Target Met Days in Last 7 Days} / 7) \times 100$ |
| **Screen-Time Trend** | 10% | Scaled based on daily foreground minutes ($<2\text{h} \rightarrow 100$, $2\text{--}4\text{h} \rightarrow 75$, $>4\text{h} \rightarrow 50$) |
| **Plan Adherence** | 5% | Consistency of active monitoring and active policies |

---

## 3. Momentum Classifications & Supportive Language

| Score Range | State | Badge | Supportive Coaching Narrative |
| :---: | :---: | :---: | :--- |
| **90 – 100** | `STRONG_MOMENTUM` | 🔥 | "Your discipline is compounding powerfully." |
| **75 – 89** | `BUILDING_MOMENTUM` | ⚡ | "You're building solid consistency day by day." |
| **50 – 74** | `INCONSISTENT` | 🌱 | "You're making progress. Focus on your vulnerable hours." |
| **25 – 49** | `NEEDS_RESET` | 🔄 | "Take a breath and start small with simple challenges." |
| **0 – 24** | `NEEDS_ATTENTION` | 💡 | "Keep going. Each interrupted distraction is a win." |
| **<10 Samples** | `INSUFFICIENT_DATA` | ⚪ | "Building baseline data (complete more sessions to unlock score)." |
