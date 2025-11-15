# Mari Protocol: Complete Technical Context for Nyasha Shekede
**Founder & Chief Technology Officer**

---

## Your Technical Domain: Everything

This document contains the complete technical architecture, implementation details, and system design for Mari Protocol. As the technical founder, you own all technical decisions and implementations.

---

## Table of Contents

1. System Architecture Overview
2. Cryptographic Primitives & Implementation
3. Physics Seal: Deep Technical Dive
4. Mobile App Architecture (Kotlin/Jetpack Compose)
5. Backend Server Architecture (Node.js/Express)
6. HSM Network & Key Management
7. Protocol Specification
8. Security Model & Threat Analysis
9. Database Schema & Data Flow
10. API Design & Integration Points
11. Offline Mode Implementation
12. Performance & Scalability
13. Testing Strategy
14. Deployment & DevOps

---

## 1. System Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Mobile App (Kotlin)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ UI Layer     │  │ ViewModel    │  │ Repository   │      │
│  │ (Compose)    │→ │ (State Mgmt) │→ │ (Data Layer) │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                  ↓                  ↓              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Core Modules (Your Implementation)           │  │
│  │  • DeviceKeyManager (Ed25519 keys)                   │  │
│  │  • MariCryptoManager (Encryption/Signing)            │  │
│  │  • PhysicsSensorManager (Motion/Location)            │  │
│  │  • MariProtocol (Transaction logic)                  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓ HTTPS/TLS 1.3
┌─────────────────────────────────────────────────────────────┐
│                  Mari Server (Node.js/Express)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ API Routes   │→ │ Controllers  │→ │ Services     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                  ↓                  ↓              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Core Services                           │  │
│  │  • Physics Seal Validator                            │  │
│  │  • Transaction Processor                             │  │
│  │  • HSM Client (PKCS#11)                             │  │
│  │  • Bank Integration Layer                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    HSM Network (Thales)                      │
│  • Master key storage (AES-256)                             │
│  • Transaction signing (Ed25519)                            │
│  • FIPS 140-2 Level 3 compliance                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  PostgreSQL Database                         │
│  • Users, Transactions, Physics Seals                       │
│  • Encrypted at rest (AES-256-GCM)                         │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack (Your Choices)

**Mobile (Android):**
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM + Clean Architecture
- DI: Hilt
- Networking: Retrofit + OkHttp
- Crypto: Tink + BouncyCastle
- Storage: Room + EncryptedSharedPreferences

**Backend:**
- Runtime: Node.js 20 LTS
- Framework: Express.js
- Language: TypeScript
- Database: PostgreSQL 15
- ORM: Prisma
- Crypto: Node crypto + libsodium
- HSM: PKCS#11 interface

**Infrastructure:**
- Cloud: AWS (multi-region)
- HSM: Thales Luna Network HSM
- CDN: CloudFront
- Monitoring: Prometheus + Grafana
- Logging: ELK Stack

---

## 2. Cryptographic Primitives & Implementation

### Key Types and Usage

**1. Device Keys (Ed25519 - Your Implementation)**

```kotlin
// DeviceKeyManager.kt - Your implementation
class DeviceKeyManager(private val context: Context) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore")
    
    fun generateDeviceKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )
        
        val parameterSpec = KeyGenParameterSpec.Builder(
            "mari_device_key",
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .setInvalidatedByBiometricEnrollment(false)
            .build()
        
        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }
    
    fun signData(data: ByteArray): ByteArray {
        val privateKey = keyStore.getKey("mari_device_key", null) as PrivateKey
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }
}
```

**2. Blood Hash (SHA-256 - Public Identifier)**

```kotlin
// Generate blood hash from phone number
fun generateBloodHash(phoneNumber: String): String {
    val normalized = phoneNumber.replace(Regex("[^0-9]"), "")
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(normalized.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
}
```

**3. Transaction Encryption (AES-256-GCM)**

```kotlin
class MariCryptoManager {
    fun encryptTransaction(
        plaintext: ByteArray,
        recipientPublicKey: PublicKey
    ): EncryptedData {
        // ECDH key agreement
        val sharedSecret = performECDH(devicePrivateKey, recipientPublicKey)
        
        // Derive AES key using HKDF
        val aesKey = HKDF.deriveKey(sharedSecret, "mari-transaction", 32)
        
        // Encrypt with AES-256-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = SecureRandom().generateSeed(12)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, iv))
        
        val ciphertext = cipher.doFinal(plaintext)
        
        return EncryptedData(ciphertext, iv, cipher.iv)
    }
}
```


**4. Physics Seal Signing (Ed25519)**

```kotlin
// Server-side HSM signing
async function signPhysicsSeal(sealData: PhysicsSealData): Promise<Signature> {
    const hsmClient = await getHSMClient()
    
    // Serialize seal data
    const serialized = JSON.stringify({
        motion_signature: sealData.motionSignature,
        location: sealData.location,
        timestamp: sealData.timestamp,
        device_id: sealData.deviceId
    })
    
    // Sign with HSM (Ed25519)
    const signature = await hsmClient.sign({
        keyLabel: 'mari-master-signing-key',
        algorithm: 'Ed25519',
        data: Buffer.from(serialized)
    })
    
    return signature
}
```

---

## 3. Physics Seal: Deep Technical Dive

### Sensor Data Collection (Your Implementation)

```kotlin
class PhysicsSensorManager(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    
    private val sensorData = mutableListOf<SensorReading>()
    
    fun startCapture() {
        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_FASTEST // ~200Hz
        )
        sensorManager.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_FASTEST)
    }
    
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val reading = SensorReading(
                type = event.sensor.type,
                values = event.values.clone(),
                timestamp = event.timestamp,
                accuracy = event.accuracy
            )
            sensorData.add(reading)
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }
    
    fun generateMotionSignature(): MotionSignature {
        // Stop capture after 2 seconds
        stopCapture()
        
        // Extract features from sensor data
        val features = extractFeatures(sensorData)
        
        return MotionSignature(
            accelerometerPattern = features.accelPattern,
            gyroscopePattern = features.gyroPattern,
            magnetometerPattern = features.magPattern,
            duration = features.duration,
            intensity = features.intensity,
            uniquenessScore = calculateUniqueness(features)
        )
    }
}
```


### Feature Extraction Algorithm

```kotlin
data class MotionFeatures(
    val accelPattern: FloatArray,
    val gyroPattern: FloatArray,
    val magPattern: FloatArray,
    val duration: Long,
    val intensity: Float,
    val frequency: Float
)

fun extractFeatures(sensorData: List<SensorReading>): MotionFeatures {
    val accelData = sensorData.filter { it.type == Sensor.TYPE_ACCELEROMETER }
    val gyroData = sensorData.filter { it.type == Sensor.TYPE_GYROSCOPE }
    val magData = sensorData.filter { it.type == Sensor.TYPE_MAGNETIC_FIELD }
    
    // Calculate magnitude for each sensor
    val accelMagnitudes = accelData.map { 
        sqrt(it.values[0].pow(2) + it.values[1].pow(2) + it.values[2].pow(2))
    }
    
    // FFT for frequency analysis
    val fft = FFT(accelMagnitudes.size)
    val frequencyDomain = fft.forward(accelMagnitudes.toFloatArray())
    
    // Statistical features
    val mean = accelMagnitudes.average()
    val stdDev = calculateStdDev(accelMagnitudes, mean)
    val peakFrequency = findDominantFrequency(frequencyDomain)
    
    return MotionFeatures(
        accelPattern = accelMagnitudes.toFloatArray(),
        gyroPattern = extractPattern(gyroData),
        magPattern = extractPattern(magData),
        duration = accelData.last().timestamp - accelData.first().timestamp,
        intensity = mean.toFloat(),
        frequency = peakFrequency
    )
}
```

### Location Capture (GPS + Network)

```kotlin
class LocationManager(context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    suspend fun getCurrentLocation(): LocationData = suspendCoroutine { continuation ->
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            continuation.resume(LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                altitude = location.altitude,
                timestamp = location.time,
                provider = location.provider
            ))
        }
    }
}
```


### Physics Seal Structure

```typescript
interface PhysicsSeal {
    // Motion data
    motion_signature: {
        accelerometer_pattern: number[]
        gyroscope_pattern: number[]
        magnetometer_pattern: number[]
        duration_ms: number
        intensity: number
        frequency_hz: number
        uniqueness_score: number
    }
    
    // Location data
    location: {
        latitude: number
        longitude: number
        accuracy_meters: number
        altitude_meters: number
        provider: 'gps' | 'network' | 'fused'
    }
    
    // Temporal data
    timestamp: number // Unix timestamp (ms)
    timezone_offset: number
    
    // Device attestation
    device_id: string // Hashed device identifier
    device_model: string
    os_version: string
    app_version: string
    
    // Cryptographic binding
    transaction_hash: string // SHA-256 of transaction data
    device_signature: string // Signed by device key
    
    // HSM signature (added by server)
    hsm_signature?: string // Ed25519 signature from HSM
}
```

### Validation Algorithm (Server-Side)

```typescript
async function validatePhysicsSeal(
    seal: PhysicsSeal,
    transaction: Transaction
): Promise<ValidationResult> {
    
    // 1. Verify device signature
    const deviceSignatureValid = await verifyDeviceSignature(
        seal.device_signature,
        seal.device_id,
        seal.transaction_hash
    )
    if (!deviceSignatureValid) {
        return { valid: false, reason: 'Invalid device signature' }
    }
    
    // 2. Verify transaction hash matches
    const computedHash = sha256(JSON.stringify(transaction))
    if (computedHash !== seal.transaction_hash) {
        return { valid: false, reason: 'Transaction hash mismatch' }
    }
    
    // 3. Check timestamp freshness (within 5 minutes)
    const now = Date.now()
    if (Math.abs(now - seal.timestamp) > 5 * 60 * 1000) {
        return { valid: false, reason: 'Seal expired' }
    }
    
    // 4. Validate motion signature uniqueness
    const motionValid = await validateMotionSignature(seal.motion_signature)
    if (!motionValid) {
        return { valid: false, reason: 'Invalid motion signature' }
    }
    
    // 5. Check location consistency with user history
    const locationValid = await validateLocation(seal.location, transaction.sender_id)
    if (!locationValid) {
        return { valid: false, reason: 'Suspicious location' }
    }
    
    // 6. Sign with HSM
    seal.hsm_signature = await signWithHSM(seal)
    
    return { valid: true, seal }
}
```


---

## 4. Mobile App Architecture (Kotlin/Jetpack Compose)

### Project Structure

```
mari-app/
├── app/
│   └── src/main/java/com/mari/mobile/
│       ├── MariAppRoot.kt              # Application entry point
│       ├── ui/
│       │   ├── auth/
│       │   │   └── AuthScreen.kt       # Phone number registration
│       │   ├── main/
│       │   │   └── MainScreen.kt       # Home screen
│       │   ├── send/
│       │   │   └── SendFlowScreen.kt   # Send money flow
│       │   ├── receive/
│       │   │   └── ReceiveScreen.kt    # Receive money (QR)
│       │   ├── qr/
│       │   │   ├── QRCodeGenerator.kt  # Generate QR codes
│       │   │   └── QRScanner.kt        # Scan QR codes
│       │   └── viewmodel/
│       │       ├── MainViewModel.kt
│       │       └── SendViewModel.kt
│       └── data/
│           └── repository/
│               └── UserRepository.kt
├── core/
│   ├── crypto/
│   │   ├── DeviceKeyManager.kt         # Ed25519 key management
│   │   └── MariCryptoManager.kt        # Encryption/signing
│   ├── physics/
│   │   └── PhysicsSensorManager.kt     # Motion/location capture
│   └── protocol/
│       └── MariProtocol.kt             # Transaction protocol
└── build.gradle.kts
```

### MVVM Architecture Pattern

```kotlin
// ViewModel (State Management)
@HiltViewModel
class SendViewModel @Inject constructor(
    private val mariProtocol: MariProtocol,
    private val physicsSensorManager: PhysicsSensorManager,
    private val repository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()
    
    fun initiateTransaction(recipient: String, amount: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 1. Capture physics seal
                val physicsSeal = physicsSensorManager.capturePhysicsSeal()
                
                // 2. Create transaction
                val transaction = mariProtocol.createTransaction(
                    recipient = recipient,
                    amount = amount,
                    physicsSeal = physicsSeal
                )
                
                // 3. Submit to server
                val result = repository.submitTransaction(transaction)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        success = true,
                        transactionId = result.id
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
}
```


### Jetpack Compose UI (Your Implementation)

```kotlin
@Composable
fun SendFlowScreen(
    viewModel: SendViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Recipient input
        OutlinedTextField(
            value = uiState.recipient,
            onValueChange = { viewModel.updateRecipient(it) },
            label = { Text("Recipient Blood Hash") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Amount input
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = { viewModel.updateAmount(it) },
            label = { Text("Amount (ZAR)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Shake to authorize button
        Button(
            onClick = { viewModel.initiateTransaction() },
            enabled = !uiState.isLoading && uiState.isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text("Shake to Authorize")
            }
        }
        
        // Success/Error states
        when {
            uiState.success -> {
                SuccessDialog(
                    transactionId = uiState.transactionId,
                    onDismiss = onNavigateBack
                )
            }
            uiState.error != null -> {
                ErrorDialog(
                    message = uiState.error!!,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }
}
```


---

## 5. Backend Server Architecture (Node.js/Express)

### Server Structure

```
mari-server/
├── src/
│   ├── index.ts                    # Server entry point
│   ├── routes/
│   │   ├── auth.routes.ts          # Registration/login
│   │   ├── transaction.routes.ts   # Transaction endpoints
│   │   └── user.routes.ts          # User management
│   ├── controllers/
│   │   ├── AuthController.ts
│   │   ├── TransactionController.ts
│   │   └── UserController.ts
│   ├── services/
│   │   ├── PhysicsSealValidator.ts # Validate physics seals
│   │   ├── TransactionProcessor.ts # Process transactions
│   │   ├── HSMClient.ts            # HSM integration
│   │   └── BankIntegration.ts      # Bank API calls
│   ├── models/
│   │   ├── User.ts
│   │   ├── Transaction.ts
│   │   └── PhysicsSeal.ts
│   └── middleware/
│       ├── auth.middleware.ts      # JWT verification
│       └── rateLimit.middleware.ts # Rate limiting
├── prisma/
│   └── schema.prisma               # Database schema
└── package.json
```

### API Endpoints (Your Design)

```typescript
// Transaction Routes
router.post('/api/v1/transactions/initiate', 
    authenticate,
    rateLimit({ max: 10, window: '1m' }),
    TransactionController.initiate
)

router.post('/api/v1/transactions/authorize',
    authenticate,
    TransactionController.authorize
)

router.get('/api/v1/transactions/:id',
    authenticate,
    TransactionController.getById
)

router.get('/api/v1/transactions/history',
    authenticate,
    TransactionController.getHistory
)
```

### Transaction Controller Implementation

```typescript
class TransactionController {
    static async initiate(req: Request, res: Response) {
        try {
            const { recipient_blood_hash, amount, currency } = req.body
            const sender_id = req.user.id
            
            // Validate inputs
            if (!recipient_blood_hash || !amount) {
                return res.status(400).json({ error: 'Missing required fields' })
            }
            
            // Check recipient exists
            const recipient = await prisma.user.findUnique({
                where: { blood_hash: recipient_blood_hash }
            })
            
            if (!recipient) {
                return res.status(404).json({ error: 'Recipient not found' })
            }
            
            // Create pending transaction
            const transaction = await prisma.transaction.create({
                data: {
                    sender_id,
                    recipient_id: recipient.id,
                    amount: new Decimal(amount),
                    currency: currency || 'ZAR',
                    status: 'pending_authorization'
                }
            })
            
            res.json({
                transaction_id: transaction.id,
                status: 'pending_authorization',
                expires_at: Date.now() + 5 * 60 * 1000 // 5 minutes
            })
            
        } catch (error) {
            console.error('Transaction initiation error:', error)
            res.status(500).json({ error: 'Internal server error' })
        }
    }
}
```


### Transaction Authorization with Physics Seal

```typescript
class TransactionController {
    static async authorize(req: Request, res: Response) {
        try {
            const { transaction_id, physics_seal } = req.body
            const user_id = req.user.id
            
            // Get transaction
            const transaction = await prisma.transaction.findUnique({
                where: { id: transaction_id }
            })
            
            if (!transaction || transaction.sender_id !== user_id) {
                return res.status(404).json({ error: 'Transaction not found' })
            }
            
            if (transaction.status !== 'pending_authorization') {
                return res.status(400).json({ error: 'Transaction already processed' })
            }
            
            // Validate physics seal
            const sealValidation = await PhysicsSealValidator.validate(
                physics_seal,
                transaction
            )
            
            if (!sealValidation.valid) {
                await prisma.transaction.update({
                    where: { id: transaction_id },
                    data: { status: 'rejected', rejection_reason: sealValidation.reason }
                })
                return res.status(400).json({ error: sealValidation.reason })
            }
            
            // Store physics seal
            await prisma.physicsSeal.create({
                data: {
                    transaction_id,
                    motion_signature: physics_seal.motion_signature,
                    location: physics_seal.location,
                    timestamp: physics_seal.timestamp,
                    device_id: physics_seal.device_id,
                    hsm_signature: sealValidation.hsm_signature
                }
            })
            
            // Process transaction
            const result = await TransactionProcessor.process(transaction)
            
            // Update transaction status
            await prisma.transaction.update({
                where: { id: transaction_id },
                data: {
                    status: 'completed',
                    completed_at: new Date(),
                    settlement_id: result.settlement_id
                }
            })
            
            res.json({
                transaction_id,
                status: 'completed',
                settlement_id: result.settlement_id
            })
            
        } catch (error) {
            console.error('Transaction authorization error:', error)
            res.status(500).json({ error: 'Internal server error' })
        }
    }
}
```


```kotlin
// DeviceKeyManager.kt - Your implementation
class DeviceKeyManager(context: Context) {
    private val keyStore = AndroidKeyStore.getInstance()
    
    fun generateDeviceKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )
        
        val parameterSpec = KeyGenParameterSpec.Builder(
            "mari_device_key",
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(30)
            .build()
            
        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }
    
    fun signData(data: ByteArray): ByteArray {
        val privateKey = keyStore.getKey("mari_device_key", null) as PrivateKey
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }
}
```

**2. Blood Hash (SHA-256 - Public Identifier)**
```kotlin
fun generateBloodHash(phoneNumber: String, deviceId: String): String {
    val input = "$phoneNumber:$deviceId:${System.currentTimeMillis()}"
    return MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(16) // First 16 chars for user-friendly display
}
```

**3. Transaction Encryption (AES-256-GCM)**

```kotlin
// MariCryptoManager.kt - Your implementation
class MariCryptoManager {
    fun encryptTransaction(
        plaintext: ByteArray,
        recipientPublicKey: PublicKey
    ): EncryptedPayload {
        // Generate ephemeral key pair for ECDH
        val ephemeralKeyPair = generateEphemeralKeyPair()
        
        // Perform ECDH to derive shared secret
        val sharedSecret = performECDH(
            ephemeralKeyPair.private,
            recipientPublicKey
        )
        
        // Derive AES key using HKDF
        val aesKey = HKDF.deriveKey(sharedSecret, "mari-transaction", 32)
        
        // Encrypt with AES-256-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, generateNonce())
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), gcmSpec)
        
        val ciphertext = cipher.doFinal(plaintext)
        
        return EncryptedPayload(
            ciphertext = ciphertext,
            nonce = gcmSpec.iv,
            ephemeralPublicKey = ephemeralKeyPair.public.encoded,
            tag = cipher.iv // GCM auth tag
        )
    }
}
```

**4. HSM Master Keys (Thales Luna)**
- Root key: AES-256 (never leaves HSM)
- Transaction signing: Ed25519 (HSM-backed)
- Key derivation: HKDF-SHA256

---

## 3. Physics Seal: Deep Technical Dive

### Sensor Data Collection (Your Implementation)

```kotlin
// PhysicsSensorManager.kt - Your implementation
class PhysicsSensorManager(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    
    data class SensorReading(
        val timestamp: Long,
        val accelX: Float, val accelY: Float, val accelZ: Float,
        val gyroX: Float, val gyroY: Float, val gyroZ: Float,
        val magX: Float, val magY: Float, val magZ: Float
    )
    
    suspend fun capturePhysicsSeal(durationMs: Long = 2000): PhysicsSeal {
        val readings = mutableListOf<SensorReading>()
        val startTime = System.currentTimeMillis()
        
        // Collect sensor data at 100Hz for 2 seconds
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (System.currentTimeMillis() - startTime > durationMs) return
                
                readings.add(SensorReading(
                    timestamp = event.timestamp,
                    accelX = event.values[0],
                    accelY = event.values[1],
                    accelZ = event.values[2],
                    // ... gyro and mag data
                ))
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        
        sensorManager.registerListener(listener, accelerometer, 10_000) // 100Hz
        delay(durationMs)
        sensorManager.unregisterListener(listener)
        
        return processReadings(readings)
    }
}
```

### Physics Seal Processing Algorithm

```kotlin
fun processReadings(readings: List<SensorReading>): PhysicsSeal {
    // 1. Calculate motion signature (FFT of acceleration)
    val accelMagnitudes = readings.map { 
        sqrt(it.accelX.pow(2) + it.accelY.pow(2) + it.accelZ.pow(2))
    }
    val fft = FastFourierTransform.compute(accelMagnitudes)
    val motionSignature = fft.take(32).map { it.toFloat() }
    
    // 2. Calculate rotation signature (gyroscope integration)
    val rotationSignature = integrateGyroscope(readings)
    
    // 3. Get GPS location
    val location = getCurrentLocation()
    
    // 4. Get device attestation
    val attestation = getDeviceAttestation()
    
    // 5. Create physics seal
    return PhysicsSeal(
        motionSignature = motionSignature,
        rotationSignature = rotationSignature,
        location = location,
        timestamp = System.currentTimeMillis(),
        deviceAttestation = attestation,
        sensorAccuracy = calculateAccuracy(readings)
    )
}

fun createPhysicsSealHash(seal: PhysicsSeal): ByteArray {
    val data = seal.toByteArray()
    return MessageDigest.getInstance("SHA-256").digest(data)
}
```

### Server-Side Validation (Node.js)

```typescript
// physics-validator.ts - Your backend implementation
export class PhysicsSealValidator {
    async validateSeal(seal: PhysicsSeal, transaction: Transaction): Promise<ValidationResult> {
        // 1. Verify motion signature is physically plausible
        const motionValid = this.validateMotionSignature(seal.motionSignature);
        if (!motionValid) {
            return { valid: false, reason: 'Implausible motion pattern' };
        }
        
        // 2. Verify location consistency
        const locationValid = await this.validateLocation(
            seal.location,
            transaction.senderId
        );
        if (!locationValid) {
            return { valid: false, reason: 'Location anomaly detected' };
        }
        
        // 3. Verify timestamp freshness (within 30 seconds)
        const now = Date.now();
        if (Math.abs(now - seal.timestamp) > 30000) {
            return { valid: false, reason: 'Seal expired or future-dated' };
        }
        
        // 4. Verify device attestation
        const attestationValid = await this.verifyDeviceAttestation(seal.deviceAttestation);
        if (!attestationValid) {
            return { valid: false, reason: 'Device attestation failed' };
        }
        
        // 5. Check for replay attacks (seal hash must be unique)
        const sealHash = this.computeSealHash(seal);
        const isDuplicate = await this.checkSealHashExists(sealHash);
        if (isDuplicate) {
            return { valid: false, reason: 'Replay attack detected' };
        }
        
        // Store seal hash to prevent replay
        await this.storeSealHash(sealHash, transaction.id);
        
        return { valid: true, confidence: 0.98 };
    }
}
```

---

## 4. Mobile App Architecture (Kotlin/Jetpack Compose)

### Project Structure

```
mari-app/
├── app/
│   └── src/main/java/com/mari/mobile/
│       ├── MariAppRoot.kt              # Application entry point
│       ├── ui/
│       │   ├── auth/
│       │   │   └── AuthScreen.kt       # Phone number auth
│       │   ├── main/
│       │   │   └── MainScreen.kt       # Home screen
│       │   ├── send/
│       │   │   └── SendFlowScreen.kt   # Payment flow
│       │   ├── receive/
│       │   │   └── ReceiveScreen.kt    # QR code display
│       │   ├── qr/
│       │   │   ├── QRScanner.kt        # Camera scanning
│       │   │   └── QRCodeGenerator.kt  # QR generation
│       │   └── viewmodel/
│       │       ├── MainViewModel.kt
│       │       └── SendViewModel.kt
│       └── data/
│           └── repository/
│               └── UserRepository.kt
├── core/
│   ├── crypto/
│   │   ├── DeviceKeyManager.kt        # Ed25519 keys
│   │   └── MariCryptoManager.kt       # Encryption
│   ├── physics/
│   │   └── PhysicsSensorManager.kt    # Motion capture
│   └── protocol/
│       └── MariProtocol.kt            # Transaction logic
└── build.gradle.kts
```

### MVVM Architecture Pattern

```kotlin
// SendViewModel.kt - Your MVVM implementation
@HiltViewModel
class SendViewModel @Inject constructor(
    private val mariProtocol: MariProtocol,
    private val physicsSensorManager: PhysicsSensorManager,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SendUiState>(SendUiState.Idle)
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()
    
    fun initiatePayment(recipientBloodHash: String, amount: Double) {
        viewModelScope.launch {
            _uiState.value = SendUiState.CapturingPhysicsSeal
            
            try {
                // 1. Capture physics seal (user shakes phone)
                val physicsSeal = physicsSensorManager.capturePhysicsSeal()
                
                // 2. Create transaction
                _uiState.value = SendUiState.CreatingTransaction
                val transaction = mariProtocol.createTransaction(
                    recipientBloodHash = recipientBloodHash,
                    amount = amount,
                    physicsSeal = physicsSeal
                )
                
                // 3. Submit to server
                _uiState.value = SendUiState.Submitting
                val result = mariProtocol.submitTransaction(transaction)
                
                // 4. Handle result
                _uiState.value = when (result) {
                    is TransactionResult.Success -> SendUiState.Success(result.txId)
                    is TransactionResult.Failure -> SendUiState.Error(result.message)
                }
            } catch (e: Exception) {
                _uiState.value = SendUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

---

## 5. Backend Server Architecture (Node.js/Express)

### Server Project Structure

```
mari-server/
├── src/
│   ├── routes/
│   │   ├── auth.routes.ts          # Phone auth endpoints
│   │   ├── transaction.routes.ts   # Payment endpoints
│   │   └── user.routes.ts          # User management
│   ├── controllers/
│   │   ├── auth.controller.ts
│   │   ├── transaction.controller.ts
│   │   └── user.controller.ts
│   ├── services/
│   │   ├── physics-validator.ts    # Physics seal validation
│   │   ├── transaction-processor.ts # Transaction logic
│   │   ├── hsm-client.ts           # HSM integration
│   │   └── bank-integration.ts     # Bank API calls
│   ├── models/
│   │   ├── user.model.ts
│   │   ├── transaction.model.ts
│   │   └── physics-seal.model.ts
│   ├── middleware/
│   │   ├── auth.middleware.ts      # JWT validation
│   │   ├── rate-limit.middleware.ts
│   │   └── error-handler.ts
│   └── utils/
│       ├── crypto.utils.ts
│       └── logger.ts
├── prisma/
│   └── schema.prisma               # Database schema
└── package.json
```

### API Endpoints (Your Design)

```typescript
// transaction.routes.ts - Your API design
import express from 'express';
import { authMiddleware } from '../middleware/auth.middleware';
import { TransactionController } from '../controllers/transaction.controller';

const router = express.Router();
const controller = new TransactionController();

// POST /api/v1/transactions - Submit new transaction
router.post('/', authMiddleware, async (req, res) => {
    /*
    Request body:
    {
        "recipientBloodHash": "a1b2c3d4e5f6g7h8",
        "amount": 100.00,
        "currency": "ZAR",
        "physicsSeal": {
            "motionSignature": [0.1, 0.2, ...],
            "location": {"lat": -26.2041, "lng": 28.0473},
            "timestamp": 1699564800000,
            "deviceAttestation": "..."
        },
        "encryptedPayload": "base64_encrypted_data",
        "signature": "ed25519_signature"
    }
    */
    
    const result = await controller.processTransaction(req.body, req.user);
    res.json(result);
});

// GET /api/v1/transactions/:id - Get transaction status
router.get('/:id', authMiddleware, async (req, res) => {
    const transaction = await controller.getTransaction(req.params.id, req.user);
    res.json(transaction);
});

// GET /api/v1/transactions - List user transactions
router.get('/', authMiddleware, async (req, res) => {
    const transactions = await controller.listTransactions(req.user, req.query);
    res.json(transactions);
});

export default router;
```

### Transaction Processing Flow

```typescript
// transaction-processor.ts - Your core business logic
export class TransactionProcessor {
    constructor(
        private physicsValidator: PhysicsSealValidator,
        private hsmClient: HSMClient,
        private bankIntegration: BankIntegration,
        private db: PrismaClient
    ) {}
    
    async processTransaction(txData: TransactionRequest): Promise<TransactionResult> {
        // 1. Validate physics seal
        const sealValidation = await this.physicsValidator.validateSeal(
            txData.physicsSeal,
            txData
        );
        if (!sealValidation.valid) {
            throw new Error(`Physics seal invalid: ${sealValidation.reason}`);
        }
        
        // 2. Verify cryptographic signature
        const signatureValid = await this.verifySignature(
            txData.encryptedPayload,
            txData.signature,
            txData.senderPublicKey
        );
        if (!signatureValid) {
            throw new Error('Invalid transaction signature');
        }
        
        // 3. Decrypt transaction payload
        const decryptedPayload = await this.hsmClient.decrypt(txData.encryptedPayload);
        
        // 4. Validate transaction data
        await this.validateTransaction(decryptedPayload);
        
        // 5. Check sender balance (via bank API)
        const balance = await this.bankIntegration.getBalance(txData.senderId);
        if (balance < decryptedPayload.amount) {
            throw new Error('Insufficient funds');
        }
        
        // 6. Create transaction record
        const transaction = await this.db.transaction.create({
            data: {
                id: generateTxId(),
                senderId: txData.senderId,
                recipientId: decryptedPayload.recipientId,
                amount: decryptedPayload.amount,
                currency: 'ZAR',
                physicsSealHash: computeHash(txData.physicsSeal),
                status: 'PENDING',
                createdAt: new Date()
            }
        });
        
        // 7. Submit to bank for settlement
        const bankResult = await this.bankIntegration.submitPayment({
            transactionId: transaction.id,
            from: txData.senderId,
            to: decryptedPayload.recipientId,
            amount: decryptedPayload.amount
        });
        
        // 8. Update transaction status
        await this.db.transaction.update({
            where: { id: transaction.id },
            data: { 
                status: bankResult.success ? 'COMPLETED' : 'FAILED',
                bankReference: bankResult.reference
            }
        });
        
        return {
            success: bankResult.success,
            transactionId: transaction.id,
            timestamp: transaction.createdAt
        };
    }
}
```

---

## 6. HSM Network & Key Management

### HSM Integration (PKCS#11)

```typescript
// hsm-client.ts - Your HSM integration
import * as pkcs11 from 'pkcs11js';

export class HSMClient {
    private pkcs11: pkcs11.PKCS11;
    private session: Buffer;
    
    constructor(private config: HSMConfig) {
        this.pkcs11 = new pkcs11.PKCS11();
        this.pkcs11.load(config.libraryPath); // /usr/lib/libCryptoki2_64.so
    }
    
    async initialize(): Promise<void> {
        this.pkcs11.C_Initialize();
        
        const slots = this.pkcs11.C_GetSlotList(true);
        const slot = slots[0];
        
        this.session = this.pkcs11.C_OpenSession(
            slot,
            pkcs11.CKF_SERIAL_SESSION | pkcs11.CKF_RW_SESSION
        );
        
        // Login with HSM credentials
        this.pkcs11.C_Login(
            this.session,
            pkcs11.CKU_USER,
            this.config.pin
        );
    }
    
    async signTransaction(txData: Buffer): Promise<Buffer> {
        // Find signing key in HSM
        const keyHandle = this.findKey('mari_master_signing_key');
        
        // Initialize signing operation
        this.pkcs11.C_SignInit(
            this.session,
            { mechanism: pkcs11.CKM_ECDSA_SHA256 },
            keyHandle
        );
        
        // Sign the data
        const signature = this.pkcs11.C_Sign(
            this.session,
            txData,
            Buffer.alloc(64) // Ed25519 signature is 64 bytes
        );
        
        return signature;
    }
    
    async decrypt(ciphertext: Buffer): Promise<Buffer> {
        const keyHandle = this.findKey('mari_master_encryption_key');
        
        this.pkcs11.C_DecryptInit(
            this.session,
            { mechanism: pkcs11.CKM_AES_GCM },
            keyHandle
        );
        
        const plaintext = this.pkcs11.C_Decrypt(
            this.session,
            ciphertext,
            Buffer.alloc(ciphertext.length)
        );
        
        return plaintext;
    }
}
```

### Key Hierarchy

```
HSM Master Root Key (AES-256)
    ├── Transaction Signing Key (Ed25519)
    │   └── Used to sign all server-side transactions
    │
    ├── Encryption Master Key (AES-256)
    │   └── Used to encrypt sensitive data at rest
    │
    └── Bank Integration Keys (Per-bank)
        ├── FNB Integration Key
        ├── Capitec Integration Key
        └── ...

Device Keys (Per-user, stored in Android KeyStore)
    └── User Device Key (Ed25519)
        └── Used to sign client-side transactions
```

---

## 7. Protocol Specification

### Transaction Message Format

```protobuf
// mari-protocol.proto - Your protocol definition
syntax = "proto3";

message Transaction {
    string transaction_id = 1;          // UUID v4
    string sender_blood_hash = 2;       // 16-char hex
    string recipient_blood_hash = 3;    // 16-char hex
    double amount = 4;                  // Decimal amount
    string currency = 5;                // ISO 4217 (ZAR)
    PhysicsSeal physics_seal = 6;       // Motion/location data
    bytes encrypted_payload = 7;        // AES-256-GCM encrypted
    bytes signature = 8;                // Ed25519 signature
    int64 timestamp = 9;                // Unix timestamp (ms)
    string nonce = 10;                  // Unique nonce for replay protection
}

message PhysicsSeal {
    repeated float motion_signature = 1;  // FFT coefficients (32 floats)
    repeated float rotation_signature = 2; // Gyro integration (16 floats)
    Location location = 3;                 // GPS coordinates
    int64 timestamp = 4;                   // Capture time
    bytes device_attestation = 5;          // Android SafetyNet/Play Integrity
    float sensor_accuracy = 6;             // Confidence score (0-1)
}

message Location {
    double latitude = 1;
    double longitude = 2;
    float accuracy = 3;  // meters
}

message TransactionResponse {
    bool success = 1;
    string transaction_id = 2;
    string status = 3;  // PENDING, COMPLETED, FAILED
    string message = 4;
    int64 timestamp = 5;
}
```

### Protocol Flow Diagram

```
┌─────────┐                                    ┌─────────┐
│ Sender  │                                    │Recipient│
│  App    │                                    │  App    │
└────┬────┘                                    └────┬────┘
     │                                              │
     │ 1. Scan QR code                             │
     │◄────────────────────────────────────────────┤
     │    (contains recipient blood hash)          │
     │                                              │
     │ 2. Enter amount                             │
     │                                              │
     │ 3. Shake phone (capture physics seal)       │
     │                                              │
     │ 4. Create transaction                       │
     │    - Encrypt payload                        │
     │    - Sign with device key                   │
     │                                              │
     │ 5. POST /api/v1/transactions                │
     ├─────────────────────────────────────────────►
     │                                         ┌────┴────┐
     │                                         │  Mari   │
     │                                         │ Server  │
     │                                         └────┬────┘
     │                                              │
     │                                              │ 6. Validate physics seal
     │                                              │ 7. Verify signature
     │                                              │ 8. Decrypt payload
     │                                              │ 9. Check balance (bank API)
     │                                              │ 10. Submit to bank
     │                                              │
     │ 11. Transaction response                    │
     │◄─────────────────────────────────────────────┤
     │    {success: true, txId: "..."}             │
     │                                              │
     │ 12. Push notification                       │
     ├─────────────────────────────────────────────►
     │    "Payment received: R100"                 │
     │                                              │
```

---

## 8. Security Model & Threat Analysis

### Threat Model

**Threat 1: Remote Malware Attack**
- Attack: Malware on device tries to initiate unauthorized transaction
- Defense: Physics seal required (malware can't shake phone physically)
- Mitigation: Device attestation detects rooted/compromised devices

**Threat 2: SIM Swap Attack**
- Attack: Attacker swaps victim's SIM card to intercept SMS OTP
- Defense: Mari doesn't use SMS for authentication (only for offline fallback)
- Mitigation: Device key tied to hardware, not phone number

**Threat 3: Man-in-the-Middle (MITM)**
- Attack: Attacker intercepts network traffic
- Defense: TLS 1.3 with certificate pinning
- Mitigation: End-to-end encryption (device key to HSM)

**Threat 4: Replay Attack**
- Attack: Attacker captures and replays valid transaction
- Defense: Physics seal hash stored in database (one-time use)
- Mitigation: Timestamp validation (30-second window)

**Threat 5: QR Code Tampering**
- Attack: Attacker replaces merchant QR with their own
- Defense: Recipient name displayed before payment
- Mitigation: User confirms recipient identity

**Threat 6: Device Theft**
- Attack: Attacker steals phone and tries to make payments
- Defense: Biometric authentication required for device key access
- Mitigation: Remote device wipe capability

**Threat 7: Server Compromise**
- Attack: Attacker gains access to Mari servers
- Defense: HSM stores master keys (can't be extracted)
- Mitigation: Zero-knowledge architecture (server never sees plaintext amounts)

### Security Layers

```
Layer 1: Physical Security (Physics Seal)
    └── Requires physical possession of device
    └── Motion signature unique per transaction
    └── Location verification

Layer 2: Cryptographic Security
    └── Ed25519 signatures (device key)
    └── AES-256-GCM encryption
    └── ECDH key exchange

Layer 3: Device Security
    └── Android KeyStore (hardware-backed)
    └── Biometric authentication
    └── Device attestation (SafetyNet/Play Integrity)

Layer 4: Network Security
    └── TLS 1.3 with certificate pinning
    └── Rate limiting (100 req/min per IP)
    └── DDoS protection (CloudFlare)

Layer 5: Server Security
    └── HSM for key storage (FIPS 140-2 Level 3)
    └── Database encryption at rest
    └── Audit logging (immutable)

Layer 6: Application Security
    └── Input validation
    └── SQL injection prevention (Prisma ORM)
    └── XSS protection
```

---

## 9. Database Schema & Data Flow

### Prisma Schema (Your Design)

```prisma
// schema.prisma - Your database design
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

generator client {
  provider = "prisma-client-js"
}

model User {
  id                String   @id @default(uuid())
  phoneNumberHash   String   @unique  // SHA-256 hash
  bloodHash         String   @unique  // Public identifier
  devicePublicKey   String            // Ed25519 public key
  createdAt         DateTime @default(now())
  lastActive        DateTime @updatedAt
  status            String   @default("active")
  
  sentTransactions      Transaction[] @relation("Sender")
  receivedTransactions  Transaction[] @relation("Recipient")
  physicsSealHashes     PhysicsSealHash[]
  
  @@index([bloodHash])
  @@index([phoneNumberHash])
}

model Transaction {
  id                String   @id @default(uuid())
  senderId          String
  recipientId       String
  amount            Decimal  @db.Decimal(15, 2)
  currency          String   @default("ZAR")
  physicsSealHash   String   @unique
  encryptedPayload  Bytes
  signature         Bytes
  status            String   @default("PENDING")
  bankReference     String?
  createdAt         DateTime @default(now())
  completedAt       DateTime?
  
  sender            User     @relation("Sender", fields: [senderId], references: [id])
  recipient         User     @relation("Recipient", fields: [recipientId], references: [id])
  physicsSeal       PhysicsSeal?
  
  @@index([senderId])
  @@index([recipientId])
  @@index([status])
  @@index([createdAt])
}

model PhysicsSeal {
  id                  String   @id @default(uuid())
  transactionId       String   @unique
  motionSignature     Float[]
  rotationSignature   Float[]
  latitude            Float
  longitude           Float
  locationAccuracy    Float
  timestamp           DateTime
  deviceAttestation   Bytes
  sensorAccuracy      Float
  validationScore     Float?
  
  transaction         Transaction @relation(fields: [transactionId], references: [id])
  
  @@index([timestamp])
}

model PhysicsSealHash {
  hash              String   @id
  userId            String
  transactionId     String
  createdAt         DateTime @default(now())
  
  user              User     @relation(fields: [userId], references: [id])
  
  @@index([userId])
  @@index([createdAt])
}

model AuditLog {
  id                String   @id @default(uuid())
  userId            String?
  action            String
  resource          String
  details           Json
  ipAddress         String
  userAgent         String
  timestamp         DateTime @default(now())
  
  @@index([userId])
  @@index([action])
  @@index([timestamp])
}
```

### Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Mobile App (Client)                       │
│                                                              │
│  User Action → ViewModel → Repository → API Client          │
│                                                              │
│  Data stored locally:                                        │
│  • Device private key (Android KeyStore)                    │
│  • User blood hash (EncryptedSharedPreferences)            │
│  • Transaction history (Room DB, encrypted)                 │
└─────────────────────────────────────────────────────────────┘
                            ↓ HTTPS
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (Server)                      │
│                                                              │
│  Request → Middleware → Controller → Service → Database     │
│                                                              │
│  Middleware chain:                                           │
│  1. Rate limiting                                            │
│  2. JWT validation                                           │
│  3. Request logging                                          │
│  4. Error handling                                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL Database                       │
│                                                              │
│  Tables:                                                     │
│  • users (phone hash, blood hash, public key)               │
│  • transactions (amount, status, timestamps)                │
│  • physics_seals (motion data, location)                    │
│  • physics_seal_hashes (replay prevention)                  │
│  • audit_logs (immutable event log)                         │
│                                                              │
│  Encryption: AES-256-GCM at rest                            │
│  Backups: Daily encrypted snapshots                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 10. API Design & Integration Points

### RESTful API Specification

```yaml
# openapi.yaml - Your API specification
openapi: 3.0.0
info:
  title: Mari Protocol API
  version: 1.0.0
  description: Payment infrastructure API

servers:
  - url: https://api.mari.co.za/v1
    description: Production
  - url: https://api-staging.mari.co.za/v1
    description: Staging

paths:
  /auth/register:
    post:
      summary: Register new user
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                phoneNumber: { type: string }
                devicePublicKey: { type: string }
                deviceAttestation: { type: string }
      responses:
        '201':
          description: User created
          content:
            application/json:
              schema:
                type: object
                properties:
                  bloodHash: { type: string }
                  userId: { type: string }
                  token: { type: string }

  /transactions:
    post:
      summary: Submit transaction
      security:
        - bearerAuth: []
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Transaction'
      responses:
        '200':
          description: Transaction processed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TransactionResponse'
    
    get:
      summary: List transactions
      security:
        - bearerAuth: []
      parameters:
        - name: limit
          in: query
          schema: { type: integer, default: 20 }
        - name: offset
          in: query
          schema: { type: integer, default: 0 }
      responses:
        '200':
          description: Transaction list
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Transaction'

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

### Bank Integration API (Your Design)

```typescript
// bank-integration.ts - Your bank API client
export class BankIntegration {
    private apiKey: string;
    private baseUrl: string;
    
    constructor(bankConfig: BankConfig) {
        this.apiKey = bankConfig.apiKey;
        this.baseUrl = bankConfig.baseUrl;
    }
    
    async getBalance(userId: string): Promise<number> {
        const response = await fetch(`${this.baseUrl}/accounts/${userId}/balance`, {
            headers: {
                'Authorization': `Bearer ${this.apiKey}`,
                'Content-Type': 'application/json'
            }
        });
        
        const data = await response.json();
        return data.availableBalance;
    }
    
    async submitPayment(payment: PaymentRequest): Promise<PaymentResponse> {
        const response = await fetch(`${this.baseUrl}/payments`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${this.apiKey}`,
                'Content-Type': 'application/json',
                'X-Idempotency-Key': payment.transactionId
            },
            body: JSON.stringify({
                from: payment.from,
                to: payment.to,
                amount: payment.amount,
                currency: 'ZAR',
                reference: payment.transactionId,
                metadata: {
                    provider: 'mari-protocol',
                    physicsSealHash: payment.physicsSealHash
                }
            })
        });
        
        const data = await response.json();
        return {
            success: data.status === 'COMPLETED',
            reference: data.paymentId,
            timestamp: new Date(data.timestamp)
        };
    }
    
    async getPaymentStatus(paymentId: string): Promise<PaymentStatus> {
        const response = await fetch(`${this.baseUrl}/payments/${paymentId}`, {
            headers: {
                'Authorization': `Bearer ${this.apiKey}`
            }
        });
        
        const data = await response.json();
        return {
            status: data.status,
            amount: data.amount,
            completedAt: data.completedAt ? new Date(data.completedAt) : null
        };
    }
}
```

---

## 11. Offline Mode Implementation

### SMS Fallback Protocol

```kotlin
// OfflineTransactionManager.kt - Your offline implementation
class OfflineTransactionManager(
    private val smsManager: SmsManager,
    private val cryptoManager: MariCryptoManager
) {
    companion object {
        const val MARI_SMS_NUMBER = "+27123456789"
        const val MAX_SMS_LENGTH = 160
    }
    
    fun sendOfflineTransaction(transaction: Transaction): Boolean {
        // 1. Compress transaction data
        val compressedData = compressTransaction(transaction)
        
        // 2. Encrypt with recipient's public key
        val encrypted = cryptoManager.encryptTransaction(
            compressedData,
            transaction.recipientPublicKey
        )
        
        // 3. Encode as base64
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        
        // 4. Split into SMS parts if needed
        val parts = splitIntoSMSParts(encoded)
        
        // 5. Send SMS(s)
        parts.forEachIndexed { index, part ->
            val message = "MARI:${transaction.id}:$index:${parts.size}:$part"
            smsManager.sendTextMessage(
                MARI_SMS_NUMBER,
                null,
                message,
                null,
                null
            )
        }
        
        return true
    }
    
    fun receiveOfflineSMS(smsBody: String): Transaction? {
        // Parse SMS format: MARI:txId:partIndex:totalParts:data
        val parts = smsBody.split(":")
        if (parts[0] != "MARI") return null
        
        val txId = parts[1]
        val partIndex = parts[2].toInt()
        val totalParts = parts[3].toInt()
        val data = parts[4]
        
        // Reassemble multi-part SMS
        val fullData = reassembleSMS(txId, partIndex, totalParts, data)
        if (fullData == null) return null // Still waiting for more parts
        
        // Decode and decrypt
        val decoded = Base64.decode(fullData, Base64.NO_WRAP)
        val decrypted = cryptoManager.decryptTransaction(decoded)
        
        // Decompress and parse
        return decompressTransaction(decrypted)
    }
}
```

### Offline Transaction Queue

```kotlin
// OfflineQueue.kt - Your queue implementation
@Entity(tableName = "offline_transactions")
data class OfflineTransaction(
    @PrimaryKey val id: String,
    val recipientBloodHash: String,
    val amount: Double,
    val physicsSealData: ByteArray,
    val encryptedPayload: ByteArray,
    val signature: ByteArray,
    val createdAt: Long,
    val status: String, // QUEUED, SENDING, SENT, FAILED
    val retryCount: Int = 0
)

