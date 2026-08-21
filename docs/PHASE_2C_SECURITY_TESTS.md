# PHASE 2C SECURITY TESTS — MULTI-TENANT & CLIENT ISOLATION

## 1. Security Architecture
The Web Control Center enforces tenant isolation directly through Cloud Firestore Security Rules (`firestore.rules`) and Firebase Authentication:

1. **Family Isolation**: Parents can only read and write documents in `/families/{familyId}` where `request.auth.uid` matches the `ownerParentId` or exists in `/families/{familyId}/parents/{parentId}`.
2. **Child Policy Authorization**: Children cannot write or tamper with policies. Only verified parents can write to `/families/{familyId}/children/{childId}/policy/current`.
3. **Single-Use Pairing Code Protection**: Pairing codes in `/pairing_codes/{code}` cannot be reused after `isUsed == true` or after `expiresAtTimestampMs` has elapsed (15-minute TTL).
4. **Client Privacy Guarantee**: No keystrokes, messages, recordings, notifications, or camera/microphone feeds are captured or transmitted.

---

## 2. Test Cases & Execution Matrix

| Test ID | Scenario | Expected Result | Status |
|---|---|---|---|
| **SEC-01** | Parent A attempts to read Parent B's `/families/{famB}` | Permission Denied by Firestore Rule (`isParentOfFamily`) | **PASSED** |
| **SEC-02** | Unauthenticated user attempts to read `/families/{famA}/children` | Permission Denied (`isAuthenticated() == false`) | **PASSED** |
| **SEC-03** | Child device attempts to write `/families/{famA}/children/{childA}/policy` | Permission Denied (Only Parent has write permission) | **PASSED** |
| **SEC-04** | Single-use pairing code redemption replay attack | Second redemption rejected (`resource.data.isUsed == false` condition fails) | **PASSED** |
| **SEC-05** | Expired pairing code (> 15 minutes) redemption attempt | Redemption rejected (`request.time.toMillis() <= expiresAtTimestampMs` fails) | **PASSED** |
| **SEC-06** | Local offline storage isolation | Local keys partitioned by `ownerParentId` and `childId` | **PASSED** |
