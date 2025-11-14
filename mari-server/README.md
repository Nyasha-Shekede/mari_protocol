# Mari Core Server

Core HTTP API for the Mari demo. Validates signed, physics-bound coupons, records transactions in MongoDB, and delegates immediate settlement to the mock bank HSM.

> Note: Core now exposes a simple `/api/settlement/process` endpoint that proxies batch settlement requests to `mock-bank-hsm`. Any older batch endpoints referenced in legacy docs are deprecated and should be considered disabled.

> Note: The legacy SMS Broker (RabbitMQ) path has been removed. Core now receives real SMS via a webhook (e.g., Twilio) and can optionally send outbound SMS receipts via Twilio.

## Architecture overview

```mermaid
flowchart LR
  subgraph Client[Android App / Scripts]
    A[Create Transaction]\n(HTTP to Core)
    D[Verify IncrementKey]\n(HTTP to Bank or via Core)
  end

  subgraph Core[Core Server :3000]
    T[Transactions API]\nValidate signature & coupon
    R[Risk (Sentinel)]\n(optional)
    M[(MongoDB)]
  end

  subgraph Bank[Mock Bank HSM :3001]
    HSM[HSM]\n(increment key, verify)
  end

  A --> T --> M
  T --> R
  T -->|increment-key| HSM
  HSM --> T
  D --> HSM
```

Key points:
- Immediate settlement: Core calls Bank HSM per transaction; client updates only after verifying the bank-signed increment key.
- Optional Sentinel risk call can be invoked by Core prior to HSM.
- Batch settlement: Core proxies `/api/settlement/process` to `mock-bank-hsm`, which validates coupons, applies commissions, and issues a batch increment key.

## Sequence diagrams

Transaction creation (physics-bound coupon):

```mermaid
sequenceDiagram
  participant Client
  participant Core
  participant Mongo as MongoDB

  Client->>Client: Get-MariMotionSeal(x,y,z)
  Client->>Core: POST /api/transactions { coupon, physicsData, ... }
  Core->>Core: Validate signature, coupon & physics; idempotency on couponHash
  Core->>Bank: POST /api/hsm/increment-key { userId, amount, couponHash, timeNs }
  Bank-->>Core: { payload, SIG }
  Core->>Mongo: Insert Transaction (SETTLED)
  Core-->>Client: 200 { ok:true, couponHash, payload, SIG }
```

Batch settlement (current)

Core implements `/api/settlement/process`, which:

- Accepts a batch ID, core merchant ID, bank merchant ID, batch seal, and a list of transactions.
- Forwards a normalized payload to `mock-bank-hsm` for validation and commission calculation.
- Returns the bank’s settlement summary and increment key to the caller.

## Prerequisites

- Docker Desktop running
- Windows PowerShell (commands below are copy/paste-ready)

## Start services (split-up, per-service)

```powershell
# Bank
Set-Location d:\mari_protocol_slim\mock-bank-hsm
make net
make bank-up
make health

# Core + Mongo
Set-Location d:\mari_protocol_slim\mari-server
docker compose up -d

## SMS Integration (Twilio)

### Inbound (webhook)

- Configure your provider (Twilio) to POST form-encoded messages to:
  - `http://<core-host>:3000/webhook/sms/incoming`
- Core will accept `From`, `To`, `Body` form fields, normalize to `{ from, to, body }`, and process Mari coupons if `Body` starts with `mari://xfer?...`.
- Optional: Set `TWILIO_AUTH_TOKEN` to verify `X-Twilio-Signature`.

### Outbound (receipts)

- Core can send short settlement receipts via Twilio after successful HSM authorization.
- Environment required (one of service SID or from number):
  - `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`
  - `TWILIO_MESSAGING_SERVICE_SID` OR `TWILIO_FROM_NUMBER`
  - Optional `TWILIO_E164_PREFIX` to turn local numbers (e.g., `0000001001`) into `+2630000001001`.

### Environment (.env or compose)