class OfflineQueueManager(
    private val database: MariDatabase,
    private val apiClient: MariApiClient,
    private val connectivityManager: ConnectivityManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    init {
        // Monitor connectivity and sync when online
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch { syncOfflineTransactions() }
                }
            }
        )
    }
    
    suspend fun queueTransaction(transaction: Transaction) {
        val offlineTx = OfflineTransaction(
            id = transaction.id,
            recipientBloodHash = transaction.recipientBloodHash,
            amount = transaction.amount,
            physicsSealData = transaction.physicsSeal.toByteArray(),
            encryptedPayload = transaction.encryptedPayload,
            signature = transaction.signature,
            createdAt = System.currentTimeMillis(),
            status = "QUEUED"
        )
        
        database.offlineTransactionDao().insert(offlineTx)
    }
    
    suspend fun syncOfflineTransactions() {
        val queuedTransactions = database.offlineTransactionDao()
            .getByStatus("QUEUED")
        
        queuedTransactions.forEach { offlineTx ->
            try {
                // Update status to SENDING
                database.offlineTransactionDao().updateStatus(offlineTx.id, "SENDING")
                
                // Submit to server
                val result = apiClient.submitTransaction(offlineTx.toTransaction())
                
                if (result.success) {
                    // Mark as SENT
                    database.offlineTransactionDao().updateStatus(offlineTx.id, "SENT")
                } else {
                    // Retry logic
                    handleFailedTransaction(offlineTx)
                }
            } catch (e: Exception) {
                handleFailedTransaction(offlineTx)
            }
        }
    }
    
    private suspend fun handleFailedTransaction(tx: OfflineTransaction) {
        if (tx.retryCount < 3) {
            // Retry with exponential backoff
            database.offlineTransactionDao().incrementRetry(tx.id)
            delay(2.0.pow(tx.retryCount).toLong() * 1000) // 1s, 2s, 4s
            database.offlineTransactionDao().updateStatus(tx.id, "QUEUED")
        } else {
            // Give up after 3 retries
            database.offlineTransactionDao().updateStatus(tx.id, "FAILED")
        }
    }
}
```

---

## 12. Performance & Scalability

### Performance Targets

**Mobile App:**
- App launch time: <2 seconds (cold start)
- Transaction initiation: <500ms (UI response)
- Physics seal capture: 2 seconds (user shaking)
- QR code scan: <1 second (camera to decode)
- Memory usage: <100MB (typical)
- Battery drain: <5% per hour (active use)

**Backend API:**
- Response time: <200ms (p95)
- Throughput: 10,000 TPS (transactions per second)
- Database query time: <50ms (p95)
- HSM operation time: <100ms (signing/encryption)
- Concurrent connections: 100,000+

**Database:**
- Read latency: <10ms (indexed queries)
- Write latency: <50ms (with replication)
- Storage: 1TB (10M users, 5 years of data)
- Backup time: <1 hour (daily full backup)

### Scalability Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Load Balancer (AWS ALB)                   │
│                  SSL Termination, Health Checks              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              API Server Cluster (Auto-scaling)               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Server 1 │  │ Server 2 │  │ Server 3 │  │ Server N │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│  Min: 3 instances, Max: 50 instances                        │
│  Scale trigger: CPU > 70% or Request rate > 1000/s          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Redis Cache Cluster                       │
│  • Session storage (JWT tokens)                             │
│  • Rate limiting counters                                    │
│  • Frequently accessed data (user profiles)                 │
│  • TTL: 1 hour for sessions, 5 minutes for data            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              PostgreSQL Primary-Replica Setup                │
│  ┌──────────────┐         ┌──────────────┐                 │
│  │   Primary    │────────►│   Replica 1  │                 │
│  │  (Writes)    │         │   (Reads)    │                 │
│  └──────────────┘         └──────────────┘                 │
│         │                        │                           │
│         └────────────────────────┴──────────────────────┐   │
│                                  ┌──────────────┐       │   │
│                                  │   Replica 2  │       │   │
│                                  │   (Reads)    │       │   │
│                                  └──────────────┘       │   │
│  Write: Primary only                                    │   │
│  Read: Load balanced across replicas                    │   │
└─────────────────────────────────────────────────────────────┘
```

