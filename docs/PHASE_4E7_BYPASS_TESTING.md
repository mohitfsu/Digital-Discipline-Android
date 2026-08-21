# Phase 4E-7: Circumvention & Bypass Resistance Audit

## Circumvention Vectors & Mitigations
1. **Home / Back Button**: Dismisses target app back to launcher; does not unlock target app.
2. **Recent Apps / Task Switcher**: Re-evaluates foreground window state on resume; triggers overlay within $\approx 58\text{ms}$.
3. **Split Screen / Multi-Window**: Accessibility window state events detect target app presence and enforce overlay across viewport.
4. **App Shortcuts & Share Sheets**: Direct activity intents intercepted on window state change.
5. **Reboot Bypass**: Block/Delay policies re-instantiate immediately upon service initialization.
