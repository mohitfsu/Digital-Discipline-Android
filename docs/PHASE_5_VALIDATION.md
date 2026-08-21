# PHASE 5 — VALIDATION ENGINE
## Movement, Timer, Cognitive & Manual Validation Specifications

---

## 1. Movement Sensor Validation
- **Hardware Signals**: Accelerometer magnitude (\(\sqrt{x^2 + y^2 + z^2}\)) with dynamic gravity baseline cancellation.
- **Anti-Circumvention Filters**:
  - Spike Filter: Drops values \(> 28.0\) m/s² (dropping/hitting phone).
  - Rate Limiter: Rejects repetitions faster than 650ms (prevents frantic phone shaking).
  - Amplitude Thresholds: Requires minimum downward plunge (\(-2.2\)) and upward rebound (\(+2.2\)).

## 2. Timer Validation
- **Monotonic Protection**: Driven by `SystemClock.elapsedRealtime()`; immune to user tampering with device calendar/wall-clock time.

## 3. Cognitive Micro-Interactions
- **Randomized Micro-Challenges**: Arithmetic, color sequence reproduction, speed tap ascending order, visual reaction tests.
- **Validation**: Strict answer verification; incorrect attempts increment error state and require re-trying without bypass.

## 4. Manual Confirmation
- **Honest Feedback**: Used strictly for actions that cannot be honestly proven with phone sensors alone (e.g. Pull-ups, Drinking Water).
