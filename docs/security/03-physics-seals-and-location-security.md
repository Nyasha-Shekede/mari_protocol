# Physics Seals & Location Security

## Scope

- **Audience**
  - Security engineers, cryptographers, and fraud specialists.
- **Goal**
  - Explain how physics (motion, location, time) is captured and bound into payments.
  - Clarify what guarantees the physics seal provides and what it does not.

## Data Elements

- **Location Grid (`g` / `grid_id` / `locationGrid`)**
  - Coarse cell derived from underlying GPS or similar coordinates.
  - Appears in:
    - Coupons: `g=` parameter.
    - Client physics: `physicsData.location.grid`.
    - Core DB: `locationGrid` and `physicsData.location.grid`.
    - Sentinel features: `grid_id` → hashed to `grid_hash`.

- **Motion Vector (`motion`)**
  - Simplified reading of device motion:
    - `x: number`, `y: number`, `z: number`.
  - Appears in:
    - Client physics: `physicsData.motion`.
    - Core DB: `physicsData.motion`.

- **Seal (`s`)**
  - 32-hex string (128-bit, e.g. `8a2f3b91c4e5d6f7...`) derived on the client from motion and other factors.
  - **High Entropy Generation:**
    - Includes: Accelerometer (x,y,z), Gyroscope (x,y,z), Magnetometer (x,y,z)
    - Includes: Light sensor, Temperature sensor
    - Includes: Nanosecond timestamp (System.nanoTime())
    - Includes: Millisecond timestamp (System.currentTimeMillis())
    - Result: SHA-256 hash of all sensor data → 128-bit seal
  - **Impossible to Reproduce:**
    - Nanosecond timing creates unique entropy for each transaction
    - Chaotic amplification (10-15x variable based on timing)
    - Multi-sensor fusion makes exact reproduction impossible
    - Even the same user cannot reproduce their own seals
  - Appears in:
    - Coupon parameter `s=`.
    - Sentinel inputs (as `seal`).
    - Signed by device key: `sig=` parameter contains ECDSA signature of seal.

- **Expiry (`exp` / `expiry_ts`)**
  - Millisecond timestamp indicating coupon expiry.
  - Appears in:
    - Coupon parameter `exp=`.
    - Sentinel input `expiry_ts`.

## How Physics Is Used

- **At Transaction Creation (Device)**
  - **Step 1: User shakes phone**
    - Motion tracking captures sensor data with noise filtering (0.8 m/s² threshold)
    - Accumulates motion until threshold reached (150+ units)
    - Requires continuous GPS lock (transaction blocked without GPS)
  - **Step 2: Device collects high-entropy data**
    - Accelerometer (x, y, z) - motion patterns
    - Gyroscope (x, y, z) - rotation patterns
    - Magnetometer (x, y, z) - orientation
    - Light sensor - ambient light level
    - Temperature - device heat
    - Nanosecond timestamp - timing chaos
    - Millisecond timestamp - additional entropy
    - GPS location - physical presence
  - **Step 3: Device computes seal**
    - Concatenates all sensor data with timestamps
    - Computes SHA-256 hash → 128-bit seal
    - Signs seal with device private key (ECDSA)
    - Generates `kid` from device public key
  - **Step 4: Device embeds into coupon**
    - Coupon parameters: `g`, `s`, `sig`, `kid`, `exp`
    - Full format: `Mari://xfer?from=...&to=...&val=...&g=...&exp=...&s=...&sig=...&kid=...`
  - Device sends transaction with all security factors.

- **At Validation (Core)**
  - **Step 1: Parse coupon**
    - Extract: `exp`, `g`, `s`, `sig`, `kid`, sender bio hash
  - **Step 2: Verify device key (KID)**
    - Lookup user by sender bio hash
    - Verify `kid` matches user's registered device key
    - Reject if KID mismatch (possible device theft or SIM swap)
  - **Step 3: Verify cryptographic signature**
    - Retrieve device public key using `kid`
    - Verify ECDSA signature: `ECDSA_Verify(seal, signature, publicKey)`
    - Reject if signature invalid (tampered seal or wrong device)
  - **Step 4: Verify physics constraints**
    - Check expiry: `currentTime < exp`
    - Check location: `g` matches expected grid patterns
    - Check seal entropy: High entropy required (no repeated seals)
  - **Step 5: Verify bio hash**
    - Coupon bio hash must match sender account
  - Outputs structured errors:
    - `TIME_EXPIRED`, `LOCATION_MISMATCH`, `LOW_ENTROPY_SEAL`, `BLOOD_MISMATCH`, `KID_MISMATCH`, `INVALID_SIGNATURE`.

