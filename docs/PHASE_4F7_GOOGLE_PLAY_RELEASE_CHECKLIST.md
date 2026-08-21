# Phase 4F-7: Google Play Release Preparation Checklist

## 1. App Identity & Android Package Information
- **Application ID**: `com.digitaldiscipline.spike`
- **App Name**: Digital Discipline
- **Version Name**: `1.0.0-prod-foundation`
- **Version Code**: `1`
- **Target SDK**: 35 (Android 15)
- **Min SDK**: 26 (Android 8.0 Oreo)

## 2. Release Artifacts & Package Identity
- **Release Bundle (AAB)**: `app/build/outputs/bundle/release/app-release.aab`
  - **Size**: `16,371,412 bytes` (16.37 MB)
  - **SHA-256 Checksum**: `2F87E15E5A23902D1512EA4F77DEE94CB126658A78FFC20CA17605772A06C263`
- **Release Standalone APK**: `app/build/outputs/apk/release/app-release.apk`
  - **Size**: `16,839,384 bytes` (16.84 MB)
  - **SHA-256 Checksum**: `602A480D39A043441B2E8FC744E49CD2BB2E86C1885A3790E4A3B2221FE5E539`

## 3. Google Play Store Listing & Declarations
- **Privacy Policy URL**: [OWNER INPUT REQUIRED]
- **Data Safety Declaration**:
  - Personal Data Collection: None (0 bytes collected or transmitted).
  - Third-Party Tracking / Ad SDKs: None.
  - Device Identifiers / Diagnostics: Stored locally only; zero analytics transmitted.
- **AccessibilityService Declaration**:
  - Declared Purpose: Assistive digital self-discipline and positive friction intervention for distracting application usage.
  - `canRetrieveWindowContent`: Set to `false` (Zero screen content or keystroke observation).
- **Content Rating**: Everyone (PEGI 3 / ESRB Everyone).
- **Target Audience**: 13+ (Students, professionals, general self-improvement).
- **App Category**: Productivity / Health & Fitness.

## 4. Store Graphics & Metadata Assets
- **App Icon (512x512 PNG)**: [OWNER INPUT REQUIRED]
- **Feature Graphic (1024x500 PNG)**: [OWNER INPUT REQUIRED]
- **Phone Screenshots (Min 2, 1080x1920)**: [OWNER INPUT REQUIRED]
- **Short Description (Max 80 chars)**: Mindful digital discipline with positive friction and earned screen time.
- **Full Description**: [OWNER INPUT REQUIRED]

## 5. Rollout & Release Tracks
- **Track Strategy**: Internal Testing $\rightarrow$ Closed Testing (Alpha/Beta) $\rightarrow$ Staged Production Rollout (10% $\rightarrow$ 50% $\rightarrow$ 100%).
- **Rollback Strategy**: Maintain previous APK/AAB build for instant rollback if catastrophic crash regression is discovered.
