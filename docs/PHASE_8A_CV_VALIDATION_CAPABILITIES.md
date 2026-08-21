# Phase 8A-CV: Validation Capability Matrix

## Validation Capabilities Model

| Category | Primary Validator | Supported Fallbacks | Camera Vision Supported? |
| :--- | :--- | :--- | :---: |
| **Movement** (Push-ups, Squats, Lunges, Plank, Jacks, Wall Sit, High Knees) | `CAMERA_POSE` / `DEVICE_MOTION` | `MANUAL_CONFIRMATION` | **YES** |
| **Upper Body & Posture** (Arm Circles, Shoulder Rolls, Neck Rolls) | `CAMERA_POSE` / `DEVICE_MOTION` | `MANUAL_CONFIRMATION` | **YES** |
| **Yoga & Mobility** (Cat-Cow, Child's Pose, Cobra Stretch) | `CAMERA_POSE` / `TIMER` | `MANUAL_CONFIRMATION` | **YES** |
| **Breathing** (Box Breathing, 4-7-8, Physiological Sigh) | `TIMER` | `MANUAL_CONFIRMATION` | NO |
| **Meditation** (Zen Counting, Sensory 5-4-3-2-1, Mindful Pause) | `TIMER` | `MANUAL_CONFIRMATION` | NO |
| **Physical Reset** (Cold Water Splash, 20-20-20 Eye Rest) | `TIMER` | `MANUAL_CONFIRMATION` | NO |
| **Cognitive** (Arithmetic, Memory Sequence, Reaction Speed) | `COGNITIVE_INTERACTION` | `MANUAL_CONFIRMATION` | NO |

---

## Graceful Fallback Guarantee
If camera permission is denied, hardware is busy, or lighting is poor:
1. System falls back automatically to `DEVICE_MOTION` (hardware accelerometer).
2. If sensors fail or are uncalibrated, system falls back to `TIMER` or `MANUAL_CONFIRMATION`.
3. The user is NEVER blocked or trapped in an intervention due to a sensor/camera issue.
