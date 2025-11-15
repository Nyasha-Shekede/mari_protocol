# Crypto Primitives & Key Management

## Scope

- **Audience**
  - Security engineers and cryptographers.
- **Goal**
  - Document which cryptographic primitives are used.
  - Describe how keys are generated, stored, and used in Mari.

## Key Types & Roles

- **Device Signing Keys (Client ECDSA)**
  - Purpose:
    - Bind payment intent (coupon) to a specific device and user.
    - Provide non-repudiation and device authentication.
  - Type:
    - ECDSA P-256 keypair held in Android Keystore (hardware-backed).
  - Implementation:
    - Generated via `KeyPairGenerator` with `AndroidKeyStore` provider.
    - Private key NEVER leaves device (hardware security module if available).
    - Uses `KeyGenParameterSpec` with:
      - Algorithm: `KEY_ALGORITHM_EC`
      - Curve: `secp256r1` (P-256)
      - Digest: `SHA256withECDSA`
      - Purpose: `PURPOSE_SIGN | PURPOSE_VERIFY`
  - Identifiers:
    - `kid` (16-char hex) — Short identifier derived from public key (first 16 chars of Base64-encoded public key).
    - `spki` (base64-encoded SubjectPublicKeyInfo) — Full public key registered with Core.
  - Usage:
    - Device signs motion seal with private key:
      - `signature = ECDSA_Sign(seal, privateKey)`
    - Transaction includes: `seal`, `signature`, `kid`
    - Core verifies signature using stored `spki`:
      - `ECDSA_Verify(seal, signature, publicKey)`

- **Bank HSM Signing Keys (Server RSA)**
  - Purpose:
    - Sign increment keys and batch settlement proofs.
  - Type:
    - RSA keypair (RSA-PSS with SHA-256 in the demo implementation).
  - Identifiers:
    - `HSM_KID` (key identifier) embedded in payloads.
  - Usage:
    - HSM signs canonical JSON representation of settlement payloads.

- **Optional User Encryption Keys**
  - Purpose:
    - Future support for encrypting sensitive payloads to user keys.
  - Type:
    - RSA-OAEP public keys (`encSpki`) registered per user.
  - Current status:
    - Present in registry but not actively used in core flows.

## Primitives & Algorithms

- **Device → Core Signing**
  - Algorithm:
    - ECDSA over P-256 or equivalent, with SHA-256.
  - Message format:
    - Canonical JSON structure sorted by key.
  - Verification on core:
    - Uses SPKI public key stored in `deviceRegistry`.
    - Accepts standard ECDSA encodings (IEEE P1363 or DER) depending on implementation.

- **Core → HSM → Core Signing**
  - Algorithm:
    - RSA-PSS with SHA-256 (mock HSM implementation).
  - Message format:
    - Canonicalized JSON payload with fixed fields.
  - Verification:
    - Clients can verify using HSM public key.
    - Public key retrieval exposed by HSM service.

- **Hashing**
  - Coupons:
    - `couponHash = SHA-256(coupon_string)` used for:
      - Idempotency.
      - Joining risk / settlement / training data.
  - Physics motion hash:
    - MD5-based hash reduced to numeric in the demo (not used as a cryptographic guarantee).
  - Feature hashing:
    - Simple string hash function for Sentinel features (non-cryptographic).

## Key Management Flows

### Device Key Registration

- **Process**
  - Device generates ECDSA keypair locally.
  - Device sends registration request to Core:
    - `kid`: 8-hex key identifier.
    - `spki`: base64-encoded public key.
    - Optional `encSpki` and `userId` mapping.
  - Core stores in-memory mappings:
    - `kid → spki` (deviceKeys map).
    - `userId → encSpki` (userEncKeys map).

- **Security Considerations**
  - Private keys never leave device (Android Keystore).
  - `kid` is small and can be brute-forced; integrity relies on SPKI binding and signature verification, not secrecy of `kid`.

### Transaction Signing

- **Step 1: Canonicalization**
  - Device builds a minimal intent object:
    - `{ from, to, amount, grid, coupon }`.
  - Keys are sorted to produce deterministic JSON.

- **Step 2: Signing**
  - Device signs canonical JSON with its ECDSA private key.
  - Sends:
    - `kid`, `sig`, and the original fields to Core.

- **Step 3: Verification**
  - Core retrieves `spki` for `kid`.
  - Recomputes canonical JSON from received fields.
  - Verifies `sig` over the canonical bytes.

### Settlement Signing (Increment Keys)

- **Payload Structure**
  - Single payment increment key:
    - `USER_ID`, `AMOUNT`, `COUPON_HASH`, `TIME_NS`, `VERSION`, `HSM_KID`.
  - Batch increment key (merchant):
    - Variation summarizing merchant account balance and batch.

- **Signing Procedure**
  - HSM sorts payload keys and serializes JSON.
  - Signs with RSA-PSS SHA-256.
  - Returns `{ payload, SIG }` to Core.

- **Verification Procedure**
  - Any verifier obtains HSM public key (PEM) and `HSM_KID`.
  - Reconstructs canonical JSON from `payload`.
  - Verifies `SIG` using RSA-PSS with SHA-256.

## Key Storage & Rotation (Demo vs Production)

- **Demo Implementation**
  - Device keys:
    - Stored on device in Android Keystore.
    - SPKI stored in Core’s in-memory registry (non-persistent).
  - HSM keys:
    - Generated at startup if not provided via environment.
    - Stored in memory within HSM process.

- **Production Considerations**
  - Device keys:
    - Still local to device; Core’s registry should be backed by persistent store and/or HSM.
    - Consider key revocation mechanisms for compromised `kid`s.
  - HSM keys:
    - Must be generated and stored in dedicated HSM / KMS.
    - Should support rotation and key versioning aligned with `HSM_KID`.
  - Public key distribution:
    - Well-defined channel for publishing trusted HSM public keys to clients.

## Security Properties & Limitations

- **What the Signatures Guarantee**
  - Device signatures:
    - Ensure that the coupon and context were approved by a device with the correct private key.
  - HSM signatures:
    - Ensure that a balance update was authorized by the HSM for a specific payload.

- **What They Do Not Guarantee**
  - They do not prove that the human user is legitimate (KYC/AML is handled by the bank).
  - They do not prevent a fully compromised device from signing fraudulent transactions.

## Recommendations for Security Reviewers

- **Focus Areas**
  - Canonicalization correctness (no ambiguity in signed payloads).
  - Key lifecycle (registrations, revocations, rotations).
  - Integrity of HSM public key distribution.

- **Future Enhancements**
  - Persistent, auditable device key registry.
  - Stronger motion seal hashing if used beyond heuristic checks.
  - Mutual TLS between components to protect keys in transit.
