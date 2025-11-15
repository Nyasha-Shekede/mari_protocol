# Recipient Lookup and Confirmation Flow

## The Simple User Experience

**User only needs to know:**
- Recipient's phone number (or scan QR code)
- Amount to send

**User does NOT need to know:**
- Recipient's bank
- Recipient's account number
- Recipient's full name
- Recipient's address

---

## The Lookup Flow

### Step 1: User Enters Phone Number

```
User opens app (FNB, Mari, WhatsApp, etc.)
    ↓
User clicks "Send Money"
    ↓
User enters: +27821234567
    ↓
App sends to Mari API
```

**API Request:**
```http
POST /api/v1/users/lookup
{
  "phone_number": "+27821234567",
  "requester_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### Step 2: Mari Looks Up Recipient

**Mari Database Query:**
```sql
-- Hash the phone number
phone_hash = SHA256("+27821234567")

-- Look up user
SELECT 
    user_id,
    blood_hash,
    display_name,
    profile_picture_url,
    status
FROM users
WHERE phone_number_hash = phone_hash
AND status = 'active'
```

**Result:**
```json
{
  "user_id": "660e8400-e29b-41d4-a716-446655440001",
  "blood_hash": "demo_user_27821234567",
  "display_name": "John Doe",
  "profile_picture_url": "https://cdn.mari.protocol/avatars/660e8400.jpg",
  "status": "active",
  "verified": true
}
```

---

### Step 3: App Shows Recipient Details

**User sees confirmation screen:**
```
┌─────────────────────────────────────┐
│  Send Money                         │
├─────────────────────────────────────┤
│                                     │
│  To:                                │
│  ┌─────────────────────────────┐   │
│  │  [Photo]  John Doe          │   │
│  │           +27 82 123 4567   │   │
│  │           ✓ Verified        │   │
│  └─────────────────────────────┘   │
│                                     │
│  Amount:                            │
│  ┌─────────────────────────────┐   │
│  │  R 100.00                   │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Shake to Confirm           │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

**User confirms:**
- "Yes, this is the right person"
- Shakes phone
- Payment sent

---

## What Information is Returned?

### Public Profile Data (Safe to Share)

**✓ Mari returns:**
```json
{
  "user_id": "660e8400-e29b-41d4-a716-446655440001",
  "blood_hash": "demo_user_27821234567",
  "display_name": "John Doe",
  "profile_picture_url": "https://cdn.mari.protocol/avatars/660e8400.jpg",
  "phone_number_masked": "+27 82 *** **67",
  "verified": true,
  "member_since": "2024-01-15",
  "transaction_count": 150,
  "trust_score": 0.98
}
```

**✗ Mari does NOT return:**
- Bank account number
- Full phone number (only masked)
- Address
- ID number
- Balance
- Transaction history
- Exact location

---

### Privacy Controls

**Users can control what's visible:**

```json
// User privacy settings
{
  "display_name_visible": true,      // Show "John Doe" or just "J***"
  "profile_picture_visible": true,   // Show photo or generic avatar
  "phone_number_visible": "masked",  // "full", "masked", or "hidden"
  "transaction_count_visible": true, // Show activity level
  "trust_score_visible": true        // Show reputation
}
```

**Default settings:**
- Display name: Visible (helps prevent wrong recipient)
- Profile picture: Visible (visual confirmation)
- Phone number: Masked (privacy)
- Transaction count: Visible (trust signal)
- Trust score: Visible (fraud prevention)

---

## The Complete Flow

### Scenario: Alice Sends R100 to Bob

**Step 1: Alice enters Bob's phone number**
```
Alice's app → Mari API
POST /api/v1/users/lookup
{
  "phone_number": "+27829876543"
}
```

**Step 2: Mari looks up Bob**
```
Mari Database:
- Hash phone number: SHA256("+27829876543")
- Find user: Bob (user_id: 660e8400...)
- Return public profile
```