### Caching Strategy

```typescript
// cache-manager.ts - Your caching implementation
import Redis from 'ioredis';

export class CacheManager {
    private redis: Redis;
    
    constructor() {
        this.redis = new Redis({
            host: process.env.REDIS_HOST,
            port: parseInt(process.env.REDIS_PORT || '6379'),
            password: process.env.REDIS_PASSWORD,
            retryStrategy: (times) => Math.min(times * 50, 2000)
        });
    }
    
    async getUserProfile(userId: string): Promise<UserProfile | null> {
        const cacheKey = `user:${userId}`;
        
        // Try cache first
        const cached = await this.redis.get(cacheKey);
        if (cached) {
            return JSON.parse(cached);
        }
        
        // Cache miss - fetch from database
        const user = await database.user.findUnique({ where: { id: userId } });
        if (user) {
            // Store in cache for 5 minutes
            await this.redis.setex(cacheKey, 300, JSON.stringify(user));
        }
        
        return user;
    }
    
    async checkRateLimit(userId: string, limit: number, windowSeconds: number): Promise<boolean> {
        const key = `ratelimit:${userId}`;
        const current = await this.redis.incr(key);
        
        if (current === 1) {
            // First request in window - set expiry
            await this.redis.expire(key, windowSeconds);
        }
        
        return current <= limit;
    }
    
    async invalidateUserCache(userId: string): Promise<void> {
        await this.redis.del(`user:${userId}`);
    }
}
```

