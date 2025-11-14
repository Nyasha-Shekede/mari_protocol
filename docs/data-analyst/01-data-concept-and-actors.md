# Mari Data Concept & Actors

## Scope

- **Audience**
  - Data analysts, risk analysts, compliance reviewers.
- **Goal**
  - Explain what Mari does in data terms.
  - Show who the actors are and what identifiers they use.
  - Connect business intent to the data that flows through the system.

## Problem & Motivation (Data View)

- **Problem**
  - People need to move value between phones and merchants with high fraud risk (stolen devices, bots, spoofed GPS).
  - Legacy systems hide risk signals inside opaque bank or card networks.
- **Mari Approach**
  - Represent each payment as a **physics-bound coupon** with:
    - Device identity data.
    - Human identity proxy (bio hash).
    - Location grid.
    - Motion-derived seal.
    - Expiry and amount.
  - Evaluate risk explicitly using **Sentinel ML** before settlement.
  - Record a durable settlement proof from the **Bank HSM**.

## Core Actors

- **Person / End-User**
  - Initiates or receives a payment on a phone.
  - Represented in data by:
    - `senderBioHash` / `receiverBioHash`.
    - Device key ID (`kid`) mapped to that user.

- **Device**
  - Android phone running Mari app.
  - Data identifiers:
    - Device keypair (ECDSA). Public key registered via `/register-device`.
    - `kid` (8-hex key ID) sent with each signed transaction.
    - Derived device-level features in Sentinel (hashed `kid`).

- **Mari Core Server**
  - Receives signed coupons and physics snapshots.
  - Data roles:
    - Validates coupons, signatures, optional physics.
    - Normalizes legacy/new payloads.
    - Calls Sentinel for risk and Bank HSM for settlement.
    - Persists transactions in MongoDB.
    - Emits label events for Sentinel training.

- **Sentinel Risk Engine**
  - ML service that turns transaction features into a risk score.
  - Data roles:
    - Receives `InferenceRequest` (coupon hash, `kid`, seal, grid, amount, expiry).
    - Converts to numeric feature vector.
    - Returns a risk score and model ID.
    - Consumes labeled `TransactionEvent` stream for continuous training.

- **Mock Bank HSM**
  - Bank-side service that maintains balances and signs settlement proofs.
  - Data roles:
    - Validates coupons and checks balances.
    - Updates ledger balances.
    - Issues signed **increment key payloads** that encode finality.

- **Data Analysts / Risk Analysts**
  - Consumers of data produced by the above actors.
  - Data tasks:
    - Understand schemas and flows.
    - Evaluate Sentinel features and score behavior.
    - Design and critique decision policies based on data.

## Transport Rails (HTTP vs SMS)

- **HTTP Rail**
  - Primary online transport from device to core (`/api/transactions`).
  - Characteristics:
    - Low latency, synchronous risk + settlement decision.
    - Device sends JSON body with coupon, physicsData, signature, etc.
  - Data impact:
    - `transportMethod` persisted as `'HTTP'` in transaction records.
    - Easiest path for correlating real-time user behavior with Sentinel scores.

- **SMS Rail (Offline / Low Connectivity)**
  - Fallback transport when device cannot reach core over HTTP.
  - Characteristics:
    - Device encodes coupon into SMS text.
    - SMS gateway or broker forwards incoming messages to core via webhook.
    - Core normalizes SMS payload into the same intake path used for HTTP.
  - Data impact:
    - `transportMethod` persisted as `'SMS'` in transaction records.
    - Same coupon, physics (when available), and risk evaluation logic.
    - Additional external logs at the SMS provider (outside Mari core).

- **Implications for Analysis**
  - Analysts should treat transport as a **dimension of behavior**:
    - Compare risk and failure rates between HTTP and SMS.
    - Understand latency and delivery differences for SMS-triggered payments.
    - Consider that SMS introduces an extra external system (gateway logs).

## Decision Points & Outcomes

- **Transaction Intake Decision**
  - Inputs:
    - Coupon contents and hash.
    - Device signature (optional but recommended).
    - PhysicsData snapshot (optional but recommended).
  - Checks:
    - Coupon parse validity.
    - Physics checks (if PhysicsData present).
    - Seal format and coupon shape.
  - Outcomes:
    - Accept for risk evaluation.
    - Reject early with structured error (parse/physics/seal).

- **Sentinel Risk Decision**
  - Inputs:
    - `InferenceRequest` fields (`coupon_hash`, `kid`, `seal`, `grid_id`, `amount`, `expiry_ts`).
  - Outputs:
    - Risk score (0–999).
    - `model_id` identifying the deployed model.
  - Policy:
    - If Sentinel unreachable and `SENTINEL_FAIL_OPEN=false` → reject as `sentinel_unavailable`.
    - If score > threshold → reject as `high_risk_transaction`.
    - Otherwise → proceed to settlement.

- **Settlement Decision (Bank HSM)**

  - Inputs:
    - User identifier (`USER_ID`), amount, coupon hash, timestamp.
  - Actions:
    - Check ledger balance state.
    - Apply settlement rules and commissions.
    - Sign increment key payload that encodes the new ledger state.
  - Outputs:
    - `{ payload, SIG }` used by clients as proof of finality.

## Key Data Principles

- **Physics-Bound Transactions**
  - Each coupon ties value movement to:
    - Who (bio hash).
    - Which device (key ID).
    - Where (location grid).
    - How (motion-derived seal).
    - When (expiry and timestamps).

- **Explicit Risk Modeling**
  - Risk is not hidden inside bank approvals.
  - Risk signals are explicit features and labeled outcomes.
  - Analysts can inspect, replay, and retrain based on events.

- **Separation of Concerns**
  - Core handles correctness and data plumbing.
  - Sentinel handles risk modeling and scores.
  - Bank HSM handles account state and settlement proofs.

- **Data-First Design**
  - Every decision point has:
    - Defined inputs.
    - A deterministic decision rule (given score/threshold).
    - Observability via events, DB records, and metrics.
