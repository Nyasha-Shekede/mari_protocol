# MARI PROTOCOL: Complete Research Brief for Kimi

**Date:** November 15, 2024
**Mission:** Deep research on marketing, VC fundraising, and go-to-market execution

---

# PART 1: EXECUTIVE SUMMARY

## What is Mari Protocol?

Mari is a **physics-secured payment protocol** that enables instant, secure, low-cost digital payments with or without internet. Built for Africa.

**The Innovation:**
- User shakes phone to authorize payment (physics seal)
- Motion + GPS + timestamp = unique signature that cannot be faked remotely
- 99.95% fraud detection accuracy
- Works online (HTTP) or offline (SMS)

**The Business:**
- B2B2C model: Banks integrate Mari SDK into their apps
- Revenue: R0.10 per transaction + bank licensing
- Market: R870B in South Africa, R9.2B Africa by Year 5

**Current Status:**
- ✅ Working prototype (Android app)
- ✅ Physics seal algorithm tested
- ✅ 50+ pages documentation
- 🔄 Patent pending
- 🔄 Bank partnerships in discussion

**The Ask:**
- Seed round: R10M ($550K)
- Target: 1M users in 18 months
- Valuation: R40M pre-money

---

# PART 2: YOUR RESEARCH MISSION

## Research Areas

### 1. VC Fundraising Strategy
- Which VCs invest in African fintech?
- What's the fundraising process in SA?
- How should we pitch Mari?
- What accelerators should we apply to?

### 2. Marketing & Growth Strategy
- How to acquire first 1M users?
- What's the CAC for fintech in SA?
- How did M-Pesa grow in Kenya?
- What channels work best?

### 3. Partnership Development
- Which banks to approach first?
- How to pitch banks?
- Which telcos, merchants, tech partners?

### 4. Regulatory Navigation
- What licenses do we need?
- What's the SARB approval process?
- How long does it take?
- What does it cost?

### 5. Competitive Intelligence
- Who are the competitors?
- How do we position vs them?
- What will they do when we launch?

### 6. Team Building
- How to find a co-founder?
- What roles to hire first?
- What are SA fintech salaries?
- How to find advisors?

---
# PART 3: MARI PROTOCOL - COMPLETE CONTEXT

## The Technology

### Physics Seal (Core Innovation)
**How it works:**
1. User shakes phone
2. Motion sensors capture unique shake pattern
3. GPS captures location
4. Timestamp recorded
5. Device attestation (TPM/TEE) proves hardware authenticity
6. All combined into cryptographic signature
7. Sent to HSM network for validation
8. Bank receives authorization token

**Why it's secure:**
- Remote malware can't shake your phone (blocks 80% of fraud)
- Attacker in different location = different GPS
- Replay attacks blocked by timestamp
- Device attestation prevents spoofing

**Performance:**
- Physics seal generation: 500ms
- HSM validation: 200ms
- Total transaction time: 2-3 seconds

### Offline Payments (SMS)
**How it works:**
- App detects no internet connection
- Switches to SMS protocol automatically
- Physics seal encoded in encrypted SMS
- Sent via cellular network (works on 2G)
- Mari gateway receives and validates
- Bank processes payment
- Confirmation sent via SMS

**Why it matters:**
- Load shedding: 4-8 hours/day in SA
- Rural areas: Spotty internet
- Zero-data customers: No data plan needed
- Works on 2G signal

**Cost:**
- SMS: R0.80 (standard rate)
- Mari fee: R0.50
- Total: R1.30 (vs R5 bank transfer)

### Architecture
```
APPLICATION LAYER (FNB App, Capitec App, etc.)
    ↓
MARI PROTOCOL LAYER (Physics seal validation, HSM network, Fraud detection)
    ↓
SETTLEMENT LAYER (Banks, Payment Providers)
```

**Key Point:** Mari doesn't hold money. Banks do. Mari validates security, banks move money.

---

## The Business Model

