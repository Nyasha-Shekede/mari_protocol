# Mari end-to-end demo script (PowerShell)
# Runs a full settlement via the core server delegating to the mock bank.
# Prereqs: bank on http://localhost:3001, core on http://localhost:3000, Mongo running.

$ErrorActionPreference = 'Stop'

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

function Get-Json($url) {
  return Invoke-RestMethod -Method GET -Uri $url
}

function Post-Json($url, $bodyObj, $headers=@{}) {
  $json = if ($bodyObj -is [string]) { $bodyObj } else { $bodyObj | ConvertTo-Json -Depth 10 }
  return Invoke-RestMethod -Method POST -Uri $url -Headers ($headers + @{ 'Content-Type'='application/json' }) -Body $json
}

function Invoke-SentinelInferenceOptional() {
  try {
    $sentinelUrl = $env:SENTINEL_URL
    if (-not $sentinelUrl -or $sentinelUrl.Trim() -eq '') { return }
    if ($sentinelUrl.EndsWith('/')) { $sentinelUrl = $sentinelUrl.TrimEnd('/') }
    $headers = @{}
    if ($env:SENTINEL_AUTH_TOKEN) { $headers['X-Mari-Auth'] = $env:SENTINEL_AUTH_TOKEN }

    Write-Host "== Sentinel readiness check =="
    $ready = Get-Json ("$sentinelUrl/ready")
    Write-Host ($ready | ConvertTo-Json -Depth 5)

    Write-Host "== Sentinel inference smoke =="
    $payload = @{ coupon_hash='a7ff3e82b53cafe'; kid='a1b2c3d4'; expiry_ts=32503680000000; seal='8a2f3b91'; grid_id='grid-xyz'; amount=1.5 }
    $resp = Post-Json ("$sentinelUrl/inference") $payload $headers
    Write-Host ("Sentinel score=" + $resp.score + " model_id=" + $resp.model_id)
  } catch {
    Write-Host "(Sentinel optional) Skipping due to error: $($_.Exception.Message)" -ForegroundColor Yellow
  }
}

