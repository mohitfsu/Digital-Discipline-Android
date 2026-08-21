# Phase 4F-4: Production Signing Architecture

## Release Keystore Integration Guidelines
- Production release keystores must **NEVER** be committed into version control.
- In CI/CD pipelines (e.g. GitHub Actions / Fastlane), inject keystore secrets via environment variables:
  - `KEYSTORE_BASE64`
  - `RELEASE_KEYSTORE_PASSWORD`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASSWORD`
- For local builds, developers configure optional `local.properties` or environment variables without altering Git-tracked files.
