# Phase 4B-2 — Session Economics & Anti-Binge Guardrails

## 1. Monotonic Consumption Pipeline
All time deductions are calculated strictly against `SystemClock.elapsedRealtime()`:
$$\Delta t = \frac{\text{nowElapsed} - \text{lastHeartbeatElapsedRealtime}}{1000}$$
- **Foreground-Only**: If the user exits the target app or locks the device, consumption pauses immediately.
- **Wall-Clock Immunity**: Any manual alteration of the system clock (forward, backward, timezone changes) has $\Delta t_{\text{wall}}$ which is completely ignored.

---

## 2. Anti-Binge Limits & Economic Caps

| Guardrail | Default Value | Description |
| :--- | :---: | :--- |
| **Max Reward per Challenge** | $15\text{ mins}$ ($900\text{s}$) | Prevents excessive balance inflation from a single exercise |
| **Daily Earning Cap** | $60\text{ mins}$ ($3600\text{s}$) | Hard ceiling on total earned screen time in a 24-hour window |
| **Max Wallet Balance** | $60\text{ mins}$ ($3600\text{s}$) | Prevents hoarding unlimited screen time |
| **Max Single Session** | $30\text{ mins}$ ($1800\text{s}$) | Enforces positive friction pause after sustained continuous usage |

---

## 3. Session State Machine

```
               [ Start Session ]
                      │
                      ▼
               ┌──────────────┐
               │    ACTIVE    │◄────────── Heartbeat (every 1s)
               └──────┬───────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼             ▼             ▼
  [ Expired ]    [ Paused/Ended ]  [ Reboot Detected ]
  (Balance=0)     (App switched)   (now < started)
        │             │             │
        ▼             ▼             ▼
  ┌──────────┐  ┌───────────┐ ┌───────────────┐
  │ EXPIRED  │  │   ENDED   │ │  INVALIDATED  │
  └──────────┘  └───────────┘ └───────────────┘
```
