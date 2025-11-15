# Mari Data Storage and Privacy Model

## Core Principle: Minimal Data Storage

**Mari's Philosophy:**
- Store only what's needed for protocol operation
- Never store passwords or sensitive credentials
- Banks handle authentication
- Mari handles authorization (physics seals)

---

## What Mari DOES Store

### 1. User Registry (Minimal Identity)

```sql
-- users table
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    phone_number_hash VARCHAR(64) NOT NULL,  -- SHA-256 hash, not plaintext
    blood_hash VARCHAR(64) NOT NULL UNIQUE,  -- User's public identifier
    created_at TIMESTAMP NOT NULL,
    status ENUM('active', 'suspended', 'closed') DEFAULT 'active',
    
    -- NO passwords
    -- NO bank account numbers
    -- NO personal information
    -- NO balances (banks store this)
);

-- Example record
{
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "phone_number_hash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
    "blood_hash": "demo_user_1234567890abcdef",
    "created_at": "2024-01-15T10:30:00Z",
    "status": "active"
}
```

**Why hash phone numbers?**
- Privacy: Can't reverse-engineer phone number from hash
- Lookup: Can verify "does this phone number exist?" without storing it
- Compliance: POPIA (South African data protection) compliant

---

### 2. Device Registry (Security Metadata)

```sql
-- devices table
CREATE TABLE devices (
    device_id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(user_id),
    device_fingerprint VARCHAR(64) NOT NULL,  -- Hash of device identifiers
    public_key TEXT NOT NULL,                 -- For signature verification
    tee_attestation TEXT,                     -- Hardware attestation certificate
    registered_at TIMESTAMP NOT NULL,
    last_seen TIMESTAMP,
    status ENUM('active', 'revoked') DEFAULT 'active',
    
    -- NO device passwords
    -- NO biometric data
    -- NO personal files
);

-- Example record
{
    "device_id": "660e8400-e29b-41d4-a716-446655440001",
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "device_fingerprint": "a3f5c8b2d9e1f4a7c6b8d2e5f9a1c3b7",
    "public_key": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
    "tee_attestation": "-----BEGIN CERTIFICATE-----\nMIIDXTCCAkWgAwIBAgIJAKL...",
    "registered_at": "2024-01-15T10:30:00Z",
    "last_seen": "2024-01-20T15:45:00Z",
    "status": "active"
}
```

**What's stored:**
- Public key (for verifying signatures, not secret)
- Device fingerprint (hash, not identifiable)
- Attestation certificate (proves device is secure)

**What's NOT stored:**
- Private keys (stored on device only, in TEE/TPM)
- Biometric data (stays on device)
- Device passwords/PINs (never transmitted)

---

### 3. Transaction Logs (Audit Trail)

```sql
-- transactions table
CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY,
    sender_user_id UUID REFERENCES users(user_id),
    recipient_user_id UUID REFERENCES users(user_id),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'ZAR',
    
    -- Physics seal metadata (for fraud investigation)
    physics_seal_hash VARCHAR(64) NOT NULL,  -- Hash of seal, not full data
    location_hash VARCHAR(64),               -- Hash of location, not exact coords
    timestamp TIMESTAMP NOT NULL,
    
    -- Authorization metadata
    authorization_token TEXT NOT NULL,       -- HSM-signed token
    confidence_score DECIMAL(5,4),           -- 0.9999 = 99.99% confidence
    
    -- Settlement metadata
    settlement_status ENUM('pending', 'settled', 'failed') DEFAULT 'pending',
    settled_at TIMESTAMP,
    
    -- NO bank account numbers
    -- NO exact GPS coordinates (only hashed)
    -- NO full physics seal data (only hash)
);

-- Example record
{
    "transaction_id": "770e8400-e29b-41d4-a716-446655440002",
    "sender_user_id": "550e8400-e29b-41d4-a716-446655440000",
    "recipient_user_id": "550e8400-e29b-41d4-a716-446655440003",
    "amount": 100.00,
    "currency": "ZAR",
    "physics_seal_hash": "b4f6d9a2c8e1f5a3c7b9d3e6f0a2c4b8",
    "location_hash": "c5g7e0b3d9f2g6b4d8c0e7g1b3d5c9f2",
    "timestamp": "2024-01-20T15:45:30Z",
    "authorization_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "confidence_score": 0.9995,
    "settlement_status": "settled",
    "settled_at": "2024-01-20T23:59:59Z"
}
```

**Why hash physics seals and locations?**
- Privacy: Can't track user movements from database
- Verification: Can verify "was this seal used before?" without storing full data
- Compliance: Minimizes personal data storage

**Full physics seal data:**
- Stored temporarily in HSM (for validation)
- Deleted after 24 hours
- Only hash kept permanently

---

### 4. Bank Integration Metadata

