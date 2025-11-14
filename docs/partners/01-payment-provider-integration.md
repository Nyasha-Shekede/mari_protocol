# Payment Provider Integration Guide

## Scope

- **Audience**
  - Payment providers, wallets, fintech partners integrating with Mari.
- **Goal**
  - Explain how to integrate with Mari Protocol end-to-end.
  - Show how to run the core stack, build clients, and reconcile payments.

## What You Integrate With

- **Mari Core (`mari-server`)**
  - HTTP API for:
    - Device key registration.
    - Transaction submission (`/api/transactions`).
    - Batch settlement (`/api/settlement/process`).
  - Connects to Sentinel (risk) and Bank/HSM (settlement).

- **Mari Sentinel (`mari-sentinel`)**
  - Optional risk engine that scores transactions before settlement.
  - You configure URL, thresholds, and fail-open/fail-close policy.

- **Bank / HSM (`mock-bank-hsm` in demo)**
  - Holds balances and issues signed increment keys (settlement proofs).
  - In production, this would be your real bank / ledger.

- **Mari App (`mari-app`)**
  - Reference Android demo client.
  - Shows how to generate coupons, collect physics, sign, and call Core.
  - You are expected to build your own app(s) or services on top of the protocol.

## High-Level Integration Steps

1. **Run or access a Mari stack** (Core + Bank/HSM + Sentinel).
2. **Register device or wallet keys** with Core.
3. **Generate and sign Mari coupons** in your client.
4. **Submit payments via HTTP** (and optionally SMS) to Core.
5. **Handle settlement responses and reconciliation**.
6. **Optionally, implement batch settlement flows for merchants.**

Each is described below.

## 1. Run or Access a Mari Stack

- **Local / PoC setup**
  - Use the provided Docker/Make targets:
    - Core + Mongo: from `mari-server/` → `docker compose up -d`.
    - Bank/HSM: from `mock-bank-hsm/` → `make bank-up` or `docker compose up -d`.
    - Sentinel: from `mari-sentinel/` → `make prod-up`, `make prod-seed`, `make train`.

- **Production-like setup**
  - Host Core, Sentinel, and a bank adapter in your environment.
  - Expose Core URLs to your apps over HTTPS.
  - Keep Bank/HSM and data stores (Mongo, Redis, RabbitMQ) on private networks.

## 2. Register Device / Wallet Keys

- **What you need**
  - For each device or wallet:
    - ECDSA P-256 keypair.
    - Derived key ID (`kid`), typically first 8 hex chars of SHA-256(SPKI).

- **Core endpoint**
  - `POST /api/transactions/register-device`
    - Body includes:
      - `kid`: 8-hex identifier.
      - `spki`: base64-encoded public key.
      - Optional `encSpki` and `userId` for future encryption use.

- **Client responsibilities**
  - Generate and store the private key securely (e.g. mobile Keystore, HSM).
  - Keep `kid` stable per key and send it with each signed transaction.

## 3. Generate and Sign Coupons

- **Coupons**
  - Human-readable transfer strings:
    - `Mari://xfer?from=...&to=...&val=...&g=...&exp=...&s=...`
  - Core fields:
    - `from`, `to`: payer/payee (pseudonymous IDs).
    - `val`: amount.
    - `g`: location grid (coarse location cell).
    - `exp`: expiry timestamp (ms since epoch).
    - `s`: motion-derived seal (8-hex string).

- **Shared libs**
  - Use `mari-shared-libs` to:
    - Parse and generate coupons.
    - Work with motion, grid, and seals.

- **Signing payload**
  - Canonical JSON object:
    - `{ from, to, amount, grid, coupon }`, keys sorted.
  - Sign with device/wallet ECDSA private key.
  - Send `kid` and `sig` with the transaction.

## 4. Submit Payments via HTTP (and SMS)

### HTTP (Online Rail)

- **Endpoint**
  - `POST /api/transactions`

- **Body (typical)**
  - Fields supported (new + legacy shapes):
    - `from`, `to`, `grid`.
    - `kid`, `sig` (for signed flows).
    - `amount`, `coupon` (required).
    - Optional `physicsData` with:
      - `location.grid` (coarse grid).
      - `motion.x/y/z`.
      - `timestamp`.

- **Server-side behavior**
  - Core:
    - Normalizes legacy/new shapes.
    - Computes `couponHash` and enforces idempotency.
    - Verifies signature using registered `spki`.
    - Optionally performs physics validation (if `physicsData` present).
    - Calls Sentinel for risk (if configured).
    - On approval, calls Bank/HSM for settlement.
    - Persists a transaction record and emits label events.

