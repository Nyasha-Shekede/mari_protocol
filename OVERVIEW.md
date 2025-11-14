# Mari Protocol – High-Level Overview

## What Mari Is

- **Mari Protocol**
  - A digital payment protocol where devices create signed, physics-bound payment "coupons" that are risk-scored by an ML engine (Sentinel) and then settled by a bank-like ledger (Bank/HSM) that issues cryptographic proofs of payment.

- **Core Components**
  - **Mari Core (mari-server)** – HTTP/SMS gateway that:
    - Receives signed coupons and optional physics data from clients.
    - Validates structure, signatures, and physics (when provided).
    - Calls **Sentinel** for fraud/risk scoring.
    - Calls **Bank/HSM** for settlement and increment-key proofs.
    - Persists transaction journals in MongoDB.
  - **Sentinel (mari-sentinel)** – Risk engine that:
    - Converts transaction context into numeric features.
    - Scores risk using ML models (0–999 score, with model ID).
    - Learns from labeled outcomes via a training pipeline.
  - **Bank/HSM (mock-bank-hsm)** – Ledger + HSM that:
    - Maintains balances for users and merchants.
    - Applies settlement and commission rules.
    - Issues signed increment keys as proofs of settlement.
  - **Shared Libraries (mari-shared-libs)** – Common types and utilities for coupons, parsing, seals, etc.

- **Mari App (Android)**
  - A **reference client**, not mandatory for production.
  - Demonstrates how to:
    - Generate physics-bound coupons.
    - Sign transactions with device keys.
    - Use HTTP and SMS rails.
    - Call the Core APIs and interpret responses.
  - In production, partners are expected to build their own apps/services on top of the protocol and SDKs.

## How a Payment Works (Short Version)

1. **Intent on Device**
   - User chooses recipient and amount.
   - App collects a physics snapshot (coarse location grid, motion, timestamp).
   - App builds a coupon describing "who pays whom, how much, until when" and includes physics-related fields (grid, seal, expiry).
   - App signs the canonical payload with a device key (ECDSA).

2. **Submit via HTTP or SMS**
   - **HTTP (online)** – App POSTs JSON to Core (`/api/transactions`).
   - **SMS (offline)** – App encodes the same coupon into SMS; an SMS gateway forwards it to Core via webhook.

3. **Core Validation & Risk**
   - Core:
     - Parses the coupon and recomputes its hash.
     - Verifies signatures using registered device keys.
     - Optionally validates physics (grid, motion, bio hash) when physicsData is attached.
   - Core calls Sentinel with a compact feature set.
   - Sentinel returns a risk score and model ID.
   - Core compares score to a threshold and either rejects or proceeds.

4. **Settlement & Proof**
   - For approved transactions, Core requests settlement from Bank/HSM.
   - Bank/HSM updates ledger balances and issues an increment key (signed payload).
   - Core records a transaction entry in MongoDB and emits label events for training.
   - Client receives a response containing `couponHash`, settlement payload, and signature.

## What Mari Does *Not* Do

- Does **not** implement penalties, punitive fees, or interest.
- Does **not** implement credit or lending products.
- Does **not** replace bank KYC/AML processes; those live with the ledger operator.

## Target Market (Finance View)

- **Primary Environments**
  - Regions where:
    - Data connectivity can be intermittent or expensive.
    - SMS is widely available and reliable.
    - Android devices are common.

- **Primary Use Cases**
  - Person-to-person payments (remittances, informal transfers).
  - Small merchant payments (shops, markets).
  - Field payouts (payroll, stipends, aid disbursements).

- **Value Proposition**
  - Digital cash with:
    - Verifiable settlement proofs.
    - Stronger fraud controls (device, physics, ML risk).
    - Better auditability for finance, risk, and regulators.

## Documentation Map by Role

- **Developers**
  - Core technical docs:
    - Service READMEs under each directory (e.g. `mari-server/README.md`, `mari-sentinel/README.md`, `mock-bank-hsm/README.md`, `mari-app/README.md`).
    - `docs/partners/01-payment-provider-integration.md` – end-to-end integration guide for external providers and wallets.

- **Data Analysts / Data Science**
  - `docs/data-analyst/`
    - `01-data-concept-and-actors.md`
    - `02-core-data-entities-and-schemas.md`
    - `03-end-to-end-data-flow-and-lineage.md`
    - `04-physics-and-location-security-model.md`
    - `05-sentinel-risk-and-training-data.md`
    - `06-storage-retention-and-privacy.md`

- **Finance / Accounting / Treasury**
  - `docs/finance/`
    - `01-mari-economic-model-and-value-proposition.md`
    - `02-settlement-lifecycle-and-ledgers.md`
    - `03-reconciliation-and-financial-reporting.md`
    - `04-risk-controls-and-compliance-surface.md`

- **Security / Cryptography**
  - `docs/security/`
    - `01-security-overview-and-threat-model.md`
    - `02-crypto-primitives-and-key-management.md`
    - `03-physics-seals-and-location-security.md`
    - `04-database-and-infrastructure-security.md`

- **UX / Product**
  - `docs/ux/`
    - `01-user-value-and-personas.md`
    - `02-core-user-flows-and-interactions.md`
    - `03-comparisons-and-positioning.md`
    - `04-concerns-limitations-and-open-questions.md`

- **Legal / Regulatory / Privacy**
  - `docs/legal/`
    - `01-regulatory-perimeter-and-roles.md`
    - `02-data-protection-and-privacy.md`
    - `03-automated-risk-decisioning-and-fairness.md`
    - `04-record-keeping-and-retention.md`

- **Platform / Ops / SRE**
  - `docs/ops/`
    - `01-operations-overview-and-runbook.md`

- **Risk Operations**
  - `docs/risk-ops/`
    - `01-risk-operations-playbook.md`

- **Customer Support / CS Ops**
  - `docs/support/`
    - `01-customer-support-playbook.md`

## How to Think About Mari in One Line (Per Team)

- **Users / UX** – "Send money from your phone and get a verifiable receipt, even when data is flaky."
- **Finance** – "A payment rail with clear settlement, commissions, and auditability."
- **Security** – "Device-signed, physics-aware transactions with an ML risk gate and HSM proofs."
- **Legal** – "A technical layer between user devices and a regulated ledger, with explicit risk and data boundaries."
- **Data** – "A well-structured stream of intent, context, decisions, and outcomes for analysis."
- **Ops** – "A set of services (Core, Sentinel, Bank/HSM, Mongo, Redis, RabbitMQ) wired together via simple HTTP and message queues."