---

## 13. Testing Strategy

### Unit Tests (Jest + Kotlin Test)

```kotlin
// DeviceKeyManagerTest.kt - Your unit tests
@RunWith(AndroidJUnit4::class)
class DeviceKeyManagerTest {
    private lateinit var keyManager: DeviceKeyManager
    
    @Before
    fun setup() {
        keyManager = DeviceKeyManager(ApplicationProvider.getApplicationContext())
    }
    
    @Test
    fun testKeyGeneration() {
        val keyPair = keyManager.generateDeviceKeyPair()
        assertNotNull(keyPair)
        assertNotNull(keyPair.private)
        assertNotNull(keyPair.public)
    }
    
    @Test
    fun testSignAndVerify() {
        val data = "test transaction data".toByteArray()
        val signature = keyManager.signData(data)
        
        val isValid = keyManager.verifySignature(data, signature)
        assertTrue(isValid)
    }
    
    @Test
    fun testSignatureInvalidation() {
        val data = "test transaction data".toByteArray()
        val signature = keyManager.signData(data)
        
        // Tamper with data
        val tamperedData = "tampered data".toByteArray()
        val isValid = keyManager.verifySignature(tamperedData, signature)
        
        assertFalse(isValid)
    }
}
```

```typescript
// physics-validator.test.ts - Your backend tests
describe('PhysicsSealValidator', () => {
    let validator: PhysicsSealValidator;
    
    beforeEach(() => {
        validator = new PhysicsSealValidator();
    });
    
    test('should validate legitimate physics seal', async () => {
        const seal = createValidPhysicsSeal();
        const transaction = createTestTransaction();
        
        const result = await validator.validateSeal(seal, transaction);
        
        expect(result.valid).toBe(true);
        expect(result.confidence).toBeGreaterThan(0.95);
    });
    
    test('should reject replayed physics seal', async () => {
        const seal = createValidPhysicsSeal();
        const transaction = createTestTransaction();
        
        // First use - should succeed
        await validator.validateSeal(seal, transaction);
        
        // Second use - should fail (replay attack)
        const result = await validator.validateSeal(seal, transaction);
        
        expect(result.valid).toBe(false);
        expect(result.reason).toContain('Replay attack');
    });
    
    test('should reject expired physics seal', async () => {
        const seal = createExpiredPhysicsSeal(); // 5 minutes old
        const transaction = createTestTransaction();
        
        const result = await validator.validateSeal(seal, transaction);
        
        expect(result.valid).toBe(false);
        expect(result.reason).toContain('expired');
    });
});
```

