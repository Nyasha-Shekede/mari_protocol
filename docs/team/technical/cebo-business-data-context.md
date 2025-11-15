# Mari Protocol: Business/Data Context for Cebolenkosi Chamane
**Co-Founder & Chief Business Officer**

---

## Your Technical Domain: Data Architecture + Business Operations

This document contains everything you need to build Mari's data infrastructure, analytics platform, and business intelligence systems. Your focus is data engineering, business metrics, and operational excellence - not cryptography or AI/ML.

---

## Table of Contents

1. Data Architecture Overview
2. Database Design & Schema
3. Analytics Platform & BI Tools
4. Business Metrics & KPIs
5. Data Pipeline Implementation
6. API Design for Business Logic
7. Financial Modeling & Reporting
8. Partnership Integration
9. User Growth & Retention Analytics
10. Operational Dashboards

---

## 1. Data Architecture Overview

### Your Responsibility: The Data Layer

```
┌─────────────────────────────────────────────────────────────┐
│                    Data Sources                              │
│  • Mobile app events                                         │
│  • Server API logs                                           │
│  • Bank transaction responses                                │
│  • User behavior data                                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Ingestion Layer (YOUR DOMAIN)                   │
│  • Apache Kafka (real-time events)                          │
│  • Apache Airflow (batch ETL)                               │
│  • Data validation & cleaning                                │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Storage Layer (YOUR DOMAIN)                     │
│  • PostgreSQL (transactional data)                          │
│  • ClickHouse (analytics data)                              │
│  • MinIO/S3 (data lake)                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Analytics Layer (YOUR DOMAIN)                   │
│  • Metabase (BI dashboards)                                 │
│  • Grafana (operational metrics)                            │
│  • JupyterHub (data science notebooks)                      │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack (Your Choices)

**Data Engineering:**
- Python 3.11 (data processing)
- Apache Kafka (streaming)
- Apache Airflow (orchestration)
- Apache Spark (big data processing)
- dbt (data transformation)

**Databases:**
- PostgreSQL 15 (OLTP)
- ClickHouse (OLAP)
- Redis (caching)
- MinIO (object storage)

**Analytics:**
- Metabase (self-service BI)
- Grafana (real-time dashboards)
- JupyterHub (ad-hoc analysis)
- Apache Superset (advanced viz)

**Business Tools:**
- Google Sheets / Excel (financial modeling)
- Causal (scenario planning)
- HubSpot (CRM for bank partnerships)
- Notion (documentation)

---

## 2. Database Design & Schema

### Core Tables (Your Design)

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number_hash VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 hash
    blood_hash VARCHAR(16) NOT NULL UNIQUE,         -- Public identifier
    device_public_key TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_active TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20) DEFAULT 'active',
    
    -- Metadata
    signup_source VARCHAR(50),  -- 'organic', 'referral', 'bank_promotion'
    referrer_user_id UUID REFERENCES users(id),
    
    -- Indexes
    INDEX idx_blood_hash (blood_hash),
    INDEX idx_phone_hash (phone_number_hash),
    INDEX idx_created_at (created_at)
);

-- Transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id),
    recipient_id UUID NOT NULL REFERENCES users(id),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'ZAR',
    status VARCHAR(20) DEFAULT 'PENDING',
    
    -- Physics seal reference
    physics_seal_hash VARCHAR(64) NOT NULL UNIQUE,
    
    -- Bank integration
    bank_id VARCHAR(50),
    bank_reference VARCHAR(100),
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    
    -- Metadata
    channel VARCHAR(20),  -- 'online', 'offline_sms'
    device_type VARCHAR(50),
    app_version VARCHAR(20),
    
    -- Indexes
    INDEX idx_sender (sender_id),
    INDEX idx_recipient (recipient_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_bank (bank_id)
);

-- Daily metrics (aggregated for fast queries)
CREATE TABLE daily_metrics (
    date DATE PRIMARY KEY,
    
    -- Transaction metrics
    total_transactions BIGINT NOT NULL,
    successful_transactions BIGINT NOT NULL,
    failed_transactions BIGINT NOT NULL,
    total_volume DECIMAL(20,2) NOT NULL,
    
    -- User metrics
    active_users INTEGER NOT NULL,
    new_users INTEGER NOT NULL,
    churned_users INTEGER NOT NULL,
    
    -- Performance metrics
    avg_transaction_amount DECIMAL(10,2),
    median_transaction_amount DECIMAL(10,2),
    p95_processing_time_ms INTEGER,
    
    -- Business metrics
    revenue DECIMAL(15,2),  -- R0.10 per transaction
    cost DECIMAL(15,2),     -- Infrastructure + operations
    gross_profit DECIMAL(15,2),
    
    -- Quality metrics
    fraud_rate DECIMAL(5,4),
    success_rate DECIMAL(5,4),
    uptime_percentage DECIMAL(5,2),
    
    INDEX idx_date (date)
);

-- User cohorts (for retention analysis)
CREATE TABLE user_cohorts (
    cohort_month DATE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    
    -- Cohort metrics
    first_transaction_date DATE,
    transactions_month_0 INTEGER DEFAULT 0,
    transactions_month_1 INTEGER DEFAULT 0,
    transactions_month_2 INTEGER DEFAULT 0,
    transactions_month_3 INTEGER DEFAULT 0,
    
    PRIMARY KEY (cohort_month, user_id),
    INDEX idx_cohort_month (cohort_month)
);

-- Bank partnerships
CREATE TABLE bank_partnerships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) DEFAULT 'active',  -- 'pilot', 'active', 'inactive'
    
    -- Contract details
    contract_start_date DATE NOT NULL,
    contract_end_date DATE,
    monthly_fee DECIMAL(10,2),
    transaction_fee_percentage DECIMAL(5,4),
    
    -- Integration details
    api_endpoint VARCHAR(255),
    api_key_hash VARCHAR(64),
    
    -- Metrics
    total_users INTEGER DEFAULT 0,
    total_transactions BIGINT DEFAULT 0,
    total_volume DECIMAL(20,2) DEFAULT 0,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Analytics Schema (ClickHouse for OLAP)

```sql
-- Transaction events (append-only, optimized for analytics)
CREATE TABLE transaction_events (
    event_id UUID,
    transaction_id UUID,
    user_id UUID,
    event_type String,  -- 'initiated', 'physics_seal_captured', 'submitted', 'completed', 'failed'
    timestamp DateTime,
    
    -- Dimensions
    amount Decimal(15,2),
    currency String,
    bank_id String,
    device_type String,
    channel String,
    
    -- Metrics
    processing_time_ms UInt32,
    sentinel_score Float32,
    
    -- Partitioning for performance
    date Date MATERIALIZED toDate(timestamp)
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (date, user_id, timestamp);

-- User activity (for behavioral analysis)
CREATE TABLE user_activity (
    user_id UUID,
    activity_type String,  -- 'login', 'transaction', 'qr_scan', 'balance_check'
    timestamp DateTime,
    
    -- Context
    device_type String,
    app_version String,
    location_hash String,
    
    date Date MATERIALIZED toDate(timestamp)
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (date, user_id, timestamp);
```

---

## 3. Analytics Platform & BI Tools

### Metabase Setup (Your Primary BI Tool)

**Installation:**
```bash
docker run -d -p 3000:3000 \
  --name metabase \
  -e MB_DB_TYPE=postgres \
  -e MB_DB_DBNAME=metabase \
  -e MB_DB_PORT=5432 \
  -e MB_DB_USER=metabase \
  -e MB_DB_PASS=secure_password \
  -e MB_DB_HOST=postgres \
  metabase/metabase
```

**Key Dashboards to Build:**

1. **Executive Dashboard**
   - Total users (current + growth rate)
   - Monthly transaction volume
   - Monthly revenue
   - Key metrics vs targets

2. **Operations Dashboard**
   - Real-time transaction count
   - Success rate (last 24 hours)
   - Average processing time
   - Error rate by type

3. **Growth Dashboard**
   - New user signups (daily/weekly/monthly)
   - User activation rate
   - Retention cohorts
   - Referral program performance

4. **Bank Partnership Dashboard**
   - Transactions per bank
   - Volume per bank
   - Revenue per bank
   - Bank satisfaction metrics

### Sample Metabase Queries

```sql
-- Daily Active Users (DAU)
SELECT 
    DATE(created_at) as date,
    COUNT(DISTINCT sender_id) as dau
FROM transactions
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY date;

-- Monthly Recurring Revenue (MRR)
SELECT 
    DATE_TRUNC('month', created_at) as month,
    COUNT(*) * 0.10 as revenue  -- R0.10 per transaction
FROM transactions
WHERE status = 'COMPLETED'
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY month;

-- User Retention Cohort
SELECT 
    cohort_month,
    COUNT(DISTINCT user_id) as cohort_size,
    AVG(transactions_month_0) as month_0_avg,
    AVG(transactions_month_1) as month_1_avg,
    AVG(transactions_month_2) as month_2_avg,
    AVG(transactions_month_3) as month_3_avg
FROM user_cohorts
GROUP BY cohort_month
ORDER BY cohort_month DESC;

-- Top Recipients (Merchants)
SELECT 
    u.blood_hash,
    COUNT(*) as transaction_count,
    SUM(t.amount) as total_volume,
    AVG(t.amount) as avg_transaction
FROM transactions t
JOIN users u ON t.recipient_id = u.id
WHERE t.created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY u.blood_hash
ORDER BY transaction_count DESC
LIMIT 20;
```

---

## 4. Business Metrics & KPIs

### North Star Metric: Monthly Transaction Volume

**Formula:**
```
Monthly Transaction Volume = SUM(transaction.amount WHERE status = 'COMPLETED' AND month = current_month)
```

**Target:** R10M (Month 12), R100M (Month 18), R1B (Month 24)

### Key Performance Indicators (Your Dashboard)

**Growth Metrics:**
```python
# Monthly Active Users (MAU)
mau = count_distinct(users WHERE last_active >= start_of_month)

# User Growth Rate
growth_rate = (mau_current - mau_previous) / mau_previous * 100

# New User Acquisition
new_users = count(users WHERE created_at BETWEEN start_of_month AND end_of_month)

# Customer Acquisition Cost (CAC)
cac = total_marketing_spend / new_users
```

**Engagement Metrics:**
```python
# Transactions Per User Per Month
tpupm = total_transactions / mau

# Average Transaction Amount
avg_amount = sum(transaction.amount) / count(transactions)

# User Retention (Day 1, Day 7, Day 30)
retention_day_1 = count(users active on day 1) / count(new users) * 100
retention_day_7 = count(users active on day 7) / count(new users) * 100
retention_day_30 = count(users active on day 30) / count(new users) * 100
```

**Business Metrics:**
```python
# Revenue Per User (RPU)
rpu = total_revenue / mau

# Lifetime Value (LTV)
ltv = avg_monthly_revenue_per_user * avg_customer_lifetime_months

# LTV/CAC Ratio
ltv_cac_ratio = ltv / cac  # Target: >3x

# Gross Margin
gross_margin = (revenue - cost_of_goods_sold) / revenue * 100  # Target: >75%

# Burn Rate
burn_rate = monthly_expenses - monthly_revenue  # Target: Break-even by Month 18
```

**Operational Metrics:**
```python
# Transaction Success Rate
success_rate = count(transactions WHERE status = 'COMPLETED') / count(transactions) * 100

# Average Processing Time
avg_processing_time = avg(transaction.processing_time_ms)

# System Uptime
uptime = (total_time - downtime) / total_time * 100  # Target: >99.9%

# Fraud Rate
fraud_rate = count(fraudulent_transactions) / count(transactions) * 100  # Target: <0.1%
```

### Python Implementation

```python
import pandas as pd
from datetime import datetime, timedelta

class BusinessMetrics:
    def __init__(self, db_connection):
        self.db = db_connection
    
    def calculate_mau(self, month: datetime) -> int:
        """Calculate Monthly Active Users"""
        query = """
            SELECT COUNT(DISTINCT sender_id) as mau
            FROM transactions
            WHERE DATE_TRUNC('month', created_at) = %s
        """
        result = self.db.execute(query, (month,))
        return result[0]['mau']
    
    def calculate_transaction_volume(self, month: datetime) -> float:
        """Calculate total transaction volume for a month"""
        query = """
            SELECT SUM(amount) as volume
            FROM transactions
            WHERE DATE_TRUNC('month', created_at) = %s
              AND status = 'COMPLETED'
        """
        result = self.db.execute(query, (month,))
        return float(result[0]['volume'] or 0)
    
    def calculate_retention_cohort(self, cohort_month: datetime) -> dict:
        """Calculate retention for a user cohort"""
        query = """
            SELECT 
                COUNT(DISTINCT user_id) as cohort_size,
                AVG(transactions_month_0) as month_0,
                AVG(transactions_month_1) as month_1,
                AVG(transactions_month_2) as month_2,
                AVG(transactions_month_3) as month_3
            FROM user_cohorts
            WHERE cohort_month = %s
        """
        result = self.db.execute(query, (cohort_month,))
        return result[0]
    
    def calculate_ltv(self, user_id: str) -> float:
        """Calculate Lifetime Value for a user"""
        query = """
            SELECT 
                COUNT(*) as total_transactions,
                SUM(amount) as total_volume,
                MIN(created_at) as first_transaction,
                MAX(created_at) as last_transaction
            FROM transactions
            WHERE sender_id = %s
              AND status = 'COMPLETED'
        """
        result = self.db.execute(query, (user_id,))
        
        if not result or result[0]['total_transactions'] == 0:
            return 0
        
        # Calculate average monthly revenue
        days_active = (result[0]['last_transaction'] - result[0]['first_transaction']).days
        months_active = max(days_active / 30, 1)
        monthly_revenue = (result[0]['total_transactions'] * 0.10) / months_active
        
        # Assume 25-month average lifetime
        ltv = monthly_revenue * 25
        
        return ltv
```

---


## 5. Data Pipeline Implementation

### Real-Time Pipeline (Apache Kafka)

```python
from kafka import KafkaProducer, KafkaConsumer
import json
from datetime import datetime

class TransactionEventProducer:
    def __init__(self):
        self.producer = KafkaProducer(
            bootstrap_servers=['kafka:9092'],
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )
    
    def publish_transaction_event(self, transaction: dict):
        """Publish transaction event to Kafka"""
        event = {
            'event_id': str(uuid.uuid4()),
            'transaction_id': transaction['id'],
            'user_id': transaction['sender_id'],
            'event_type': 'transaction_completed',
            'timestamp': datetime.utcnow().isoformat(),
            'amount': float(transaction['amount']),
            'currency': transaction['currency'],
            'bank_id': transaction['bank_id']
        }
        
        self.producer.send('transaction-events', value=event)
        self.producer.flush()

class MetricsAggregator:
    def __init__(self):
        self.consumer = KafkaConsumer(
            'transaction-events',
            bootstrap_servers=['kafka:9092'],
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
            group_id='metrics-aggregator'
        )
        self.db = get_database_connection()
    
    def run(self):
        """Consume events and update real-time metrics"""
        for message in self.consumer:
            event = message.value
            
            # Update daily metrics
            self.update_daily_metrics(event)
            
            # Update user activity
            self.update_user_activity(event)
            
            # Update bank metrics
            self.update_bank_metrics(event)
    
    def update_daily_metrics(self, event: dict):
        """Update daily aggregated metrics"""
        date = datetime.fromisoformat(event['timestamp']).date()
        
        query = """
            INSERT INTO daily_metrics (date, total_transactions, total_volume, revenue)
            VALUES (%s, 1, %s, %s)
            ON CONFLICT (date) DO UPDATE SET
                total_transactions = daily_metrics.total_transactions + 1,
                total_volume = daily_metrics.total_volume + EXCLUDED.total_volume,
                revenue = daily_metrics.revenue + EXCLUDED.revenue
        """
        
        revenue = 0.10  # R0.10 per transaction
        self.db.execute(query, (date, event['amount'], revenue))
```

### Batch Pipeline (Apache Airflow)

```python
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta

# DAG for daily metrics calculation
default_args = {
    'owner': 'cebo',
    'depends_on_past': False,
    'start_date': datetime(2024, 1, 1),
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 3,
    'retry_delay': timedelta(minutes=5)
}

dag = DAG(
    'daily_metrics_calculation',
    default_args=default_args,
    description='Calculate daily business metrics',
    schedule_interval='0 2 * * *',  # Run at 2 AM daily
    catchup=False
)

def calculate_daily_metrics(**context):
    """Calculate comprehensive daily metrics"""
    execution_date = context['execution_date'].date()
    
    db = get_database_connection()
    
    # Calculate transaction metrics
    tx_metrics = db.execute("""
        SELECT 
            COUNT(*) as total_transactions,
            COUNT(*) FILTER (WHERE status = 'COMPLETED') as successful_transactions,
            COUNT(*) FILTER (WHERE status = 'FAILED') as failed_transactions,
            SUM(amount) FILTER (WHERE status = 'COMPLETED') as total_volume,
            AVG(amount) FILTER (WHERE status = 'COMPLETED') as avg_amount,
            PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY amount) as median_amount
        FROM transactions
        WHERE DATE(created_at) = %s
    """, (execution_date,))
    
    # Calculate user metrics
    user_metrics = db.execute("""
        SELECT 
            COUNT(DISTINCT sender_id) as active_users,
            COUNT(*) FILTER (WHERE DATE(created_at) = %s) as new_users
        FROM transactions
        WHERE DATE(created_at) = %s
    """, (execution_date, execution_date))
    
    # Insert into daily_metrics table
    db.execute("""
        INSERT INTO daily_metrics (
            date, total_transactions, successful_transactions, failed_transactions,
            total_volume, active_users, new_users, avg_transaction_amount, median_transaction_amount
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT (date) DO UPDATE SET
            total_transactions = EXCLUDED.total_transactions,
            successful_transactions = EXCLUDED.successful_transactions,
            failed_transactions = EXCLUDED.failed_transactions,
            total_volume = EXCLUDED.total_volume,
            active_users = EXCLUDED.active_users,
            new_users = EXCLUDED.new_users,
            avg_transaction_amount = EXCLUDED.avg_transaction_amount,
            median_transaction_amount = EXCLUDED.median_transaction_amount
    """, (execution_date, *tx_metrics[0].values(), *user_metrics[0].values()))

def calculate_cohort_retention(**context):
    """Calculate user cohort retention"""
    execution_date = context['execution_date'].date()
    
    db = get_database_connection()
    
    # Get all cohorts that need updating
    cohorts = db.execute("""
        SELECT DISTINCT DATE_TRUNC('month', created_at) as cohort_month
        FROM users
        WHERE created_at <= %s
    """, (execution_date,))
    
    for cohort in cohorts:
        cohort_month = cohort['cohort_month']
        
        # Calculate retention for each month
        db.execute("""
            INSERT INTO user_cohorts (cohort_month, user_id, transactions_month_0, transactions_month_1, transactions_month_2, transactions_month_3)
            SELECT 
                %s as cohort_month,
                u.id as user_id,
                COUNT(*) FILTER (WHERE t.created_at BETWEEN u.created_at AND u.created_at + INTERVAL '1 month') as transactions_month_0,
                COUNT(*) FILTER (WHERE t.created_at BETWEEN u.created_at + INTERVAL '1 month' AND u.created_at + INTERVAL '2 months') as transactions_month_1,
                COUNT(*) FILTER (WHERE t.created_at BETWEEN u.created_at + INTERVAL '2 months' AND u.created_at + INTERVAL '3 months') as transactions_month_2,
                COUNT(*) FILTER (WHERE t.created_at BETWEEN u.created_at + INTERVAL '3 months' AND u.created_at + INTERVAL '4 months') as transactions_month_3
            FROM users u
            LEFT JOIN transactions t ON u.id = t.sender_id
            WHERE DATE_TRUNC('month', u.created_at) = %s
            GROUP BY u.id
            ON CONFLICT (cohort_month, user_id) DO UPDATE SET
                transactions_month_0 = EXCLUDED.transactions_month_0,
                transactions_month_1 = EXCLUDED.transactions_month_1,
                transactions_month_2 = EXCLUDED.transactions_month_2,
                transactions_month_3 = EXCLUDED.transactions_month_3
        """, (cohort_month, cohort_month))

# Define tasks
task_daily_metrics = PythonOperator(
    task_id='calculate_daily_metrics',
    python_callable=calculate_daily_metrics,
    dag=dag
)

task_cohort_retention = PythonOperator(
    task_id='calculate_cohort_retention',
    python_callable=calculate_cohort_retention,
    dag=dag
)

# Set dependencies
task_daily_metrics >> task_cohort_retention
```

---

## 6. API Design for Business Logic

### User Management API

```typescript
// user.controller.ts - Your business logic
export class UserController {
    async getUserProfile(userId: string): Promise<UserProfile> {
        const user = await db.user.findUnique({ where: { id: userId } });
        
        // Calculate user statistics
        const stats = await db.transaction.aggregate({
            where: { senderId: userId },
            _count: true,
            _sum: { amount: true },
            _avg: { amount: true }
        });
        
        return {
            bloodHash: user.bloodHash,
            createdAt: user.createdAt,
            totalTransactions: stats._count,
            totalVolume: stats._sum.amount,
            avgTransactionAmount: stats._avg.amount,
            status: user.status
        };
    }
    
    async getUserTransactionHistory(
        userId: string,
        limit: number = 20,
        offset: number = 0
    ): Promise<Transaction[]> {
        return await db.transaction.findMany({
            where: {
                OR: [
                    { senderId: userId },
                    { recipientId: userId }
                ]
            },
            orderBy: { createdAt: 'desc' },
            take: limit,
            skip: offset,
            include: {
                sender: { select: { bloodHash: true } },
                recipient: { select: { bloodHash: true } }
            }
        });
    }
}
```

### Analytics API (For Internal Dashboards)

```typescript
// analytics.controller.ts - Your analytics endpoints
export class AnalyticsController {
    async getGrowthMetrics(startDate: Date, endDate: Date): Promise<GrowthMetrics> {
        const metrics = await db.dailyMetrics.findMany({
            where: {
                date: {
                    gte: startDate,
                    lte: endDate
                }
            },
            orderBy: { date: 'asc' }
        });
        
        return {
            dates: metrics.map(m => m.date),
            activeUsers: metrics.map(m => m.activeUsers),
            newUsers: metrics.map(m => m.newUsers),
            totalTransactions: metrics.map(m => m.totalTransactions),
            totalVolume: metrics.map(m => m.totalVolume),
            revenue: metrics.map(m => m.revenue)
        };
    }
    
    async getCohortAnalysis(cohortMonth: Date): Promise<CohortAnalysis> {
        const cohort = await db.userCohorts.findMany({
            where: { cohortMonth: cohortMonth }
        });
        
        const cohortSize = cohort.length;
        
        return {
            cohortMonth: cohortMonth,
            cohortSize: cohortSize,
            retention: {
                month0: cohort.filter(u => u.transactionsMonth0 > 0).length / cohortSize,
                month1: cohort.filter(u => u.transactionsMonth1 > 0).length / cohortSize,
                month2: cohort.filter(u => u.transactionsMonth2 > 0).length / cohortSize,
                month3: cohort.filter(u => u.transactionsMonth3 > 0).length / cohortSize
            },
            avgTransactions: {
                month0: cohort.reduce((sum, u) => sum + u.transactionsMonth0, 0) / cohortSize,
                month1: cohort.reduce((sum, u) => sum + u.transactionsMonth1, 0) / cohortSize,
                month2: cohort.reduce((sum, u) => sum + u.transactionsMonth2, 0) / cohortSize,
                month3: cohort.reduce((sum, u) => sum + u.transactionsMonth3, 0) / cohortSize
            }
        };
    }
    
    async getBankPerformance(): Promise<BankPerformance[]> {
        return await db.bankPartnerships.findMany({
            select: {
                bankName: true,
                totalUsers: true,
                totalTransactions: true,
                totalVolume: true,
                status: true
            },
            orderBy: { totalVolume: 'desc' }
        });
    }
}
```

---

## 7. Financial Modeling & Reporting

### 5-Year Financial Model (Your Spreadsheet)

```python
import pandas as pd
import numpy as np

class FinancialModel:
    def __init__(self):
        self.transaction_fee = 0.10  # R0.10 per transaction
        self.bank_licensing_fee = 50000  # R50K per month per bank
        
    def project_revenue(self, years: int = 5) -> pd.DataFrame:
        """Project revenue for next 5 years"""
        
        # User growth assumptions
        users = [
            350_000,   # Year 1
            700_000,   # Year 2
            1_750_000, # Year 3
            3_500_000, # Year 4
            5_250_000  # Year 5
        ]
        
        # Transactions per user per month
        tpupm = 20
        
        # Bank partnerships
        banks = [1, 3, 5, 8, 10]
        
        projections = []
        
        for year in range(years):
            # Transaction revenue
            monthly_transactions = users[year] * tpupm
            annual_transactions = monthly_transactions * 12
            transaction_revenue = annual_transactions * self.transaction_fee
            
            # Bank licensing revenue
            bank_revenue = banks[year] * self.bank_licensing_fee * 12
            
            # Total revenue
            total_revenue = transaction_revenue + bank_revenue
            
            projections.append({
                'year': year + 1,
                'users': users[year],
                'transactions': annual_transactions,
                'transaction_revenue': transaction_revenue,
                'bank_revenue': bank_revenue,
                'total_revenue': total_revenue
            })
        
        return pd.DataFrame(projections)
    
    def project_costs(self, years: int = 5) -> pd.DataFrame:
        """Project costs for next 5 years"""
        
        projections = []
        
        for year in range(years):
            # Team costs (growing team)
            team_size = [10, 25, 50, 75, 100][year]
            avg_salary = 600_000  # R600K per person per year
            team_costs = team_size * avg_salary
            
            # Infrastructure costs (scales with users)
            users = [350_000, 700_000, 1_750_000, 3_500_000, 5_250_000][year]
            infrastructure_cost_per_user = 6  # R6 per user per year
            infrastructure_costs = users * infrastructure_cost_per_user
            
            # Marketing costs
            marketing_costs = [5_000_000, 10_000_000, 20_000_000, 30_000_000, 40_000_000][year]
            
            # Compliance & legal
            compliance_costs = [2_000_000, 3_000_000, 5_000_000, 7_000_000, 10_000_000][year]
            
            # Total costs
            total_costs = team_costs + infrastructure_costs + marketing_costs + compliance_costs
            
            projections.append({
                'year': year + 1,
                'team_costs': team_costs,
                'infrastructure_costs': infrastructure_costs,
                'marketing_costs': marketing_costs,
                'compliance_costs': compliance_costs,
                'total_costs': total_costs
            })
        
        return pd.DataFrame(projections)
    
    def generate_financial_summary(self) -> pd.DataFrame:
        """Generate complete financial summary"""
        revenue_df = self.project_revenue()
        costs_df = self.project_costs()
        
        summary = pd.merge(revenue_df, costs_df, on='year')
        
        # Calculate profitability
        summary['gross_profit'] = summary['total_revenue'] - summary['total_costs']
        summary['gross_margin'] = (summary['gross_profit'] / summary['total_revenue'] * 100).round(2)
        summary['cumulative_profit'] = summary['gross_profit'].cumsum()
        
        return summary

# Generate and export
model = FinancialModel()
summary = model.generate_financial_summary()
summary.to_csv('mari_financial_projections.csv', index=False)
print(summary)
```

### Monthly Investor Report (Your Responsibility)

```python
class InvestorReport:
    def generate_monthly_report(self, month: datetime) -> dict:
        """Generate monthly report for investors"""
        
        db = get_database_connection()
        
        # Get metrics for the month
        metrics = db.execute("""
            SELECT 
                SUM(total_transactions) as transactions,
                SUM(total_volume) as volume,
                SUM(revenue) as revenue,
                AVG(active_users) as avg_active_users,
                SUM(new_users) as new_users
            FROM daily_metrics
            WHERE DATE_TRUNC('month', date) = %s
        """, (month,))
        
        # Get previous month for comparison
        prev_month = month - timedelta(days=30)
        prev_metrics = db.execute("""
            SELECT 
                SUM(total_transactions) as transactions,
                SUM(total_volume) as volume,
                SUM(revenue) as revenue
            FROM daily_metrics
            WHERE DATE_TRUNC('month', date) = %s
        """, (prev_month,))
        
        # Calculate growth rates
        transaction_growth = ((metrics[0]['transactions'] - prev_metrics[0]['transactions']) / 
                             prev_metrics[0]['transactions'] * 100)
        volume_growth = ((metrics[0]['volume'] - prev_metrics[0]['volume']) / 
                        prev_metrics[0]['volume'] * 100)
        
        return {
            'month': month.strftime('%B %Y'),
            'metrics': {
                'total_users': metrics[0]['avg_active_users'],
                'new_users': metrics[0]['new_users'],
                'transactions': metrics[0]['transactions'],
                'volume': f"R{metrics[0]['volume']:,.2f}",
                'revenue': f"R{metrics[0]['revenue']:,.2f}"
            },
            'growth': {
                'transaction_growth': f"{transaction_growth:+.1f}%",
                'volume_growth': f"{volume_growth:+.1f}%"
            },
            'milestones': self.get_milestones_achieved(month),
            'challenges': self.get_challenges(month),
            'next_month_goals': self.get_next_month_goals(month)
        }
```

---

## 8. Partnership Integration

### Bank Partnership Tracking

```python
class PartnershipManager:
    def track_bank_performance(self, bank_id: str, month: datetime) -> dict:
        """Track performance metrics for a bank partnership"""
        
        db = get_database_connection()
        
        metrics = db.execute("""
            SELECT 
                COUNT(DISTINCT sender_id) as active_users,
                COUNT(*) as transactions,
                SUM(amount) as volume,
                AVG(amount) as avg_transaction,
                COUNT(*) FILTER (WHERE status = 'COMPLETED') as successful_transactions,
                COUNT(*) FILTER (WHERE status = 'FAILED') as failed_transactions
            FROM transactions
            WHERE bank_id = %s
              AND DATE_TRUNC('month', created_at) = %s
        """, (bank_id, month))
        
        # Calculate revenue share
        transaction_fee_revenue = metrics[0]['transactions'] * 0.10
        bank_share = transaction_fee_revenue * 0.30  # Bank gets 30%
        mari_share = transaction_fee_revenue * 0.70  # Mari gets 70%
        
        return {
            'bank_id': bank_id,
            'month': month.strftime('%B %Y'),
            'active_users': metrics[0]['active_users'],
            'transactions': metrics[0]['transactions'],
            'volume': metrics[0]['volume'],
            'success_rate': (metrics[0]['successful_transactions'] / metrics[0]['transactions'] * 100),
            'revenue': {
                'total': transaction_fee_revenue,
                'bank_share': bank_share,
                'mari_share': mari_share
            }
        }
```

---

## 9. User Growth & Retention Analytics

### Cohort Analysis Implementation

```python
class CohortAnalytics:
    def analyze_retention(self, cohort_month: datetime) -> dict:
        """Analyze retention for a user cohort"""
        
        db = get_database_connection()
        
        cohort_data = db.execute("""
            SELECT 
                COUNT(DISTINCT user_id) as cohort_size,
                COUNT(DISTINCT user_id) FILTER (WHERE transactions_month_0 > 0) as active_month_0,
                COUNT(DISTINCT user_id) FILTER (WHERE transactions_month_1 > 0) as active_month_1,
                COUNT(DISTINCT user_id) FILTER (WHERE transactions_month_2 > 0) as active_month_2,
                COUNT(DISTINCT user_id) FILTER (WHERE transactions_month_3 > 0) as active_month_3,
                AVG(transactions_month_0) as avg_tx_month_0,
                AVG(transactions_month_1) as avg_tx_month_1,
                AVG(transactions_month_2) as avg_tx_month_2,
                AVG(transactions_month_3) as avg_tx_month_3
            FROM user_cohorts
            WHERE cohort_month = %s
        """, (cohort_month,))
        
        data = cohort_data[0]
        cohort_size = data['cohort_size']
        
        return {
            'cohort_month': cohort_month.strftime('%B %Y'),
            'cohort_size': cohort_size,
            'retention_rate': {
                'month_0': f"{data['active_month_0'] / cohort_size * 100:.1f}%",
                'month_1': f"{data['active_month_1'] / cohort_size * 100:.1f}%",
                'month_2': f"{data['active_month_2'] / cohort_size * 100:.1f}%",
                'month_3': f"{data['active_month_3'] / cohort_size * 100:.1f}%"
            },
            'avg_transactions': {
                'month_0': round(data['avg_tx_month_0'], 2),
                'month_1': round(data['avg_tx_month_1'], 2),
                'month_2': round(data['avg_tx_month_2'], 2),
                'month_3': round(data['avg_tx_month_3'], 2)
            }
        }
```

---

## 10. Operational Dashboards

### Real-Time Operations Dashboard (Grafana)

```yaml
# grafana-dashboard.json - Your operations dashboard
{
  "dashboard": {
    "title": "Mari Operations Dashboard",
    "panels": [
      {
        "title": "Transactions Per Minute",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(mari_transactions_total[1m])",
            "legendFormat": "TPS"
          }
        ]
      },
      {
        "title": "Success Rate",
        "type": "gauge",
        "targets": [
          {
            "expr": "mari_transactions_total{status='completed'} / mari_transactions_total * 100"
          }
        ]
      },
      {
        "title": "Average Processing Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, mari_transaction_duration_seconds)"
          }
        ]
      }
    ]
  }
}
```

---

## Your Development Roadmap

### Month 1-3: Foundation
- [ ] Set up PostgreSQL + ClickHouse
- [ ] Build data ingestion pipeline (Kafka)
- [ ] Create core database schema
- [ ] Set up Metabase dashboards
- [ ] Implement basic analytics API

### Month 4-6: Analytics
- [ ] Build executive dashboard
- [ ] Implement cohort analysis
- [ ] Set up Airflow for batch jobs
- [ ] Create financial model
- [ ] Monthly investor reporting

### Month 7-12: Scale
- [ ] Advanced analytics (predictive models)
- [ ] Real-time dashboards (Grafana)
- [ ] Partnership tracking system
- [ ] Automated reporting
- [ ] Data warehouse optimization

### Month 13-18: Optimize
- [ ] Machine learning for business insights
- [ ] Automated forecasting
- [ ] Advanced segmentation
- [ ] International expansion analytics
- [ ] IPO-ready financial reporting

---

## Key Success Metrics

**Data Quality:**
- Data freshness: <5 minutes lag
- Query performance: <2 seconds (p95)
- Data accuracy: >99.9%
- Dashboard uptime: >99.9%

**Business Impact:**
- Financial model accuracy: ±5% vs actuals
- Investor reporting: 100% on-time
- Partnership pipeline: 10 active discussions
- User insights: Weekly reports to team

---

## Conclusion

You're building the data backbone of Mari Protocol. Your work enables:

1. **Data-driven decisions** through accurate metrics
2. **Investor confidence** through transparent reporting
3. **Partnership success** through performance tracking
4. **Growth optimization** through cohort analysis
5. **Operational excellence** through real-time monitoring

**Your business + CS background is your superpower. Use it to:**
- Bridge technical and business teams
- Make data accessible to everyone
- Drive growth through insights
- Optimize unit economics
- Build investor confidence

**This is not just data engineering. You're building the business intelligence that will guide Mari from startup to R21.75B company.**

**Let's build the most data-driven fintech in Africa.** 📊

---

**Document Version:** 1.0  
**Last Updated:** 2024-01-15  
**Owner:** Cebolenkosi Chamane (Chief Business Officer)  
**Status:** Living Document