try {
  Write-Host "== Health checks =="
  $coreHealth = Get-Json "http://localhost:3000/health"
  $bankHealth = Get-Json "http://localhost:3001/health"
  Write-Host "Core:" ($coreHealth | ConvertTo-Json -Depth 3)
  Write-Host "Bank:" ($bankHealth | ConvertTo-Json -Depth 3)

  # Optional: verify Sentinel wiring if SENTINEL_URL is provided
  Invoke-SentinelInferenceOptional

  Write-Host "== Register + Login (core) =="
  $reg = @{ username='merchant123'; email='demo@mari.local'; password='Passw0rd!'; bioHash='bioMerchant001'; phoneNumber='+15555550123' }
  try { Post-Json "http://localhost:3000/api/auth/register" $reg | Out-Null } catch {}
  $login = @{ email='demo@mari.local'; password='Passw0rd!' }
  $loginResp = Post-Json "http://localhost:3000/api/auth/login" $login
  $token = $loginResp.data.token
  $userId = $loginResp.data.user.id
  Write-Host "token prefix:" $token.Substring(0,12) "..." " userId:" $userId

  Write-Host "== Prepare bank merchant + reserve =="
  $merchantReq = @{ name='Demo Merchant'; mariBioHash='bioMerchant001'; initialBalance=0 }
  $merchantResp = Post-Json "http://localhost:3001/api/accounts" $merchantReq
  $bankMerchantId = $merchantResp.data.id
  Write-Host "bankMerchantId:" $bankMerchantId

  $reserveReq = @{ bioHash='bioSender123'; initialReserve=1000; userId='user-for-demo' }
  $reserveResp = Post-Json "http://localhost:3001/api/accounts/reserve" $reserveReq

  Write-Host "== Create valid transactions (core) =="
  $motionSeal = Get-MariMotionSeal -x 0.1 -y 0.2 -z 0.3

  # Transaction 1: amount 100
  $tx1 = @{ senderBioHash='bioSender123'; receiverBioHash='bioReceiver789'; amount=100.0; locationGrid='grid123';
            coupon="mari://xfer?from=bioSender123&to=bioReceiver789&val=100&g=grid123&exp=4102444800000&s=$motionSeal";
            physicsData=@{ location=@{ grid='grid123' }; motion=@{ x=0.1; y=0.2; z=0.3 }; timestamp='2025-01-01T00:00:00Z' } }
  $txResp1 = Post-Json "http://localhost:3000/api/transactions" $tx1
  Write-Host ($txResp1 | ConvertTo-Json -Depth 6)

  # Transaction 2: amount 50
  $tx2 = @{ senderBioHash='bioSender123'; receiverBioHash='bioReceiver789'; amount=50.0; locationGrid='grid123';
            coupon="mari://xfer?from=bioSender123&to=bioReceiver789&val=50&g=grid123&exp=4102444800000&s=$motionSeal";
            physicsData=@{ location=@{ grid='grid123' }; motion=@{ x=0.1; y=0.2; z=0.3 }; timestamp='2025-01-01T00:00:00Z' } }
  $txResp2 = Post-Json "http://localhost:3000/api/transactions" $tx2
  Write-Host ($txResp2 | ConvertTo-Json -Depth 6)

  Write-Host "== Settlement via core (multi-transaction batch) =="
  $batchId = "batch-" + ([guid]::NewGuid().ToString())
  $batchItems = @(@{ id='txn-1001'; amount=100.00 }, @{ id='txn-1002'; amount=50.00 })
  $batchSeal = Get-BatchSeal $batchItems
  $batch = @{ batchId=$batchId; merchantId=$userId; bankMerchantId=$bankMerchantId; seal=$batchSeal;
              transactions=@(
                @{ id='txn-1001'; amount=100.00; coupon=$tx1.coupon; physicsData=$tx1.physicsData },
                @{ id='txn-1002'; amount=50.00;  coupon=$tx2.coupon; physicsData=$tx2.physicsData }
              ) }
  $headers = @{ Authorization = "Bearer $token" }
  $settlementResp = Post-Json "http://localhost:3000/api/settlement/process" $batch $headers
  Write-Host ($settlementResp | ConvertTo-Json -Depth 10)

  $incrementKey = $settlementResp.data.incrementKey
  if (-not $incrementKey) { throw 'No incrementKey returned from settlement' }

  Write-Host "== Verify increment key (bank) =="
  $verifyResp = Post-Json "http://localhost:3001/api/hsm/verify" @{ incrementKey=$incrementKey }
  Write-Host ($verifyResp | ConvertTo-Json -Depth 6)

  Write-Host "== Write report file =="
  $reportDir = Join-Path (Get-Location) "reports"
  if (-not (Test-Path $reportDir)) { New-Item -ItemType Directory -Path $reportDir | Out-Null }
  $ts = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
  $reportPath = Join-Path $reportDir ("demo-report-" + $ts + ".json")
  $report = [ordered]@{
    timestampUtc = [DateTime]::UtcNow.ToString("o")
    batchId = $batchId
    incrementKey = $incrementKey
    coreUserId = $userId
    bankMerchantId = $bankMerchantId
    totals = @{
      processed = $settlementResp.data.processed
      successful = $settlementResp.data.successful
      failed = $settlementResp.data.failed
      totalAmount = $settlementResp.data.totalAmount
      totalCommission = $settlementResp.data.totalCommission
    }
    transactions = $settlementResp.data.transactions
  }
  ($report | ConvertTo-Json -Depth 10) | Out-File -FilePath $reportPath -Encoding UTF8
  Write-Host "Report written to: $reportPath"

  if ($verifyResp.success -and $verifyResp.data.isValid) {
    Write-Host "\n=== DEMO PASSED ===" -ForegroundColor Green
    exit 0
  } else {
    Write-Host "\n=== DEMO FAILED: increment key invalid ===" -ForegroundColor Red
    exit 2
  }
} catch {
  Write-Host "\n=== DEMO FAILED ===" -ForegroundColor Red
  Write-Host $_.Exception.Message
  if ($_.Exception.Response -and $_.Exception.Response.Content) {
    Write-Host ( $_.Exception.Response.Content | ConvertTo-Json -Depth 10 )
  }
  exit 1
}
