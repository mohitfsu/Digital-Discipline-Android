# Phase 4F-3: Production Security Audit

## 1. Static Security Assessment
- **Component Exposure**: Only `MainActivity` is exported with `android.intent.action.MAIN` and `android.intent.category.LAUNCHER`.
- **Accessibility Service**: Protected by `android.permission.BIND_ACCESSIBILITY_SERVICE`, not exported.
- **PendingIntents**: All PendingIntents declare `PendingIntent.FLAG_IMMUTABLE`.
- **Broadcast Receivers & WorkManager**: Internal application context only.

## 2. Cryptographic Controls
- **Parent PIN Protection**: Salted PBKDF2WithHmacSHA256 hash stored via Android Keystore AES-256 GCM (`EncryptedSharedPreferences`).
- **Monotonic Time Enforcement**: Monotonic clock (`SystemClock.elapsedRealtime()`) prevents wall-clock manipulation from extending active unlock sessions.
