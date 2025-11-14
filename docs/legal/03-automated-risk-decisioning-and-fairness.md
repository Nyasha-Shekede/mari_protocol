# Automated Risk Decisioning & Fairness

## Scope

- **Audience**
  - Legal, compliance, risk governance, model risk management.
- **Goal**
  - Explain how automated risk decisions are made.
  - Highlight controls, transparency points, and fairness considerations.

## What Is Automated

- **Transaction Risk Scoring (Sentinel)**
  - Core sends a compact feature set derived from each transaction.
  - Sentinel returns a numeric **risk score (0–999)** and `model_id`.
  - Core compares score to a configurable threshold.

- **Decision Logic (Core)**
  - If Sentinel unreachable and `SENTINEL_FAIL_OPEN=false`:
    - Transaction is rejected with `sentinel_unavailable`.
  - If `score > threshold`:
    - Transaction is rejected with `high_risk_transaction`.
  - Otherwise:
    - Core proceeds to bank settlement.

- **What Is Not Automated**
  - Customer onboarding, KYC/AML.
  - Long-term credit decisions.
  - Manual investigations and dispute resolution.

## Inputs to the Risk Model

- **Feature Categories**
  - Pseudonymous identifiers:
    - Device key ID (hashed), coupon hash segments.
  - Behavioral/contextual:
    - Amount.
    - Time to expiry.
    - Motion-derived seal (hashed).
    - Location grid (hashed).
  - Model is **not** designed to use direct personal attributes (e.g. name, phone number).

- **Data Origin**
  - Features are derived **per transaction** from:
    - Coupon and physics data.
    - Device and context identifiers.

## Model Training & Updating

- **Labels**
  - Derived from settlement outcomes:
    - `SUCCESS` vs various failure categories.
  - Used to build training datasets.

- **Pipeline**
  - Core publishes `PRE_SETTLEMENT` and `SETTLEMENT_OUTCOME` events.
  - Trainer consumes events, featurizes them, and builds labeled examples.
  - New models are trained and stored in Redis.
  - Inference service hot-swaps to new models via pub/sub.

- **Governance Hooks**
  - Each inference response includes `model_id`.
  - Metrics and logs record distribution of scores per model.
  - Enables later audit of which model produced which decisions.

## Fairness & Bias Considerations

> **Note:** This describes technical design; a full fairness assessment requires statistical analysis on real data.

- **Protected Attributes**
  - Model is not given explicit protected attributes (e.g. race, religion).
  - Pseudonymous and behavioral features may still correlate with protected classes.

- **Potential Bias Vectors**
  - Location grid patterns.
  - Device usage patterns.
  - Amount and timing behavior.

- **Controls & Mitigations (Design Intent)**
  - Ability to:
    - Analyze outcomes and scores by cohort (once operators link pseudonyms to legal demographics).
    - Adjust thresholds or retrain models to address observed bias.
  - Clear separation between:
    - Core risk scoring.
    - Bank's legal and regulatory responsibilities.

## Transparency & Explainability

- **Per-Transaction**
  - Stored data includes:
    - Risk score.
    - Model ID.
    - Transaction features (or enough to reconstruct them).

- **For Regulators / Auditors**
  - Possible to:
    - Reconstruct the feature vector used for a specific decision.
    - Re-run or approximate the risk model on historical data.
    - Analyze error and rejection rates by cohort or product.

- **User-Facing**
  - Current product design does not expose full feature-level explanations.
  - Operators can choose how much explanation to surface (e.g. generic "security check failed").

## Legal & Regulatory Touchpoints

- **Automated Decision-Making Rules**
  - In some jurisdictions, automated decisions that have legal or similarly significant effects may trigger:
    - Transparency requirements.
    - Rights to explanation or human review.
  - Operators should consider policies for:
    - Manual review options.
    - Appeals or second-look processes.

- **Model Risk Management**
  - Recommended practices:
    - Document model objectives, inputs, and known limitations.
    - Validate performance and fairness on representative data.
    - Track changes to thresholds and models over time.

- **Documentation & Evidence**
  - The system provides technical hooks (model IDs, logs, metrics) to support:
    - Model inventories.
    - Periodic validation and backtesting.