```
BANK_BASE_URL=http://localhost:3001
SENTINEL_URL=http://localhost:3002
TWILIO_ACCOUNT_SID=...
TWILIO_AUTH_TOKEN=...
TWILIO_MESSAGING_SERVICE_SID=...
# or TWILIO_FROM_NUMBER=+15555550123
TWILIO_E164_PREFIX=+263
```

### Quick test (PowerShell)

```powershell
$body = @{ From = "+10000000001"; To = "+19999999999"; Body = "mari://xfer?from=0000001001&to=0000001002&val=1&g=grid123&exp=4102444800000&s=deadbeef" }
Invoke-RestMethod -Uri http://localhost:3000/webhook/sms/incoming -Method Post -Body $body
```

## Sentinel Integration

Sentinel provides an optional risk score before Core calls the Bank HSM. Core can operate in fail-close (default) or fail-open mode when Sentinel is unavailable.

### Environment (.env or compose)

```
SENTINEL_URL=http://inference:3002
SENTINEL_AUTH_TOKEN=changeme-supersecret   # optional, if Sentinel enforces auth
SENTINEL_THRESHOLD=850                     # reject if score > threshold
SENTINEL_FAIL_OPEN=false                   # true to proceed when Sentinel is down
```

### Availability policy

- When `SENTINEL_FAIL_OPEN=false` (default): If Sentinel is unreachable, Core rejects the transaction with `503 sentinel_unavailable` and publishes a `REJECTED_BY_SENTINEL` outcome label.
- When `SENTINEL_FAIL_OPEN=true`: If Sentinel is unreachable, Core proceeds to HSM settlement (score recorded as `NA`). Threshold-based rejections still apply when a score is present.

### Health endpoint

- `GET /health/sentinel` → Pings `${SENTINEL_URL}/inference` with a tiny payload and returns:
  - `200 { ok: true, status: 'reachable', score, modelId }` on success
  - `503 { ok: false, status: 'unreachable', error }` on failure

Use `scripts/wire-sentinel.ps1` to connect Core/Bank to the `mari-prod` Docker network so `SENTINEL_URL=http://inference:3002` resolves via internal DNS.

## Helper functions (PowerShell)

```powershell
function Get-MariMotionSeal([double]$x, [double]$y, [double]$z) {
  $motion = "$x,$y,$z"
  $md5 = [System.Security.Cryptography.MD5]::Create()
  $bytes = [System.Text.Encoding]::UTF8.GetBytes($motion)
  $hashBytes = $md5.ComputeHash($bytes)
  $hex = -join ($hashBytes | ForEach-Object { $_.ToString("x2") })
  $first8 = $hex.Substring(0,8)
  $intVal = [Convert]::ToInt64($first8,16)
  return ($intVal / 100000000.0)
}

function Get-BatchSeal($items) {
  $parts = @()
  foreach ($it in $items) {
    $amt = [double]$it.amount
    if ($amt -eq [math]::Truncate($amt)) { $amtStr = [int]$amt } else { $amtStr = $it.amount.ToString() }
    $parts += ($it.id + $amtStr)
  }
  $parts = $parts | Sort-Object
  $concat = ($parts -join "")
  $bytes = [System.Text.Encoding]::UTF8.GetBytes($concat)
  $sha256 = [System.Security.Cryptography.SHA256]::Create()
  $hashBytes = $sha256.ComputeHash($bytes)
  return (-join ($hashBytes | ForEach-Object { $_.ToString("x2") }))
}
```

## End-to-end demo (copy/paste)

1) Create a valid transaction via core

```powershell
$motionSeal = Get-MariMotionSeal -x 0.1 -y 0.2 -z 0.3

$txBody = @{
  senderBioHash = "bioSender123"
  receiverBioHash = "bioReceiver789"
  amount = 100.0
  locationGrid = "grid123"
  coupon = "mari://xfer?from=bioSender123&to=bioReceiver789&val=100&g=grid123&exp=4102444800000&s=$motionSeal"
  physicsData = @{
    location = @{ grid = "grid123" }
    motion   = @{ x = 0.1; y = 0.2; z = 0.3 }
    timestamp = "2025-01-01T00:00:00Z"
  }
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method POST -Uri "http://localhost:3000/api/transactions" `
  -Headers @{ "Content-Type" = "application/json" } -Body $txBody
