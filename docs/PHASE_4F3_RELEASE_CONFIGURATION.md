# Phase 4F-3: Release Configuration Audit

## Build Variant & Release Separation
- **`debug`**: Debuggable enabled, standard debug signing key.
- **`release`**: Minification and resource shrinking enabled; debug logs stripped; keystore signing configuration prepared for Phase 4F-4.
- **`versionCode` / `versionName`**: Configured cleanly in `app/build.gradle.kts`.
- **SDK Compatibility**: `minSdk = 26`, `targetSdk = 34`.