### B2B2C Infrastructure
**Not a standalone app. We're the protocol.**

**How it works:**
1. Banks integrate Mari SDK into their existing apps
2. Users never download "Mari app"
3. Users use their trusted bank app with new capabilities
4. Mari provides infrastructure, banks provide customers

**Why this wins:**
- Lower CAC: Banks bring existing customers
- Trust: Users trust their bank
- Faster scale: Tap into existing user bases
- Regulatory: Banks handle compliance

### Revenue Streams

**1. Transaction Fees (Primary)**
- R0.10 per transaction
- Charged to banks
- Banks may pass to users or absorb
- Volume discounts available

**2. Bank Licensing (Secondary)**
- Enterprise tier: R50K/month (unlimited transactions)
- Custom pricing for major banks

**3. Value-Added Services (Tertiary)**
- Advanced fraud analytics: R10K/month
- Custom integration: R50K/month
- White-label SDK: R100K/month

### Unit Economics
**Per User (Monthly):**
- Transactions: 20
- Revenue: R2 (20 × R0.10)
- Cost: R0.50 (infrastructure)
- Profit: R1.50 per user/month

**At Scale (5M users):**
- Revenue: R10M/month = R120M/year
- Costs: R2.5M/month = R30M/year
- Profit: R7.5M/month = R90M/year

---
# PART 4: MARKET ANALYSIS

## South Africa (Launch Market)

### Market Size
- **Population:** 60M
- **Smartphone users:** 40M (67% penetration)
- **Banked:** 80% (48M people)
- **Total payment market:** R2.5 trillion/year
- **Addressable market:** R870B/year

### Market Breakdown
**P2P transfers:** R200B/year
- Current: Bank transfers (1-3 days, R5 fee)
- Mari opportunity: Instant, R0.50 fee

**Merchant payments:** R300B/year
- Current: Card fees 3%
- Mari opportunity: 1% fee, instant settlement

**Informal economy:** R370B/year
- Current: Cash-based (risky, no records)
- Mari opportunity: Digital, secure, instant

### Target Users (35M addressable)

**Young professionals (5M):**
- Age: 25-40
- Income: R15K-50K/month
- Use case: Split bills, pay friends
- Transactions: 10/month × R200 = R2K/month

**Township residents (15M):**
- Age: 20-50
- Income: R3K-15K/month
- Use case: Send money home, pay at spaza shop
- Transactions: 20/month × R100 = R2K/month

**Informal traders (10M):**
- Age: 25-60
- Income: R5K-20K/month
- Use case: Accept payments, pay suppliers
- Transactions: 50/month × R50 = R2.5K/month

**Students (5M):**
- Age: 18-25
- Income: R0-5K/month
- Use case: Receive from parents, split costs
- Transactions: 15/month × R100 = R1.5K/month

### Revenue Projections

**Year 1:**
- Users: 350K (1% penetration)
- Volume: R8.7B
- Revenue: R43.5M

**Year 3:**
- Users: 1.75M (5% penetration)
- Volume: R43.5B
- Revenue: R217.5M

**Year 5 (Conservative):**
- Users: 5.25M (15% penetration)
- Volume: R130.5B
- Revenue: R652.5M
- Valuation: R6.5B

**Year 5 (Aggressive):**
- Users: 17.5M (50% penetration - M-Pesa level)
- Volume: R435B
- Revenue: R2.175B
- Valuation: R21.75B

---

## Africa Expansion

### Phase 1: SADC (Years 2-3)
**Target countries:**
- Botswana: 500K users
- Namibia: 500K users
- Zimbabwe: 3M users (remittances from SA!)
- Zambia: 3M users
- Mozambique: 5M users

**Total SADC:** 12M users, R600M revenue

### Phase 2: Pan-African (Years 3-5)
**Target countries:**
- Nigeria: 20M users (220M population!)
- Kenya: 10M users (compete with M-Pesa)
- Ghana: 5M users
- Uganda: 7M users
- Tanzania: 10M users
- Egypt: 15M users

