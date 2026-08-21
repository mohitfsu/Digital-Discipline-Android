# Phase 4F-1: Static Security & Component Audit

## 1. Android Component Export Analysis
- **`MainActivity`**: `android:exported="true"` with `MAIN`/`LAUNCHER` intent filters.
- **`DigitalDisciplineAccessibilityService`**: `android:exported="false"`, protected by `android.permission.BIND_ACCESSIBILITY_SERVICE`.
- **`OverlayActivity` / Services**: Not exported, internal application context only.
- **PendingIntents**: All use `FLAG_IMMUTABLE` (Android 12+ / 14+ compliant).

## 2. Cryptographic Storage
- **Parent PIN**: Hashed using PBKDF2WithHmacSHA256 with random salt; stored in EncryptedSharedPreferences (Android Keystore AES-256 GCM).
- **Brute-force Throttling**: Exponential backoff on invalid PIN attempts.

## 3. Clock Manipulation Resistance
- **Monotonic Protection**: All session validity, cooldowns, and unlock durations rely exclusively on `SystemClock.elapsedRealtime()`.
