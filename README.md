# Mari Protocol

Mari is a mobile‑first value transfer protocol that provides immediate, bank‑mediated finality across two rails:

- Online (HTTP): low‑latency, signed submission from the Android app to Core.
- Offline (SMS): robust store‑and‑forward transport of the same intent (no offline finality).

Finality is authoritative only when the Bank HSM issues a signed increment key and the client verifies it. SMS is a transport channel, not a source of finality.

## Highlights

- **Device‑bound identity**: ECDSA P‑256 device keys with short `kid` identifiers (first 8 hex of SHA‑256(SPKI)).
- **Human‑verifiable intent**: `mari://xfer?...` coupons with motion‑based seal `s`.
- **HSM settlement proofs**: increment‑key payload + signature; audit‑ready.
- **Idempotency**: duplicates rejected by `couponHash`.
- **Low‑resource friendly**: works with basic phones via SMS.
- **QR Code Support**: Modern payment request UX for receiving payments.
- **Batch Payments**: Single seal for multiple transactions (payroll, merchant settlements).

## Monorepo of Independent Services

Although contained in a single top‑level repository, each service is an independent repo with its own Docker environment and `.env(.example)`:

- `mari-server/`: Core directory, device registry, transaction intake; calls Sentinel for risk scoring; talks to HSM.
- `mari-sentinel/`: Real‑time fraud/risk scoring pipeline (adapters, trainer, ONNX inference, monitoring).
- `mari-app/`: Android reference client (Jetpack Compose) — demo app showing how to integrate with the protocol.
- `mock-bank-hsm/`: Mock bank HSM; issues/verifies increment‑key proofs.

You can run them together (see below) or in isolation for targeted tests.

## Running the Demo Locally

See `USAGE.md` for the full bring‑up sequence, Android build notes, and Sentinel wiring. The short version:

```powershell
# 1) Start Core
cd d:\mari_protocol_slim\mari-server
docker compose up -d

# 2) Start Mock Bank HSM
cd d:\mari_protocol_slim\mock-bank-hsm
docker compose up -d

# 3) Start Sentinel (optional)
cd d:\mari_protocol_slim\mari-sentinel
make prod-up
make prod-seed
make train

# 4) Wire Core to Sentinel (same Docker network)
cd d:\mari_protocol_slim\mari-server
pwsh scripts\wire-sentinel.ps1
$env:SENTINEL_URL = "http://inference:3002"
pwsh scripts\check-sentinel.ps1

# 5) Android app
cd d:\mari_protocol_slim\mari-app
make clean && make debug && make install_run
```

Basic health check:
```powershell
Invoke-RestMethod -Uri http://localhost:3000/health
```

## Security & Finality (At a Glance)

- Immediate finality: Only when the Bank HSM issues a signed increment key and the client verifies it.
- No pending balances: UI updates after verified settlement.
- Replay resistance: `couponHash` idempotency and expiry.
- Authentication: standard device biometrics/PIN to unlock signing.

## Documentation

### Core Protocol
- `OVERVIEW.md` — high‑level summary and highlights
- `docs/MARI-WHITEPAPER.md` — theory, philosophy, and threat model

### Integration & Feature Guides
- `docs/partners/01-payment-provider-integration.md` — Payment provider / wallet integration guide

## License

MIT © 2025 Nyasha Shekede