- **At Risk Scoring (Sentinel)**
  - Sentinel uses:
    - `seal` (as a hashable string) to detect low-entropy or repeated seals.
    - `grid_id` to detect location patterns.
    - `expiry_ts` to compute `time_to_expiry`.

## Security Intent

- **Goals**
  - Make it harder to:
    - Script transactions without physical presence (bot farms).
    - Reuse coupons in different locations or times.
  - Provide additional signals for ML to distinguish legitimate vs automated behavior.

- **Non-Goals**
  - Physics is not a replacement for cryptographic signatures.
  - Physics does not provide identity; it provides behavioral context.

## Guarantees & Limitations

- **What Physics + Crypto Validation GUARANTEES**
  - ✅ **Device Authentication**: Transaction came from registered device (KID verification)
  - ✅ **Non-Repudiation**: User cannot deny making transaction (ECDSA signature)
  - ✅ **Replay Protection**: Each seal is unique and cannot be reused (nanosecond entropy)
  - ✅ **Time Binding**: Coupons expire and cannot be used outside time window
  - ✅ **Location Binding**: Grid inconsistencies detected and flagged
  - ✅ **Physical Presence**: Motion seal requires actual device movement
  - ✅ **Bot Resistance**: Impossible to script due to chaotic entropy requirements

- **What Physics + Crypto Can Detect**
  - Coupons used after their intended time window
  - Coupons from unregistered or stolen devices (KID mismatch)
  - Tampered seals (signature verification fails)
  - Low-entropy seals (scripted/automated attacks)
  - Repeated seals (replay attacks)
  - Location anomalies (unusual grid patterns)
  - Bio hash mismatches (account impersonation)

- **What Physics + Crypto Cannot Guarantee**
  - Cannot prevent fraud from fully compromised devices with root access
  - Cannot prevent attacks where adversary has physical device + unlock credentials
  - Coarse grid cannot pinpoint exact location (intentionally low-resolution for privacy)
  - Cannot prevent sophisticated emulator attacks that fake all sensors + hardware keystore

- **Attack Surface Considerations**
  - **Mitigated Attacks:**
    - ✅ SIM swap (KID is device-bound, not SIM-bound)
    - ✅ Replay attacks (unique entropy per transaction)
    - ✅ Bot farms (motion seal impossible to script)
    - ✅ Remote attacks (requires physical device)
  - **Remaining Risks:**
    - ⚠️ Physical device theft + biometric bypass (mitigated by device lock)
    - ⚠️ Malware on device (mitigated by OS sandboxing)
    - ⚠️ Advanced emulator with hardware keystore emulation (very difficult)
  - **Defense in Depth:**
    - Physics + Crypto is **hard control** (cryptographic verification)
    - Risk scoring is **soft control** (ML-based anomaly detection)
    - Combined approach provides multiple layers of security

## Design Rationale

- **Privacy vs Security Balance**
  - Use of grids instead of raw coordinates reduces long-term privacy risk.
  - Seals encode motion into low-entropy numeric form, not raw sensor streams.

- **Defense in Depth**
  - Physics sits alongside:
    - Device signatures.
    - Sentinel risk scoring.
    - Bank-side ledger controls.
  - It is one layer among many, not a single point of defense.

- **Analytical Value**
  - Physics-related fields can be used to:
    - Detect abnormal patterns (e.g. repeated seals, unlikely grid transitions).
    - Calibrate ML features and thresholds.

## Recommendations for Security Reviewers

- **Review points**
  - Correctness of coupon parsing and parameter extraction.
  - Logic of physics validation tolerances and error handling.
  - Interactions between physics validation outcomes and Sentinel thresholds.

- **Potential Hardening Steps**
  - Stronger binding between seals and device keys (e.g. keyed seals).
  - More robust motion feature extraction (e.g. sequences, not single vectors).
  - Integration with device attestation (e.g. SafetyNet / Play Integrity).
