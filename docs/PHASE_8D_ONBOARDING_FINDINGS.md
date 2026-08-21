# Phase 8D — Onboarding Findings & Known Limitations

## P0 — Critical (None)

No critical blocking issues identified.

## P1 — High Priority

### P1-01: Screen 9 auto-refresh on permission grant
**Finding**: After granting Accessibility or Overlay permission, the system may not immediately
update `isAccessibilityGranted`/`isOverlayGranted` states since `updatePermissionStates()` is
called in `onResume`. The user may need to return to the app for the UI to refresh.
**Mitigation**: `onResume` in `MainActivity` calls `updatePermissionStates()`, so returning from
Settings immediately refreshes. Explicit "Refresh" button not added to keep UI clean.
**Impact**: Minor UX delay on permission grant feedback.

### P1-02: Step 7 skip path
**Finding**: When user taps "SKIP FOR NOW" on Screen 7 (micro-intervention), they proceed to
Screen 8 without `microDone = true`. This is intentional (not a bug), but means the user
bypasses the product demo.
**Decision**: Acceptable. The intervention is optional during onboarding.

## P2 — Medium Priority

### P2-01: App list loading on UI thread
**Finding**: `GoalTemplateRepository.getAllDistractionRecommendations()` and the installed app
scan in `allDistractions = remember { }` both run synchronously on the composition thread.
**Impact**: Potential brief jank (< 200ms) on very old devices with many installed apps.
**Mitigation**: The `remember {}` block only runs once on first composition. Modern devices
(API 29+) handle this without visible lag.

### P2-02: Display name optional — not surfaced in onboarding
**Finding**: The user never sees a "What should we call you?" field in the onboarding.
The display name is populated if Firebase auth has a display name (future work) or defaults
to empty (showing "Today" in TodayScreen).
**Recommendation**: Add an optional name field to Screen 0 or Screen 10 in a future iteration.

### P2-03: Category selection default pre-selects 3 categories
**Finding**: `MOVEMENT`, `BREATHING`, `COGNITIVE` are pre-selected on Screen 6. Users who
don't change these may not realize they can deselect or add more.
**Decision**: Acceptable. Defaults are well-chosen and clearly labeled as changeable.

## P3 — Low Priority / Future Enhancements

- **Screen 0 skip**: Advanced users cannot skip the cinematic opening. Could add a "skip" link
  after 2 seconds for power users.
- **Screen 4 aha moment**: Uses hard-coded year projections. A more accurate calculation
  based on actual daily average screen time would be stronger.
- **Screen 7 animation**: The breath circle dots don't animate (pulse/glow). A pulsing
  animation would improve the meditation UX.
- **Haptic feedback**: No haptic feedback on card selection. Adding it would improve premium feel.
- **Dark overlay gradient**: Screen 0 background could use a subtle animated gradient instead
  of flat `Bg0` for more cinematic depth.

## Architecture Contract Violations (None)

No violations found:
- ✅ Room VERSION = 9 (unchanged)
- ✅ No second PolicyEngine
- ✅ No second wallet instance
- ✅ Parent precedence unaffected
- ✅ No cloud/network dependencies added
- ✅ No analytics SDK added
- ✅ No screenshot/screen recording
- ✅ Existing test files unmodified