**Total Africa:** 98M users, R9.2B revenue, R92B valuation

### Phase 3: Global (Years 5-10)
- India: 50M users
- Brazil: 30M users
- Indonesia: 40M users
- Europe: 50M users
- USA: 50M users

**Total Global:** 463M users, R136B revenue, R1.36T valuation ($75B)

---

## Competitive Landscape

### Direct Competitors

**SnapScan / Zapper:**
- QR payment apps
- Merchant-focused
- Still uses card rails (3% fees)
- Limited adoption (5M users)
- **Mari advantage:** P2P + merchant, lower fees, physics security

**Bank transfers:**
- Slow (1-3 days)
- Complicated (16-digit account number)
- High fees (R5)
- **Mari advantage:** Instant, phone number only, R0.50

**Cash:**
- Instant, free
- But: Risky (robbery), no digital record
- **Mari advantage:** Digital, secure, remote

### Indirect Competitors

**M-Pesa (if they enter SA):**
- Dominant in Kenya (30M users)
- Telco-owned (Safaricom)
- **Mari advantage:** Bank-integrated, physics security, better fraud prevention

**International players (Venmo, Cash App, PayPal):**
- Not in SA yet
- Not localized (Rand support)
- **Mari advantage:** Local, Rand-native, bank-integrated

### Competitive Positioning

**vs SnapScan/Zapper:** "We do P2P + merchant, they only do merchant"
**vs Bank transfers:** "We're instant, they take 3 days"
**vs Cash:** "We're digital and secure, cash is risky"
**vs M-Pesa:** "We're bank-integrated with physics security"

---
# PART 5: STRATEGY & EXECUTION

## Banking Integration Strategy

### Three-Tier Approach

**Tier 1: Bank-Integrated (Ideal)**
- Banks integrate Mari SDK
- Instant settlement within same bank
- Inter-bank via existing rails (RTGS/ACH)
- Mari provides security layer

**Tier 2: Payment Provider (Fallback)**
- Mari operates own ledger
- Users load money into Mari accounts
- Instant settlement on Mari's ledger
- Cash in/out to banks (1-2 days)
- Like M-Pesa model

**Tier 3: Crypto/Stablecoin (Nuclear Option)**
- Rand-pegged stablecoins
- Blockchain settlement
- If banks completely refuse

### Bank Partnership Process

**Phase 1: Pilot (6 months)**
- Target: 1 bank (Capitec or TymeBank)
- Users: 10K
- Pricing: R0.05 per transaction (50% discount)
- Success metrics: >99.5% success rate, <0.1% fraud

**Phase 2: Commercial (12 months)**
- Target: 5 banks
- Users: 1M
- Pricing: R0.10 per transaction
- Exclusive contracts: 12-24 months

**Phase 3: Dominate (24 months)**
- Target: All major SA banks
- Users: 5M
- Network effects kick in
- Winner-takes-all

### Why Banks Will Integrate

**Cost Savings:**
- R35M savings per 1M transactions
- 80-90% fraud reduction
- ROI: 6,900%

**Competitive Advantage:**
- Instant payments (vs 1-3 days)
- Lower fraud (vs current methods)
- Better UX (phone number vs account number)

**Regulatory Support:**
- Fraud reduction benefits everyone
- Financial inclusion mandate
- Regulators will encourage adoption

---

## Competitive Moats (9 Defensibility Factors)

### 1. Network Effects (Strongest)
- More users = more valuable
- Winner-takes-all market
- First to 1M users wins

### 2. Bank Partnerships
- Exclusive contracts (12-24 months)
- High switching costs
- Integration takes 6-12 months

### 3. HSM Infrastructure
- R85M + 18 months to build
- Capital intensive
- Hardware moat

### 4. Fraud Detection Data
- More transactions = better models
- Competitors can't catch up
- Data moat