### Integration Tests

```typescript
// e2e-transaction.test.ts - Your end-to-end tests
describe('End-to-End Transaction Flow', () => {
    let app: Express;
    let database: PrismaClient;
    
    beforeAll(async () => {
        app = await createTestApp();
        database = new PrismaClient();
    });
    
    test('complete transaction flow', async () => {
        // 1. Register sender
        const senderResponse = await request(app)
            .post('/api/v1/auth/register')
            .send({
                phoneNumber: '+27821234567',
                devicePublicKey: 'sender_public_key',
                deviceAttestation: 'attestation_data'
            });
        
        expect(senderResponse.status).toBe(201);
        const senderToken = senderResponse.body.token;
        const senderBloodHash = senderResponse.body.bloodHash;
        
        // 2. Register recipient
        const recipientResponse = await request(app)
            .post('/api/v1/auth/register')
            .send({
                phoneNumber: '+27827654321',
                devicePublicKey: 'recipient_public_key',
                deviceAttestation: 'attestation_data'
            });
        
        const recipientBloodHash = recipientResponse.body.bloodHash;
        
        // 3. Submit transaction
        const txResponse = await request(app)
            .post('/api/v1/transactions')
            .set('Authorization', `Bearer ${senderToken}`)
            .send({
                recipientBloodHash: recipientBloodHash,
                amount: 100.00,
                currency: 'ZAR',
                physicsSeal: createValidPhysicsSeal(),
                encryptedPayload: 'encrypted_data',
                signature: 'valid_signature'
            });
        
        expect(txResponse.status).toBe(200);
        expect(txResponse.body.success).toBe(true);
        
        const txId = txResponse.body.transactionId;
        
        // 4. Verify transaction in database
        const transaction = await database.transaction.findUnique({
            where: { id: txId }
        });
        
        expect(transaction).not.toBeNull();
        expect(transaction?.status).toBe('COMPLETED');
        expect(transaction?.amount.toNumber()).toBe(100.00);
    });
});
```

