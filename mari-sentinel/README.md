# Mari Sentinel – Production Implementation

Zero‑PII, real‑time fraud scoring for the Mari Protocol. Sentinel ingests cross‑chain activity, accumulates labeled examples, trains an ONNX model, and serves low‑latency risk scores with hot‑swappable models.

## Quick Start

Requirements:
- Docker Desktop (with Docker Compose)
- Windows/macOS/Linux. No local Python/Node installs needed – everything runs in containers.

Commands:
```bash
make build          # build all images
make prod-up        # start Redis, RabbitMQ, Inference, Adapters, Trainer, Model Builder, Prometheus, Grafana
make prod-seed      # generate baseline ONNX and seed Redis (first-time requirement)
make prod-seed-synth SEED_SYNTH_COUNT=1000  # optional: seed synthetic examples quickly
make train          # publish a new ONNX model from labeled examples
make infer-smoke    # POST a sample payload to inference and pretty-print the result
make logs           # tail all logs (Ctrl+C to stop)
```

Inference endpoint:
```http
POST http://localhost:3002/inference
Content-Type: application/json

{
  "coupon_hash": "a7ff3e82b53cafe",
  "kid": "a1b2c3d4",
  "expiry_ts": 32503680000000,
  "seal": "8a2f3b91",
  "grid_id": "grid-xyz",
  "amount": 1.5
}
```
Response (example):
```json
{ "score": 0, "model_id": "vXXXXXXXX" }
```

Health endpoints (inference):
- `GET /health` – liveness
- `GET /ready` – readiness (Redis + model loaded)

## Documentation

The `docs/` directory contains detailed guides:
- `docs/overview.md` – What Sentinel is and why it benefits Mari
- `docs/getting-started.md` – End‑to‑end runbook and commands
- `docs/architecture.md` – Data flow and components
- `docs/security.md` – Enabling `X-Mari-Auth` and hardening
- `docs/data-hygiene.md` – Inspecting, draining, and clearing training data

## Architecture (Summary)

- Ingestion: BTC/ETH/SOL adapters publish PRE_SETTLEMENT events to RabbitMQ.
- Labels: Label Oracle emits SETTLEMENT_OUTCOME events.
- Trainer: Accumulates labeled examples into Redis list `labeled:examples`.
- Model Builder: Trains and publishes ONNX to Redis (`model:<id>`, `model:current`).
- Inference: Loads current model from Redis, hot‑swaps via pub/sub, exposes `/inference`.

## Troubleshooting

- Inference returns `{ "error": "inference_failed" }`:
  - Ensure a model exists: `make prod-seed` or `make train`.
  - Check logs: `docker compose logs -f inference`.
  - Readiness: `curl http://localhost:3002/ready` should show `ready: true` and the current `model_id`.
- Seeding fails to reach Redis:
  - Use `make prod-seed` (runs on the correct Docker network so `redis` resolves).
- No labeled examples:
  - Seed synthetic data: `make prod-seed-synth SEED_SYNTH_COUNT=1000`, then `make train`.

## License

Mari Protocol Research – proprietary, patent pending.