```sql
-- bank_integrations table
CREATE TABLE bank_integrations (
    integration_id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(user_id),
    bank_id VARCHAR(50) NOT NULL,            -- 'fnb_za', 'capitec_za', etc.
    bank_user_id_hash VARCHAR(64) NOT NULL,  -- Hash of bank's user ID
    linked_at TIMESTAMP NOT NULL,
    status ENUM('active', 'revoked') DEFAULT 'active',
    
    -- NO bank account numbers
    -- NO bank passwords
    -- NO bank balances
);

-- Example record
{
    "integration_id": "880e8400-e29b-41d4-a716-446655440004",
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "bank_id": "fnb_za",
    "bank_user_id_hash": "d6h8f1c4e0g3h7c5e9d1f8h2c4e6d0h4",
    "linked_at": "2024-01-15T10:30:00Z",
    "status": "active"
}
```

**What this enables:**
- Mari knows: "User X has account at FNB"
- Mari doesn't know: Account number, balance, transaction history
- Bank provides: Real-time balance check via API (when needed)

---

## What Mari DOES NOT Store

### ❌ Never Stored by Mari

**1. Passwords**
- User passwords: Handled by banks (OAuth)
- Device PINs: Stored on device only
- Biometric data: Stays on device (never transmitted)

**2. Bank Account Details**
- Account numbers: Banks store this
- Balances: Banks store this
- Transaction history: Banks store this
- Credit scores: Banks store this

**3. Personal Information**
- ID numbers: Banks verify, Mari never sees
- Addresses: Banks verify, Mari never sees
- Names: Optional (only for display in Mari App)
- Email addresses: Optional (only for notifications)

**4. Sensitive Location Data**
- Exact GPS coordinates: Hashed immediately
- Location history: Not tracked
- Movement patterns: Not analyzed

**5. Full Physics Seal Data**
- Motion signatures: Hashed after validation
- Sensor readings: Deleted after 24 hours
- Device sensor data: Never stored long-term

---

## Authentication: Banks Handle It

### How Authentication Works

**Scenario 1: Bank-Integrated User**

```
User opens FNB app
    ↓
User logs in with FNB credentials
    ↓
FNB authenticates user (password, biometric, etc.)
    ↓
FNB generates OAuth token
    ↓
FNB app calls Mari API with OAuth token
    ↓
Mari validates token with FNB
    ↓
Mari authorizes transaction (physics seal)
    ↓
FNB processes payment
```

**Mari never sees:**
- User's FNB password
- User's biometric data
- User's account number

**Mari only sees:**
- OAuth token (temporary, expires in 1 hour)
- User's phone number hash
- Physics seal for transaction

---

**Scenario 2: Mari App User (Fallback)**

```
User opens Mari app
    ↓
User logs in with phone number
    ↓
SMS OTP sent to phone
    ↓
User enters OTP
    ↓
Mari validates OTP
    ↓
Mari generates session token (stored on device)
    ↓
User can transact
```

**Mari stores:**
- Phone number hash (not plaintext)
- Session token (temporary, expires in 30 days)

**Mari never stores:**
- SMS OTP (deleted after validation)
- User's device PIN
- User's biometric data

---

## Data Flow: Who Stores What

### Registration Flow

```
┌─────────────────────────────────────────────────┐
│              User's Phone                       │
│  - Device generates key pair                    │
│  - Private key stored in TEE (never leaves)     │
│  - Public key sent to Mari                      │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│              Bank (FNB)                         │
│  - User logs in with FNB credentials            │
│  - FNB authenticates (password, biometric)      │
│  - FNB verifies identity (KYC already done)     │
│  - FNB sends OAuth token to Mari                │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│              Mari Database                      │
│  Stores:                                        │
│  - Phone number hash                            │
│  - Blood hash (public identifier)               │
│  - Device public key                            │
│  - Bank integration metadata                    │
│                                                  │
│  Does NOT store:                                │
│  - FNB password                                 │
│  - Account number                               │
│  - Private key                                  │
│  - Personal information                         │
└─────────────────────────────────────────────────┘
```

---

### Transaction Flow

```
┌─────────────────────────────────────────────────┐
│              User's Phone                       │
│  - User shakes phone                            │
│  - Physics seal generated                       │
│  - Signed with private key (on device)          │
│  - Sent to Mari HSM                             │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│              Mari HSM Network                   │
│  - Validates physics seal                       │
│  - Checks device attestation                    │
│  - Generates authorization token                │
│  - Stores physics seal hash (not full data)     │
│  - Deletes full seal after 24 hours             │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│              Bank (FNB)                         │
│  - Receives authorization token                 │
│  - Validates Mari's signature                   │
│  - Checks user's balance                        │
│  - Debits account                               │
│  - Stores transaction in bank's ledger          │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│              Mari Database                      │
│  Stores:                                        │
│  - Transaction ID                               │
│  - Sender/recipient user IDs                    │
│  - Amount                                       │
│  - Physics seal hash                            │
│  - Authorization token                          │
│  - Settlement status                            │
│                                                  │
│  Does NOT store:                                │
│  - Full physics seal data                       │
│  - Exact GPS coordinates                        │
│  - Bank account numbers                         │
│  - User balances                                │
└─────────────────────────────────────────────────┘
```

---

## Data Retention Policy

### Short-Term Storage (24 Hours)

