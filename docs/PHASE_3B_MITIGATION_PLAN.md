# Phase 3B — Circumvention Mitigation & Hardening Plan

## 1. Guiding Mitigation Criteria
Every proposed mitigation is evaluated against 10 strict architectural questions:
1. *Does Android officially support it?*
2. *Does it require Accessibility?*
3. *Does it require Device Owner (MDM)?*
4. *Does it require VPN?*
5. *Does it require special permissions?*
6. *Does it increase battery usage?*
7. *Does it increase privacy risk?*
8. *Does it create Google Play policy risk?*
9. *Does it increase support complexity?*
10. *Is it worth adding for a tiny-team product?*

---

## 2. Mitigation Decision Framework

| Circumvention Vector | Proposed Mitigation | Evaluation & Trade-offs | MVP Recommendation |
| :--- | :--- | :--- | :---: |
| **Accessibility Revocation** | Cloud Heartbeat Reporting + Persistent Parent Alert | Supported by generic APIs; zero battery overhead; zero Play Store policy risk; clear UX. | 🟢 **ADOPT IN MVP** |
| **Settings App Blocking** | Overlay over `com.android.settings` | Violates Google Play policy; high risk of account suspension; frustrates emergency user access. | 🔴 **REJECT** |
| **Overlay Revocation** | Safe fallback warning banner + Cloud status alert | Prevents window token crash; clean error messaging; zero privacy or battery impact. | 🟢 **ADOPT IN MVP** |
| **Clock Tampering** | Dual-Timestamp Monotonic Check (`elapsedRealtime` + `createdAt`) | Fully supported; zero battery cost; prevents clock manipulation and hardware reboot extensions. | 🟢 **ADOPT IN MVP** |
| **Force Stop / Task Kill** | Background Heartbeat Timeout ($>15\text{ min}$) | Marks device as offline/unreachable on parent dashboard; non-intrusive; Play Store compliant. | 🟢 **ADOPT IN MVP** |
| **Uninstallation** | Heartbeat De-registration Alert | Supported; consumer Android allows user uninstall; notifies parent without malware-like anti-uninstall hacks. | 🟢 **ADOPT IN MVP** |
| **Mobile Web Bypass** | Local Loopback VPN / DNS Filtering | Effective for browser domains, but adds battery drain, VPN notification key, and complexity. | 🟡 **DEFER TO PHASE 4** |
| **OEM App Cloner** | Extensible App Rule Presets (`+ Add Target App`) | Parent can add cloned package name (e.g. `com.instagram.android.clone`) in 1-tap. | 🟢 **ADOPT IN MVP** |
| **Multi-User Guest Profile** | Device Owner User Restriction | Requires factory reset & enterprise provisioning; incompatible with consumer Play Store MVP. | 🔴 **REJECT (Out of Scope)** |

---

## 3. Implementation Roadmap
1. **MVP (Phase 3B / Complete)**:
   - Heartbeat-driven protection health monitoring (`PROTECTION DISABLED` alert).
   - Dual-timestamp monotonic temporary unlocks.
   - Fail-closed offline Room policy enforcement.
   - Structured local diagnostic logging.
2. **Phase 3C (Next)**:
   - Polishing parent notification webhooks and real-time dashboard status badges.
3. **Phase 4 (Future Roadmap)**:
   - Architecture evaluation of Local Loopback `VpnService` for browser domain blocking.