```

2) Register + login to core (JWT)

```powershell
$reg = @{
  username    = "merchant123"
  email       = "demo@mari.local"
  password    = "Passw0rd!"
  bioHash     = "bioMerchant001"
  phoneNumber = "+15555550123"
} | ConvertTo-Json

try { Invoke-RestMethod -Method POST -Uri "http://localhost:3000/api/auth/register" -Headers @{ "Content-Type"="application/json" } -Body $reg | Out-Null } catch {}

$login = @{ email="demo@mari.local"; password="Passw0rd!" } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Method POST -Uri "http://localhost:3000/api/auth/login" -Headers @{ "Content-Type"="application/json" } -Body $login
$token  = $loginResp.data.token
$userId = $loginResp.data.user.id
```

3) Create bank merchant account + sender reserve (call bank directly)

```powershell
# BANK merchant account → UUID (bankMerchantId)
$merchantReq = @{
  name = "Demo Merchant"
  mariBioHash = "bioMerchant001"
  initialBalance = 0
} | ConvertTo-Json

$merchantResp = Invoke-RestMethod -Method POST -Uri "http://localhost:3001/api/accounts" -Headers @{ "Content-Type" = "application/json" } -Body $merchantReq
$bankMerchantId = $merchantResp.data.id

# Sender reserve funding
$reserveReq = @{
  bioHash = "bioSender123"
  initialReserve = 1000
  userId = "user-for-demo"
} | ConvertTo-Json

Invoke-RestMethod -Method POST -Uri "http://localhost:3001/api/accounts/reserve" -Headers @{ "Content-Type" = "application/json" } -Body $reserveReq
```

4) Compute batch seal and run settlement via core

```powershell
$batchId = "batch-" + ([guid]::NewGuid().ToString())
$batchItems = @(@{ id="txn-1001"; amount=100.00 })
$batchSeal = Get-BatchSeal $batchItems

$batch = @{
  batchId = $batchId
  merchantId = $userId             # core user (ObjectId)
  bankMerchantId = $bankMerchantId # bank merchant (UUID)
  seal = $batchSeal
  transactions = @(
    @{
      id = "txn-1001"
      amount = 100.00
      coupon = "mari://xfer?from=bioSender123&to=bioReceiver789&val=100&g=grid123&exp=4102444800000&s=$motionSeal"
      physicsData = @{
        location = @{ grid = "grid123" }
        motion   = @{ x = 0.1; y = 0.2; z = 0.3 }
      }
    }
  )
} | ConvertTo-Json -Depth 6

$settlementResp = Invoke-RestMethod -Method POST -Uri "http://localhost:3000/api/settlement/process" `
  -Headers @{ "Content-Type"="application/json"; "Authorization" = "Bearer $token" } `
  -Body $batch

$settlementResp | ConvertTo-Json -Depth 10
```

5) Verify increment key (bank)

```powershell
$incrementKey = $settlementResp.data.incrementKey
$incKeyBody = @{ incrementKey = $incrementKey } | ConvertTo-Json
Invoke-RestMethod -Method POST -Uri "http://localhost:3001/api/hsm/verify" -Headers @{ "Content-Type" = "application/json" } -Body $incKeyBody | ConvertTo-Json -Depth 6
```

## Make targets (core)

```powershell
# From d:\mari_protocol_slim\mari-server
make health
make core-logs
make mongo-up
make core-up
make core-down
```

## Notes

- Use a fresh `batchId` for each run.
- The coupon’s motion seal (`s`) must match the value returned by `Get-MariMotionSeal` for the physics vector.
- `merchantId` in core payload is the core Mongo ObjectId; `bankMerchantId` is the UUID returned by the bank.
- The core validates batch seal and physics, forwards valid items to the bank, stores the bank’s `incrementKey`, and exposes the batch via its APIs.
