# Phase 4F-7: Production Release Runbook

## Production Release Execution Procedure

### Step 1: Environment Preparation & Secret Injection
In the secure CI/CD build runner (e.g. GitHub Actions / GitLab CI), configure the following environment secrets:
- `RELEASE_STORE_FILE`: Base64-encoded production keystore or secure runner filesystem path.
- `RELEASE_STORE_PASSWORD`: Keystore password.
- `RELEASE_KEY_ALIAS`: Production key alias.
- `RELEASE_KEY_PASSWORD`: Key alias password.

### Step 2: Clean Build Execution
```bash
./gradlew clean
```

### Step 3: Run Full Automated Regression
```bash
./gradlew testDebugUnitTest
```
Ensure all 639 automated unit & integration tests pass with 100% success rate.

### Step 4: Build Release Bundles
```bash
./gradlew bundleRelease assembleRelease
```

### Step 5: Verify Artifact Cryptographic Checksums
```powershell
Get-FileHash -Algorithm SHA256 app/build/outputs/bundle/release/app-release.aab
Get-FileHash -Algorithm SHA256 app/build/outputs/apk/release/app-release.apk
```

### Step 6: Verify Signing Signature & Scheme
```bash
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

### Step 7: Deploy to Google Play Console
1. Log into Google Play Console.
2. Navigate to **Digital Discipline** $\rightarrow$ **Production** (or **Closed Testing**).
3. Upload `app-release.aab`.
4. Review release notes and rollout percentage.
5. Submit for Google Play Store review.
