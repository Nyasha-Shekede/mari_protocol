# Mari: Protocol Infrastructure, Not Just an App

## The Paradigm Shift

**Mari is NOT:**
- Just a payment app
- A competitor to banking apps
- A closed ecosystem

**Mari IS:**
- A **protocol layer** (like HTTPS, SMTP, TCP/IP)
- **Payment infrastructure** that anyone can build on
- An **open standard** for physics-secured transactions

---

## The Architecture: Three Layers

```
┌─────────────────────────────────────────────────┐
│         APPLICATION LAYER (What Users See)      │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │  FNB App │  │ Capitec  │  │  WhatsApp│      │
│  │          │  │   App    │  │  Payments│      │
│  └──────────┘  └──────────┘  └──────────┘      │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ Mari App │  │ Shoprite │  │  Uber    │      │
│  │(Reference)│  │   POS    │  │  Rides   │      │
│  └──────────┘  └──────────┘  └──────────┘      │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│      PROTOCOL LAYER (Mari Infrastructure)       │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │     Mari Protocol SDK                    │  │
│  │  - Physics seal generation               │  │
│  │  - Device attestation                    │  │
│  │  - Cryptographic signing                 │  │
│  │  - Location verification                 │  │
│  └──────────────────────────────────────────┘  │
│                     ↓                           │
│  ┌──────────────────────────────────────────┐  │
│  │     Mari HSM Network                     │  │
│  │  - Validate physics seals                │  │
│  │  - Sign authorization tokens             │  │
│  │  - Fraud detection                       │  │
│  │  - Audit logging                         │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│       SETTLEMENT LAYER (Banking System)         │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │  Bank A  │  │  Bank B  │  │  Bank C  │      │
│  │ Ledger   │  │ Ledger   │  │ Ledger   │      │
│  └──────────┘  └──────────┘  └──────────┘      │
│                     ↓                           │
│         Inter-Bank Settlement (RTGS/ACH)        │
└─────────────────────────────────────────────────┘
```

---

## How Different Apps Build on Mari

### 1. Banking Apps (FNB, Capitec, Standard Bank)

**Integration:**
```kotlin
// FNB App code
import com.mari.protocol.MariSDK

fun sendMoney(recipient: String, amount: Double) {
    // User shakes phone
    val physicsSeal = MariSDK.generatePhysicsSeal(
        motionData = sensorManager.getMotionData(),
        location = locationManager.getCurrentLocation(),
        deviceAttestation = teeManager.getAttestation()
    )
    
    // Send to FNB backend
    fnbAPI.sendMoney(
        recipient = recipient,
        amount = amount,
        mariSeal = physicsSeal
    )
    
    // FNB backend validates with Mari HSM
    // FNB debits account immediately
    // User sees instant confirmation
}
```

**User Experience:**
- User opens FNB app (not Mari app)
- User sends money using FNB's UI
- Behind the scenes: Mari protocol validates security
- User sees: "FNB Instant Pay - Secured by Mari"

---

### 2. Payment Apps (WhatsApp, Telegram, WeChat)

**Integration:**
```kotlin
// WhatsApp Payments code
import com.mari.protocol.MariSDK

fun sendMoneyInChat(chatId: String, amount: Double) {
    // User shakes phone while in WhatsApp chat
    val physicsSeal = MariSDK.generatePhysicsSeal()
    
    // WhatsApp sends to recipient's bank via Mari
    mariAPI.initiatePayment(
        sender = currentUser.phoneNumber,
        recipient = chatRecipient.phoneNumber,
        amount = amount,
        physicsSeal = physicsSeal
    )
    
    // Mari routes to appropriate banks
    // Settlement happens bank-to-bank
    // Chat shows: "💸 You sent R100 to John"
}
```

**User Experience:**
- User chats with friend on WhatsApp
- User clicks "Send Money" in chat
- User shakes phone (Mari protocol)
- Money sent instantly
- Never leaves WhatsApp

---

### 3. E-Commerce (Shoprite, Takealot, Uber)

