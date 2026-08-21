# Phase 4F-4: Versioning Strategy & Upgrade Policy

## Semantic Versioning Matrix
- **`versionCode`**: `1` (Monotonically incrementing integer for Google Play Store updates).
- **`versionName`**: `"1.0.0-prod-foundation"` (Major.Minor.Patch semantic release format).

### Upgrade & Migration Policy
- Monotonic database schema migrations (currently at Room v8).
- DataStore preference keys remain backwards-compatible with fallback default values.
