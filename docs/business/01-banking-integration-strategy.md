# Mari Banking Integration Strategy

## Overview
Mari is designed as a **payment protocol layer** that sits on top of existing banking infrastructure, not a replacement for banks. Think of it like how email protocols (SMTP) work across different email providers.

## Three-Tier Architecture

### Tier 1: Bank-Integrated Mari (Ideal State)
**How it works:**
- Banks integrate Mari protocol into their core banking systems
- Users register through their existing bank accounts (KYC already done)
- Transactions settle instantly within the same bank
- Inter-bank transfers use existing settlement rails (RTGS, ACH, etc.)
- Mari provides the **security layer** (physics seals, location proofs)

**User Experience:**
```
User A (Bank X) → Mari Protocol → User B (Bank X)
└─ Instant settlement (same bank ledger)

User A (Bank X) → Mari Protocol → User B (Bank Y)
└─ Deferred settlement via existing inter-bank rails
└─ But transaction is AUTHORIZED instantly with physics proof
```

**Settlement Flow:**
1. **Instant Authorization**: Physics seal validates transaction intent
2. **Immediate Debit**: User A's account debited immediately
3. **Batch Settlement**: Banks settle inter-bank transfers in batches (like they do now)
4. **Credit on Settlement**: User B credited when settlement completes

**Key Point**: The physics seal proves the transaction was legitimate, so banks can safely debit immediately and settle later.

---

### Tier 2: Mari as Banking Partner (Pragmatic Start)
**If banks don't integrate immediately:**

Mari operates as a **licensed payment service provider** (like M-Pesa, PayPal, Venmo):

1. **Mari holds pooled accounts** at partner banks
2. Users register with Mari (Mari handles KYC/AML)
3. Users link their bank accounts to Mari
4. Transactions happen on Mari's ledger (instant)
5. Users can cash in/out to their bank accounts

**Architecture:**
```
┌─────────────────────────────────────┐
│         Mari Platform               │
│  ┌─────────────────────────────┐   │
│  │   User Ledger (Instant)     │   │
│  │  - User A: R1000            │   │
│  │  - User B: R500             │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │   Pooled Bank Accounts      │   │
│  │  - Bank X: R50M             │   │
│  │  - Bank Y: R30M             │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
         ↕                    ↕
    ┌────────┐          ┌────────┐
    │ Bank X │          │ Bank Y │
    └────────┘          └────────┘
```

