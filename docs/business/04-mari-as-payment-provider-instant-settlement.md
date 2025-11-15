# Mari as Payment Provider: Instant Settlement Model

## The Scenario: Banks Refuse to Integrate

**Worst case:**
- Banks don't want to integrate Mari protocol
- We can't be infrastructure layer
- We must become a payment app (like M-Pesa, Venmo, Cash App)

**Challenge:**
- How do we provide instant settlement?
- While maintaining thorough verification?
- Without bank integration?

**Answer: Mari operates its own ledger + pooled bank accounts**

---

## The Architecture: Two-Tier System

### Tier 1: Mari Ledger (Instant Settlement)

```
┌─────────────────────────────────────────────────┐
│           Mari Platform (Our System)            │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │        User Ledger (Database)            │  │
│  │                                          │  │
│  │  Alice:  R1,000.00                       │  │
│  │  Bob:    R500.00                         │  │
│  │  Carol:  R2,500.00                       │  │
│  │  Dave:   R750.00                         │  │
│  │                                          │  │
│  │  Total: R4,750.00                        │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  Transactions happen instantly on this ledger   │
│  (Just database updates, no bank involved)      │
└─────────────────────────────────────────────────┘
```

### Tier 2: Bank Accounts (Cash In/Out)

```
┌─────────────────────────────────────────────────┐
│         Mari's Pooled Bank Accounts             │
│                                                  │
│  FNB Account:           R50,000,000             │
│  Standard Bank Account: R30,000,000             │
│  Capitec Account:       R20,000,000             │
│                                                  │
│  Total Float:           R100,000,000            │
└─────────────────────────────────────────────────┘
         ↕                    ↕
    ┌────────┐          ┌────────┐
    │  FNB   │          │Capitec │
    └────────┘          └────────┘
```

---

## How Instant Settlement Works

### Scenario: Alice Sends R100 to Bob

**Both users have Mari accounts with money loaded:**

```
Step 1: Alice initiates payment
┌─────────────────────────────────────┐
│  Alice's Mari Balance: R1,000       │
│  Bob's Mari Balance:   R500         │
└─────────────────────────────────────┘

Step 2: Alice shakes phone (physics seal)
┌─────────────────────────────────────┐
│  Physics seal validated ✓           │
│  Confidence: 99.95%                 │
│  Fraud risk: 0.05%                  │
└─────────────────────────────────────┘

Step 3: Mari updates ledger (INSTANT)
┌─────────────────────────────────────┐
│  Alice's Mari Balance: R900  (-R100)│
│  Bob's Mari Balance:   R600  (+R100)│
│                                     │
│  Transaction complete: 2 seconds    │
└─────────────────────────────────────┘

Step 4: Both users notified
Alice sees: "✓ R100 sent to Bob"
Bob sees:   "✓ R100 received from Alice"
```

**Key insight: No bank involved in the transaction!**
- Money moves on Mari's ledger (database update)
- Instant (2-3 seconds)
- No inter-bank settlement needed
- Physics seal ensures security

---

## How Users Get Money Into Mari

### Cash In: Bank → Mari

**User wants to load R1,000 into Mari:**

```
Step 1: User initiates cash-in
User: "Load R1,000 from my FNB account"

Step 2: Mari generates payment reference
Mari: "Transfer R1,000 to:
       Account: Mari Payments (Pty) Ltd
       Bank: FNB
       Account #: 62812345678
       Reference: MARI-USER-550e8400"

Step 3: User makes bank transfer
User logs into FNB app
User transfers R1,000 to Mari's account
(Uses existing bank transfer, 1-2 days)

Step 4: Mari receives bank notification
FNB → Mari: "Received R1,000 from Alice (Ref: MARI-USER-550e8400)"

Step 5: Mari credits user's account
Mari Ledger:
  Alice: R0 → R1,000 (+R1,000)

Step 6: User notified
Alice sees: "✓ R1,000 loaded. Ready to send!"
```

**Timeline:**
- User initiates: Day 1
- Bank transfer settles: Day 2-3
- Mari credits account: Day 2-3
- User can now send instantly to other Mari users