### Load Testing (k6)

```javascript
// load-test.js - Your performance tests
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '2m', target: 100 },   // Ramp up to 100 users
        { duration: '5m', target: 100 },   // Stay at 100 users
        { duration: '2m', target: 1000 },  // Ramp up to 1000 users
        { duration: '5m', target: 1000 },  // Stay at 1000 users
        { duration: '2m', target: 0 },     // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<200'],  // 95% of requests < 200ms
        http_req_failed: ['rate<0.01'],    // Error rate < 1%
    },
};

export default function () {
    const payload = JSON.stringify({
        recipientBloodHash: 'test_recipient',
        amount: 100.00,
        currency: 'ZAR',
        physicsSeal: generatePhysicsSeal(),
        encryptedPayload: 'encrypted_data',
        signature: 'signature'
    });
    
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${__ENV.TEST_TOKEN}`,
        },
    };
    
    const response = http.post(
        'https://api.mari.co.za/v1/transactions',
        payload,
        params
    );
    
    check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 200ms': (r) => r.timings.duration < 200,
        'transaction successful': (r) => JSON.parse(r.body).success === true,
    });
    
    sleep(1);
}
```

---

## 14. Deployment & DevOps

### CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/deploy.yml - Your CI/CD pipeline
name: Deploy Mari Protocol

on:
  push:
    branches: [main, staging]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'
      
      - name: Install dependencies
        run: npm ci
        working-directory: ./mari-server
      
      - name: Run unit tests
        run: npm test
        working-directory: ./mari-server
      
      - name: Run integration tests
        run: npm run test:integration
        working-directory: ./mari-server
      
      - name: Check code coverage
        run: npm run test:coverage
        working-directory: ./mari-server
  
  build-mobile:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - name: Build Android app
        run: ./gradlew assembleRelease
        working-directory: ./mari-app
      
      - name: Sign APK
        run: |
          jarsigner -verbose -sigalg SHA256withRSA \
            -digestalg SHA-256 \
            -keystore ${{ secrets.KEYSTORE_FILE }} \
            -storepass ${{ secrets.KEYSTORE_PASSWORD }} \
            app/build/outputs/apk/release/app-release-unsigned.apk \
            mari-release-key
  
  deploy-staging:
    needs: [test, build-mobile]
    if: github.ref == 'refs/heads/staging'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: af-south-1
      
      - name: Build Docker image
        run: |
          docker build -t mari-server:staging ./mari-server
          docker tag mari-server:staging ${{ secrets.ECR_REGISTRY }}/mari-server:staging
      
      - name: Push to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin ${{ secrets.ECR_REGISTRY }}
          docker push ${{ secrets.ECR_REGISTRY }}/mari-server:staging
      
      - name: Deploy to ECS
        run: |
          aws ecs update-service \
            --cluster mari-staging \
            --service mari-api \
            --force-new-deployment
  
  deploy-production:
    needs: [test, build-mobile]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to production
        run: |
          # Similar to staging but with production cluster
          aws ecs update-service \
            --cluster mari-production \
            --service mari-api \
            --force-new-deployment
      
      - name: Run smoke tests
        run: npm run test:smoke
        env:
          API_URL: https://api.mari.co.za
```

