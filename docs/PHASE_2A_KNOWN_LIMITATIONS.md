# Digital Discipline — Phase 2A Known Limitations & Edge Cases

**Document**: `docs/PHASE_2A_KNOWN_LIMITATIONS.md`  
**Phase Scope**: Single-Device Local MVP  

---

## 1. Intentional MVP Scope Boundaries (Deferred to Future Phases)

1. **Honor-System Physical Interventions**:
   - *Current State*: The Squat Challenge presents "Complete 10 Squats" and relies on the child tapping `[ ✓ I COMPLETED IT ]`.
   - *Rationale*: Intentional for Phase 2A to validate behavioral habit friction before adding camera/MediaPipe pose geometry.
   - *Future Solution (Phase 3)*: On-device real-time pose estimation using Google MediaPipe without storing or transmitting images.

2. **Mobile Web Browser Circumvention**:
   - *Current State*: The native Instagram app (`com.instagram.android`) is blocked, but navigating to `https://www.instagram.com` in Google Chrome is not blocked.
   - *Rationale*: Deferred to Phase 2B/3 to keep Phase 2A focused strictly on native app detection.
   - *Future Solution (Phase 2B/3)*: Real-time URL address bar monitoring in major browsers or a local loopback DNS firewall.

3. **Android Settings Access**:
   - *Current State*: If a child navigates to Android Settings and turns off Accessibility, the app detects this and transitions to `PROTECTION DISABLED`, but does not physically block Settings.
   - *Rationale*: Complying strictly with Google Play policies against unauthorized OS locking.
   - *Future Solution (Phase 2B)*: Parent PIN gatekeeper on `com.android.settings`.

4. **Single-Device Configuration**:
   - *Current State*: All policy rules and statistics are managed directly on the child's phone via the local Parent Dashboard protected by Parent PIN.
   - *Rationale*: Validating the local enforcement core before adding cloud synchronization.
   - *Future Solution (Phase 2B)*: Next.js Parent Web Dashboard + Firebase synchronization.

---

## 2. OEM-Specific Android Behaviors

1. **Xiaomi HyperOS / MIUI**:
   - Aggressive battery managers can stop background services after extended idle periods.
   - *Mitigation*: Parents must enable "Autostart" and set battery saver to "No Restrictions" for Digital Discipline.
2. **OnePlus / Oppo OxygenOS & ColorOS**:
   - "App Battery Management" may kill Accessibility service during sleep.
   - *Mitigation*: Parents must toggle "Allow background activity".
3. **Samsung One UI**:
   - Must exempt Digital Discipline from "Sleeping Apps" list.