---

### Cash Out: Mari → Bank

**User wants to withdraw R500 from Mari:**

```
Step 1: User initiates cash-out
User: "Withdraw R500 to my FNB account"

Step 2: Mari validates request
- Check balance: Alice has R1,000 ✓
- Check withdrawal limits: R500 < R10,000/day ✓
- Physics seal: User shakes phone ✓

Step 3: Mari debits user's account (INSTANT)
Mari Ledger:
  Alice: R1,000 → R500 (-R500)

Step 4: Mari queues bank transfer
Mari's FNB account → Alice's FNB account
Amount: R500
Reference: MARI-WITHDRAWAL-550e8400

Step 5: Bank transfer processes (1-2 days)
FNB processes transfer

Step 6: Alice receives money in bank
Alice's FNB account: +R500

Step 7: Both parties notified
Alice sees: "✓ R500 withdrawn to FNB account"
```

**Timeline:**
- User initiates: Day 1
- Mari debits immediately: Day 1 (instant)
- Bank transfer settles: Day 2-3
- User receives money: Day 2-3

---

## The Verification Process

### Registration: Thorough KYC/AML

**When user signs up for Mari:**

```
Step 1: Phone verification
- User enters phone number
- SMS OTP sent
- User verifies (proves phone ownership)

Step 2: Identity verification
- User takes selfie
- User takes photo of ID (driver's license or passport)
- User provides proof of address (utility bill)

Step 3: Automated verification (Onfido/Jumio)
- Face matching (selfie vs ID photo)
- ID document validation (not fake)
- Liveness detection (not a photo of a photo)
- Address verification (utility bill matches ID)

Step 4: Manual review (if needed)
- Human reviewer checks edge cases
- Approves or rejects within 24 hours

Step 5: Bank account linking
- User enters bank account details
- Mari makes micro-deposit (R1.23)
- User confirms amount (proves account ownership)

Step 6: Account activated
- User can now load money
- Daily limits: R10,000 (until verified)
- Monthly limits: R50,000 (until verified)

Step 7: Enhanced verification (optional)
- User provides additional documents
- Higher limits: R100,000/day, R1M/month
- Business accounts: R1M/day, R10M/month
```

**Verification timeline:**
- Automated: 5-10 minutes
- Manual review: 1-24 hours
- Total: Same day (usually)

**Cost:**
- Automated verification: R20 per user (Onfido/Jumio)
- Manual review: R50 per user (if needed)
- Total: R20-70 per user

---

### Transaction Verification: Physics Seal

**Every transaction requires physics seal:**

```
Step 1: User initiates payment
User: "Send R100 to Bob"

Step 2: User shakes phone
- Motion sensors capture shake pattern
- GPS captures location
- Timestamp recorded
- Device attestation verified

Step 3: Physics seal validated
Mari HSM:
- Motion signature: ✓ Unique, matches user pattern
- Location: ✓ User's home/work area
- Timestamp: ✓ Recent, not replayed
- Device: ✓ Hardware-attested, not rooted
- Confidence: 99.95%

Step 4: Fraud checks
Mari ML model:
- Transaction pattern: ✓ Normal for user
- Recipient: ✓ Not flagged
- Amount: ✓ Within limits
- Velocity: ✓ Not too many transactions
- Risk score: 0.05% (very low)

Step 5: Transaction approved
Mari Ledger:
- Debit sender: -R100
- Credit recipient: +R100
- Transaction complete: 2 seconds
```

**Verification is thorough but fast:**
- Physics seal: 500ms
- Fraud checks: 200ms
- Database update: 100ms
- Total: <1 second

---

## How We Ensure Instant Settlement

### The Key: Pre-Funded Accounts

**Users must load money BEFORE sending:**

```
Traditional banking:
User sends R100 → Bank checks balance → Bank transfers
(Requires real-time bank balance check)

Mari model:
User loads R1,000 → Mari holds it → User sends R100 instantly
(No bank check needed, money already in Mari)
```