**Step 3: Alice sees Bob's profile**
```
┌─────────────────────────────────────┐
│  Send to:                           │
│  ┌─────────────────────────────┐   │
│  │  [Photo]  Bob Smith         │   │
│  │           +27 82 *** **43   │   │
│  │           ✓ Verified        │   │
│  │           Member since Jan  │   │
│  └─────────────────────────────┘   │
│                                     │
│  Is this the right person?          │
│  [Yes, Continue]  [No, Cancel]      │
└─────────────────────────────────────┘
```

**Step 4: Alice confirms and enters amount**
```
Amount: R100.00
[Continue]
```

**Step 5: Alice shakes phone (physics seal)**
```
Generating security seal...
[Shake animation]
```

**Step 6: Alice confirms payment**
```
┌─────────────────────────────────────┐
│  Confirm Payment                    │
├─────────────────────────────────────┤
│  To: Bob Smith                      │
│  Amount: R100.00                    │
│  Security: ✓ Verified               │
│                                     │
│  [Confirm & Send]                   │
└─────────────────────────────────────┘
```

**Step 7: Payment processed**
```
Alice's app → Mari API
POST /api/v1/transactions/authorize
{
  "sender_id": "550e8400...",
  "recipient_id": "660e8400...",
  "amount": 100.00,
  "physics_seal": {...}
}

Mari validates → Bank debits Alice → Bank credits Bob
```

**Step 8: Both users notified**
```
Alice sees: "✓ R100 sent to Bob Smith"
Bob sees: "✓ R100 received from Alice Johnson"
```

---

## Edge Cases

### Case 1: Recipient Not Found

**User enters: +27821111111**

```
Mari API Response:
{
  "found": false,
  "message": "This phone number is not registered with Mari"
}
```

**User sees:**
```
┌─────────────────────────────────────┐
│  Recipient Not Found                │
├─────────────────────────────────────┤
│  +27 82 111 1111 is not registered  │
│  with Mari.                         │
│                                     │
│  Options:                           │
│  • Invite them to join Mari         │
│  • Use different payment method     │
│  • Check the number and try again   │
│                                     │
│  [Send Invite]  [Cancel]            │
└─────────────────────────────────────┘
```

---

### Case 2: Multiple Users with Same Name

**User enters: +27821234567**

```
Mari API Response:
{
  "found": true,
  "user": {
    "display_name": "John Doe",
    "phone_number_masked": "+27 82 *** **67",
    "profile_picture_url": "...",
    "disambiguation": {
      "bank": "FNB",
      "member_since": "2024-01-15",
      "mutual_contacts": 3
    }
  }
}
```

**User sees:**
```
┌─────────────────────────────────────┐
│  Is this the right John Doe?        │
├─────────────────────────────────────┤
│  [Photo]  John Doe                  │
│           +27 82 *** **67           │
│           Banks with FNB            │
│           Member since Jan 2024     │
│           3 mutual contacts         │
│                                     │
│  [Yes, This is Correct]             │
│  [No, Wrong Person]                 │
└─────────────────────────────────────┘
```

---

### Case 3: Recipient Account Suspended

**User enters: +27829999999**

```
Mari API Response:
{
  "found": true,
  "user": {
    "display_name": "Suspended User",
    "status": "suspended"
  },
  "error": "RECIPIENT_SUSPENDED",
  "message": "This account is temporarily suspended"
}
```

**User sees:**
```
┌─────────────────────────────────────┐
│  Cannot Send Payment                │
├─────────────────────────────────────┤
│  This account is temporarily        │
│  suspended and cannot receive       │
│  payments.                          │
│                                     │
│  Please contact the recipient       │
│  directly or try again later.       │
│                                     │
│  [OK]                               │
└─────────────────────────────────────┘
```

---

## Security Considerations

### Preventing Enumeration Attacks

**Problem:**
- Attacker tries many phone numbers
- Discovers which numbers are registered
- Privacy violation

