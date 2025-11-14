# Data Protection & Privacy Overview

## Scope

- **Audience**
  - Legal, regulatory, data protection officers, privacy teams.
- **Goal**
  - Describe what data Mari processes and stores.
  - Clarify identifiability levels and privacy design choices.

## Categories of Data

- **Pseudonymous Identifiers**
  - `senderBioHash`, `receiverBioHash` (bio hashes).
  - Device key IDs (`kid`).
  - `USER_ID` fields in HSM increment keys.

- **Transaction Data**
  - Amount, counterparties (via pseudonyms), timestamps.
  - Coupon string representing payment intent.
  - Transport method (HTTP/SMS).

- **Context & Security Data**
  - Physics snapshots (location grid, motion vector, timestamp).
  - Risk features sent to Sentinel (hashed identifiers, seal, grid, time-to-expiry).
  - Sentinel scores and model IDs.

- **Operational Data**
  - Logs (errors, decisions, health checks).
  - Metrics (latency, error rates, score distributions, model versions).

## Data Minimization Design Choices

- **No Direct PII in Core Transaction Store**
  - Names, phone numbers, raw GPS coordinates are **not** stored in the core transaction collection.
  - Core uses hashes and grids instead.

- **Coarse Geolocation**
  - Location is stored as a grid ID, not raw latitude/longitude.
  - Grid resolution is chosen to balance security signal with privacy.

- **Hashed Identifiers for Risk**
  - Sentinel uses numerical hashes of device IDs, seals, and grids.
  - Feature space is numeric and abstracted from raw tokens.

- **Limited Retention in Event Pipeline**
  - RabbitMQ queue for training events has a 24h TTL by default.
  - Events are intended to be transient, with long-term storage decisions left to operators.

## Identifiability Levels (Legal View)

- **Direct Identifiers**
  - Usernames, phone numbers, government IDs, raw GPS.
  - Stored and managed primarily in the **bank/identity** systems, not in Mari Core.

- **Pseudonymous Identifiers**
  - Bio hashes and user IDs used by Core.
  - May be linkable to individuals when combined with bank records.

- **Quasi-Identifiers**
  - Grids, timestamps, amounts, risk scores.
  - May allow re-identification if combined with other data sources.

- **Derived Features**
  - Hashed/sealed features used by Sentinel.
  - Designed to be less directly identifying, while retaining security signal.

## Data Flows & Storage Locations (Summary)

- **Core MongoDB**
  - Stores transaction journals with pseudonymous IDs and optional physics.

- **Sentinel Redis & Files**
  - Stores models, not raw user transaction logs.

- **RabbitMQ**
  - Transient storage of `TransactionEvent` messages for training.

- **Bank / HSM**
  - Holds ledger balances and HSM increment key data, linked to legal identities.

## Legal Considerations

> **Note:** Implementation is designed to be privacy-supportive, but legal classification depends on jurisdiction and use.

- **Controller / Processor Roles**
  - Likely:
    - Bank/operator is controller for customer data.
    - Mari Core/Sentinel components may act as processors or joint controllers, depending on contractual structure.

- **Lawful Basis for Processing**
  - Typically framed around:
    - Contractual necessity (processing payments).
    - Legitimate interests in fraud prevention and security.

- **International Transfers**
  - Need to consider:
    - Data center locations for Core, Sentinel, and Bank.
    - Applicable cross-border transfer rules (e.g. GDPR, other regimes).

- **Data Subject Rights**
  - Design questions for operators:
    - How users can access, rectify, or erase their data in the transaction journal.
    - How to handle rights requests that intersect with mandatory record-keeping.

## Policy & Documentation Hooks

- **Privacy Notice**
  - Should:
    - Explain that physics and device information are used for fraud prevention.
    - Clarify that the bank/ledger operator is responsible for customer identity and funds.

- **Data Processing Agreements (DPAs)**
  - Between operator and any third parties running Sentinel or related components.

- **Records of Processing Activities**
  - Should reference:
    - Core transaction processing.
    - Sentinel risk scoring as a fraud prevention activity.
