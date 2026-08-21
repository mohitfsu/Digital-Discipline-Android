# Digital Discipline — Phase 2B Security & Threat Model Report

**Classification**: Security Specification & Test Results  
**Target Milestone**: Phase 2B Minimal Cloud Control Plane  

---

## 1. Threat Model & Security Objectives

| Threat Actor | Vector | Mitigation Strategy | Validation Status |
| :--- | :--- | :--- | :---: |
| **Malicious Parent A** | Attempting to read/modify Family B or Child B in Firestore | Enforced in `firestore.rules` via `isParentOfFamily(familyId)` check against `request.auth.uid`. | ✅ VERIFIED |
| **Child Device** | Attempting to query arbitrary families or alter policies | Paired device only queries assigned `familyId` & `childId`. Cloud policy write permissions restricted strictly to parents. | ✅ VERIFIED |
| **Attacker Replay** | Reusing a previously intercepted pairing code | Single-use validation (`isUsed == false`) and immediate update lock in security rules. | ✅ VERIFIED |
| **Brute Force Pairing** | Guessing 6-digit pairing codes | Short 15-minute TTL, rate-limiting on client, and random distribution across $10^6$ combinations. | ✅ VERIFIED |
| **Network Interception** | Man-in-the-middle during policy download | All Firestore and Auth traffic is strictly TLS 1.3 encrypted over HTTPS/gRPC. | ✅ VERIFIED |
| **Policy Rollback** | Forcing device to revert to an older permissive policy | Monotonic version check (`cloudPolicy.version >= localVersion`) before committing to Room. | ✅ VERIFIED |

---

## 2. Firestore Security Rules Test Matrix

```
1. Parent A reads Family A data                       ──► [ ALLOWED ]
2. Parent A reads Family B data                       ──► [ DENIED (403 Permission Denied) ]
3. Parent A updates Child B in Family B               ──► [ DENIED (403 Permission Denied) ]
4. Unauthenticated user creates family                ──► [ DENIED (401 Unauthenticated) ]
5. Paired Device updates its own device status        ──► [ ALLOWED ]
6. Paired Device modifies child policy document       ──► [ DENIED (403 Permission Denied) ]
7. Device redeems code within 15m                     ──► [ ALLOWED ]
8. Device redeems code after 15m                      ──► [ DENIED (Code Expired) ]
9. Device redeems already used code                   ──► [ DENIED (Already Used) ]
```

---

## 3. Pairing Security & Device Identity Guarantee

- **No Hardware Fingerprinting**: The pairing mechanism uses a randomly generated installation UUID generated upon first run and stored in Android Keystore EncryptedSharedPreferences / Jetpack DataStore.
- **Google Play Compliance**: Meets all Google Play User Data policies by completely avoiding access to `READ_PRIVILEGED_PHONE_STATE`, `ANDROID_ID`, or `IMEI`.
