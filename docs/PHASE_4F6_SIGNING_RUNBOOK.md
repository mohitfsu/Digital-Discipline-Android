# Phase 4F-6: Production Signing Runbook & Keystore Protocol

## Release Signing Architecture
1. **Local Verification**: The local test candidate is signed using Gradle debug signing fallback for reproducible offline QA.
2. **Production Deployment**: Production signing requires injection of the production keystore through the controlled CI/CD release environment (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
3. **Zero Secrets In Repository**: Zero production keys or credentials are committed to version control.