### 5. Regulatory Approvals
- 12+ months to get licensed
- Compliance history = trust
- First-mover advantage

### 6. Patents
- Physics seal method (pending)
- 20-year protection
- Legal moat

### 7. Brand & Trust
- "Mari" becomes verb ("I'll Mari you")
- Trust takes time to build
- Reputation moat

### 8. Developer Ecosystem
- 10,000+ apps integrate SDK
- High switching costs
- Platform lock-in

### 9. Operational Excellence
- 99.99% uptime
- Sub-500ms response time
- Execution moat

---

## Go-to-Market Plan

### Phase 1: Pilot (Months 1-12)

**Target:** 10,000 users

**Strategy:**
- Partner with 1 bank (Capitec or TymeBank)
- University campus activations
- Referral program (R50 bonus)
- Social media marketing

**Budget:** R5M
**CAC:** R500/user

**Metrics:**
- >99.5% transaction success
- <0.1% fraud rate
- <3 second transaction time
- >90% user satisfaction

### Phase 2: Scale (Months 13-24)

**Target:** 1,000,000 users

**Strategy:**
- Expand to 5 banks
- Township activations
- Spaza shop partnerships
- Radio/TV advertising

**Budget:** R50M
**CAC:** R50/user (word of mouth)

**Metrics:**
- 1M active users
- R10B transaction volume
- R50M revenue
- Break-even

### Phase 3: Dominate (Months 25-36)

**Target:** 5,000,000 users

**Strategy:**
- All major SA banks
- Merchant onboarding
- App integrations (Uber, Takealot)
- Regional expansion (SADC)

**Budget:** R100M
**CAC:** R20/user (network effects)

**Metrics:**
- 5M active users
- R130B transaction volume
- R650M revenue
- Profitable

---

## Fundraising Strategy

### Seed Round (Now)

**Amount:** R10M ($550K)
**Valuation:** R40M pre-money, R50M post-money
**Equity:** 20%

**Use of Funds:**
- Technology (40%): R4M (HSM, app, backend)
- Regulatory (20%): R2M (legal, compliance, licensing)
- Team (30%): R3M (CTO, compliance, developers)
- Go-to-market (10%): R1M (pilot launch, marketing)

**Milestones:**
- Month 6: Pilot launch (10K users)
- Month 12: Commercial launch (100K users)
- Month 18: Scale (1M users, break-even)

### Series A (18 months)

**Amount:** R50M ($2.75M)
**Valuation:** R200M pre-money
**Target:** Scale to 5M users

### Series B (36 months)

**Amount:** R200M ($11M)
**Valuation:** R2B pre-money
**Target:** Pan-African expansion

---
# PART 6: YOUR SPECIFIC RESEARCH TASKS

## Task 1: VC Fundraising (Week 1)

### Deliverable 1.1: Target Investor List (50 VCs)
**Research:**
- Which VCs invested in: M-Pesa, Flutterwave, Paystack, Chipper Cash, Wave, Yoco?
- Which VCs focus on African fintech?
- Which VCs invest in infrastructure/protocol plays?
- Which VCs do seed rounds (R10M/$550K)?
- Which VCs have South African presence?

**Format:**
| VC Name | Location | Ticket Size | Portfolio | Contact | Fit Score (1-10) |
|---------|----------|-------------|-----------|---------|------------------|

**Prioritize by:**
1. African fintech focus
2. Infrastructure investments
3. Seed stage focus
4. SA presence
5. Relevant portfolio

### Deliverable 1.2: Fundraising Process Map
**Research:**
- What's the typical seed round timeline in SA?
- What documents are needed? (Pitch deck, financial model, term sheet, SHA)
- What due diligence to expect? (Technical, financial, legal, market)
- What valuation multiples are typical? (Revenue, users, comparable exits)
- What equity dilution is standard? (15-25%?)

**Format:**
```
Week 1: Warm intros, initial meetings
Week 2-4: Pitch presentations
Week 4-8: Due diligence
Week 8-12: Term sheet negotiation
Week 12-16: Legal docs, closing
```