**Why this enables instant settlement:**
- Mari already has the money (in pooled accounts)
- No need to wait for bank transfer
- Just update database (instant)
- Settlement already happened (when user loaded money)

---

### The Float Management

**Mari maintains float in bank accounts:**

```
Total user balances: R100M
Mari's bank accounts: R120M (120% reserve)

Why 120%?
- 100% = user balances
- 20% = buffer for withdrawals

Example:
- Users have R100M in Mari accounts
- Mari has R120M in bank accounts
- Users withdraw R10M in one day
- Mari still has R110M in banks (110% reserve)
- Safe
```

**Reserve requirements:**
- Minimum: 100% (fully backed)
- Target: 120% (buffer for withdrawals)
- Maximum: 150% (too much idle cash)

**How Mari manages float:**
```
If reserve < 110%:
- Slow down withdrawals (24-48 hour processing)
- Incentivize cash-ins (0.5% bonus)
- Raise capital (investors)

If reserve > 140%:
- Speed up withdrawals (instant)
- Incentivize cash-outs (0.5% bonus)
- Invest excess (low-risk bonds)
```

---

## The Economics

### Revenue Model

**Transaction fees:**
```
P2P transfers: R0.50 per transaction
Cash in: Free (encourages loading)
Cash out: R5.00 per withdrawal
Merchant payments: 1% (vs 3% for cards)
```

**Example user:**
```
Alice loads R10,000 (free)
Alice sends 20 transactions @ R0.50 = R10
Alice withdraws R5,000 @ R5.00 = R5
Total fees: R15

Mari's cost:
- Transaction processing: R2 (20 × R0.10)
- Bank transfer (withdrawal): R2
- Total cost: R4

Mari's profit: R11 per user per month
```

**At scale:**
```
1M users × R11/month = R11M/month = R132M/year
10M users × R11/month = R110M/month = R1.32B/year
```

---

### Cost Structure

**Fixed costs:**
```
HSM network: R5M/year
Data centers: R2M/year
Compliance: R3M/year
Staff (50 people): R30M/year
Total: R40M/year
```

**Variable costs:**
```
Transaction processing: R0.10 per transaction
KYC verification: R20 per new user
Bank transfer fees: R2 per withdrawal
Fraud losses: 0.01% of transaction volume
```

**Break-even:**
```
Fixed costs: R40M/year
Need: R40M / R11 per user = 3.6M users

At 3.6M users: Break-even
At 5M users: R15M profit/year
At 10M users: R1.28B profit/year
```

---

## Risk Management

### Fraud Risk

**Problem: What if physics seal is bypassed?**

**Solution: Multi-layer fraud detection**
```
Layer 1: Physics seal (99.95% accuracy)
Layer 2: Transaction patterns (ML model)
Layer 3: Velocity checks (not too many transactions)
Layer 4: Amount limits (max R10,000/day for new users)
Layer 5: Manual review (flagged transactions)

Combined accuracy: 99.99%
Fraud rate: 0.01%
```

**Fraud loss calculation:**
```
Transaction volume: R10B/year
Fraud rate: 0.01%
Fraud loss: R1M/year

Insurance: R5M/year (covers up to R50M)
Net cost: R6M/year (0.06% of volume)
```

---

### Liquidity Risk

**Problem: What if everyone withdraws at once?**

**Solution: Reserve requirements + withdrawal limits**
```
Reserve: 120% of user balances
Daily withdrawal limit: 10% of reserves

Example:
- User balances: R100M
- Mari reserves: R120M
- Max daily withdrawals: R12M
- Even if 10% of users withdraw max, we're safe
```

**Bank run scenario:**
```
Day 1: 10% of users withdraw (R10M)
- Mari reserves: R120M → R110M
- Still 110% reserve ✓

Day 2: Another 10% withdraw (R10M)
- Mari reserves: R110M → R100M
- Still 100% reserve ✓

Day 3: Another 10% withdraw (R10M)
- Mari reserves: R100M → R90M
- Below 100% reserve ✗
- Mari pauses withdrawals
- Mari raises capital (R20M from investors)
- Mari reserves: R90M → R110M
- Withdrawals resume
```

