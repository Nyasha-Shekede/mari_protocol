# Risk Operations Playbook

## Scope

- **Audience**
  - Fraud operations, risk ops, monitoring teams.
- **Goal**
  - Explain how to use Mari data to monitor risk.
  - Provide playbooks for investigating suspicious activity.

## Key Data & Tools for Risk Ops

- **Transaction Journals (Core)**
  - Fields of interest:
    - `senderBioHash`, `receiverBioHash`.
    - `amount`, `locationGrid`, `status`.
    - `transportMethod` (HTTP/SMS).
    - Timestamps.

- **Sentinel Risk Data**
  - Risk scores (0–999) per transaction.
  - `model_id` used for each score.
  - Distribution of scores over time.

- **Events (Training Stream)**
  - `PRE_SETTLEMENT` and `SETTLEMENT_OUTCOME` events.
  - Include:
    - `coupon_hash`, `kid`, `expiry_ts`, `seal`, `grid_id`, `amount`, `result`.

- **Bank / HSM Data**
  - Account balances and increment keys.
  - Used to confirm whether suspicious transactions actually settled.

## Day-to-Day Monitoring

- **Risk Score Monitoring**
  - Watch:
    - Volume of transactions by risk band (e.g. 0–300, 300–500, 500–700, 700–850, 850+).
    - Rejection rates (`high_risk_transaction`) over time.
    - Model version changes and impact on scores.

- **Transport Mix**
  - Track proportion of HTTP vs SMS transactions.
  - Investigate spikes in SMS usage (could signal connectivity issues or abuse of a particular rail).

- **Location & Physics Patterns**
  - Monitor:
    - `LOCATION_MISMATCH` and `MOTION_MISMATCH` rates (from physics validation).
    - Grids with unusually high failure or risk rates.

## Investigating a Suspicious Transaction

1. **Identify the transaction**
   - Start from:
     - `transactionId`, or
     - `couponHash`, or
     - user/merchant complaints.

2. **Pull core transaction record**
   - Confirm:
     - `senderBioHash`, `receiverBioHash`.
     - `amount`, `locationGrid`.
     - `transportMethod`, timestamps.
     - `status`.

3. **Retrieve risk data**
   - Find corresponding risk score and `model_id`.
   - If needed, reconstruct features using stored fields.

4. **Check settlement**
   - Confirm whether settlement was requested and completed:
     - Look for increment key at Bank/HSM.
     - Verify signature and payload for the increment key.

5. **Check context & neighbors**
   - Examine:
     - Other transactions from same `senderBioHash` / `kid` / `grid` / time window.
     - Patterns of repeated seals or rapid sequences.

6. **Decide on action**
   - Possible outcomes:
     - Mark as confirmed fraud.
     - Flag for further observation.
     - Deem legitimate and adjust rules/thresholds if necessary.

## Patterns to Watch

- **High-Risk but Successful**
  - Transactions with scores close to threshold but allowed.
  - Clusters of such transactions may signal emerging attacks.

- **Replay / Duplicate Behavior**
  - Reuse of same coupon hash or very similar features.
  - High `hashSeen` (if implemented) or manually detected duplicates.

- **Anomalous Transport or Geography**
  - Sudden shift in SMS vs HTTP usage for certain users or regions.
  - Unusual movement between distant grids in short periods.

## Interaction with Policy

- **Threshold Tuning**
  - Work with product/legal to:
    - Adjust `SENTINEL_THRESHOLD` per environment.
    - Balance false positives vs false negatives.

- **Blocklists / Step-Up Controls**
  - For consistently abusive devices or patterns:
    - Design non-technical policies (e.g. manual review, temporary blocks).

- **Feedback Loop**
  - Feed confirmed fraud and false positives back into:
    - Training data.
    - Model retraining and feature design.

## Coordination with Other Teams

- **With Support / CS Ops**
  - Provide guidance on:
    - How to explain risk-based declines to users.
    - When to escalate user complaints to risk.

- **With Legal / Compliance**
  - Provide data and analysis for:
    - Suspicious activity reporting (where applicable).
    - Model governance and fairness reviews.

- **With Ops / SRE**
  - Ensure monitoring covers:
    - Sentinel latency and availability.
    - Training pipeline health (so risk scores reflect recent data).
