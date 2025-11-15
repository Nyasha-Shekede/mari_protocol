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

