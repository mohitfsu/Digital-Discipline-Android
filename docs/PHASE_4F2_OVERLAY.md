# Phase 4F-2: Overlay Behavior & Multi-Window Management

## Overlay Window Invariants
- Uses `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
- **Layout Flags**: `FLAG_NOT_TOUCH_MODAL`, `FLAG_LAYOUT_IN_SCREEN`, `FLAG_WATCH_OUTSIDE_TOUCH`.
- **Multi-Window & Split Screen**: Viewport dimensions adapt dynamically to split-screen window bounds without throwing `WindowManager.BadTokenException`.
- **Touch Routing**: Intercepts touches exclusively within the challenge card view, releasing touch events once mindful pause or challenge concludes.
