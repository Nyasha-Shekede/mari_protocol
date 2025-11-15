# Mari-Bank Partnership Agreement Structure

## Overview

Yes, we need **formal agreements with banks** that stipulate:
1. Banks will authorize Mari-verified transactions immediately
2. Banks trust Mari's physics seal as a strong fraud signal
3. Liability and risk-sharing framework
4. Technical integration requirements
5. Settlement procedures

---

## The Core Agreement: Immediate Authorization Guarantee

### Key Contractual Clause

```
IMMEDIATE AUTHORIZATION AGREEMENT

Bank agrees to:
1. Process Mari-verified transactions with immediate authorization
2. Debit sender's account within 2 seconds of receiving Mari authorization token
3. Treat Mari physics seal as equivalent to or stronger than:
   - EMV chip verification
   - 3D Secure authentication
   - Biometric authentication
   
Mari agrees to:
1. Maintain 99.5%+ fraud detection accuracy
2. Provide cryptographic proof for all authorizations
3. Maintain insurance fund covering fraud losses
4. Indemnify bank for fraud on Mari-verified transactions
```

---

## The Three-Tier Agreement Structure

### Tier 1: Technical Integration Agreement

**What it covers:**
- API specifications
- Security requirements
- Performance SLAs
- Data sharing protocols

**Key clauses:**

```
TECHNICAL INTEGRATION TERMS

1. API Response Time
   - Mari shall respond to authorization requests within 500ms
   - Bank shall process Mari authorization within 2 seconds
   - Total user-facing transaction time: <3 seconds

2. Availability
   - Mari uptime: 99.95% (excluding scheduled maintenance)
   - Bank integration uptime: 99.9%
   - Fallback procedures if either system is down

3. Security Standards
   - TLS 1.3 for all communications
   - Mutual authentication (bank ↔ Mari)
   - HSM-signed authorization tokens
   - Audit logs retained for 7 years

4. Data Sharing
   - Bank provides: User account status, balance check
   - Mari provides: Physics seal verification, fraud score
   - Neither party shares data with third parties without consent
```

---

### Tier 2: Risk & Liability Agreement

**What it covers:**
- Who pays if fraud occurs
- Insurance requirements
- Dispute resolution
- Fraud investigation procedures

**Key clauses:**

```
RISK SHARING FRAMEWORK

1. Fraud Liability Tiers

   Tier A: Mari-Verified Transactions (99.5%+ confidence)
   - Mari provides insurance coverage up to R10M per incident
   - Mari maintains reserve fund of R100M
   - If fraud occurs: Mari reimburses bank within 48 hours
   - Bank's liability: R0 (Mari assumes all risk)

   Tier B: Medium Confidence (95-99.5%)
   - Shared liability: 50% Mari, 50% Bank
   - Bank may apply additional fraud checks
   - Bank may delay authorization for manual review

   Tier C: Low Confidence (<95%)
   - Bank's existing fraud policies apply
   - Mari provides advisory signal only
   - Bank assumes full liability

2. Insurance Requirements
   - Mari maintains R500M fraud insurance policy
   - Underwritten by [Major Insurer]
   - Bank is named beneficiary
   - Annual audit of reserve fund

3. Fraud Investigation
   - Mari provides full audit trail (HSM logs, physics data)
   - Bank provides transaction details
   - Joint investigation within 24 hours
   - Resolution within 7 days

4. Dispute Resolution
   - Mediation first (30 days)
   - Arbitration if mediation fails
   - Governing law: South African law
```

---

### Tier 3: Commercial Agreement

**What it covers:**
- Pricing and fees
- Revenue sharing
- Volume commitments
- Marketing and branding

**Key clauses:**

```
COMMERCIAL TERMS

1. Pricing Structure
   
   Option A: Per-Transaction Fee
   - Mari charges bank: R0.10 per transaction
   - Bank may pass cost to users or absorb
   - Volume discounts:
     * >1M transactions/month: R0.08
     * >10M transactions/month: R0.05
     * >100M transactions/month: R0.03

   Option B: Revenue Share
   - Bank charges users: R2.00 per transaction
   - Mari receives: 30% (R0.60)
   - Bank keeps: 70% (R1.40)
   - Incentivizes bank to promote Mari

   Option C: Hybrid
   - Base fee: R0.05 per transaction
   - Revenue share: 20% of bank's transaction fee
   - Balances risk and reward

2. Minimum Volume Commitment
   - Bank commits to minimum 100K transactions in Year 1
   - Grows to 1M transactions by Year 3
   - If not met: Renegotiation clause

3. Exclusivity (Optional)
   - Bank may request exclusivity in region/segment
   - Mari charges premium: +50% on fees
   - Duration: 12-24 months maximum

4. Marketing & Branding
   - Co-branded marketing materials
   - Bank may use "Secured by Mari" badge
   - Joint press releases
   - User education campaigns
```

