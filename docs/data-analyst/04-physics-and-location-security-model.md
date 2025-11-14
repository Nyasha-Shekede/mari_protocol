# Physics & Location Security Model

## Scope

- **Goal**
  - Describe how physics and location data are captured, encoded, and used for security.
  - Clarify limitations and assumptions for analysts reviewing spoofing and fraud risk.

## Data Inputs (Client Side)

- **Sensors**
  - Motion (accelerometer-like vector):
    - `x: number`, `y: number`, `z: number`.
  - Location (grid, not raw GPS):
    - `grid: string` (coarse cell, derived from underlying coordinates).
  - Time:
    - `timestamp` (device clock).

- **Captured Snapshot: `physicsData`**
  - Fields:
    - `location.grid: string`.
    - `motion.x: number`, `motion.y: number`, `motion.z: number`.
    - `timestamp: string | number | Date`.
  - Usage:
    - Included once per transaction at creation time.
    - Represents a coarse view of where and how the device was moved.

## Coupon Binding to Physics

- **Coupon Parameters Related to Physics**
  - `g: string` → location grid, expected to align with `physicsData.location.grid`.
  - `s: string` → seal (8-hex string) derived from motion.
  - `exp: number` → expiry timestamp.

- **High-Level Semantics**
  - **Location grid (`g`)**
    - Device-side function converts GPS or other source into a grid ID.
    - Same grid ID is embedded into coupon and sent separately as `locationGrid`.
  - **Motion seal (`s`)**
    - Device-side function hashes motion vector into an 8-hex numeric code.
    - Used as a lightweight "proof" of movement.
  - **Expiry (`exp`)**
    - Coupon is intended to be used within a constrained time window.

## Core Physics Validation

- **Inputs**
  - `coupon: string` (full coupon URL).
  - `currentPhysics: { location: { grid }, motion }`.
  - `currentBioHash?: string` (optional bio hash from context).

- **Parsing Step**
  - Core uses shared parser to extract:
    - `exp` (expiry).
    - `g` (coupon grid).
    - `s` (coupon seal).
    - `b` (sender bio hash proxy).

- **Checks Performed**
  - **Time Check**
    - Condition:
      - `exp` < current server time.
    - Effect:
      - Add error `TIME_EXPIRED`.
  - **Location Check**
    - Condition:
      - Coupon `g` equals `currentPhysics.location.grid`.
    - Effect:
      - On mismatch, add error `LOCATION_MISMATCH`.
    - Notes:
      - Demo implementation uses exact match; production would use geohash-like tolerance.
  - **Motion Check**
    - Derived value:
      - `motionHash = hash(motion.x, motion.y, motion.z)` (MD5-based, reduced to numeric).
    - Condition:
      - Absolute difference between `couponSeal` and `motionHash` < tolerance (~0.1 in demo).
    - Effect:
      - On mismatch, add error `MOTION_MISMATCH`.
  - **Bio Hash Check (Blood Analogy)**
    - Uses:
      - `b` from coupon vs `currentBioHash` (or `currentPhysics.bioHash` if present).
    - Condition:
      - Equality check.
    - Effect:
      - On mismatch, add error `BLOOD_MISMATCH`.

- **Output**
  - `isValid: boolean`.
  - `errors: Array<{ type: string }>`.

## Data Flow: Physics

```mermaid
flowchart LR
  Sensor[Device Sensors] -->|motion, GPS| App[App Physics Module]
  App -->|physicsData + coupon (g,s,exp)| Core[Core /api/transactions]
  Core -->|coupon parse| Parser[Shared MariStringParser]
  Parser --> Core
  Core --> PV[PhysicsValidationService]
  PV --> Core
  Core --> Mongo[(MongoDB transactions.physicsData)]
```

## Analytical Uses

- **Consistency Checks**
  - Compare `physicsData.location.grid` vs coupon `g` for fraud patterns.
  - Inspect distribution of `MOTION_MISMATCH` and `LOCATION_MISMATCH` across users/devices.

- **Risk Modeling Features**
  - Physics is indirectly present in Sentinel features via:
    - `grid_id` (location grid).
    - `seal` (motion-derived code).
  - Analysts can:
    - Examine correlation between `MOTION_MISMATCH` occurrences and high Sentinel scores.
    - Propose additional features or stricter validation rules.

## Limitations & Assumptions

- **Precision**
  - Grid resolution is coarse by design to protect location privacy.
  - Exact GPS coordinates are not stored server-side.
- **Device Trust**
  - Assumes sensors and OS are not maliciously tampered with.
  - Attacks via rooted devices or sensor injection are possible and should be considered.
- **Time Source**
  - Device timestamps may drift; server time is used for expiry comparisons.
- **Hashing & Seals**
  - Motion hash is a lightweight heuristic, not a cryptographic guarantee.
