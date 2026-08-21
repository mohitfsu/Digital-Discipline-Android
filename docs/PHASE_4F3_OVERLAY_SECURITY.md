# Phase 4F-3: Overlay Window Security

## Overlay Safety & Invariants
- Uses `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
- Touch interception strictly confined to positive friction challenge views.
- Clean lifecycle release prevents lingering window leaks.
- Bypass resistance across Recent Apps, Home, and Back navigation verified.