**Integration:**
```kotlin
// Shoprite POS Terminal code
import com.mari.protocol.MariSDK

fun processPayment(customerId: String, amount: Double) {
    // Customer shakes their phone near POS
    val physicsSeal = MariSDK.generatePhysicsSeal()
    
    // POS validates with Mari
    val authorization = mariAPI.authorize(
        customer = customerId,
        merchant = "shoprite_store_123",
        amount = amount,
        physicsSeal = physicsSeal
    )
    
    if (authorization.approved) {
        // Instant payment confirmation
        printReceipt()
        releaseGoods()
    }
}
```

**User Experience:**
- Customer shops at Shoprite
- At checkout: "Shake your phone to pay"
- Customer shakes phone
- Payment approved instantly
- Receipt prints: "Paid via Mari Protocol"

---

### 4. Ride-Hailing (Uber, Bolt)

**Integration:**
```kotlin
// Uber app code
import com.mari.protocol.MariSDK

fun payForRide(rideId: String, amount: Double) {
    // Ride ends, payment triggered
    val physicsSeal = MariSDK.generatePhysicsSeal()
    
    // Uber charges via Mari
    mariAPI.chargeCustomer(
        customer = currentUser.phoneNumber,
        merchant = "uber_driver_${driverId}",
        amount = amount,
        physicsSeal = physicsSeal
    )
    
    // Driver receives money instantly
    // Customer sees: "Paid R85 - Secured by Mari"
}
```

**User Experience:**
- Ride ends
- App says: "Shake to confirm payment"
- User shakes phone
- Driver paid instantly
- No credit card needed

---

## The Mari SDK: What Developers Get

### SDK Components

```kotlin
// Mari Protocol SDK
package com.mari.protocol

class MariSDK {
    // 1. Physics Seal Generation
    fun generatePhysicsSeal(
        motionData: MotionData,
        location: Location,
        deviceAttestation: ByteArray
    ): PhysicsSeal
    
    // 2. Transaction Authorization
    fun authorizeTransaction(
        sender: String,
        recipient: String,
        amount: Double,
        physicsSeal: PhysicsSeal
    ): AuthorizationResult
    
    // 3. Device Registration
    fun registerDevice(
        phoneNumber: String,
        deviceId: String,
        publicKey: ByteArray
    ): RegistrationResult
    
    // 4. QR Code Generation
    fun generatePaymentQR(
        userId: String,
        amount: Double? = null
    ): Bitmap
    
    // 5. QR Code Scanning
    fun scanPaymentQR(
        qrData: String
    ): PaymentRequest
    
    // 6. Offline Transaction
    fun createOfflineTransaction(
        recipient: String,
        amount: Double,
        physicsSeal: PhysicsSeal
    ): OfflineTransaction
    
    // 7. Transaction Status
    fun getTransactionStatus(
        transactionId: String
    ): TransactionStatus
}
```

### SDK Documentation

```markdown
# Mari Protocol SDK

## Installation

### Android
```gradle
dependencies {
    implementation 'com.mari.protocol:mari-sdk:1.0.0'
}
```

### iOS
```swift
pod 'MariProtocol', '~> 1.0'
```

### Web
```javascript
npm install @mari/protocol-sdk
```

## Quick Start

```kotlin
// Initialize SDK
MariSDK.initialize(
    apiKey = "your_api_key",
    environment = Environment.PRODUCTION
)

// Generate physics seal
val seal = MariSDK.generatePhysicsSeal(
    motionData = getMotionData(),
    location = getLocation(),
    deviceAttestation = getAttestation()
)

// Authorize transaction
val result = MariSDK.authorizeTransaction(
    sender = "+27821234567",
    recipient = "+27829876543",
    amount = 100.0,
    physicsSeal = seal
)

if (result.approved) {
    // Payment successful
}
```

## API Reference

See full documentation at: https://docs.mari.protocol
```

---

## The Business Model: Multi-Sided Platform

### Revenue Streams

**1. Transaction Fees (Primary)**
```
Every transaction using Mari protocol:
- R0.10 per transaction
- Charged to: Bank or app developer
- Volume discounts available
- Revenue: R100M at 1B transactions/year
```