**Stored in HSM (Hardware Security Module):**
- Full physics seal data
- Exact GPS coordinates
- Raw sensor readings
- Device attestation details

**Purpose:**
- Fraud investigation (if flagged)
- Dispute resolution (if contested)
- Algorithm improvement (if anomaly detected)

**Deletion:**
- Automatically deleted after 24 hours
- Only hash kept permanently
- Cannot be recovered after deletion

---

### Medium-Term Storage (90 Days)

**Stored in Mari Database:**
- Transaction logs (with hashes)
- Authorization tokens
- Session tokens
- API access logs

**Purpose:**
- Regulatory compliance (AML/CFT)
- Fraud investigation
- Dispute resolution
- Performance monitoring

**Deletion:**
- Archived after 90 days
- Moved to cold storage
- Accessible only for legal/regulatory requests

---

### Long-Term Storage (7 Years)

**Stored in Encrypted Archive:**
- Transaction hashes (not full data)
- User registration records
- Bank integration metadata
- Audit logs

**Purpose:**
- Regulatory compliance (FICA requires 5-7 years)
- Legal disputes
- Tax audits
- Fraud pattern analysis

**Access:**
- Requires multi-party authorization
- Logged and audited
- Only for legal/regulatory purposes

---

## Privacy Guarantees

### What Mari Can See

**✓ Mari knows:**
- User X sent money to User Y
- Amount: R100
- Time: 2024-01-20 15:45:30
- Physics seal was valid (99.95% confidence)
- Transaction settled successfully

**✗ Mari doesn't know:**
- User X's bank account number
- User X's balance
- User X's exact location (only hash)
- User X's transaction history at bank
- User X's personal information

---

### What Banks Can See

**✓ Bank knows:**
- User X's account number
- User X's balance
- User X's transaction history
- User X's personal information (KYC)
- User X's exact location (if they choose to share)

**✗ Bank doesn't know:**
- User X's physics seal data (only hash)
- User X's device private key
- User X's transactions at other banks (unless shared)

---

### What Users Control

**Users can:**
- Revoke device access (delete device from Mari)
- Unlink bank integration (disconnect from FNB)
- Request data deletion (POPIA right to erasure)
- Export their data (POPIA right to portability)
- Opt out of analytics (privacy mode)

**Users cannot:**
- Delete transaction history (regulatory requirement)
- Hide from fraud detection (security requirement)
- Bypass physics seal (protocol requirement)

---

## Compliance & Regulations

### POPIA (Protection of Personal Information Act)

**Mari's compliance:**
- ✓ Minimal data collection (only what's needed)
- ✓ Purpose limitation (only for payment authorization)
- ✓ Data minimization (hashing, not storing plaintext)
- ✓ Storage limitation (24 hours for sensitive data)
- ✓ User rights (access, deletion, portability)
- ✓ Security safeguards (encryption, HSM, audit logs)

---

### FICA (Financial Intelligence Centre Act)

**Mari's compliance:**
- ✓ Record keeping (7 years for transactions)
- ✓ Suspicious transaction reporting (automated flagging)
- ✓ Customer due diligence (via bank integration)
- ✓ Audit trail (immutable logs in HSM)

---

### GDPR (If Operating in EU)

**Mari's compliance:**
- ✓ Data minimization (hashing, not storing)
- ✓ Right to erasure (user can delete account)
- ✓ Right to portability (user can export data)
- ✓ Consent management (explicit opt-in)
- ✓ Data breach notification (within 72 hours)

---

## Security Measures

### Encryption at Rest

```
All data in Mari database is encrypted:
- AES-256 encryption
- Keys stored in HSM (not on database server)
- Key rotation every 90 days
- Separate keys for different data types
```

### Encryption in Transit

```
All API communications:
- TLS 1.3 (latest standard)
- Certificate pinning (prevent MITM)
- Mutual authentication (bank ↔ Mari)
```

### Access Controls

```
Database access:
- Role-based access control (RBAC)
- Multi-factor authentication required
- All access logged and audited
- No direct production access (only via API)
```

### Audit Logging

```
All operations logged:
- Who accessed what data
- When it was accessed
- Why it was accessed (purpose)
- What was changed
- Logs immutable (cannot be deleted/modified)
- Logs stored in separate system (not main database)
```

---

## Bottom Line

**Mari stores MINIMAL data:**
- Phone number hash (not plaintext)
- Blood hash (public identifier)
- Device public key (not private key)
- Transaction metadata (not full details)
- Physics seal hash (not full seal)

**Banks handle the complicated stuff:**
- User authentication (passwords, biometrics)
- Account management (balances, history)
- KYC/AML (identity verification)
- Regulatory compliance (reporting)

**Users control their data:**
- Can revoke access anytime
- Can request deletion (POPIA right)
- Can export data (portability)
- Can opt out of analytics

**Privacy by design:**
- Hash everything possible
- Delete sensitive data after 24 hours
- Store only what's legally required
- Encrypt everything at rest and in transit

**Mari is a protocol, not a data warehouse.**
**Banks are the data custodians.**
**Mari just validates physics seals and routes transactions.**
