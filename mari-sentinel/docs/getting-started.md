# Getting Started

This guide walks you through running Mari Sentinel locally (production-like) using Docker and the Makefile in the root of `mari-sentinel/`.

## Prerequisites
- Docker Desktop with Docker Compose
- Windows, macOS, or Linux
- No local Python/Node installs required (everything runs in containers)

## One-time setup
1. Build all services
   ```bash
   make build
   ```
2. Start the stack (production-like network)
   ```bash
   make prod-up
   ```
3. Tail logs (Ctrl+C to stop)
   ```bash
   make logs
   ```

## Seed a baseline model
Inference requires an ONNX model in Redis. Seed one:
```bash
make prod-seed
```
This will:
- Generate `scripts/initial_model.onnx` inside a Python container
- Seed the model into Redis on the `mari-prod` network

## (Optional) Seed synthetic labeled data
To quickly exercise training:
```bash
make prod-seed-synth            # seeds 200 examples by default
make prod-seed-synth SEED_SYNTH_COUNT=1000
```

## Train a model (offline)
Publishes a new ONNX to Redis and notifies inference to hot-swap:
```bash
make train
```

## Smoke test inference
```bash
make infer-smoke
```
Expected output:
```json
{ "score": 0, "model_id": "vXXXXXXXX" }
```
Score is 0/1 when Sentinel falls back to the label tensor. With recent exporters, probability tensors are preferred automatically.

## Health and readiness
```bash
# readiness (checks Redis + model)
docker run --rm curlimages/curl:8.7.1 -sS \
  http://host.docker.internal:3002/ready | docker run --rm -i imega/jq .
```
Response:
```json
{ "ready": true, "modelReady": true, "redis": true, "model_id": "v..." }
```

## Common operational commands
```bash
make ps            # container status
make logs          # tail all services
make prod-down     # stop stack
make clean         # prune dangling images/containers
```

## Next steps
- Read `docs/architecture.md` for deeper details
- Enable auth: see `docs/security.md`
- Manage training data: see `docs/data-hygiene.md`
- Troubleshoot: see `docs/troubleshooting.md`
