# Regulatory Perimeter & Roles

## Scope

- **Audience**
  - Legal, regulatory, compliance teams.
- **Goal**
  - Clarify what Mari does and does not do from a regulatory standpoint.
  - Describe the roles of Mari Core, Sentinel, and Bank/HSM in the overall system.

## High-Level Summary

- **Mari Core + Sentinel**
  - Provide a **technical protocol and risk engine** for payments.
  - Do **not** hold the primary ledger of balances.
  - Do **not** implement penalties, interest, or credit products.

- **Bank / HSM**
  - Holds customer and merchant accounts.
  - Maintains the authoritative balance ledger.
  - Issues signed settlement proofs (increment keys).
  - Owns KYC/AML obligations and regulatory reporting for accounts.

## Roles & Responsibilities

- **Mari Core (Application Layer)**
  - Responsibilities:
    - Validate transaction inputs (coupon structure, signatures, optional physics).
    - Call Sentinel to obtain risk scores.
    - Call Bank/HSM for settlement decisions and ledger updates.
    - Maintain transaction journals for audit and analytics.
  - Non-responsibilities:
    - Maintaining account balances as the legal ledger of record.
    - Directly onboarding customers (no KYC/AML engine in core).

- **Sentinel (Risk Engine)**
  - Responsibilities:
    - Provide risk scores based on pseudonymous transaction features.
    - Learn from labeled outcomes to improve fraud detection.
  - Non-responsibilities:
    - Holding funds or moving balances.
    - Making final legal decisions about customer onboarding.

- **Bank / HSM (Ledger & Settlement Layer)**
  - Responsibilities:
    - Maintain balances for users and merchants.
    - Apply settlement rules and commissions.
    - Issue signed increment keys as proofs of settlement.
    - Fulfill banking regulatory obligations (KYC/AML, reporting, capital).
  - Non-responsibilities:
    - Managing device keys or physics collection.

## Regulatory Perimeter (Indicative)

> **Note:** This is a technical description, not legal advice. Final classification depends on jurisdiction and regulatory interpretation.

- **Within Mari Core/Sentinel Perimeter**
  - Data processing of:
    - Pseudonymous identifiers (bio hashes, device IDs).
    - Transaction intent and context (coupon, physics, risk scores).
  - Activities:
    - Risk scoring and fraud detection.
    - Technical orchestration of payment flows.

- **Within Bank/HSM Perimeter**
  - Customer money and balances.
  - KYC/AML/CTF processes and monitoring.
  - Regulatory reporting of transactions and suspicious activity.

- **Outside Mari Perimeter (Examples)**
  - Licensing decisions (e.g. EMI, bank, PSP licenses) held by the bank or operator.
  - Cross-border FX and tax treatment.

## Key Design Choices Relevant to Regulation

- **No Penalty / Interest Logic**
  - The system explicitly does **not** implement penalties, late fees, or interest.
  - Transactions are approve/reject with immediate settlement when approved.

- **Pseudonyms vs PII**
  - Mari Core uses bio hashes and device IDs instead of storing direct PII.
  - The Bank layer maps these to legal identities.

- **Physics & Risk**
  - Physics (grid, motion) and ML scores are used to reduce fraud risk.
  - They do not replace human or bank-side KYC/AML processes.

## Questions Legal/Reg Teams May Ask

- **Who is the regulated entity?**
  - Typically the bank / operator running the ledger.
- **Who is responsible for KYC/AML?**
  - Bank / operator, not Mari Core code.
- **How are users identified?**
  - Core sees pseudonymous identifiers; bank links them to legal identity.
- **Does Mari extend credit or charge penalties?**
  - No; only immediate settlement of approved transactions is implemented.
