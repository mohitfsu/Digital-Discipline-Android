# Digital Discipline — Security Architecture Specification

**Classification**: System Security, Threat Model & Cryptographic Specification  
**Scope**: Android Client, Parent Web Portal & Cloud Synchronization Layer  

---

## 1. Threat Model & Security Perimeter

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                     THREAT BOUNDARY                                     │
│                                                                                         │
│   [UNSUPERVISED CHILD]                      [EXTERNAL ADVERSARY]                        │
│   • Advances System Clock                   • Replay Attacks on Sync API                │
│   • Clears App Data / Force-Stops           • Unauthorized Firestore Rule Tampering     │
│   • Disables Accessibility in Settings      • Fake Device Emulation                     │
│   • Guesses Parent PIN                      • API Token Theft                           │
│   ▼                                         ▼                                           │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │                         DIGITAL DISCIPLINE DEFENSE MATRIX                       │   │
│   │  • Monotonic Clock (Hardware Uptime)  • Firebase App Check (Attestation)        │   │
│   │  • PBKDF2 Hashed Keystore PIN         • Firestore Row-Level Security Rules      │   │
│   │  • Settings Accessibility Guard       • AES-256 GCM Encrypted DataStore         │   │
│   │  • Local Protection Health Heartbeat  • Asymmetric Token Pairing Protocol       │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Parent PIN & Local Cryptographic Security

### 2.1 Cryptographic Storage Protocol
- **Plaintext Forbidden**: Plaintext PIN strings are never written to disk, flash memory, or memory logs.
- **Key Derivation Function**: `PBKDF2WithHmacSHA256` with:
  - Iterations: $N = 12,000$
  - Salt: 16 bytes cryptographically secure random bytes (`java.security.SecureRandom`)
  - Key Length: 256 bits
- **Encrypted Container**: Salt and hash are persisted in `EncryptedSharedPreferences` backed by the **Android Keystore System** using AES-256 GCM master key encryption.

### 2.2 Brute-Force & Lockout Protocol
- Maximum permitted consecutive failures: **5 attempts**.
- Upon 5th failure: System enters **Hardware Lockout State for 300 seconds (5 minutes)**.
- Lockout timestamps use monotonic `SystemClock.elapsedRealtime()`, preventing children from resetting lockout by changing system wall clock.

---

## 3. Device Pairing & Identity Protocol

1. **Parent Portal (Web/App)**:
   - Authenticated parent requests a 6-character short pairing code.
   - Cloud Function creates a short-lived document in `pairing_requests/{code}` containing `{ familyId, childId, expiresAt: Now + 10min }`.
2. **Child Android Device**:
   - Parent inputs pairing code during initial onboarding.
   - Child app exchanges code for `{ deviceId, syncToken }`.
   - Child app stores `familyId`, `childId`, and `deviceId` in Encrypted DataStore.
   - Pairing request document is immediately destroyed (single-use).

---

## 4. Cloud Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper Functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isFamilyMember(familyId) {
      return isAuthenticated() && 
        exists(/databases/$(database)/documents/families/$(familyId)/parents/$(request.auth.uid));
    }

    function isEnrolledDevice(familyId, deviceId) {
      return isAuthenticated() && 
        request.auth.token.firebase.sign_in_provider == 'custom' &&
        request.auth.token.deviceId == deviceId;
    }

    // Family Root
    match /families/{familyId} {
      allow read, write: if isFamilyMember(familyId);

      // Child Profiles & Devices
      match /children/{childId} {
        allow read, write: if isFamilyMember(familyId);
      }

      match /devices/{deviceId} {
        allow read: if isFamilyMember(familyId);
        allow write: if isFamilyMember(familyId) || isEnrolledDevice(familyId, deviceId);
      }

      // Policy Document
      match /policies/{childId} {
        allow read: if isFamilyMember(familyId) || isAuthenticated();
        allow write: if isFamilyMember(familyId); // Only parent can edit policy
      }

      // Daily Aggregated Summaries
      match /daily_summaries/{summaryId} {
        allow read: if isFamilyMember(familyId);
        allow write: if isFamilyMember(familyId) || isAuthenticated();
      }
    }
  }
}
```

---

## 5. Offline Security & Replay Protections

1. **Autonomous Operation**: If the device loses internet connection, all existing rules remain in effect indefinitely from the local Room database.
2. **Replay Attack Resistance**: All policy update payloads include an incrementing integer `version` field. Devices reject incoming policies with version numbers less than or equal to the locally stored version.
3. **Monotonic Realtime Security**: All temporary unlock durations, lockouts, and rate limits compute against `SystemClock.elapsedRealtime()`. Time zone changes, NTP spoofing, and manual clock alterations have zero impact on enforcement state.