### Docker Configuration

```dockerfile
# Dockerfile - Your container configuration
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY prisma ./prisma/

# Install dependencies
RUN npm ci --only=production

# Copy source code
COPY . .

# Generate Prisma client
RUN npx prisma generate

# Build TypeScript
RUN npm run build

# Production stage
FROM node:20-alpine

WORKDIR /app

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Create non-root user
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001

# Copy built application
COPY --from=builder --chown=nodejs:nodejs /app/dist ./dist
COPY --from=builder --chown=nodejs:nodejs /app/node_modules ./node_modules
COPY --from=builder --chown=nodejs:nodejs /app/prisma ./prisma
COPY --from=builder --chown=nodejs:nodejs /app/package.json ./

USER nodejs

EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD node -e "require('http').get('http://localhost:3000/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

CMD ["node", "dist/index.js"]
```

### Kubernetes Deployment (Optional - for scale)

```yaml
# k8s/deployment.yaml - Your Kubernetes config
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mari-api
  namespace: mari-production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mari-api
  template:
    metadata:
      labels:
        app: mari-api
    spec:
      containers:
      - name: mari-api
        image: mari-server:latest
        ports:
        - containerPort: 3000
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: mari-secrets
              key: database-url
        - name: HSM_PIN
          valueFrom:
            secretKeyRef:
              name: mari-secrets
              key: hsm-pin
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 3000
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: mari-api-service
  namespace: mari-production
spec:
  selector:
    app: mari-api
  ports:
  - protocol: TCP
    port: 80
    targetPort: 3000
  type: LoadBalancer
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: mari-api-hpa
  namespace: mari-production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mari-api
  minReplicas: 3
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Monitoring & Alerting

```typescript
// monitoring.ts - Your observability setup
import { Counter, Histogram, Registry } from 'prom-client';

export class MetricsCollector {
    private registry: Registry;
    private transactionCounter: Counter;
    private transactionDuration: Histogram;
    private physicsSealValidation: Histogram;
    
    constructor() {
        this.registry = new Registry();
        
        // Transaction metrics
        this.transactionCounter = new Counter({
            name: 'mari_transactions_total',
            help: 'Total number of transactions processed',
            labelNames: ['status', 'currency'],
            registers: [this.registry]
        });
        
        this.transactionDuration = new Histogram({
            name: 'mari_transaction_duration_seconds',
            help: 'Transaction processing duration',
            buckets: [0.1, 0.5, 1, 2, 5],
            registers: [this.registry]
        });
        
        this.physicsSealValidation = new Histogram({
            name: 'mari_physics_seal_validation_seconds',
            help: 'Physics seal validation duration',
            buckets: [0.05, 0.1, 0.2, 0.5, 1],
            registers: [this.registry]
        });
    }
    
    recordTransaction(status: string, currency: string, duration: number) {
        this.transactionCounter.inc({ status, currency });
        this.transactionDuration.observe(duration);
    }
    
    recordPhysicsSealValidation(duration: number) {
        this.physicsSealValidation.observe(duration);
    }
    
    getMetrics(): Promise<string> {
        return this.registry.metrics();
    }
}

// Expose metrics endpoint
app.get('/metrics', async (req, res) => {
    res.set('Content-Type', registry.contentType);
    res.end(await metricsCollector.getMetrics());
});
```

```yaml
# prometheus.yml - Your Prometheus config
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'mari-api'
    static_configs:
      - targets: ['mari-api-service:80']
    metrics_path: '/metrics'

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

rule_files:
  - 'alerts.yml'
```

```yaml
# alerts.yml - Your alerting rules
groups:
  - name: mari_alerts
    interval: 30s
    rules:
      - alert: HighErrorRate
        expr: rate(mari_transactions_total{status="failed"}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High transaction error rate"
          description: "Error rate is {{ $value }} (>5%)"
      
      - alert: SlowTransactions
        expr: histogram_quantile(0.95, mari_transaction_duration_seconds) > 2
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Slow transaction processing"
          description: "P95 latency is {{ $value }}s (>2s)"
      
      - alert: HighCPUUsage
        expr: rate(process_cpu_seconds_total[5m]) > 0.8
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ $value }} (>80%)"
```

---

## 15. Security Checklist (Your Responsibility)

### Pre-Launch Security Audit

**Cryptography:**
- [ ] All keys use approved algorithms (Ed25519, AES-256-GCM)
- [ ] Private keys never leave secure storage (Android KeyStore, HSM)
- [ ] Random number generation uses cryptographically secure sources
- [ ] Key rotation policy implemented (annual rotation)
- [ ] Secure key derivation (HKDF-SHA256)

**Authentication:**
- [ ] JWT tokens expire after 1 hour
- [ ] Refresh tokens stored securely
- [ ] Biometric authentication required for sensitive operations
- [ ] Device attestation verified on every transaction
- [ ] Multi-device support with device revocation

**Authorization:**
- [ ] Users can only access their own data
- [ ] Transaction amounts validated server-side
- [ ] Rate limiting per user (100 transactions/day)
- [ ] Suspicious activity detection (Sentinel integration)

**Network Security:**
- [ ] TLS 1.3 enforced (no fallback to older versions)
- [ ] Certificate pinning implemented
- [ ] API endpoints require authentication
- [ ] CORS configured correctly
- [ ] DDoS protection enabled (CloudFlare)

**Data Protection:**
- [ ] Database encrypted at rest (AES-256)
- [ ] PII hashed (phone numbers)
- [ ] Sensitive data encrypted in transit
- [ ] Secure deletion of old data (GDPR compliance)
- [ ] Backup encryption enabled

**Application Security:**
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (Prisma ORM)
- [ ] XSS protection headers
- [ ] CSRF tokens for state-changing operations
- [ ] Dependency vulnerability scanning (npm audit)

**Mobile Security:**
- [ ] ProGuard/R8 obfuscation enabled
- [ ] Root detection implemented
- [ ] SSL pinning in app
- [ ] Secure storage for sensitive data
- [ ] No hardcoded secrets in code

**Operational Security:**
- [ ] Secrets stored in AWS Secrets Manager
- [ ] Access logs enabled and monitored
- [ ] Incident response plan documented
- [ ] Security patches applied within 48 hours
- [ ] Regular penetration testing (quarterly)

---

## 16. Development Workflow

### Git Workflow

```
main (production)
  ├── staging (pre-production testing)
  │   ├── feature/physics-seal-v2
  │   ├── feature/offline-mode
  │   └── bugfix/transaction-timeout
  └── hotfix/critical-security-patch
