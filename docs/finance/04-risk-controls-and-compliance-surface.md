# Risk Controls & Compliance Surface

## Scope

- **Audience**
  - Risk, compliance, internal audit, and finance leadership.
- **Goal**
  - Describe how Mari enforces risk controls around payments.
  - Explain what evidence and logs exist to support audits.

## High-Level Control Framework

- **Pre-settlement controls**
  - Validation of coupon structure and expiry.
  - Optional physics checks (location grid, motion, bio hash consistency).
  - Sentinel ML risk scoring.

- **Settlement controls**
  - Settlement only after risk approval.
  - Bank-side balance checks and ledger updates.
  - Signed increment keys as tamper-evident receipts.

- **Post-settlement controls**
  - Transaction journaling in Mongo.
  - Label events (PRE_SETTLEMENT / SETTLEMENT_OUTCOME) for learning and audit.
  - Metrics and logs for monitoring performance and anomalies.

## What Mari Does Not Do (Financially)

- **No penalties**
  - No penalty fees or punitive charges in the codebase.
- **No interest or credit**
  - No interest accrual or loan products.
  - No revolving credit lines.
- **No internal KYC engine**
  - Mari expects bank-side systems to handle KYC/AML and legal customer identity.

## Controls at Each Stage

### 1. Intent & Physics Capture (Device)

- **Data captured**
  - Parties (via pseudonymous IDs).
  - Amount.
  - Physics snapshot (location grid, motion, timestamp).

- **Control objective**
  - Establish that the transaction corresponds to a specific time, place, and device.

### 2. Core Ingestion & Validation

- **Checks**
  - Coupon parse and expiry.
  - Signature verification (device key matches registered key ID).
  - Optional physics validation:
    - Grid consistency.
    - Motion pattern consistency.
    - Bio hash match.

- **Outcomes**
  - Reject malformed or suspicious inputs before money movement.

### 3. Sentinel Risk Scoring

- **Inputs**
  - Pseudonymous, non-PII features derived from:
    - Device key ID, seal, grid, amount, time to expiry, coupon hash.

- **Model behavior**
  - Produces a risk score (0–999) and model ID.
  - Core compares score to configurable threshold.

- **Control mode**
  - **Fail-close** (recommended for production):
    - If Sentinel is unreachable → reject transaction.
  - **Fail-open** (for testing):
    - If Sentinel is unreachable → allow transaction and log that risk was not evaluated.

### 4. Bank Settlement & Increment Keys

- **Controls**
  - Settlement requested only for transactions that passed risk checks.
  - Bank-side logic:
    - Confirms account existence and sufficient funds.
    - Applies commission rules.
    - Updates ledger balances.
    - Issues increment keys (per user or per batch) signed by HSM.

- **Auditability**
  - Increment keys link physical ledger state changes to specific coupon hashes and timestamps.

### 5. Journaling, Events & Monitoring

- **Transaction journal (Mongo)**
  - Provides a detailed per-transaction view for:
    - Who, what, when, where (coarse), how (rail).
    - Status, errors, and physics presence.

- **Events for training and audit**
  - `PRE_SETTLEMENT` and `SETTLEMENT_OUTCOME` events include:
    - Key risk features.
    - Final result (SUCCESS vs failure category).

- **Metrics & alerts**
  - Sentinel metrics: request counts, latency, score distributions, model versions.
  - Trainer metrics: event consumption health.
  - These can be wired into dashboards and alerting.

## Compliance-Relevant Surface

- **Traceability**
  - Each transaction is traceable through:
    - Coupon hash.
    - Transaction record.
    - Sentinel score and model ID.
    - Settlement increment key.
    - Training events.

- **Data minimization**
  - Core avoids storing direct PII:
    - Uses grid instead of raw GPS.
    - Uses bio hashes and device IDs instead of names/phone numbers.

- **Division of responsibilities**
  - **Mari Core / Sentinel**
    - Provide risk analysis and rich contextual logs.
    - Do not manage legal account onboarding.
  - **Bank / HSM**
    - Own customer identity, KYC/AML, and regulatory reporting.

## How Risk & Compliance Teams Can Use This

- **Risk policy design**
  - Tune Sentinel thresholds and commission levels based on:
    - Loss rates per risk band.
    - Customer acceptance rates.

- **Audit examinations**
  - Demonstrate:
    - That high-risk patterns were blocked before settlement.
    - That settlement events correspond to signed increment keys.

- **Incident investigation**
  - For a suspicious case:
    - Locate core transaction record.
    - Link to Sentinel score and features.
    - Confirm ledger changes via increment keys.
    - Review surrounding events to detect patterns.
