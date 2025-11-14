# Mari User Value & Personas

## Scope

- **Audience**
  - UX designers, product managers, user research.
- **Goal**
  - Explain who Mari is for.
  - Summarize the main user-facing benefits.
  - Highlight real-world usage contexts.

## Primary Personas

- **Everyday Payer (Consumer)**
  - Needs to send money to friends, family, or small merchants.
  - Often has:
    - Unreliable data connectivity.
    - A low- to mid-range Android device.
  - Cares about:
    - Speed and simplicity.
    - Trust that money actually arrived.
    - Low fees.

- **Small Merchant / Shop Owner**
  - Accepts payments from many consumers each day.
  - Cares about:
    - Simple way to receive and confirm payments.
    - Clear settlement and payout visibility.
    - Low friction for customers.

- **Field Agent / Payroll Distributor**
  - Moves money out to many recipients (e.g. salaries, stipends).
  - Cares about:
    - Reliability in remote areas.
    - Proof that each payout was made.
    - Ability to reconcile totals at the end of the day.

## Core User Benefits

- **Fast, Predictable Settlement**
  - Each successful payment comes with a verifiable proof (increment key) from the bank layer.
  - Users see clear success/fail outcomes; no hidden “pending” states.

- **Works in Unreliable Network Conditions**
  - HTTP (online) for rich, fast interactions.
  - SMS (offline/low-connectivity) as a fallback rail.
  - Same security model and settlement logic regardless of transport.

- **Stronger Protection Against Fraud & Bots**
  - Payments include:
    - Device identity.
    - Coarse location grid.
    - Motion-based seal.
  - Transactions are evaluated by a risk engine before money moves.

- **Privacy-Conscious by Design**
  - Uses hashes and grids instead of storing raw PII and GPS.
  - Designed so that the protocol can be analyzed and audited without exposing user identities.

- **Transparent Records**
  - Every transaction can be traced:
    - From the user-facing app history.
    - Through server logs and bank settlement proofs.
  - Easier to explain “where the money went” to users.

## What Makes Mari Different (User-Facing)

- **Compared to Cash**
  - Pros:
    - No need to carry physical money.
    - Digital audit trail for disputes and reporting.
    - Possible to recover patterns and detect fraud.
  - Cons:
    - Requires a device and some form of connectivity.

- **Compared to Card Payments**
  - Pros:
    - Built-in risk features tied to device, motion, and context.
    - Can function via SMS where data networks are weak.
  - Cons:
    - Currently less widely integrated with existing POS hardware.

- **Compared to Generic Wallet Apps**
  - Pros:
    - Explicit physics and risk model; not a black box.
    - Stronger tooling for audit and fraud analysis.
  - Cons:
    - UX must be carefully designed to explain physics and risk decisions simply.

## High-Level UX Goals

- **Simple Mental Model**
  - Users should feel Mari is:
    - "Send money" and "Get receipt", not a complex protocol.
  - Most of the physics and risk detail stays behind the scenes.

- **Clear Status & Feedback**
  - Every payment should show:
    - Sending → risk check → settled / failed.
  - Errors should be actionable and human-readable.

- **Respect for Constraints**
  - Design must account for:
    - Low-end devices.
    - Intermittent connectivity.
    - Varying levels of digital literacy.
