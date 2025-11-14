# Mari Sentinel Overview

Mari Sentinel provides zero-PII, real‑time fraud scoring for the Mari Protocol. It ingests cross‑chain activity, produces PRE_SETTLEMENT events, joins with SETTLEMENT_OUTCOME labels, and continuously trains and serves a lightweight model over an isolated runtime.

## Why Sentinel matters to Mari
- Resilience: Stateless inference with hot‑swappable models keeps risk scoring online during deployments.
- Privacy by design: No PII leaves your systems; features are hashed or derived, and model artifacts are stored in Redis.
- Multi‑chain coverage: Bitcoin, Ethereum, and Solana adapters stream recent transactions/slots/blocks.
- Rapid iteration: Online accumulation + offline training enables quick feedback cycles and controlled releases.
- Operability: Built‑in Prometheus metrics and Grafana dashboards; simple Makefile workflows.

## High‑level capabilities
- Event ingestion from public chains via adapters (BTC/ETH/SOL)
- Label gathering via a “label oracle” (mocked if API keys are unset)
- Online example accumulation into Redis
- Offline model building to ONNX (skl2onnx)
- Redis‑backed model distribution and pub/sub hot‑swap
- ONNX Runtime inference API with health/readiness endpoints

## Key components
- `crypto-adapter/`: Polls chains and publishes PRE_SETTLEMENT events to RabbitMQ.
- `label-oracle/`: Emits SETTLEMENT_OUTCOME labels.
- `trainer/`: Joins events/labels and pushes JSONL examples to Redis list `labeled:examples`.
- `model-builder/`: Trains a small model and publishes ONNX to Redis (`model:current`, `model:<id>`).
- `inference/`: Loads the latest model from Redis and serves `POST /inference`.
- `monitoring/`: Prometheus + Grafana.

See also:
- Getting started: `docs/getting-started.md`
- Architecture details: `docs/architecture.md`
- Security: `docs/security.md`
- Data hygiene: `docs/data-hygiene.md`
- Troubleshooting: `docs/troubleshooting.md`