**Solution: Rate Limiting**
```
Rate limits per user:
- 10 lookups per minute
- 100 lookups per hour
- 1000 lookups per day

If exceeded:
- Temporary block (15 minutes)
- CAPTCHA challenge
- Account flagged for review
```

**Solution: Delayed Response**
```
If user not found:
- Add random delay (200-500ms)
- Makes enumeration slower
- Harder to automate
```

---

### Preventing Phishing

**Problem:**
- Attacker creates fake profile
- Uses similar name to real person
- Tricks user into sending money

**Solution: Verification Badges**
```
Verification levels:
✓ Phone verified (SMS)
✓✓ Bank verified (KYC)
✓✓✓ Business verified (registration docs)
```

**Solution: Mutual Contacts**
```
Show mutual contacts:
"You both know: Alice, Bob, Charlie"

Helps user confirm:
"Yes, this is the right John Doe"
```

**Solution: Transaction History**
```
Show previous transactions:
"You sent R50 to this person on Jan 15"

Helps user confirm:
"Yes, I've paid them before"
```

---

## API Specification

### Lookup Endpoint

```http
POST /api/v1/users/lookup
Authorization: Bearer <token>
Content-Type: application/json

{
  "phone_number": "+27821234567",
  "requester_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Success Response (200 OK):**
```json
{
  "found": true,
  "user": {
    "user_id": "660e8400-e29b-41d4-a716-446655440001",
    "blood_hash": "demo_user_27821234567",
    "display_name": "John Doe",
    "profile_picture_url": "https://cdn.mari.protocol/avatars/660e8400.jpg",
    "phone_number_masked": "+27 82 *** **67",
    "verified": true,
    "verification_level": "bank_verified",
    "member_since": "2024-01-15",
    "transaction_count": 150,
    "trust_score": 0.98,
    "mutual_contacts": 3,
    "previous_transactions": {
      "count": 2,
      "last_transaction_date": "2024-01-10",
      "total_amount": 250.00
    }
  }
}
```

**Not Found Response (404 Not Found):**
```json
{
  "found": false,
  "message": "This phone number is not registered with Mari",
  "invite_url": "https://mari.protocol/invite?ref=550e8400"
}
```

**Error Response (429 Too Many Requests):**
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many lookup requests. Please try again in 15 minutes.",
  "retry_after": 900
}
```

---

## Privacy-Preserving Lookup

### Alternative: Blind Lookup (Future Enhancement)

**Problem:**
- Current lookup reveals to Mari who's looking up whom
- Privacy concern: "Mari knows Alice is about to pay Bob"

**Solution: Cryptographic Blind Lookup**
```
1. Alice's app hashes phone number locally
2. Alice's app sends hash to Mari (not plaintext)
3. Mari returns encrypted profile
4. Alice's app decrypts profile locally
5. Mari never knows which phone number was looked up
```

**Implementation:**
```
Alice's app:
phone_hash = SHA256("+27821234567")
blind_token = HMAC(phone_hash, alice_secret_key)

POST /api/v1/users/blind-lookup
{
  "blind_token": "a3f5c8b2d9e1f4a7c6b8d2e5f9a1c3b7"
}

Mari returns:
{
  "encrypted_profile": "base64_encrypted_data",
  "nonce": "random_nonce"
}

Alice's app decrypts:
profile = decrypt(encrypted_profile, alice_secret_key, nonce)
```

**Benefit:**
- Mari can't see who's looking up whom
- Still prevents enumeration (rate limiting on blind tokens)
- Maximum privacy

---

## Bottom Line

**Yes, exactly right!**

**The flow is:**
1. User enters phone number (or scans QR)
2. App queries Mari database
3. Mari returns recipient's public profile (name, photo, verification status)
4. User confirms: "Yes, this is the right person"
5. User enters amount
6. User shakes phone (physics seal)
7. Payment sent

**User never needs to know:**
- Recipient's bank
- Recipient's account number
- Recipient's address

**Just phone number → Mari looks up → User confirms → Payment sent.**

**Simple, fast, secure.**

**This is how modern payments should work.**