**2. SDK Licensing (Secondary)**
```
Developers integrating Mari:
- Free tier: Up to 10K transactions/month
- Startup tier: R5,000/month (up to 100K transactions)
- Enterprise tier: R50,000/month (unlimited)
- Custom pricing for banks
```

**3. Value-Added Services (Tertiary)**
```
Premium features:
- Advanced fraud analytics: R10,000/month
- Custom integration support: R50,000/month
- White-label SDK: R100,000/month
- Dedicated HSM nodes: R500,000/month
```

---

## Who Builds on Mari?

### Tier 1: Banks (Core Partners)
**Why they integrate:**
- Fraud reduction (80-90%)
- Cost savings (R35M per 1M transactions)
- Competitive advantage (instant payments)
- Regulatory compliance

**What they build:**
- Integrate Mari into existing banking apps
- Offer "Instant Pay" feature to customers
- Use Mari for inter-bank transfers
- Brand as: "FNB Instant Pay - Secured by Mari"

**Revenue model:**
- Bank pays Mari: R0.10 per transaction
- Bank charges users: R2.00 per transaction
- Bank profit: R1.90 per transaction

---

### Tier 2: Payment Apps (Growth Partners)
**Who:**
- WhatsApp, Telegram, WeChat (messaging apps)
- Venmo, Cash App, PayPal (payment apps)
- M-Pesa, Airtel Money (mobile money)

**Why they integrate:**
- Add payment feature to existing app
- No need to build fraud detection
- Instant settlement
- Cross-border capability

**What they build:**
- In-app payment buttons
- Chat-based payments
- QR code payments
- Peer-to-peer transfers

**Revenue model:**
- App pays Mari: R0.10 per transaction
- App charges users: R1.00 per transaction
- App profit: R0.90 per transaction

---

### Tier 3: Merchants (Volume Partners)
**Who:**
- Shoprite, Pick n Pay (retail)
- Uber, Bolt (ride-hailing)
- Takealot, Amazon (e-commerce)
- Restaurants, gas stations (SMEs)

**Why they integrate:**
- Lower payment processing fees (vs credit cards)
- Instant settlement (vs 3-day card settlement)
- No chargebacks (physics seal proves intent)
- Better fraud protection

**What they build:**
- POS terminal integration
- Mobile app checkout
- QR code payment acceptance
- Subscription billing

**Revenue model:**
- Merchant pays Mari: R0.10 per transaction
- Merchant saves: R2.00 per transaction (vs 3% card fee on R100)
- Merchant profit: R1.90 per transaction

---

### Tier 4: Developers (Innovation Partners)
**Who:**
- Fintech startups
- App developers
- System integrators
- Independent developers

**Why they integrate:**
- Build payment features without banking license
- Access to Mari network (all banks)
- Fraud protection included
- Fast time-to-market

**What they build:**
- Niche payment apps (e.g., student payments)
- Industry-specific solutions (e.g., agriculture)
- Cross-border remittances
- Micro-lending platforms

**Revenue model:**
- Developer pays Mari: R0.10 per transaction
- Developer charges users: Variable
- Developer profit: Variable

---

## The Network Effect

### Phase 1: Seed the Network (Year 1)
```
1 bank integrated
↓
10,000 users can send to each other
↓
Limited network value
```

### Phase 2: Critical Mass (Year 2)
```
5 banks integrated
↓
1,000,000 users can send to each other
↓
Network becomes useful
↓
WhatsApp integrates (wants access to 1M users)
↓
WhatsApp brings 10M users
↓
More banks want to integrate (FOMO)
```

### Phase 3: Dominant Platform (Year 3)
```
All major banks integrated
↓
50M users on network
↓
Every app wants to integrate
↓
Mari becomes default payment protocol
↓
Network effects = winner-takes-all
```

---

## The Reference App: Mari App

**Purpose:**
- Demonstrate the protocol
- Provide fallback for users without bank integration
- Test new features
- Marketing tool

**NOT the main product:**
- Mari Protocol is the product
- Mari App is just one implementation
- Like how Google Chrome demonstrates web standards
- But anyone can build a browser

**User journey:**
```
User downloads Mari App
↓
User registers (links bank account)
↓
User sends money to friend
↓
Friend receives in their bank app (FNB)
↓
Friend doesn't need Mari App
↓
Protocol works across apps
```