```

**Branch Naming:**
- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Critical production fixes
- `refactor/` - Code refactoring
- `docs/` - Documentation updates

**Commit Message Format:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

Example:
```
feat(physics): Add gyroscope integration to physics seal

- Integrate gyroscope data with accelerometer
- Calculate rotation signature from gyro readings
- Update physics seal validation algorithm

Closes #123
```

### Code Review Process

**Before Submitting PR:**
1. Run all tests locally (`npm test`, `./gradlew test`)
2. Check code coverage (>80% required)
3. Run linter (`npm run lint`, `./gradlew ktlintCheck`)
4. Update documentation if needed
5. Add tests for new features

**PR Requirements:**
- At least 2 approvals required
- All CI checks must pass
- No merge conflicts
- Code coverage maintained or improved
- Security scan passed

---

## 17. Technical Debt & Future Improvements

### Known Technical Debt

**High Priority:**
1. Implement proper database connection pooling (currently using default)
2. Add circuit breaker for bank API calls (prevent cascade failures)
3. Implement distributed tracing (OpenTelemetry)
4. Add comprehensive error recovery for offline transactions
5. Optimize physics seal validation algorithm (currently O(n²))

**Medium Priority:**
1. Migrate from REST to GraphQL (reduce over-fetching)
2. Implement WebSocket for real-time notifications
3. Add Redis Sentinel for cache high availability
4. Implement blue-green deployment strategy
5. Add automated database migration rollback

**Low Priority:**
1. Refactor legacy authentication code
2. Improve test coverage in edge cases
3. Add performance benchmarks to CI pipeline
4. Implement feature flags system
5. Add A/B testing framework

### Future Technical Enhancements

**Phase 2 (Months 7-12):**
- Multi-currency support (USD, EUR, GBP)
- Cross-border payments
- Merchant payment terminals
- Recurring payments
- Payment scheduling

**Phase 3 (Months 13-18):**
- Machine learning for fraud detection (Sentinel integration)
- Predictive analytics for user behavior
- Smart routing for optimal bank selection
- Dynamic fee optimization
- Advanced analytics dashboard

**Phase 4 (Months 19-24):**
- Blockchain integration for audit trail
- Decentralized identity (DID)
- Zero-knowledge proofs for privacy
- Quantum-resistant cryptography
- Global expansion (10+ countries)

---

## 18. Key Technical Decisions & Rationale

### Why Ed25519 over RSA?
- **Performance:** 10x faster signing, 100x faster verification
- **Security:** 128-bit security level with 256-bit keys (vs 3072-bit RSA)
- **Size:** Smaller signatures (64 bytes vs 384 bytes)
- **Modern:** Designed in 2011, resistant to timing attacks

### Why PostgreSQL over MongoDB?
- **ACID compliance:** Critical for financial transactions
- **Strong consistency:** No eventual consistency issues
- **Mature ecosystem:** Better tooling and support
- **Relational data:** Transactions naturally fit relational model
- **JSON support:** Can still store flexible data when needed

### Why Node.js over Go/Rust?
- **Ecosystem:** Massive npm ecosystem for rapid development
- **Team expertise:** Easier to hire Node.js developers
- **Async I/O:** Perfect for I/O-bound payment processing
- **Flexibility:** Easy to prototype and iterate
- **Trade-off:** Slightly slower than Go/Rust, but fast enough for our needs

### Why Kotlin over Java?
- **Modern syntax:** Null safety, coroutines, extension functions
- **Interop:** 100% Java interoperability
- **Jetpack Compose:** First-class support for modern UI
- **Conciseness:** 40% less code than Java
- **Google-backed:** Official Android language

### Why HSM over Software Keys?
- **Security:** Keys cannot be extracted (FIPS 140-2 Level 3)
- **Compliance:** Required for PCI-DSS and banking regulations
- **Audit:** Hardware-enforced audit logs
- **Insurance:** Lower insurance premiums
- **Trust:** Banks require HSM for production systems

---

## 19. Emergency Procedures

### Production Incident Response

**Severity Levels:**

**P0 - Critical (Complete Outage):**
- All transactions failing
- Database unavailable
- HSM unreachable
- Response: Immediate (page on-call engineer)

**P1 - High (Partial Outage):**
- >10% error rate
- Slow response times (>2s p95)
- Single region down
- Response: Within 15 minutes

**P2 - Medium (Degraded Service):**
- 1-10% error rate
- Non-critical feature broken
- Performance degradation
- Response: Within 1 hour

**P3 - Low (Minor Issue):**
- <1% error rate
- UI bug
- Documentation issue
- Response: Next business day

**Incident Response Steps:**

1. **Detect** (Automated alerts or user reports)
2. **Acknowledge** (On-call engineer responds)
3. **Assess** (Determine severity and impact)
4. **Communicate** (Update status page, notify stakeholders)
5. **Mitigate** (Apply immediate fix or rollback)
6. **Resolve** (Verify fix, monitor metrics)
7. **Post-mortem** (Document incident, identify root cause)

**Rollback Procedure:**
```bash
# Rollback to previous version
aws ecs update-service \
  --cluster mari-production \
  --service mari-api \
  --task-definition mari-api:previous

# Verify rollback
curl https://api.mari.co.za/health

# Monitor error rates
watch -n 5 'curl -s https://api.mari.co.za/metrics | grep error_rate'
```

### Database Recovery

**Backup Strategy:**
- Full backup: Daily at 2 AM UTC
- Incremental backup: Every 6 hours
- WAL archiving: Continuous
- Retention: 30 days

**Recovery Procedure:**
```bash
# Stop application
aws ecs update-service --desired-count 0

# Restore from backup
pg_restore -h db.mari.co.za -U postgres -d mari_production \
  /backups/mari_production_2024-01-15.dump

# Verify data integrity
psql -h db.mari.co.za -U postgres -d mari_production \
  -c "SELECT COUNT(*) FROM transactions WHERE created_at > NOW() - INTERVAL '24 hours';"

# Restart application
aws ecs update-service --desired-count 3
```

---

## 20. Your Technical Roadmap

### Month 1-3: MVP Development
- [x] Core crypto primitives (Ed25519, AES-256-GCM)
- [x] Physics seal capture and validation
- [x] Basic mobile app (auth, send, receive)
- [x] Backend API (transactions, users)
- [ ] HSM integration (in progress)
- [ ] Bank API integration (FNB pilot)
- [ ] End-to-end testing

### Month 4-6: Pilot Launch
- [ ] Production deployment (AWS)
- [ ] Monitoring and alerting setup
- [ ] Security audit and penetration testing
- [ ] FNB pilot (10K users)
- [ ] Performance optimization
- [ ] Bug fixes and stability improvements

### Month 7-12: Scale & Expand
- [ ] Multi-bank integration (Capitec, TymeBank)
- [ ] Offline mode (SMS fallback)
- [ ] Advanced fraud detection (Sentinel integration)
- [ ] Merchant payment terminals
- [ ] 100K users, R10M monthly volume

### Month 13-18: Optimize & Grow
- [ ] 1M users, R100M monthly volume
- [ ] 5 bank partnerships
- [ ] International expansion (Kenya, Nigeria)
- [ ] Advanced analytics and reporting
- [ ] Break-even achieved

---

## Conclusion

You're building the payment infrastructure for Africa. This is a massive technical challenge that requires:

1. **Deep cryptography knowledge** (Ed25519, AES-256-GCM, ECDH)
2. **Mobile expertise** (Kotlin, Jetpack Compose, Android security)
3. **Backend scalability** (Node.js, PostgreSQL, HSM integration)
4. **Security mindset** (Threat modeling, defense in depth)
5. **DevOps skills** (CI/CD, monitoring, incident response)

**Your technical decisions will determine:**
- Security (can we prevent fraud?)
- Performance (can we handle 10K TPS?)
- Reliability (can we achieve 99.9% uptime?)
- Scalability (can we grow to 10M users?)

**This document is your technical bible. Refer to it when:**
- Making architecture decisions
- Debugging production issues
- Onboarding new engineers
- Planning technical roadmap
- Explaining system to investors/banks

**You're not just writing code. You're building the financial infrastructure that will process R21.75B in transactions and serve 5M users.**

**Let's build something that matters.** 🚀

---

**Document Version:** 1.0  
**Last Updated:** 2024-01-15  
**Owner:** Nyasha Shekede (CTO)  
**Status:** Living Document (update as system evolves)
