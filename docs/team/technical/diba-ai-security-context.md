# Mari Protocol: AI/Security/Data Context for Dibanisa Fakude
**Co-Founder & Chief AI Officer**

---

## Your Technical Domain: Sentinel Platform + Banking Integration

This document contains everything you need to build Sentinel (Mari's fraud detection system), integrate with banks, and ensure regulatory compliance. Your focus is AI/ML, security, and data - not mobile app development.

---

## Table of Contents

1. Sentinel Platform Architecture
2. Fraud Detection ML Models
3. Physics Seal Validation (AI Perspective)
4. Banking System Integration
5. Data Pipeline & Feature Engineering
6. Regulatory Compliance (FICA, POPIA, SARB)
7. Security Threat Analysis
8. Real-Time Scoring Engine
9. Explainable AI & Reporting
10. Performance Requirements

---

## 1. Sentinel Platform Architecture

### What Sentinel Does

Sentinel is Mari's real-time fraud detection and risk assessment platform. It sits between the Mari Protocol and banking systems, analyzing every transaction before approval.

```
┌─────────────────────────────────────────────────────────────┐
│                    Mari Mobile App                           │
│  User initiates payment with physics seal                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Mari Protocol Layer                       │
│  Validates cryptographic signatures                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              SENTINEL PLATFORM (YOUR DOMAIN)                │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  1. Physics Seal Validator                           │  │
│  │     - Motion pattern analysis                        │  │
│  │     - Location consistency check                     │  │
│  │     - Device attestation verification                │  │
│  └──────────────────────────────────────────────────────┘  │
│                            ↓                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  2. Behavioral Biometrics Engine                     │  │
│  │     - User behavior patterns                         │  │
│  │     - Transaction velocity                           │  │
│  │     - Anomaly detection                              │  │
│  └──────────────────────────────────────────────────────┘  │
│                            ↓                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  3. Network Analysis Engine                          │  │
│  │     - Fraud ring detection                           │  │
│  │     - Money mule identification                      │  │
│  │     - Graph neural networks                          │  │
│  └──────────────────────────────────────────────────────┘  │
│                            ↓                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  4. Ensemble Scoring Engine                          │  │
│  │     - Combine all model outputs                      │  │
│  │     - Final risk score (0-100%)                      │  │
│  │     - Decision: APPROVE / REVIEW / DECLINE          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Banking Systems                           │
│  Transaction submitted only if approved by Sentinel         │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack (Your Choices)

**ML/AI:**
- Python 3.11
- TensorFlow 2.x / PyTorch 2.x
- Scikit-learn
- XGBoost / LightGBM
- Graph Neural Networks (PyTorch Geometric)

**Data Processing:**
- Apache Kafka (real-time streaming)
- Apache Spark (batch processing)
- Pandas / Polars (data manipulation)
- NumPy (numerical computing)

**ML Operations:**
- MLflow (experiment tracking)
- Kubeflow (ML pipelines)
- TensorFlow Serving / Seldon (model serving)
- DVC (data version control)

**Databases:**
- PostgreSQL (transactional data)
- InfluxDB (time-series data)
- Neo4j (graph data for network analysis)
- Redis (feature cache)

**Infrastructure:**
- Docker + Kubernetes
- AWS SageMaker (model training)
- AWS Lambda (serverless inference)
- Prometheus + Grafana (monitoring)

---

## 2. Fraud Detection ML Models

### Model 1: Physics Seal Authenticity Classifier

**Purpose:** Determine if a physics seal is legitimate or synthetic/replayed

**Input Features:**
```python
{
    # Motion signature (32 FFT coefficients)
    "motion_fft": [0.12, 0.34, 0.56, ...],  # 32 floats
    
    # Rotation signature (16 gyro integration values)
    "rotation_signature": [0.01, 0.02, ...],  # 16 floats
    
    # Location data
    "latitude": -26.2041,
    "longitude": 28.0473,
    "location_accuracy": 5.0,  # meters
    
    # Temporal features
    "timestamp": 1699564800000,
    "time_since_last_tx": 3600,  # seconds
    
    # Device features
    "device_model": "Samsung Galaxy S21",
    "android_version": "13",
    "sensor_accuracy": 0.95,
    
    # Contextual features
    "network_type": "wifi",  # wifi, 4g, 5g
    "battery_level": 0.75,
    "is_charging": false
}
```

**Model Architecture:**
```python
import tensorflow as tf

def build_physics_seal_model():
    # Input layers
    motion_input = tf.keras.Input(shape=(32,), name='motion_fft')
    rotation_input = tf.keras.Input(shape=(16,), name='rotation_signature')
    location_input = tf.keras.Input(shape=(3,), name='location')  # lat, lng, accuracy
    temporal_input = tf.keras.Input(shape=(2,), name='temporal')  # timestamp, time_since_last
    device_input = tf.keras.Input(shape=(10,), name='device')  # encoded device features
    
    # Motion branch (LSTM for time-series)
    motion_lstm = tf.keras.layers.LSTM(64, return_sequences=True)(
        tf.keras.layers.Reshape((32, 1))(motion_input)
    )
    motion_lstm = tf.keras.layers.LSTM(32)(motion_lstm)
    motion_dense = tf.keras.layers.Dense(16, activation='relu')(motion_lstm)
    
    # Rotation branch
    rotation_dense = tf.keras.layers.Dense(32, activation='relu')(rotation_input)
    rotation_dense = tf.keras.layers.Dense(16, activation='relu')(rotation_dense)
    
    # Location branch
    location_dense = tf.keras.layers.Dense(16, activation='relu')(location_input)
    
    # Temporal branch
    temporal_dense = tf.keras.layers.Dense(16, activation='relu')(temporal_input)
    
    # Device branch
    device_dense = tf.keras.layers.Dense(16, activation='relu')(device_input)
    
    # Concatenate all branches
    concatenated = tf.keras.layers.Concatenate()([
        motion_dense,
        rotation_dense,
        location_dense,
        temporal_dense,
        device_dense
    ])
    
    # Final layers
    x = tf.keras.layers.Dense(64, activation='relu')(concatenated)
    x = tf.keras.layers.Dropout(0.3)(x)
    x = tf.keras.layers.Dense(32, activation='relu')(x)
    output = tf.keras.layers.Dense(1, activation='sigmoid', name='authenticity_score')(x)
    
    model = tf.keras.Model(
        inputs=[motion_input, rotation_input, location_input, temporal_input, device_input],
        outputs=output
    )
    
    model.compile(
        optimizer='adam',
        loss='binary_crossentropy',
        metrics=['accuracy', 'precision', 'recall', 'auc']
    )
    
    return model
```

**Training Data:**
- Legitimate seals: 100K samples from pilot users
- Synthetic attacks: 50K simulated attacks (motion replay, GPS spoofing)
- Edge cases: 20K samples (phone in car, public transport, etc.)

**Performance Targets:**
- Accuracy: >99.5%
- False positive rate: <0.1%
- Inference time: <50ms

---


### Model 2: Behavioral Biometrics (Anomaly Detection)

**Purpose:** Detect unusual user behavior patterns that indicate account takeover or fraud

**Features:**
```python
{
    # Transaction patterns
    "avg_transaction_amount": 150.00,
    "std_transaction_amount": 50.00,
    "max_transaction_amount": 500.00,
    "transaction_frequency": 20,  # per month
    
    # Temporal patterns
    "typical_hours": [8, 9, 12, 13, 18, 19],  # hours of day
    "typical_days": [1, 2, 3, 4, 5],  # weekdays
    "time_since_last_tx": 3600,  # seconds
    
    # Location patterns
    "typical_locations": [
        {"lat": -26.2041, "lng": 28.0473, "name": "home"},
        {"lat": -26.1076, "lng": 28.0567, "name": "work"}
    ],
    "location_entropy": 0.3,  # low = predictable, high = random
    
    # Recipient patterns
    "frequent_recipients": ["recipient1_hash", "recipient2_hash"],
    "new_recipient": false,
    "recipient_relationship_score": 0.8,  # 0 = new, 1 = frequent
    
    # Velocity features
    "transactions_last_hour": 2,
    "transactions_last_day": 5,
    "amount_last_hour": 200.00,
    "amount_last_day": 500.00
}
```

**Model: Isolation Forest + Autoencoder**

```python
from sklearn.ensemble import IsolationForest
import tensorflow as tf

class BehavioralBiometricsModel:
    def __init__(self):
        # Isolation Forest for anomaly detection
        self.isolation_forest = IsolationForest(
            n_estimators=100,
            contamination=0.01,  # 1% expected fraud rate
            random_state=42
        )
        
        # Autoencoder for learning normal behavior
        self.autoencoder = self.build_autoencoder()
    
    def build_autoencoder(self):
        input_dim = 50  # number of features
        encoding_dim = 10
        
        # Encoder
        input_layer = tf.keras.Input(shape=(input_dim,))
        encoded = tf.keras.layers.Dense(32, activation='relu')(input_layer)
        encoded = tf.keras.layers.Dense(16, activation='relu')(encoded)
        encoded = tf.keras.layers.Dense(encoding_dim, activation='relu')(encoded)
        
        # Decoder
        decoded = tf.keras.layers.Dense(16, activation='relu')(encoded)
        decoded = tf.keras.layers.Dense(32, activation='relu')(decoded)
        decoded = tf.keras.layers.Dense(input_dim, activation='sigmoid')(decoded)
        
        autoencoder = tf.keras.Model(input_layer, decoded)
        autoencoder.compile(optimizer='adam', loss='mse')
        
        return autoencoder
    
    def train(self, normal_transactions):
        """Train on legitimate user behavior"""
        # Train Isolation Forest
        self.isolation_forest.fit(normal_transactions)
        
        # Train Autoencoder
        self.autoencoder.fit(
            normal_transactions,
            normal_transactions,
            epochs=50,
            batch_size=32,
            validation_split=0.2,
            verbose=0
        )
    
    def predict_anomaly_score(self, transaction_features):
        """Return anomaly score (0 = normal, 1 = anomalous)"""
        # Isolation Forest score
        if_score = self.isolation_forest.score_samples([transaction_features])[0]
        if_score = 1 / (1 + np.exp(if_score))  # Normalize to 0-1
        
        # Autoencoder reconstruction error
        reconstruction = self.autoencoder.predict([transaction_features], verbose=0)
        ae_score = np.mean((transaction_features - reconstruction) ** 2)
        ae_score = min(ae_score / 0.1, 1.0)  # Normalize to 0-1
        
        # Ensemble score
        final_score = 0.6 * if_score + 0.4 * ae_score
        
        return final_score
```

**Training Strategy:**
- Train per-user models (personalized behavior)
- Retrain weekly with new data
- Cold start: Use population-level model for new users

---

### Model 3: Network Analysis (Fraud Ring Detection)

**Purpose:** Identify fraud rings, money mules, and suspicious transaction networks

**Graph Structure:**
```python
# Nodes: Users
# Edges: Transactions between users
# Node features: User attributes
# Edge features: Transaction attributes

{
    "nodes": [
        {
            "id": "user_abc",
            "features": {
                "account_age_days": 365,
                "total_transactions": 100,
                "total_volume": 10000.00,
                "avg_transaction_amount": 100.00,
                "num_unique_recipients": 20,
                "fraud_history": 0  # 0 = clean, 1 = flagged before
            }
        },
        ...
    ],
    "edges": [
        {
            "source": "user_abc",
            "target": "user_def",
            "features": {
                "num_transactions": 5,
                "total_amount": 500.00,
                "avg_amount": 100.00,
                "first_transaction_date": "2024-01-01",
                "last_transaction_date": "2024-01-15"
            }
        },
        ...
    ]
}
```

**Model: Graph Neural Network (GNN)**

```python
import torch
import torch.nn.functional as F
from torch_geometric.nn import GCNConv, global_mean_pool

class FraudDetectionGNN(torch.nn.Module):
    def __init__(self, num_node_features, num_edge_features):
        super(FraudDetectionGNN, self).__init__()
        
        # Graph convolutional layers
        self.conv1 = GCNConv(num_node_features, 64)
        self.conv2 = GCNConv(64, 32)
        self.conv3 = GCNConv(32, 16)
        
        # Edge feature processing
        self.edge_mlp = torch.nn.Sequential(
            torch.nn.Linear(num_edge_features, 32),
            torch.nn.ReLU(),
            torch.nn.Linear(32, 16)
        )
        
        # Final classification layers
        self.fc1 = torch.nn.Linear(16, 8)
        self.fc2 = torch.nn.Linear(8, 1)
    
    def forward(self, data):
        x, edge_index, edge_attr = data.x, data.edge_index, data.edge_attr
        
        # Process edge features
        edge_features = self.edge_mlp(edge_attr)
        
        # Graph convolutions
        x = self.conv1(x, edge_index)
        x = F.relu(x)
        x = F.dropout(x, p=0.3, training=self.training)
        
        x = self.conv2(x, edge_index)
        x = F.relu(x)
        x = F.dropout(x, p=0.3, training=self.training)
        
        x = self.conv3(x, edge_index)
        
        # Final classification
        x = F.relu(self.fc1(x))
        x = torch.sigmoid(self.fc2(x))
        
        return x

# Training
model = FraudDetectionGNN(num_node_features=6, num_edge_features=5)
optimizer = torch.optim.Adam(model.parameters(), lr=0.001)
criterion = torch.nn.BCELoss()

def train_gnn(model, data_loader, epochs=100):
    model.train()
    for epoch in range(epochs):
        total_loss = 0
        for batch in data_loader:
            optimizer.zero_grad()
            out = model(batch)
            loss = criterion(out, batch.y)
            loss.backward()
            optimizer.step()
            total_loss += loss.item()
        
        if epoch % 10 == 0:
            print(f'Epoch {epoch}, Loss: {total_loss / len(data_loader):.4f}')
```

**Fraud Patterns to Detect:**
1. **Circular transactions:** A → B → C → A (money laundering)
2. **Star pattern:** One user receiving from many (money mule)
3. **Rapid account creation:** Multiple new accounts transacting together
4. **Unusual velocity:** High transaction frequency in short time

---

## 3. Physics Seal Validation (AI Perspective)

### What You Need to Validate

Nyasha's mobile app captures physics seals. Your job is to validate them server-side using ML.

**Physics Seal Structure:**
```python
{
    "motion_signature": [0.12, 0.34, ...],  # 32 FFT coefficients
    "rotation_signature": [0.01, 0.02, ...],  # 16 gyro values
    "location": {
        "latitude": -26.2041,
        "longitude": 28.0473,
        "accuracy": 5.0
    },
    "timestamp": 1699564800000,
    "device_attestation": "base64_encoded_safetynet_response",
    "sensor_accuracy": 0.95
}
```

### Validation Checks (Your Implementation)

```python
class PhysicsSealValidator:
    def __init__(self):
        self.ml_model = load_model('physics_seal_classifier.h5')
        self.location_history_db = LocationHistoryDB()
        self.seal_hash_db = SealHashDB()
    
    async def validate(self, seal: PhysicsSeal, user_id: str) -> ValidationResult:
        checks = []
        
        # 1. ML-based authenticity check
        authenticity_score = self.ml_model.predict(seal.to_features())
        checks.append({
            'name': 'ml_authenticity',
            'score': authenticity_score,
            'passed': authenticity_score > 0.95
        })
        
        # 2. Motion plausibility check
        motion_plausible = self.check_motion_plausibility(seal.motion_signature)
        checks.append({
            'name': 'motion_plausibility',
            'passed': motion_plausible
        })
        
        # 3. Location consistency check
        location_consistent = await self.check_location_consistency(
            seal.location,
            user_id
        )
        checks.append({
            'name': 'location_consistency',
            'score': location_consistent,
            'passed': location_consistent > 0.7
        })
        
        # 4. Timestamp freshness check
        timestamp_valid = self.check_timestamp_freshness(seal.timestamp)
        checks.append({
            'name': 'timestamp_freshness',
            'passed': timestamp_valid
        })
        
        # 5. Replay attack check
        seal_hash = compute_hash(seal)
        is_duplicate = await self.seal_hash_db.exists(seal_hash)
        checks.append({
            'name': 'replay_check',
            'passed': not is_duplicate
        })
        
        # 6. Device attestation check
        attestation_valid = await self.verify_device_attestation(
            seal.device_attestation
        )
        checks.append({
            'name': 'device_attestation',
            'passed': attestation_valid
        })
        
        # Calculate overall confidence
        passed_checks = sum(1 for c in checks if c['passed'])
        confidence = passed_checks / len(checks)
        
        # Store seal hash if valid
        if confidence > 0.8:
            await self.seal_hash_db.store(seal_hash, user_id)
        
        return ValidationResult(
            valid=confidence > 0.8,
            confidence=confidence,
            checks=checks
        )
    
    def check_motion_plausibility(self, motion_signature: List[float]) -> bool:
        """Check if motion signature is physically plausible"""
        # Check magnitude (human shake is typically 0.5-3.0 g)
        magnitude = np.linalg.norm(motion_signature)
        if magnitude < 0.5 or magnitude > 3.0:
            return False
        
        # Check frequency distribution (human shake is 2-8 Hz)
        dominant_freq = np.argmax(motion_signature)
        if dominant_freq < 2 or dominant_freq > 8:
            return False
        
        return True
    
    async def check_location_consistency(
        self,
        location: Location,
        user_id: str
    ) -> float:
        """Check if location is consistent with user's history"""
        # Get user's typical locations
        typical_locations = await self.location_history_db.get_typical_locations(user_id)
        
        if not typical_locations:
            return 0.5  # Neutral score for new users
        
        # Calculate distance to nearest typical location
        min_distance = min(
            haversine_distance(location, typical_loc)
            for typical_loc in typical_locations
        )
        
        # Score based on distance (0 = far, 1 = close)
        if min_distance < 1:  # Within 1 km
            return 1.0
        elif min_distance < 10:  # Within 10 km
            return 0.8
        elif min_distance < 50:  # Within 50 km
            return 0.5
        else:
            return 0.2  # Suspicious (>50 km from typical locations)
```

---

## 4. Banking System Integration

### Your Responsibility: FNB Integration

You have insider knowledge of FNB's systems. Use it to design the integration.

**FNB API Endpoints (Hypothetical):**
```
POST /api/v1/payments
GET /api/v1/accounts/{accountId}/balance
GET /api/v1/payments/{paymentId}/status
POST /api/v1/fraud-check
```

### Integration Architecture

```python
class FNBIntegration:
    def __init__(self):
        self.base_url = "https://api.fnb.co.za"
        self.api_key = os.getenv("FNB_API_KEY")
        self.client_id = os.getenv("FNB_CLIENT_ID")
        self.session = requests.Session()
        self.session.headers.update({
            'Authorization': f'Bearer {self.api_key}',
            'X-Client-ID': self.client_id,
            'Content-Type': 'application/json'
        })
    
    async def check_balance(self, account_id: str) -> float:
        """Check user's account balance"""
        response = self.session.get(
            f"{self.base_url}/api/v1/accounts/{account_id}/balance"
        )
        response.raise_for_status()
        data = response.json()
        return data['availableBalance']
    
    async def submit_payment(self, payment: Payment) -> PaymentResult:
        """Submit payment to FNB for processing"""
        payload = {
            'fromAccount': payment.sender_account_id,
            'toAccount': payment.recipient_account_id,
            'amount': payment.amount,
            'currency': 'ZAR',
            'reference': payment.transaction_id,
            'metadata': {
                'provider': 'mari-protocol',
                'physicsSealHash': payment.physics_seal_hash,
                'sentinelScore': payment.sentinel_score
            }
        }
        
        response = self.session.post(
            f"{self.base_url}/api/v1/payments",
            json=payload,
            headers={'X-Idempotency-Key': payment.transaction_id}
        )
        response.raise_for_status()
        data = response.json()
        
        return PaymentResult(
            success=data['status'] == 'COMPLETED',
            payment_id=data['paymentId'],
            status=data['status'],
            timestamp=data['timestamp']
        )
    
    async def submit_fraud_alert(self, transaction_id: str, reason: str):
        """Submit suspicious activity report to FNB"""
        payload = {
            'transactionId': transaction_id,
            'reason': reason,
            'severity': 'HIGH',
            'source': 'mari-sentinel'
        }
        
        response = self.session.post(
            f"{self.base_url}/api/v1/fraud-check",
            json=payload
        )
        response.raise_for_status()
```

### Bank Partnership Strategy

**Phase 1: FNB Pilot (Your Entry Point)**
- Leverage your FNB connection
- Start with 10K users
- Prove fraud reduction (target: 80-90%)
- Build case study for other banks

**Phase 2: Digital-First Banks**
- Capitec (20M users, tech-forward)
- TymeBank (3M users, mobile-first)
- Bank Zero (1M users, digital-native)

**Phase 3: Traditional Banks**
- Standard Bank (12M users)
- ABSA (10M users)
- Nedbank (8M users)

**Your Pitch to Banks:**
1. **Fraud reduction:** 80-90% reduction in fraud losses
2. **Cost savings:** R35M per 1M transactions
3. **Instant authorization:** No more 1-3 day delays
4. **Regulatory compliance:** FICA/POPIA compliant
5. **Competitive advantage:** Offer instant payments before competitors

---

## 5. Data Pipeline & Feature Engineering

### Real-Time Data Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                    Data Sources                              │
│  • Transaction events (Kafka topic: transactions)           │
│  • Physics seals (Kafka topic: physics-seals)               │
│  • User events (Kafka topic: user-events)                   │
│  • Bank responses (Kafka topic: bank-responses)             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Apache Kafka (Message Broker)                   │
│  • Partitioned by user_id for parallelism                   │
│  • Retention: 7 days                                         │
│  • Replication factor: 3                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│           Stream Processing (Apache Flink)                   │
│  • Feature extraction                                        │
│  • Real-time aggregations                                    │
│  • Windowed computations                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Feature Store (Redis + PostgreSQL)              │
│  • Hot features (Redis): Last 24 hours                      │
│  • Cold features (PostgreSQL): Historical data              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                 ML Model Serving                             │
│  • TensorFlow Serving / Seldon                              │
│  • <50ms inference latency                                   │
└─────────────────────────────────────────────────────────────┘
```

### Feature Engineering Code

```python
from pyspark.sql import SparkSession
from pyspark.sql.functions import *
from pyspark.sql.window import Window

class FeatureEngineer:
    def __init__(self):
        self.spark = SparkSession.builder \
            .appName("Mari Feature Engineering") \
            .getOrCreate()
    
    def compute_user_features(self, transactions_df):
        """Compute user-level features from transaction history"""
        
        # Window for last 30 days
        window_30d = Window.partitionBy("user_id") \
            .orderBy("timestamp") \
            .rangeBetween(-30*24*3600, 0)
        
        features = transactions_df.groupBy("user_id").agg(
            # Transaction statistics
            count("*").alias("total_transactions"),
            sum("amount").alias("total_volume"),
            avg("amount").alias("avg_transaction_amount"),
            stddev("amount").alias("std_transaction_amount"),
            max("amount").alias("max_transaction_amount"),
            
            # Temporal features
            countDistinct(hour("timestamp")).alias("active_hours"),
            countDistinct(dayofweek("timestamp")).alias("active_days"),
            
            # Recipient features
            countDistinct("recipient_id").alias("num_unique_recipients"),
            
            # Velocity features (last 30 days)
            sum(when(col("timestamp") > unix_timestamp() - 30*24*3600, 1).otherwise(0))
                .alias("transactions_last_30d"),
            sum(when(col("timestamp") > unix_timestamp() - 30*24*3600, col("amount")).otherwise(0))
                .alias("volume_last_30d")
        )
        
        return features
    
    def compute_transaction_features(self, transaction, user_history):
        """Compute features for a single transaction"""
        
        features = {
            # Amount features
            "amount": transaction.amount,
            "amount_zscore": (transaction.amount - user_history.avg_amount) / user_history.std_amount,
            "amount_percentile": self.compute_percentile(transaction.amount, user_history.amounts),
            
            # Temporal features
            "hour_of_day": transaction.timestamp.hour,
            "day_of_week": transaction.timestamp.weekday(),
            "is_weekend": transaction.timestamp.weekday() >= 5,
            "time_since_last_tx": (transaction.timestamp - user_history.last_tx_timestamp).seconds,
            
            # Location features
            "distance_from_home": haversine_distance(transaction.location, user_history.home_location),
            "distance_from_last_tx": haversine_distance(transaction.location, user_history.last_tx_location),
            
            # Recipient features
            "is_new_recipient": transaction.recipient_id not in user_history.recipients,
            "recipient_frequency": user_history.recipient_counts.get(transaction.recipient_id, 0),
            
            # Velocity features
            "transactions_last_hour": user_history.count_last_hour,
            "transactions_last_day": user_history.count_last_day,
            "amount_last_hour": user_history.amount_last_hour,
            "amount_last_day": user_history.amount_last_day
        }
        
        return features
```

---


## 6. Regulatory Compliance (FICA, POPIA, SARB)

### FICA (Financial Intelligence Centre Act)

**Your Responsibilities:**
1. **Suspicious Activity Reporting (SAR)**
2. **Customer Due Diligence (CDD)**
3. **Record Keeping**
4. **AML/CFT Compliance**

**Implementation:**

```python
class FICACompliance:
    def __init__(self):
        self.fic_api = FICAPIClient()
        self.threshold_high_value = 25000  # R25K triggers enhanced monitoring
        self.threshold_sar = 50000  # R50K triggers SAR consideration
    
    async def check_transaction(self, transaction: Transaction) -> ComplianceResult:
        checks = []
        
        # 1. High-value transaction check
        if transaction.amount >= self.threshold_high_value:
            checks.append({
                'type': 'HIGH_VALUE',
                'action': 'ENHANCED_MONITORING',
                'details': f'Transaction amount R{transaction.amount} exceeds R25K threshold'
            })
        
        # 2. Suspicious pattern detection
        suspicious_patterns = await self.detect_suspicious_patterns(transaction)
        if suspicious_patterns:
            checks.append({
                'type': 'SUSPICIOUS_PATTERN',
                'action': 'REVIEW_REQUIRED',
                'patterns': suspicious_patterns
            })
        
        # 3. SAR threshold check
        if transaction.amount >= self.threshold_sar:
            sar_required = await self.evaluate_sar_requirement(transaction)
            if sar_required:
                await self.submit_sar(transaction)
                checks.append({
                    'type': 'SAR_SUBMITTED',
                    'action': 'REPORTED_TO_FIC',
                    'reference': sar_reference
                })
        
        return ComplianceResult(checks=checks)
    
    async def detect_suspicious_patterns(self, transaction: Transaction) -> List[str]:
        """Detect patterns that may indicate money laundering"""
        patterns = []
        
        # Pattern 1: Structuring (multiple transactions just below reporting threshold)
        recent_txs = await self.get_recent_transactions(transaction.sender_id, hours=24)
        if len(recent_txs) > 5 and all(tx.amount < 25000 for tx in recent_txs):
            total = sum(tx.amount for tx in recent_txs)
            if total > 100000:
                patterns.append('STRUCTURING')
        
        # Pattern 2: Rapid movement (money in, money out)
        if await self.check_rapid_movement(transaction):
            patterns.append('RAPID_MOVEMENT')
        
        # Pattern 3: Circular transactions
        if await self.check_circular_transactions(transaction):
            patterns.append('CIRCULAR_TRANSACTIONS')
        
        # Pattern 4: Unusual recipient (high-risk country, sanctioned entity)
        if await self.check_high_risk_recipient(transaction.recipient_id):
            patterns.append('HIGH_RISK_RECIPIENT')
        
        return patterns
    
    async def submit_sar(self, transaction: Transaction) -> str:
        """Submit Suspicious Activity Report to FIC"""
        sar = {
            'reportingEntity': 'Mari Protocol',
            'transactionId': transaction.id,
            'amount': transaction.amount,
            'currency': 'ZAR',
            'suspiciousIndicators': await self.get_suspicious_indicators(transaction),
            'narrative': await self.generate_sar_narrative(transaction),
            'timestamp': datetime.utcnow().isoformat()
        }
        
        response = await self.fic_api.submit_sar(sar)
        return response['reference']
```

### POPIA (Protection of Personal Information Act)

**Data Minimization:**
```python
class POPIACompliance:
    @staticmethod
    def hash_pii(phone_number: str) -> str:
        """Hash phone numbers (PII) before storage"""
        return hashlib.sha256(phone_number.encode()).hexdigest()
    
    @staticmethod
    def anonymize_location(lat: float, lng: float, precision: int = 3) -> tuple:
        """Reduce GPS precision for privacy"""
        return (round(lat, precision), round(lng, precision))
    
    @staticmethod
    def apply_retention_policy(data: dict) -> dict:
        """Apply data retention limits"""
        # Transaction data: 7 years (FICA requirement)
        # Physics seals: 90 days (operational need only)
        # User profiles: Until account closure + 7 years
        
        if data['type'] == 'physics_seal':
            if (datetime.now() - data['created_at']).days > 90:
                return None  # Delete
        
        return data
```

**User Rights Implementation:**
```python
class UserDataRights:
    async def handle_data_request(self, user_id: str, request_type: str):
        """Handle POPIA data subject requests"""
        
        if request_type == 'ACCESS':
            # Right to access: Provide all data we have
            return await self.export_user_data(user_id)
        
        elif request_type == 'RECTIFICATION':
            # Right to correct: Allow user to update their data
            return await self.update_user_data(user_id)
        
        elif request_type == 'ERASURE':
            # Right to be forgotten: Delete user data
            # Exception: Must retain for 7 years per FICA
            return await self.anonymize_user_data(user_id)
        
        elif request_type == 'PORTABILITY':
            # Right to data portability: Export in machine-readable format
            return await self.export_user_data(user_id, format='json')
```

### SARB (South African Reserve Bank) Compliance

**Payment System Authorization:**
- Apply for SARB sandbox participation
- Demonstrate compliance with National Payment System Act
- Prove operational resilience
- Show consumer protection measures

**Reporting Requirements:**
```python
class SARBReporting:
    async def generate_monthly_report(self, month: int, year: int):
        """Generate monthly report for SARB"""
        return {
            'reportingPeriod': f'{year}-{month:02d}',
            'totalTransactions': await self.count_transactions(month, year),
            'totalVolume': await self.sum_transaction_volume(month, year),
            'averageTransactionAmount': await self.avg_transaction_amount(month, year),
            'fraudRate': await self.calculate_fraud_rate(month, year),
            'systemUptime': await self.calculate_uptime(month, year),
            'incidentCount': await self.count_incidents(month, year),
            'userComplaints': await self.count_complaints(month, year)
        }
```

---

## 7. Security Threat Analysis

### Threat Matrix (Your Focus)

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|------------|
| Account Takeover | High | High | Behavioral biometrics, device attestation |
| Synthetic Identity Fraud | Medium | High | KYC verification, network analysis |
| Money Laundering | Medium | Critical | Transaction monitoring, SAR reporting |
| Fraud Rings | Medium | High | Graph neural networks, pattern detection |
| Insider Threat (Bank Employee) | Low | Critical | Audit logging, access controls |
| Data Breach | Low | Critical | Encryption, HSM, security audits |

### Attack Scenarios & Detection

**Scenario 1: Account Takeover**
```
Attacker steals user's phone → Tries to make payment
Detection:
- Behavioral biometrics detects unusual pattern
- Location anomaly (attacker in different city)
- Device attestation may fail (if phone rooted)
- Physics seal may be unusual (different shake pattern)
Response: Block transaction, alert user, require re-authentication
```

**Scenario 2: Fraud Ring**
```
10 new accounts created → All transact with each other → Money exits to single account
Detection:
- Graph neural network detects star pattern
- Rapid account creation flagged
- Circular transaction pattern detected
- Velocity checks trigger (too many transactions too fast)
Response: Flag all accounts, submit SAR, freeze suspicious accounts
```

**Scenario 3: Money Laundering (Structuring)**
```
User makes 20 transactions of R24K each (just below R25K threshold) in 24 hours
Detection:
- Structuring pattern detected
- Total volume exceeds R100K threshold
- Unusual transaction frequency
Response: Enhanced monitoring, possible SAR submission
```

---

## 8. Real-Time Scoring Engine

### Ensemble Model Architecture

```python
class SentinelScoringEngine:
    def __init__(self):
        self.physics_model = load_model('physics_seal_classifier')
        self.behavioral_model = BehavioralBiometricsModel()
        self.network_model = load_model('fraud_ring_detector')
        self.rule_engine = RuleEngine()
    
    async def score_transaction(self, transaction: Transaction) -> ScoringResult:
        # Run all models in parallel
        results = await asyncio.gather(
            self.score_physics_seal(transaction.physics_seal),
            self.score_behavioral(transaction),
            self.score_network(transaction),
            self.apply_rules(transaction)
        )
        
        physics_score, behavioral_score, network_score, rule_score = results
        
        # Weighted ensemble
        final_score = (
            physics_score * 0.4 +
            behavioral_score * 0.3 +
            network_score * 0.2 +
            rule_score * 0.1
        )
        
        # Decision logic
        if final_score >= 0.95:
            decision = 'APPROVE'
            confidence = final_score
        elif final_score >= 0.80:
            decision = 'REVIEW'
            confidence = final_score
        else:
            decision = 'DECLINE'
            confidence = 1 - final_score
        
        return ScoringResult(
            score=final_score,
            decision=decision,
            confidence=confidence,
            breakdown={
                'physics': physics_score,
                'behavioral': behavioral_score,
                'network': network_score,
                'rules': rule_score
            },
            processing_time_ms=self.get_processing_time()
        )
```

### Performance Requirements

- **Latency:** <200ms (p95)
- **Throughput:** 10,000 TPS
- **Accuracy:** >99.5%
- **False Positive Rate:** <0.1%
- **Uptime:** >99.9%

---

## 9. Explainable AI & Reporting

### Why Explainability Matters

Banks and regulators need to understand WHY a transaction was flagged. "Black box" ML is not acceptable.

### SHAP (SHapley Additive exPlanations)

```python
import shap

class ExplainableAI:
    def __init__(self, model):
        self.model = model
        self.explainer = shap.TreeExplainer(model)  # For tree-based models
    
    def explain_prediction(self, transaction_features):
        """Generate explanation for a single prediction"""
        shap_values = self.explainer.shap_values(transaction_features)
        
        # Get top contributing features
        feature_importance = sorted(
            zip(transaction_features.columns, shap_values[0]),
            key=lambda x: abs(x[1]),
            reverse=True
        )[:5]
        
        explanation = {
            'prediction': self.model.predict([transaction_features])[0],
            'top_factors': [
                {
                    'feature': feature,
                    'contribution': float(contribution),
                    'direction': 'increases_risk' if contribution > 0 else 'decreases_risk'
                }
                for feature, contribution in feature_importance
            ]
        }
        
        return explanation

# Example output:
{
    'prediction': 0.92,  # 92% fraud probability
    'top_factors': [
        {'feature': 'amount_zscore', 'contribution': 0.35, 'direction': 'increases_risk'},
        {'feature': 'is_new_recipient', 'contribution': 0.25, 'direction': 'increases_risk'},
        {'feature': 'distance_from_home', 'contribution': 0.15, 'direction': 'increases_risk'},
        {'feature': 'time_since_last_tx', 'contribution': -0.10, 'direction': 'decreases_risk'},
        {'feature': 'recipient_frequency', 'contribution': -0.08, 'direction': 'decreases_risk'}
    ]
}
```

### Fraud Investigation Dashboard

```python
class FraudDashboard:
    def generate_investigation_report(self, transaction_id: str):
        """Generate detailed report for fraud investigators"""
        
        transaction = self.get_transaction(transaction_id)
        user = self.get_user(transaction.sender_id)
        
        report = {
            'transaction': {
                'id': transaction.id,
                'amount': transaction.amount,
                'timestamp': transaction.timestamp,
                'status': transaction.status
            },
            'user_profile': {
                'account_age_days': user.account_age_days,
                'total_transactions': user.total_transactions,
                'fraud_history': user.fraud_history
            },
            'sentinel_analysis': {
                'overall_score': transaction.sentinel_score,
                'physics_seal_score': transaction.physics_score,
                'behavioral_score': transaction.behavioral_score,
                'network_score': transaction.network_score,
                'explanation': self.explainer.explain(transaction)
            },
            'similar_cases': self.find_similar_fraud_cases(transaction),
            'recommended_action': self.recommend_action(transaction)
        }
        
        return report
```

---

## 10. Your Development Roadmap

### Month 1-3: Foundation
- [ ] Set up ML infrastructure (Kubeflow, MLflow)
- [ ] Build physics seal validation model (99% accuracy)
- [ ] Implement basic behavioral biometrics
- [ ] FNB API integration (test environment)
- [ ] FICA/POPIA compliance framework

### Month 4-6: Pilot
- [ ] Deploy Sentinel to production
- [ ] FNB pilot launch (10K users)
- [ ] Real-time monitoring dashboard
- [ ] Fraud investigation tools
- [ ] Model performance tracking

### Month 7-12: Scale
- [ ] Advanced fraud detection (GNN for fraud rings)
- [ ] Multi-bank integration (Capitec, TymeBank)
- [ ] Automated SAR generation
- [ ] Model retraining pipeline
- [ ] 100K users, <0.1% fraud rate

### Month 13-18: Optimize
- [ ] 99.5% accuracy achieved
- [ ] 5 bank partnerships
- [ ] Explainable AI dashboard
- [ ] Predictive fraud prevention
- [ ] 1M users, R100M monthly volume

---

## Key Success Metrics

**Model Performance:**
- Physics seal validation: >99.5% accuracy
- Behavioral biometrics: <0.1% false positive rate
- Network analysis: >95% fraud ring detection
- Overall fraud rate: <0.1% (vs 2-3% industry average)

**Business Impact:**
- Fraud reduction: 80-90% vs baseline
- Cost savings: R35M per 1M transactions
- Bank satisfaction: >90% (NPS score)
- Regulatory compliance: 100% (zero violations)

**Operational:**
- Latency: <200ms (p95)
- Uptime: >99.9%
- SAR accuracy: >95% (no false SARs)
- Model drift detection: <5% degradation before retraining

---

## Conclusion

You're building the AI brain of Mari Protocol. Your Sentinel platform will:

1. **Protect users** from fraud and account takeover
2. **Save banks** R35M+ per 1M transactions
3. **Enable instant payments** through trusted fraud detection
4. **Ensure compliance** with FICA, POPIA, and SARB regulations
5. **Scale to millions** of users across Africa

**Your FNB connection is your superpower. Use it to:**
- Get early access to bank APIs
- Understand fraud patterns from insider perspective
- Build credibility with other banks
- Design integration that banks actually want

**This is not just ML engineering. You're building the security infrastructure for Africa's digital payments.**

**Let's make fraud impossible.** 🛡️

---

**Document Version:** 1.0  
**Last Updated:** 2024-01-15  
**Owner:** Dibanisa Fakude (Chief AI Officer)  
**Status:** Living Document