**Mitigation:**
- Maintain 120% reserve (buffer)
- Daily withdrawal limits (R10,000/user)
- Staggered processing (24-48 hours)
- Emergency credit line (R50M from bank)
- Insurance (covers bank run up to R100M)

---

### Regulatory Risk

**Problem: What if regulators shut us down?**

**Solution: Full compliance from day 1**
```
Licenses:
- Payment service provider license (SARB)
- AML/CFT compliance (FIC)
- Data protection compliance (POPIA)

Audits:
- Annual financial audit
- Quarterly compliance audit
- Monthly reserve verification

Reporting:
- Suspicious transactions (real-time)
- Large transactions (>R25,000)
- Cross-border transactions
- User data requests (law enforcement)

Result: Regulators trust us, no shutdown risk
```

---

## The User Experience

### Onboarding

```
Day 1: User downloads Mari app
- Sign up (5 minutes)
- Verify phone (SMS OTP)
- Take selfie + ID photo
- Automated verification (5 minutes)
- Account activated ✓

Day 1: User loads money
- Link bank account
- Transfer R1,000 to Mari
- Wait 1-2 days for settlement

Day 3: Money arrives
- Mari credits R1,000
- User can now send instantly

Day 3: User sends money
- Enter recipient phone
- Enter amount (R100)
- Shake phone
- Payment sent (2 seconds) ✓
- Recipient receives instantly ✓
```

**Key insight: After initial 1-2 day load, everything is instant**

---

### Daily Usage

```
Morning:
- Alice sends R50 to Bob (coffee) - Instant ✓
- Bob receives R50 - Instant ✓

Afternoon:
- Alice sends R200 to Carol (lunch) - Instant ✓
- Carol receives R200 - Instant ✓

Evening:
- Alice sends R100 to Dave (taxi) - Instant ✓
- Dave receives R100 - Instant ✓

Total time: 6 seconds (2 seconds per transaction)
Total fees: R1.50 (R0.50 × 3 transactions)
```

**User experience: Instant, cheap, secure**

---

## Comparison: Mari vs Traditional Banking

### Speed

```
Traditional bank transfer:
- Initiate: Day 1
- Settlement: Day 2-3
- Total: 1-3 days

Mari transfer:
- Initiate: Day 1
- Settlement: Day 1 (2 seconds)
- Total: 2 seconds
```

### Cost

```
Traditional bank transfer:
- Fee: R5-10 per transaction
- Hidden fees: Currency conversion, etc.
- Total: R5-15

Mari transfer:
- Fee: R0.50 per transaction
- No hidden fees
- Total: R0.50
```

### Security

```
Traditional bank:
- Password (easily phished)
- SMS OTP (SIM swap vulnerable)
- Security: Medium

Mari:
- Physics seal (can't be faked remotely)
- Device attestation (hardware-backed)
- Security: High
```

---

## The Bottom Line

**If banks refuse to integrate, Mari becomes a payment provider:**

**How instant settlement works:**
1. Users load money into Mari (1-2 days, one-time)
2. Mari holds money in pooled bank accounts
3. Transactions happen on Mari's ledger (instant)
4. Users withdraw when needed (1-2 days)

**Verification is thorough:**
- KYC/AML at registration (5-10 minutes)
- Physics seal for every transaction (99.95% accuracy)
- Multi-layer fraud detection (99.99% combined)
- Regulatory compliance (SARB, FIC, POPIA)

**Economics work:**
- R11 profit per user per month
- Break-even at 3.6M users
- R1.28B profit at 10M users

**Risks are managed:**
- Fraud: 0.01% (insurance covers)
- Liquidity: 120% reserve (buffer)
- Regulatory: Full compliance (no shutdown risk)

**User experience is great:**
- Instant transfers (2 seconds)
- Cheap fees (R0.50 vs R5-10)
- Secure (physics seal)

**This is the M-Pesa model. It works. We can do it.**

**Preferred: Banks integrate (infrastructure layer)**
**Fallback: Payment provider (still works, still profitable)**

**Either way, we win.**