---

## The Negotiation Strategy

### Phase 1: Pilot Agreement (First Bank)

**Approach:**
- Start with smaller, innovative bank (Capitec, TymeBank, Bank Zero)
- Offer favorable terms to prove concept
- Limited scope: 10,000 users, 6-month pilot

**Pilot Terms:**
```
PILOT AGREEMENT

Duration: 6 months
Users: 10,000 maximum
Pricing: R0.05 per transaction (50% discount)
Liability: Mari assumes 100% fraud risk
Success Metrics:
- Fraud rate: <0.1%
- User satisfaction: >90%
- Transaction success rate: >99%
- Average authorization time: <3 seconds

If successful:
- Expand to full commercial agreement
- Bank gets 12-month preferred pricing
- Bank gets "First Bank Partner" marketing rights
```

**Why banks will sign:**
- Low risk (small pilot)
- High reward (competitive advantage if it works)
- No upfront cost
- Mari assumes all fraud liability

---

### Phase 2: Commercial Rollout (Multiple Banks)

**Approach:**
- Use pilot success data to negotiate with major banks
- Offer tiered pricing based on volume
- Create competitive pressure (FOMO)

**Negotiation Leverage:**
```
"We've proven with [Pilot Bank]:
- 95% fraud reduction
- R30M savings per 1M transactions
- 98% user satisfaction
- Zero fraud losses

[Competitor Bank] is in discussions with us.
If you don't integrate, your customers will switch."
```

**Commercial Terms:**
```
STANDARD COMMERCIAL AGREEMENT

Duration: 3 years (auto-renew)
Pricing: R0.10 per transaction (volume discounts apply)
Liability: Mari assumes fraud risk on Tier A transactions
Integration: Bank completes within 6 months
Marketing: Co-branded launch campaign
Exclusivity: None (multi-bank network)

Bank Benefits:
- Immediate authorization capability
- Fraud reduction (80-90%)
- Customer satisfaction increase
- Competitive differentiation
- Revenue share opportunity
```

---

### Phase 3: Network Effects (Dominant Position)

**Approach:**
- Once 3+ major banks integrated, network effects kick in
- Remaining banks forced to integrate or lose customers
- Mari can dictate terms

**Dominant Position Terms:**
```
NETWORK PARTICIPATION AGREEMENT

Duration: 1 year (renewable)
Pricing: R0.15 per transaction (premium for late adopters)
Liability: Shared (70% Mari, 30% Bank)
Integration: Bank completes within 3 months (expedited)
Marketing: Bank must promote Mari to all customers

Bank Benefits:
- Access to Mari network (interoperability)
- Avoid customer churn
- Regulatory compliance (if mandated)

Mari Benefits:
- Higher fees (late adopter penalty)
- Stronger negotiating position
- Market dominance
```

---

## The Legal Framework

### Key Legal Documents

1. **Master Services Agreement (MSA)**
   - Overarching relationship terms
   - Governance structure
   - Termination clauses
   - Intellectual property rights

2. **Technical Integration Addendum**
   - API specifications
   - Security requirements
   - SLA commitments
   - Change management procedures

3. **Risk & Liability Addendum**
   - Fraud liability framework
   - Insurance requirements
   - Dispute resolution
   - Indemnification clauses

4. **Commercial Terms Addendum**
   - Pricing and fees
   - Volume commitments
   - Revenue sharing
   - Marketing rights

5. **Data Processing Agreement (DPA)**
   - POPIA compliance (South African data protection)
   - Data sharing protocols
   - User consent management
   - Data retention policies

---

## Sample Agreement Excerpt

### Immediate Authorization Clause

