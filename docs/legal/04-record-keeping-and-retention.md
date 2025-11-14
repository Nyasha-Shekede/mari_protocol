# Record-Keeping & Retention

## Scope

- **Audience**
  - Legal, regulatory, compliance, internal audit.
- **Goal**
  - Describe what records Mari can provide.
  - Outline how they relate to typical financial and data-retention requirements.

## Record Types

- **Transaction Journals (Core MongoDB)**
  - Per-transaction entries including:
    - Pseudonymous parties (bio hashes / user IDs).
    - Amount.
    - Timestamps.
    - Transport method (HTTP/SMS).
    - Optional physics snapshot.
    - Status (PENDING/SETTLED/FAILED).

- **Settlement Proofs (Bank / HSM)**
  - Increment keys per user and per merchant batch:
    - Encode USER_ID, amounts, coupon hashes, time, and version.
    - Signed by HSM.

- **Risk & Decision Logs**
  - Sentinel scores and model IDs.
  - Error and rejection codes (e.g. high_risk_transaction, sentinel_unavailable).

- **Training Events**
  - `PRE_SETTLEMENT` and `SETTLEMENT_OUTCOME` events linking intent to outcomes.

- **Operational Logs & Metrics**
  - Service health, request counts, latencies, error rates.
  - Model version timelines.

## How These Support Regulatory Needs

> Final mapping depends on the specific regime and operator policies, but technically Mari can support the following.

- **Payment Transaction Records**
  - Core journals + bank increment keys together provide:
    - Evidence of each payment attempt.
    - Evidence of each successful settlement.

- **Ledger & Reconciliation Evidence**
  - Increment keys and bank-side balances can be used to:
    - Reconstruct account history per user/merchant.
    - Reconcile reported balances with transaction streams.

- **Fraud & Risk Decisions**
  - Stored risk scores and model IDs enable:
    - Demonstrating when and why automated decisions were made.
    - Reviewing outcomes for disputed or suspicious cases.

- **Model Governance**
  - Model IDs, metrics, and training pipelines support:
    - Periodic validation and backtesting.
    - Regulatory expectations around model risk management.

## Retention Considerations

- **Core Journals (MongoDB)**
  - Code does not enforce a TTL; retention is a policy decision per operator.
  - Operators typically decide:
    - Minimum retention period for transaction records.
    - Whether and how to archive or anonymize historical data.

- **Training Events (RabbitMQ)**
  - Queue TTL is 24 hours in the demo configuration.
  - Operators may:
    - Mirror events to a long-term store if needed for audit.

- **Models (Redis / Model Store)**
  - Multiple model versions may be retained for:
    - Backtesting.
    - Incident and bias investigations.

- **Bank / HSM Records**
  - Ledger records and increment keys fall under banking record-keeping obligations.
  - Retention policies are set by the bank/operator, not by the Mari Core codebase.

## Alignment with Typical Regulatory Expectations

- **Financial Services / Payments**
  - Requirement: Maintain records of transactions and balances.
  - Mari support:
    - Detailed transaction journals (core) + authoritative ledger (bank).

- **AML / CTF Monitoring**
  - Requirement: Retain data to detect suspicious activity.
  - Mari support:
    - Structured, linkable records (transaction + risk + settlement outcomes).

- **Data Protection**
  - Requirement: Retain data only as long as necessary.
  - Mari support:
    - Technical flexibility: code does not hard-code long retention.
    - Operators can apply policies and tooling to enforce retention limits.

## Policy Hooks for Operators

- **Retention Schedules**
  - Define per-record-type:
    - Minimum and maximum durations.
    - Archival/anonymization strategies.

- **Access Controls**
  - Align data access permissions with roles (finance, risk, ops, support).

- **Audit Trails**
  - Ensure that:
    - Access to sensitive stores is logged.
    - Changes to retention or deletion settings are tracked.