### Deliverable 1.3: Pitch Strategy
**Research:**
- What do VCs look for in fintech pitches?
- How should we position Mari? (Infrastructure vs app, Africa's Stripe vs M-Pesa 2.0)
- What metrics matter most? (Users, revenue, fraud rate, CAC, LTV)
- What objections will we face? (Competition, regulatory, execution risk)
- How to demonstrate traction without users? (Prototype, LOIs, pilot agreements)

**Format:**
- Positioning statement
- Key messages (3-5 bullets)
- Objection handling (Q&A)
- Traction proof points

### Deliverable 1.4: Accelerator Analysis
**Research:**
- Y Combinator: Application process, acceptance rate, terms
- Techstars: Same as above
- 500 Startups: Same as above
- African-focused accelerators: Flat6Labs, Grindstone, etc.

**Format:**
| Accelerator | Funding | Equity | Duration | Benefits | Application Deadline |
|-------------|---------|--------|----------|----------|---------------------|

---

## Task 2: Marketing & Growth (Week 2)

### Deliverable 2.1: Customer Acquisition Playbook
**Research:**
- How did M-Pesa acquire first 1M users in Kenya? (Timeline, tactics, CAC)
- How did Capitec grow to 20M customers in SA? (Strategy, channels)
- What's typical CAC for fintech in SA? (By channel, by segment)
- What channels work best? (Social media, radio, TV, campus, word-of-mouth)

**Format:**
Month-by-month plan:
- Month 1: Campus activations (target: 1K users, CAC: R500)
- Month 2: Referral program (target: 2K users, CAC: R250)
- Month 3: Social media (target: 5K users, CAC: R200)
- etc.

### Deliverable 2.2: Bank Partnership Pitch
**Research:**
- Who are the decision makers at banks? (CTO, CIO, Head of Innovation, CEO)
- What's the typical sales cycle? (6 months? 12 months?)
- What case studies convince banks? (Fraud reduction, cost savings, competitive advantage)
- What pilot terms are standard? (Duration, user count, pricing, success metrics)

**Format:**
- Bank pitch deck (10 slides)
- ROI calculator (Excel model)
- Pilot proposal template
- Decision maker contact list

### Deliverable 2.3: Launch Strategy
**Research:**
- Should we do soft launch or big bang?
- Which city first? (Johannesburg, Cape Town, Durban - pros/cons)
- Which segment first? (Students, young professionals, traders - why)
- What's the ideal launch event? (Campus, press conference, influencer event)
- How to generate PR? (TechCrunch, local media, influencers)

**Format:**
- Launch timeline (Week-by-week)
- Launch budget (By activity)
- PR contact list (Media, influencers)
- Launch event plan

### Deliverable 2.4: Brand Positioning
**Research:**
- What messaging resonates with South Africans?
- Should we emphasize: Security, offline payments, instant settlement, or low cost?
- What brand personality? (Trustworthy, innovative, accessible, African)
- What competitors' positioning? (How to differentiate)

**Format:**
- Positioning statement (1 sentence)
- Key messages (3-5 bullets)
- Brand personality (3-5 adjectives)
- Tagline options (5-10 options)

---

## Task 3: Partnerships & Regulatory (Week 3)

### Deliverable 3.1: Bank Partnership Targets
**Research:**
- Rank SA banks by innovation: Which have fintech partnerships? Digital adoption? Innovation labs?
- Which banks should we approach first? (Capitec, TymeBank, Bank Zero - why)
- Who are the key contacts? (Names, titles, LinkedIn, email)
- What's their current payment infrastructure? (Gaps Mari can fill)

**Format:**
| Bank | Innovation Score | Digital Users | Key Contact | Approach Strategy | Priority |
|------|------------------|---------------|-------------|-------------------|----------|

### Deliverable 3.2: Regulatory Roadmap
**Research:**
- What licenses do we need? (Payment service provider, other)
- What's the SARB approval process? (Steps, timeline, cost)
- What's the SARB fintech sandbox? (How to apply, requirements, benefits)
- What are FICA/POPIA requirements? (AML/CFT, data protection)
- What are compliance costs? (Legal fees, ongoing compliance)

**Format:**
```
Month 1-2: Engage with SARB (informal consultations)
Month 3-4: Sandbox application
Month 5-8: Sandbox approval and pilot
Month 9-12: Full license application
Month 13-18: Full license approval
```

### Deliverable 3.3: Telco & Merchant Partnerships
**Research:**
- Which telcos operate in SA? (MTN, Vodacom, Cell C, Telkom)
- Do they have mobile money? (Would they partner or compete?)
- What are SMS costs? (Bulk rates for millions of messages)
- Which merchants to target? (Shoprite, Pick n Pay, Spar)
- Which payment processors? (Yoco, iKhokha, PayFast - partner or compete?)

**Format:**
- Telco partnership proposal
- Merchant onboarding plan
- SMS gateway integration guide

---

## Task 4: Competitive & Team (Week 4)

### Deliverable 4.1: Competitive Analysis
**Research:**
- SnapScan: Users, revenue, funding, strengths, weaknesses
- Zapper: Same as above
- Bank instant payments: Which banks offer? How do they work?
- M-Pesa: If they enter SA, what's their strategy?

**Format:**
| Competitor | Users | Revenue | Strengths | Weaknesses | Our Advantage |
|------------|-------|---------|-----------|------------|---------------|

### Deliverable 4.2: Hiring Plan
**Research:**
- What roles do we need? (CTO, compliance, developers, marketing)
- What are SA fintech salaries? (By role, by experience level)
- Where to find talent? (Universities, banks, startups, LinkedIn)
- Should we hire remote or local? (Pros/cons)

**Format:**
| Role | Salary Range | Where to Find | When to Hire | Priority |
|------|--------------|---------------|--------------|----------|

### Deliverable 4.3: Co-Founder Search
**Research:**
- Where to find fintech co-founders in SA? (Networks, events, platforms)
- What background? (Finance, banking, business, sales)
- What equity split? (50/50, 60/40, 70/30 - what's fair?)
- How to vet? (Questions to ask, red flags)

**Format:**
- Co-founder profile (ideal background, skills, personality)
- Search strategy (where to look, how to approach)
- Vetting checklist (questions, assessments)
- Equity negotiation guide

---

## Success Criteria

Your research is successful if:

1. **Actionable**: We can execute immediately
2. **Specific**: Names, numbers, timelines (not generic)
3. **Realistic**: Grounded in SA context (not Silicon Valley)
4. **Comprehensive**: No major gaps
5. **Prioritized**: Clear what to do first

---

## Deliverable Format

For each task, provide:

### 1. Executive Summary (1 page)
- Key findings
- Top 3 recommendations
- Critical risks
- Next steps

### 2. Detailed Analysis (5-10 pages)
- Data and sources
- Case studies
- Benchmarks
- Best practices

### 3. Actionable Playbook (2-5 pages)
- Step-by-step guide
- Timeline
- Budget
- Success metrics

### 4. Resources (1 page)
- Contact lists
- Template documents
- Tools and services
- Further reading

---

## Timeline

**Week 1:** VC Fundraising
**Week 2:** Marketing & Growth
**Week 3:** Partnerships & Regulatory
**Week 4:** Competitive & Team

**Total:** 4 weeks of deep research

---

## Final Note

This is not academic research. This is battle plans.

We need specific, actionable intelligence to:
- Raise R10M seed round
- Acquire first 1M users
- Partner with banks
- Navigate regulations
- Beat competitors
- Build the team

**Go deep. Be specific. Think like a founder.**

**Help us turn Mari from prototype to R21.75B company.**

**Let's build the payment infrastructure for Africa.** 🚀
