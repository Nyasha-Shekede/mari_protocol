# SIM Swap Protection & Phone Number Security

## Overview

Mari Protocol uses phone numbers as **user-friendly identifiers only**, not as authentication factors. This document explains how Mari protects against SIM swap attacks and phone number hijacking.

---

## The SIM Swap Threat

### What is a SIM Swap Attack?

An attacker convinces a mobile carrier to transfer a victim's phone number to a new SIM card controlled by the attacker. This allows the attacker to:
- Receive SMS messages intended for the victim
- Bypass SMS-based 2FA
- Potentially access accounts tied to that phone number

### Why Traditional Systems Are Vulnerable

Most payment systems use phone numbers for:
- ❌ Authentication (SMS OTP)
- ❌ Transaction authorization
- ❌ Account recovery
- ❌ Primary user identifier

**Result:** SIM swap = complete account takeover

---

## Mari's Defense Strategy

### 1. Phone Number is NOT a Security Factor

**Phone numbers in Mari are used ONLY for:**
- ✅ User-friendly lookup (find recipient's account)
- ✅ SMS transport (encrypted payload delivery)
- ✅ Contact discovery (address book integration)
- ✅ Account recovery (with additional verification)

**Phone numbers are NEVER used for:**
- ❌ Transaction authorization
- ❌ Authentication
- ❌ Security verification
- ❌ Cryptographic operations

### 2. Multi-Layer Transaction Security

Every transaction requires **ALL** of these factors:

```
Transaction Authorization Requires:
┌─────────────────────────────────────────────────────────┐
│ 1. Device Key (KID)                                     │
│    - ECDSA P-256 signature from Android Keystore       │
│    - Private key never leaves device                    │
│    - Hardware-backed security                           │
│    ✓ NOT affected by SIM swap                          │
├─────────────────────────────────────────────────────────┤
│ 2. Motion Seal                                          │
│    - Unique shake pattern with nanosecond timing       │
│    - High entropy (impossible to reproduce)            │
│    - Multi-sensor fusion (accel + gyro + mag)         │
│    ✓ NOT affected by SIM swap                          │
├─────────────────────────────────────────────────────────┤
│ 3. GPS Location                                         │
│    - Physical presence verification                     │
│    - Fraud detection via location patterns             │
│    - Continuous updates during transaction             │
│    ✓ NOT affected by SIM swap                          │
├─────────────────────────────────────────────────────────┤
│ 4. bloodHash (Account ID)                              │
│    - Derived from face biometric                       │
│    - NOT tied to phone number                          │
│    - Cryptographically secure identifier               │
│    ✓ NOT affected by SIM swap                          │
└─────────────────────────────────────────────────────────┘
```

---

## SIM Swap Attack Scenario Analysis

### Scenario: Attacker Steals Victim's SIM

**What the attacker gains:**
- ✓ Can receive SMS to victim's phone number
- ✓ Can see encrypted transaction coupons
- ✓ Knows victim's phone number

**What the attacker CANNOT do:**

#### 1. Cannot Generate Valid Signatures
```
❌ Device private key is in Android Keystore
❌ Keystore is hardware-backed and device-bound
❌ Cannot export or extract private key
❌ New device = different KID = transaction rejected
```

#### 2. Cannot Reproduce Motion Seals
```
❌ Motion seal includes nanosecond timing entropy
❌ Chaotic amplification makes reproduction impossible
❌ Multi-sensor fusion (accel + gyro + magnetometer)
❌ Even victim cannot reproduce their own seals
```

#### 3. Cannot Spoof GPS Location
```
❌ Backend tracks user's location patterns
❌ Sudden location change triggers fraud alert
❌ Continuous GPS updates during transaction
❌ Location must match historical patterns
```

#### 4. Cannot Access bloodHash
```
❌ bloodHash derived from face biometric
❌ Not stored on SIM card
❌ Not tied to phone number
❌ Requires face match to generate
```

### Result: **SIM Swap Attack FAILS**

Even with full control of the phone number, the attacker cannot:
- Authorize transactions
- Access the account
- Generate valid coupons
- Bypass security checks

---

## Backend Verification Flow

### Transaction Verification (Immune to SIM Swap)

```kotlin
fun verifyTransaction(tx: Transaction): VerificationResult {
    // 1. Extract claimed identity
    val phoneNumber = tx.senderPhone
    val kid = tx.kid
    val bloodHash = tx.senderBioHash
    
    // 2. Lookup user account
    val user = getUserByPhone(phoneNumber)
    if (user == null) return REJECT("Unknown user")
    
    // 3. CRITICAL: Verify KID matches registered device
    if (kid != user.registeredKID) {
        return REJECT("Device not registered - possible SIM swap")
    }
    
    // 4. Verify ECDSA signature using KID (public key)
    if (!verifySignature(tx.seal, tx.signature, kid)) {
        return REJECT("Invalid signature - device key mismatch")
    }
    
    // 5. Verify bloodHash matches account
    if (bloodHash != user.bloodHash) {
        return REJECT("Account mismatch - possible impersonation")
    }
    
    // 6. Verify motion seal entropy
    if (!hasHighEntropy(tx.seal)) {
        return REJECT("Low entropy seal - possible replay attack")
    }
    
    // 7. Verify GPS location
    if (!isValidLocation(tx.location, user.locationHistory)) {
        return REJECT("Suspicious location - possible fraud")
    }
    
    // 8. Check Sentinel risk score
    val riskScore = sentinel.scoreTransaction(tx)
    if (riskScore > THRESHOLD) {
        return REJECT("High risk score - manual review required")
    }
    
    return APPROVE("All checks passed")
}
```

### Key Insight

**Phone number is just a lookup key.** The real verification happens through:
1. Device key (KID) - proves it's the registered device
2. Signature - proves possession of private key
3. bloodHash - proves account ownership (not phone ownership)
4. Motion + GPS - proves physical presence and intent

---

## User ID Format Options

Mari supports multiple identifier formats:

### 1. Phone Number (Convenient, SIM-swappable)
```
Format: +1234567890 or 0000001002
Use: User-friendly lookup
Security: Low (vulnerable to SIM swap)
Recommendation: Use for lookup only, not security
```

### 2. bloodHash (Secure, NOT SIM-swappable)
```
Format: 64 hex characters (SHA-256)
Example: abc123def456...789
Use: Cryptographic account identifier
Security: High (derived from face, not phone)
Recommendation: Primary identifier for security-critical operations
```

### 3. QR Code (Convenient + Secure)
```
Format: QR code containing bloodHash
Use: In-person payments
Security: High (contains bloodHash, not phone)
Recommendation: Best for peer-to-peer transactions
```

---

## Account Recovery (SIM Swap Scenario)

### If User's Phone Number is Stolen

**Victim can recover account by:**

1. **Register new device with new phone number**
   - Provide face biometric → generates same bloodHash
   - Register new KID from new device
   - Link new phone number to existing bloodHash

2. **Backend verification**
   - bloodHash matches existing account
   - Face biometric confirms identity
   - Old KID is revoked
   - New KID is registered
   - New phone number is linked

3. **Attacker's access is revoked**
   - Old KID no longer valid
   - Transactions from stolen SIM are rejected
   - Account remains secure

### Recovery Flow

```
User loses phone/SIM → Gets new device → Opens Mari app
  ↓
Captures face → Generates bloodHash → Matches existing account
  ↓
Generates new KID → Registers with backend → Old KID revoked
  ↓
Enters new phone number → Links to account → Old number unlinked
  ↓
Account recovered ✓ Attacker locked out ✓
```

---

## Security Guarantees

### What Mari Guarantees

✅ **SIM swap cannot authorize transactions**
- Device key (KID) is device-bound, not SIM-bound
- Signature verification requires original device

✅ **Phone number theft cannot access account**
- bloodHash is biometric-derived, not phone-derived
- Account identity is separate from phone identity

✅ **SMS interception cannot steal funds**
- SMS only carries encrypted coupons
- Coupons require valid signature to redeem
- Signature requires device private key

✅ **Account recovery is possible**
- Face biometric proves identity
- New device can be registered
- Old device is revoked

### What Mari Does NOT Guarantee

⚠️ **Physical device theft + biometric bypass**
- If attacker has device AND can unlock it (face/PIN)
- Mitigation: Device lock, biometric security, remote wipe

⚠️ **Malware on user's device**
- If device is compromised before transaction
- Mitigation: OS security, app sandboxing, anomaly detection

⚠️ **Social engineering for account recovery**
- If attacker can fake face biometric
- Mitigation: Liveness detection, manual review for suspicious recovery

---

## Comparison with Traditional Systems

### Traditional Banking App (Vulnerable)

```
Authentication: Phone number + SMS OTP
Transaction Auth: SMS OTP
Account Recovery: Phone number + SMS

SIM Swap Impact: ❌ COMPLETE ACCOUNT TAKEOVER
```

### Mari Protocol (Protected)

```
Authentication: Face biometric → bloodHash
Transaction Auth: Device key + Motion seal + GPS + bloodHash
Account Recovery: Face biometric + new device registration

SIM Swap Impact: ✅ NO IMPACT - All security factors intact
```

---

## Implementation Notes

### For Developers

1. **Never use phone number for authentication**
   ```kotlin
   // ❌ WRONG
   if (user.phoneNumber == inputPhone) { authenticate() }
   
   // ✅ CORRECT
   if (user.bloodHash == derivedBloodHash && 
       verifySignature(seal, signature, user.registeredKID)) {
       authenticate()
   }
   ```

2. **Always verify KID in transactions**
   ```kotlin
   // ❌ WRONG
   val user = getUserByPhone(tx.senderPhone)
   processTransaction(tx)
   
   // ✅ CORRECT
   val user = getUserByPhone(tx.senderPhone)
   if (tx.kid != user.registeredKID) throw SecurityException("KID mismatch")
   if (!verifySignature(tx.seal, tx.signature, tx.kid)) throw SecurityException("Invalid signature")
   processTransaction(tx)
   ```

3. **Treat phone number as display name only**
   ```kotlin
   // Phone number is like an email address:
   // - Convenient for users
   // - Easy to remember
   // - But NOT a security credential
   ```

---

## Conclusion

**Mari Protocol is immune to SIM swap attacks** because:

1. Phone numbers are identifiers, not authenticators
2. Security relies on device keys, not phone numbers
3. Multiple independent factors must all be valid
4. Each factor is independent of phone number ownership

**Key Principle:** 
> "Your phone number is like your email address - it helps people find you, but it doesn't prove who you are."

In Mari, **your identity is your face (bloodHash)** and **your authority is your device (KID)**, not your phone number.