**Cash In/Out:**
- **Cash In**: User transfers from bank → Mari account (1-2 days)
- **Cash Out**: User transfers from Mari → bank account (1-2 days)
- **P2P on Mari**: Instant (happens on Mari's ledger)

**Regulatory Requirements:**
- Payment service provider license
- AML/KYC compliance
- Reserve requirements (hold X% of user funds)
- Regular audits

---

### Tier 3: Pure Crypto/Stablecoin (If Banks Refuse)
**Nuclear option if traditional banking won't cooperate:**

Mari becomes a **stablecoin-based payment network**:

1. Users hold Rand-pegged stablecoins (ZARX)
2. Transactions are instant on blockchain
3. Cash in/out through crypto exchanges or agents
4. Physics seals still provide security layer

**Why this works:**
- No bank permission needed
- Truly instant settlement
- Cross-border by default
- But: Higher regulatory risk, harder user onboarding

---

## Batch Processing & Settlement

### How Traditional Inter-Bank Settlement Works
Banks don't actually move money for every transaction. They batch:

```
Day 1:
- Bank X owes Bank Y: R10M (1000 transactions)
- Bank Y owes Bank X: R8M (800 transactions)
- Net settlement: Bank X pays Bank Y R2M (1 transaction)

This happens via:
- RTGS (Real-Time Gross Settlement) for large amounts
- ACH (Automated Clearing House) for smaller batches
- Settlement happens 1-3 times per day
```

### How Mari Handles This

**Option A: Mari as Settlement Layer (Bank Integration)**
```
1. User A sends R100 to User B (different banks)
2. Mari validates physics seal → transaction authorized
3. Bank X debits User A immediately (trusts Mari's proof)
4. Transaction queued for settlement
5. End of day: Banks settle net positions
6. Bank Y credits User B
```

**User sees**: "Payment sent" (immediate)
**Reality**: Settlement happens in batch (invisible to user)

**Option B: Mari as Intermediary (Payment Provider)**
```
1. User A sends R100 to User B
2. Mari debits User A's Mari balance (instant)
3. Mari credits User B's Mari balance (instant)
4. Both users see instant settlement
5. Mari handles bank settlement in background
```

---

## Registration & KYC Strategy

### Bank-Integrated Registration (Tier 1)
**Banks handle the hard stuff:**

```
User Flow:
1. User opens Mari app
2. Selects their bank (FNB, Standard Bank, etc.)
3. Redirected to bank's OAuth/API
4. Bank authenticates user (existing credentials)
5. Bank confirms identity to Mari (KYC already done)
6. User registered on Mari with bank-verified identity
```

**What Mari gets from bank:**
- Verified phone number
- Verified ID number
- Account status (active, good standing)
- Risk score (optional)

**What Mari doesn't need to do:**
- ID verification
- Proof of address
- Credit checks
- Fraud screening (bank already did this)

**Technical Implementation:**
```
POST /api/v1/register/bank-oauth
{
  "bank_id": "fnb_za",
  "oauth_token": "...",
  "phone_number": "+27821234567"
}

Response from Bank API:
{
  "user_verified": true,
  "id_number_hash": "sha256(...)",
  "account_active": true,
  "risk_level": "low"
}
```

### Mari-Direct Registration (Tier 2)
**If banks don't integrate, Mari does KYC:**

```
User Flow:
1. User downloads Mari app
2. Enters phone number (SMS verification)
3. Takes selfie + ID photo
4. Provides proof of address
5. Mari verifies via third-party (Onfido, Jumio, etc.)
6. Manual review for edge cases
7. Account activated (1-24 hours)
```

**Compliance Requirements:**
- FICA (Financial Intelligence Centre Act) compliance
- Store verified documents for 5 years
- Report suspicious transactions
- Maintain audit trail

**Cost**: R20-50 per user verification

---

## What If Banks Refuse?

### Scenario: No Bank Wants to Integrate

**Short Answer**: We're not cooked, but it's harder.

**Strategy Progression:**

#### Phase 1: Start with One Bank (Easiest)
- Approach smaller, more innovative banks first
- Capitec, TymeBank, Bank Zero (digital-first banks)
- Offer them competitive advantage: "Be the first bank with physics-secured payments"
- Pilot with 10,000 users

#### Phase 2: Payment Provider License (Fallback)
- Get licensed as payment service provider
- Operate like M-Pesa, Venmo, Cash App
- Build user base without bank integration
- Prove the model works

#### Phase 3: Regulatory Pressure (Long Game)
- Once we have 1M+ users, banks will notice
- Regulators may mandate interoperability (like PSD2 in Europe)
- Banks forced to integrate or lose customers

#### Phase 4: Crypto Rails (Nuclear Option)
- If all else fails, go full crypto
- Stablecoin-based system
- Cash in/out through agents, ATMs, crypto exchanges
- Harder but possible (see: Bitcoin in El Salvador)

---

## Real-World Examples

### M-Pesa (Kenya)
- Started without bank integration
- Became so big banks had to integrate
- Now processes 50% of Kenya's GDP
- **Lesson**: Build user base first, banks follow

### UPI (India)
- Government-mandated interbank protocol
- All banks forced to integrate
- Now 10B+ transactions/month
- **Lesson**: Regulatory support is powerful

### Venmo (USA)
- Started as payment app (no bank integration)
- Built massive user base
- Banks now integrate with Venmo
- **Lesson**: User demand drives bank adoption

---

## Recommended Strategy

### Year 1: Hybrid Approach
1. **Partner with 1-2 digital banks** (Capitec, TymeBank)
   - Offer bank-integrated registration
   - Instant settlement within bank
   - Prove the physics seal concept

2. **Get payment provider license**
   - Allows operation without full bank integration
   - Can onboard users from any bank
   - Mari handles settlement

3. **Build to 100K users**
   - Focus on use case: informal traders, townships
   - Prove fraud reduction with physics seals
   - Generate data on cost savings

### Year 2: Scale & Negotiate
1. **Approach major banks with data**
   - "We have 100K users, X% fraud reduction, Y% cost savings"
   - Offer integration as competitive advantage
   - Or threaten to steal their customers

2. **Expand to 1M users**
   - Network effects kick in
   - Banks can't ignore us

3. **Push for regulatory support**
   - Lobby for interoperability mandates
   - Position as financial inclusion solution

### Year 3: Dominate
1. **All major banks integrated** (or losing customers)
2. **10M+ users**
3. **Expand to other African countries**

---

## Key Insight

**Mari doesn't need banks to succeed, but banks need Mari to stay relevant.**

The physics seal is genuinely innovative security. Once we prove it reduces fraud by 80%+, banks will integrate because:
1. They lose money to fraud
2. Their customers demand it
3. Regulators may mandate it

**We're not asking banks for permission. We're offering them a better fraud prevention system.**

---

## Technical Settlement Architecture

### Bank-Integrated Settlement
```
┌─────────────────────────────────────────────────┐
│              Mari Protocol Layer                │
│  ┌──────────────────────────────────────────┐  │
│  │  Physics Seal Validation                 │  │
│  │  - Motion signature verified             │  │
│  │  - Location proof validated              │  │
│  │  - Timestamp checked                     │  │
│  └──────────────────────────────────────────┘  │
│                     ↓                           │
│  ┌──────────────────────────────────────────┐  │
│  │  Transaction Authorization               │  │
│  │  - Approved: Forward to bank             │  │
│  │  - Rejected: Fraud alert                 │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│           Bank Core Banking System              │
│  ┌──────────────────────────────────────────┐  │
│  │  Immediate Debit (User A)                │  │
│  │  Queue for Settlement (User B)           │  │
│  └──────────────────────────────────────────┘  │
│                     ↓                           │
│  ┌──────────────────────────────────────────┐  │
│  │  Batch Settlement (End of Day)           │  │
│  │  - Net positions calculated              │  │
│  │  - RTGS/ACH transfer                     │  │
│  │  - Credit User B                         │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### Mari-Intermediated Settlement
```
┌─────────────────────────────────────────────────┐
│              Mari Platform                      │
│  ┌──────────────────────────────────────────┐  │
│  │  User Ledger (Instant Settlement)        │  │
│  │  - Debit User A: R100                    │  │
│  │  - Credit User B: R100                   │  │
│  │  - Transaction complete (instant)        │  │
│  └──────────────────────────────────────────┘  │
│                     ↓                           │
│  ┌──────────────────────────────────────────┐  │
│  │  Bank Settlement Queue                   │  │
│  │  - Aggregate cash in/out requests        │  │
│  │  - Batch process daily                   │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│           Partner Banks                         │
│  - Process cash in/out (1-2 days)              │
│  - Mari maintains float in pooled accounts     │
└─────────────────────────────────────────────────┘
```

---

## Bottom Line

**We have three viable paths:**
1. **Best case**: Banks integrate (we're the security layer)
2. **Realistic case**: We're a payment provider (like M-Pesa)
3. **Worst case**: We go crypto (like Bitcoin)

**All three work. We're not dependent on banks saying yes.**

The physics seal is the killer feature. Once we prove it works, everyone will want it.