---

## Comparison: Mari vs Other Protocols

### Like HTTP (Web Protocol)
```
HTTP = Protocol for web pages
Browsers = Apps that use HTTP (Chrome, Firefox, Safari)
Websites = Content delivered via HTTP

Mari = Protocol for payments
Banking apps = Apps that use Mari (FNB, Capitec)
Transactions = Payments delivered via Mari
```

### Like SMTP (Email Protocol)
```
SMTP = Protocol for email
Email clients = Apps that use SMTP (Gmail, Outlook)
Messages = Content delivered via SMTP

Mari = Protocol for payments
Payment apps = Apps that use Mari (WhatsApp, Uber)
Money = Value delivered via Mari
```

### Like TCP/IP (Internet Protocol)
```
TCP/IP = Protocol for internet
Apps = Use TCP/IP (WhatsApp, Netflix, Zoom)
Data = Delivered via TCP/IP

Mari = Protocol for payments
Apps = Use Mari (Banks, merchants, apps)
Money = Delivered via Mari
```

---

## The Open Standard Strategy

### Why Open Standard?
**Network effects:**
- More apps integrate = more users
- More users = more valuable to apps
- More valuable = more apps integrate
- Virtuous cycle

**Avoid platform risk:**
- If Mari is closed, apps won't integrate (fear of lock-in)
- If Mari is open, apps integrate freely
- Open standard = faster adoption

**Regulatory advantage:**
- Regulators prefer open standards (interoperability)
- Closed platforms face antitrust scrutiny
- Open standard = regulatory support

### What's Open vs Closed?

**Open (Free to Use):**
- Protocol specification (how physics seals work)
- SDK source code (how to generate seals)
- API documentation (how to integrate)
- Test environment (sandbox for developers)

**Closed (Mari's Competitive Advantage):**
- HSM network (hardware infrastructure)
- Fraud detection algorithms (ML models)
- Bank partnerships (commercial agreements)
- Insurance fund (risk management)

**Analogy:**
- HTTP specification is open (anyone can implement)
- But Cloudflare's CDN network is closed (competitive advantage)
- Mari is the same: Open protocol, closed infrastructure

---

## The Developer Ecosystem

### Mari Developer Portal

```
https://developers.mari.protocol

Features:
- SDK downloads (Android, iOS, Web)
- API documentation
- Code examples
- Sandbox environment
- Developer dashboard
- Community forum
- Integration guides
- Video tutorials
```

### Developer Incentives

**1. Free Tier**
```
Up to 10K transactions/month: Free
- Perfect for startups
- No credit card required
- Full API access
- Sandbox environment
```

**2. Hackathons**
```
Quarterly hackathons:
- Prize: R100,000
- Theme: "Build on Mari"
- Winners get featured
- Free enterprise tier for 1 year
```

**3. Partner Program**
```
Certified Mari Partners:
- Training and certification
- Co-marketing opportunities
- Revenue share (20% of fees)
- Priority support
```

**4. Open Source Contributions**
```
Contribute to Mari SDK:
- Bug fixes
- New features
- Documentation
- Rewards: Free enterprise tier
```

---

## Bottom Line

**Mari is infrastructure, not an app.**

**Think of it like:**
- **Visa/Mastercard**: Payment network that banks and merchants use
- **AWS**: Cloud infrastructure that apps build on
- **Twilio**: Communication API that apps integrate
- **Stripe**: Payment API that websites use

**The Mari App is just a reference implementation.**

**The real product is the Mari Protocol:**
- Banks integrate it into their apps
- Payment apps build on it
- Merchants accept it
- Developers extend it

**Revenue comes from:**
- Transaction fees (R0.10 per transaction)
- SDK licensing (enterprise tier)
- Value-added services (analytics, support)

**Success looks like:**
- Every banking app uses Mari for instant payments
- WhatsApp uses Mari for in-chat payments
- Shoprite uses Mari at checkout
- Uber uses Mari for ride payments
- Developers build niche apps on Mari

**Mari becomes the default payment protocol for Africa.**

**Just like HTTP became the default protocol for the web.**
