# Phase 4E-5: Goal Lifecycle States & State Machine

## Lifecycle States Matrix

| State | Enforcement Behavior | Notifications | Accessible Via |
|---|---|---|---|
| `ACTIVE` | Positive friction enforced on distraction apps | Active | Today Dashboard |
| `PAUSED` | Self Mode positive friction silenced | Suppressed | Today Dashboard |
| `COMPLETED` | Positive friction disabled; chapter closed | Suppressed | Goal History |
| `REPLACED` | Previous triggers disabled; new plan active | Suppressed | Goal History |
| `ARCHIVED` | Permanent historical record | Suppressed | Goal History |

## Valid State Transition Matrix

$$\begin{aligned}
\text{ACTIVE} &\longrightarrow \text{PAUSED} \mid \text{COMPLETED} \mid \text{REPLACED} \mid \text{START\_FRESH} \mid \text{ARCHIVED} \\
\text{PAUSED} &\longrightarrow \text{ACTIVE} \mid \text{COMPLETED} \mid \text{REPLACED} \mid \text{START\_FRESH} \mid \text{ARCHIVED} \\
\text{COMPLETED} &\longrightarrow \text{REPLACED} \mid \text{START\_FRESH} \mid \text{ARCHIVED} \\
\text{REPLACED} &\longrightarrow \text{ARCHIVED}
\end{aligned}$$
