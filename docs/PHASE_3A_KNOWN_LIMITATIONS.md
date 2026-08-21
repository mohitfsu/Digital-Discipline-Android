# Phase 3A — Known Android & OEM Limitations

## 1. Android Platform Constraints

### A. Accessibility Service Permission Lifecycle
- **Limitation**: Android OS restricts automated re-enabling of an Accessibility Service if a user explicitly disables it in Android Settings.
- **Mitigation**: Digital Discipline employs the `TamperDetector` subsystem which constantly monitors the active service binding. If revoked, the parent dashboard immediately flags `PROTECTION DISABLED` and provides direct deep-link navigation back to Accessibility Settings.

### B. Device Reboot Service Binding Latency
- **Limitation**: Upon a cold hardware reboot, Android does not immediately bind to custom AccessibilityServices until the user completes the first device unlock (Credential Encrypted storage decryption).
- **Mitigation**: `BootCompletedReceiver` listens to both `ACTION_LOCKED_BOOT_COMPLETED` and `ACTION_BOOT_COMPLETED` to prepare the database and schedules as early as the Android OS permits.

---

## 2. OEM-Specific Aggressive Power Managers

### A. Xiaomi / Redmi / Poco (MIUI & HyperOS)
- **Limitation**: MIUI Security includes an aggressive RAM cleaner that occasionally terminates non-whitelisted background processes when launching memory-intensive games.
- **Mitigation**: `OemBatteryHelper` provides 1-tap navigation to the MIUI Autostart and Battery Saver screens (`com.miui.securitycenter`), guiding parents to set the app to "No restrictions".

### B. Samsung One UI (4.x – 6.x)
- **Limitation**: Samsung's "Device Care" may place apps that haven't been directly opened in several days into "Deep Sleep", pausing background WorkManager sync.
- **Mitigation**: Exclude Digital Discipline from sleeping apps via "Never sleeping apps" in Settings $\rightarrow$ Battery $\rightarrow$ Background usage limits.

### C. OnePlus / Oppo / Realme (ColorOS & OxygenOS)
- **Limitation**: "Quick Launch" and "Auto-Freeze" mechanisms can prevent background network sync when the screen is off for extended periods.
- **Mitigation**: Enable "Allow background activity" and "Allow auto-launch" in App Management.

---

## 3. Temporary Unlock Boundaries

### A. Monotonic Clock Resets on Hardware Reboot
- **Limitation**: `SystemClock.elapsedRealtime()` resets to zero when a physical phone restarts.
- **Mitigation**: `TemporaryUnlockEntity` evaluates both monotonic elapsed time AND the persistent `createdAt` timestamp. If `currentElapsedRealtime < unlockGrantedElapsedRealtime`, the system recognizes a hardware restart occurred and immediately expires the unlock session (fail-closed security).