```
ARTICLE 5: TRANSACTION AUTHORIZATION

5.1 Immediate Authorization Commitment

Upon receipt of a Mari Authorization Token with a confidence score 
of 99.5% or higher ("Tier A Transaction"), Bank shall:

(a) Process the authorization request within two (2) seconds;
(b) Debit the sender's account immediately upon successful authorization;
(c) Provide confirmation to the sender within three (3) seconds of 
    initial request;
(d) Queue the transaction for inter-bank settlement according to 
    Bank's standard settlement procedures.

5.2 Mari's Obligations

Mari shall:

(a) Validate the physics seal using cryptographic verification;
(b) Verify device attestation and location proof;
(c) Generate an authorization token signed by Mari's HSM network;
(d) Provide a confidence score based on fraud risk assessment;
(e) Maintain audit logs for seven (7) years.

5.3 Fraud Liability

For Tier A Transactions:

(a) Mari assumes 100% liability for fraud losses;
(b) Mari shall reimburse Bank within forty-eight (48) hours of 
    confirmed fraud;
(c) Mari maintains a reserve fund of not less than R100,000,000 
    (One Hundred Million Rand);
(d) Mari maintains fraud insurance coverage of not less than 
    R500,000,000 (Five Hundred Million Rand);
(e) Bank is named as beneficiary on Mari's insurance policy.

5.4 Performance Guarantees

Mari guarantees:

(a) Fraud detection accuracy: ≥99.5%
(b) False positive rate: ≤0.1%
(c) API response time: ≤500ms (95th percentile)
(d) System uptime: ≥99.95%

If Mari fails to meet these guarantees for two (2) consecutive months:

(a) Bank may suspend immediate authorization requirement;
(b) Bank may apply additional fraud checks;
(c) Mari shall provide service credits: 20% of monthly fees;
(d) Parties shall negotiate remediation plan.

5.5 Settlement Procedures

(a) Immediate authorization does not imply immediate settlement;
(b) Inter-bank settlement follows existing RTGS/ACH procedures;
(c) Settlement timing: End of business day or as per Bank's 
    standard procedures;
(d) Mari is not party to settlement process;
(e) Bank assumes settlement risk (as per current practice).
```

---

## The Regulatory Approval Process

### Required Approvals

1. **South African Reserve Bank (SARB)**
   - Payment system authorization
   - Compliance with National Payment System Act
   - Approval of risk management framework

2. **Financial Sector Conduct Authority (FSCA)**
   - Consumer protection compliance
   - Fair treatment of customers
   - Disclosure requirements

3. **Financial Intelligence Centre (FIC)**
   - AML/CFT compliance
   - Suspicious transaction reporting
   - Record-keeping requirements

### Regulatory Strategy

```
REGULATORY ENGAGEMENT PLAN

Phase 1: Pre-Application (Months 1-3)
- Informal consultations with SARB
- Present technology and security model
- Address regulatory concerns
- Gather feedback

Phase 2: Formal Application (Months 4-6)
- Submit payment system authorization application
- Provide technical documentation
- Demonstrate security measures
- Present risk management framework

Phase 3: Pilot Approval (Months 7-9)
- Request sandbox/pilot approval
- Limited scope: 10,000 users, 1 bank
- Regular reporting to regulator
- Demonstrate compliance

Phase 4: Full Authorization (Months 10-12)
- Present pilot results
- Request full payment system authorization
- Expand to multiple banks
- Ongoing compliance monitoring
```

---

## The Insurance Structure

### Mari's Insurance Requirements

**Primary Coverage:**
- Fraud loss insurance: R500M
- Cyber liability insurance: R100M
- Professional indemnity: R50M
- Directors & officers liability: R25M

**Reserve Fund:**
- Minimum balance: R100M
- Funded by: 10% of transaction fees
- Invested in: Low-risk government bonds
- Audited: Quarterly by independent auditor

**Reinsurance:**
- For losses exceeding R10M per incident
- Reinsurer: International reinsurance company
- Coverage: Up to R1B aggregate

---

## The Competitive Dynamics

### Why Banks Will Sign

**First-Mover Advantage:**
- Bank that integrates first gets marketing rights
- "First bank with physics-secured payments"
- Attracts tech-savvy customers
- Competitive differentiation

**FOMO (Fear of Missing Out):**
- Once 2-3 banks integrate, others must follow
- Network effects: Users want interoperability
- Risk of customer churn to competitor banks

**Regulatory Pressure:**
- Regulators may mandate interoperability
- Banks that don't integrate face compliance risk
- Precedent: PSD2 in Europe, UPI in India

**Cost Savings:**
- R35M savings per 1M transactions
- ROI: 6,900%
- CFO will demand integration

---

## Bottom Line

**Yes, we need formal agreements with banks that:**

1. **Mandate immediate authorization** for Mari-verified transactions
2. **Define liability framework** (Mari assumes fraud risk)
3. **Specify technical requirements** (API, SLA, security)
4. **Establish commercial terms** (pricing, volume, revenue share)
5. **Ensure regulatory compliance** (SARB, FSCA, FIC)

**The agreement structure:**
- **Pilot**: Prove it works (6 months, 10K users, favorable terms)
- **Commercial**: Scale it up (3 years, volume discounts, standard terms)
- **Network**: Dominate market (premium pricing for late adopters)

**Why banks will sign:**
- Massive cost savings (R35M per 1M transactions)
- Competitive advantage (instant payments)
- Regulatory support (fraud reduction)
- Customer demand (better UX)
- FOMO (competitors are integrating)

**The key clause: "Bank shall authorize Mari-verified transactions immediately, and Mari shall assume 100% fraud liability."**

**This is the deal. This is how we get banks to commit to instant authorization.**
