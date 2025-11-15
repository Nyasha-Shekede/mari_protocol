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

