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
  - 8-hex string (e.g. `8a2f3b91`) derived on the client from motion and other factors.
  - Appears in:
    - Coupon parameter `s=`.
    - Sentinel inputs (as `seal`).

- **Expiry (`exp` / `expiry_ts`)**
  - Millisecond timestamp indicating coupon expiry.
  - Appears in:
    - Coupon parameter `exp=`.
    - Sentinel input `expiry_ts`.

## How Physics Is Used

- **At Transaction Creation (Device)**
  - Device collects:
    - Motion vector around the time of payment initiation.
    - Location and time.
  - Device computes:
    - `grid` cell ID from location.
    - `seal` from motion (and possibly other data).
  - Device embeds these into coupon parameters `g`, `s`, `exp`.
  - Device optionally sends `physicsData` alongside coupon for server-side validation.

- **At Validation (Core)**
  - Core parses coupon to extract:
    - `exp` (expiry), `g` (grid), `s` (seal), and sender bio hash.
  - Core compares:
    - Coupon grid `g` vs `physicsData.location.grid`.
    - Coupon seal `s` vs hash of `physicsData.motion`.
    - Coupon bio vs provided bio hash.
  - Outputs structured errors:
    - `TIME_EXPIRED`, `LOCATION_MISMATCH`, `MOTION_MISMATCH`, `BLOOD_MISMATCH`.

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

- **What Physics Validation Can Detect**
  - Coupons used after their intended time window.
  - Coupons whose grid is inconsistent with observed device grid.
  - Coupons whose motion-derived seal is inconsistent with observed motion.
  - Coupons bound to a different bio hash than current context.

- **What Physics Cannot Guarantee**
  - Cannot prevent fraud from fully compromised devices that can forge sensor data.
  - Cannot prevent attacks where an adversary physically mimics motion.
  - Coarse grid cannot pinpoint exact location; it is intentionally low-resolution.

- **Attack Surface Considerations**
  - GPS spoofing apps can manipulate underlying location.
  - Emulators can simulate motion vectors.
  - Physics is therefore a **soft control** that feeds into risk scoring, not a hard gate.

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
