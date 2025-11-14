# Security Overview & Threat Model

## Scope

- **Audience**
  - Security engineers, cryptographers, and reviewers.
- **Goal**
  - Describe Mari's security model in terms of trust, boundaries, and threats.
  - Summarize how cryptography and physics are used to meet those goals.

## Trust Boundaries & Actors

- **End User Device**
  - Assumed honest-but-vulnerable (may be lost, stolen, or malware-infected).
  - Holds private keys in Android Keystore and collects physics data.

- **Mari Core Server**
  - Runs in a controlled environment.
  - Responsible for:
    - Validating coupons, signatures, and physics (optional).
    - Orchestrating Sentinel risk, bank settlement, and journaling.
  - Trusted to enforce protocol rules and not tamper with data.

- **Sentinel (Risk Engine)**
  - Trusted to score transactions fairly according to deployed models.
  - Does not hold balances; only influences whether settlement is requested.

- **Bank / HSM Service**
  - Source of truth for balances and settlement proofs.
  - Holds long-lived signing keys for increment keys.
  - Trusted not to leak private keys and to maintain ledger integrity.

- **External Providers (SMS, network, infrastructure)**
  - Treated as untrusted transport.
  - Messages over these rails must be validated and authenticated at Core.

## Core Security Objectives

- **Integrity of Value Transfers**
  - Payments represent genuine user intent and cannot be forged or replayed.
  - Ledger changes at the bank are matched to specific, validated coupons.

- **Resistance to Device Theft & Bot Attacks**
  - Stolen or scripted devices should be harder to use for fraud.
  - Physics and risk scoring increase cost for automated attacks.

- **Separation of Duties**
  - Core validates and orchestrates but does not hold the final ledger.
  - Bank / HSM signs settlement proofs and maintains account balances.

- **Auditability & Non-Repudiation**
  - Every accepted payment has:
    - Cryptographic receipts (increment keys).
    - Detailed logs and events suitable for forensic analysis.

## Threat Model (High-Level)

- **In-Scope Threats**
  - Stolen phones used to initiate unauthorized payments.
  - Botnets generating fake transactions at scale.
  - Scripted attacks replaying coupons.
  - Attempted tampering with server-side logic or API misuse.

- **Partial-Scope Threats**
  - Cloud infrastructure compromise (mitigated by defense-in-depth and key isolation).
  - Insider misuse of logs and data (mitigated by access control and monitoring).

- **Out-of-Scope for Mari Core**
  - Underlying banking regulatory compliance (KYC/AML) handled by bank.
  - Hardware-level attacks on secure elements (beyond standard mobile OS guarantees).

## Security Mechanisms (Summary)

- **Cryptography**
  - Device-side signatures (ECDSA) for transaction intent.
  - Bank-side signatures (RSA) for increment keys and settlement proofs.
  - Optional encryption keys for future features (user encryption keys in registry).

- **Physics-Bound Coupons**
  - Location grid, motion-derived seal, and expiry bind payments to:
    - A specific place.
    - A physical motion pattern.
    - A time window.

- **ML Risk Scoring (Sentinel)**
  - Uses pseudonymous features to score transactions before settlement.
  - Thresholds can be tuned for security vs UX.

- **Transport Normalization & Validation**
  - HTTP and SMS inputs normalized to a single transaction intake path.
  - All inputs validated for structure, signature, and optional physics.

## Security Trade-Offs & Design Choices

- **No Penalties or Credit**
  - Removes complexity and regulatory risk from the protocol layer.
  - Focus is on approve/reject and provable settlement.

- **Physics vs PII**
  - Physics and grids provide behavioral signals without storing raw GPS or personal details.
  - Reduces privacy and legal risk while still supporting security goals.

- **Pseudonymous Identifiers**
  - Bio hashes and device IDs are used instead of direct PII.
  - Bank side maps these to real identities for regulatory requirements.

## How to Read Deeper

- **Crypto details**
  - See `02-crypto-primitives-and-key-management.md`.
- **Physics & seals**
  - See `03-physics-seals-and-location-security.md`.
- **Database & infrastructure security**
  - See `04-database-and-infrastructure-security.md`.