### SMS (Offline/Low-Connectivity Rail)

- **Client side**
  - Encode the coupon into SMS text.
  - Send to your configured SMS gateway number.

- **Server side**
  - SMS provider forwards to Core’s webhook (e.g. `/webhook/sms/incoming`).
  - Core extracts the coupon and routes it into the same `/api/transactions` logic.
  - Audit and risk behavior are identical after normalization.

## 5. Handle Settlement Responses & Reconciliation

- **Single transaction response**
  - Core returns:
    - `ok: true/false`.
    - `couponHash`.
    - `payload` and `SIG` (HSM settlement proof) when successful.

- **Provider responsibilities**
  - Optionally verify the HSM signature using the public key from bank/HSM.
  - Update internal wallets / ledgers according to your own accounting rules.
  - Store `couponHash` and settlement proof for reconciliation.

- **Reconciliation**
  - Use:
    - Core transaction journals (Mongo) for detailed attempts.
    - Bank/HSM ledger and increment keys for final balances.
    - The join keys documented in `docs/finance/03-reconciliation-and-financial-reporting.md`.

## 6. Batch Settlement for Merchants

- **When to use**
  - For merchants or payout campaigns where many coupons should be settled together.

- **Core endpoint**
  - `POST /api/settlement/process`
    - Body includes:
      - `batchId`.
      - `merchantId` (core-side).
      - `bankMerchantId` (bank-side account ID).
      - `seal` (batch seal computed over items).
      - `transactions`: list of `{ id, amount, coupon, physicsData? }`.

- **Flow**
  - Core forwards a normalized payload to Bank/HSM.
  - Bank validates coupons and amounts, applies commissions, updates merchant balance.
  - Bank returns settlement summary + batch increment key.
  - Core forwards summary to the caller.

- **Docs for finance & ops**
  - See `docs/finance/02-settlement-lifecycle-and-ledgers.md` and `docs/finance/03-reconciliation-and-financial-reporting.md`.

## 7. Sentinel Risk Integration (Optional but Recommended)

- **Configuration (Core)**
  - Environment variables:
    - `SENTINEL_URL` – URL of inference service.
    - `SENTINEL_THRESHOLD` – risk threshold.
    - `SENTINEL_FAIL_OPEN` – fail-open vs fail-close.
    - `SENTINEL_AUTH_TOKEN` – optional auth token.

- **Behavior**
  - For each transaction, Core:
    - Builds an `InferenceRequest` with:
      - `coupon_hash`, `kid`, `expiry_ts`, `seal`, `grid_id`, `amount`.
    - Calls `/inference` on Sentinel.
    - Receives a score and `model_id`.
    - Rejects or proceeds based on threshold and availability policy.

- **Training data**
  - Core publishes `SETTLEMENT_OUTCOME` events to RabbitMQ.
  - Sentinel trainer consumes events and updates models.
  - See `docs/data-analyst/05-sentinel-risk-and-training-data.md` for details.

## 8. Environments & Onboarding Flow for Providers

- **Recommended phases**
  1. **Local / sandbox integration**
     - Run the full stack locally.
     - Integrate your app/server against Core’s HTTP APIs.
  2. **Staging**
     - Connect to a shared staging stack with representative data.
     - Validate risk thresholds and settlement flows.
  3. **Production**
     - Point clients at production Core endpoints.
     - Use production bank/HSM and Sentinel configuration.

- **Minimal steps for onboarding a new provider**
  - Provide:
    - Core and Sentinel base URLs.
    - Auth tokens / credentials as needed.
    - This integration guide and the role-based docs map (`OVERVIEW.md`).
  - Expect from provider:
    - Their own client implementation (mobile/web/service).
    - Secure key management on their side.
    - Their own UX, support, and compliance processes.

## Where to Go Next

- **Technical deep dives**
  - `OVERVIEW.md` — high-level summary and architecture.
  - Service READMEs under `mari-server/`, `mari-sentinel/`, `mock-bank-hsm/`.

- **Finance & Reporting**
  - `docs/finance/` — economic model, settlement lifecycle, reconciliation.

- **Risk & Data**
  - `docs/data-analyst/` and `docs/risk-ops/` — features, flows, and playbooks for analytics and fraud ops.

- **Security & Legal**
  - `docs/security/` and `docs/legal/` — threat model, crypto, data protection, automated decisioning.
