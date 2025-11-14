# Operations Overview & Runbook

## Scope

- **Audience**
  - Platform engineers, SRE, DevOps.
- **Goal**
  - Describe Mari services, how to run them, and what to monitor.
  - Provide quick runbooks for common incidents.

## Services & Responsibilities

- **MongoDB**
  - Stores core `transactions` collection.
  - Critical for audit, analytics, and some runtime reads.

- **Mari Core (`mari-server`)**
  - HTTP API for device registration, transactions, and settlement.
  - Connects to MongoDB, Sentinel, Bank/HSM, RabbitMQ.

- **Mari Sentinel (`mari-sentinel`)**
  - Inference service (risk scoring via `/inference`).
  - Trainer (consumes `mari-tx-events`, updates models in Redis).
  - Redis (stores models, used by inference and trainer).

- **Mock Bank HSM (`mock-bank-hsm`)**
  - Simulated bank ledger and HSM for increment keys.
  - Core depends on it for settlement.

- **RabbitMQ**
  - `mari-tx-events` queue for `TransactionEvent` messages.
  - Used by Sentinel trainer for continuous learning.

## Ports & Endpoints (Dev/Prod Defaults)

- **Core (`mari-server`)**
  - HTTP: `:3000` (prod), `:3003` (dev).
  - Health: `/health`, plus service-specific health checks.

- **Mock Bank HSM**
  - HTTP: `:3001`.
  - Increment key API: `/api/hsm/increment-key`.

- **Sentinel Inference**
  - HTTP: `:3002`.
  - Health: `/health`, `/ready`, `/live`.
  - Metrics: `/metrics`.

- **Sentinel Trainer**
  - Health server: `:8083`.
  - Endpoints: `/ready`, `/live`, `/metrics`.

- **RabbitMQ**
  - AMQP: `:5672` (in Docker network).

- **Redis**
  - TCP: `:6379` (in Docker network).

## Starting & Stopping Services (Indicative)

> Exact commands may differ per environment; these are typical.

- **Core + Mongo (local)**
  - From `mari-server` directory:
    - `docker compose up -d`

- **Mock Bank HSM (local)**
  - From `mock-bank-hsm` directory:
    - `make up` (or `docker compose up -d` per Makefile).

- **Sentinel Stack (local)**
  - From `mari-sentinel` directory:
    - `make up` or `docker compose up -d` (depending on scripts provided).

## Health Checks & Monitoring

- **Core**
  - Health endpoint(s) for core and sub-services.
  - Watch for:
    - 5xx spikes.
    - Latency increases on `/api/transactions` and `/api/settlement/process`.

- **Sentinel Inference**
  - `/health`, `/ready`, `/metrics`.
  - Key metrics:
    - Request count and error rate.
    - Inference latency histograms.
    - Score distribution and `model_version` gauge.

- **Trainer**
  - `/ready`, `/live`, `/metrics` on `HEALTH_PORT` (default `8083`).
  - Signs of trouble:
    - Not ready (Redis/RabbitMQ problems).
    - Low event consumption vs publish rate.

- **RabbitMQ**
  - Monitor:
    - Queue depth for `mari-tx-events`.
    - Connection and channel health.

- **MongoDB**
  - Monitor:
    - Availability and replication (if used).
    - Disk utilization and slow queries.

## Common Incident Runbooks

### 1. Sentinel Unavailable

- **Symptoms**
  - Core returns `503 { error: "sentinel_unavailable" }`.
  - Inference `/ready` or `/health` failing.

- **Immediate Actions**
  - Check Sentinel container status and logs.
  - Check Redis availability (model store).
  - Check network between Core and Sentinel.

- **Mitigations**
  - For dev/test environments only:
    - Temporarily set `SENTINEL_FAIL_OPEN=true` to avoid blocking all traffic.
  - For production:
    - Recommended: keep fail-close and restore Sentinel quickly.

### 2. Bank/HSM Unavailable

- **Symptoms**
  - Core 5xx errors during settlement calls.
  - Increment keys not being returned.

- **Immediate Actions**
  - Check mock-bank-hsm service status and logs.
  - Verify connectivity from Core to bank service.

- **Impact**
  - Transactions cannot settle; UX should show failures or temporary inability to complete payments.

### 3. Mongo Issues

- **Symptoms**
  - Core errors on transaction persistence.
  - Logs showing failures to write to `transactions` collection.

- **Immediate Actions**
  - Check MongoDB container status.
  - Verify disk space and connection string.

- **Impact**
  - Payments may still settle (bank side) but fail to be recorded in journal.
  - Core logs will show persistence errors for later reconciliation.

### 4. RabbitMQ / Trainer Backlog

- **Symptoms**
  - `mari-tx-events` queue growing without being consumed.
  - Trainer `/ready` false or errors.

- **Immediate Actions**
  - Check Trainer logs for Redis/RabbitMQ connection issues.
  - Check RabbitMQ node health and disk space.

- **Impact**
  - Online risk scoring continues (inference uses existing model).
  - Continuous learning slows or pauses until trainer recovers.

## Configuration Highlights

- **Core Environment Variables (security/risk relevant)**
  - `SENTINEL_URL`, `SENTINEL_THRESHOLD`, `SENTINEL_FAIL_OPEN`, `SENTINEL_AUTH_TOKEN`.
  - `BANK_BASE_URL` for bank/HSM connectivity.
  - `RABBITMQ_URL` to enable label publishing.

- **Sentinel Environment Variables**
  - `REDIS_URL` for model store.
  - `RABBITMQ_URL` for trainer.
  - Auth token for inference if required.

## Operations Best Practices

- **Separate Environments**
  - Maintain distinct dev, test, and prod stacks.

- **Change Management**
  - Coordinate model updates and threshold changes with:
    - Risk teams.
    - Analytics and legal where required.

- **Backups & DR**
  - Ensure:
    - Regular backups of MongoDB and bank ledger (if persistent).
    - Documented procedures for restore and failover.
