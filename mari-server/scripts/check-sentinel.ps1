# Verify Sentinel connectivity from mari-server context
# - Uses SENTINEL_URL (required) and optional SENTINEL_AUTH_TOKEN
# - Exits 0 on success, 1 on failure

param(
  [string]$Url = $env:SENTINEL_URL
)

$ErrorActionPreference = 'Stop'

function Get-Json($url) {
  return Invoke-RestMethod -Method GET -Uri $url
}

function Post-Json($url, $bodyObj, $headers=@{}) {
  $json = if ($bodyObj -is [string]) { $bodyObj } else { $bodyObj | ConvertTo-Json -Depth 10 }
  return Invoke-RestMethod -Method POST -Uri $url -Headers ($headers + @{ 'Content-Type'='application/json' }) -Body $json
}

try {
  if (-not $Url -or $Url.Trim() -eq '') { throw 'SENTINEL_URL is not set' }
  if ($Url.EndsWith('/')) { $Url = $Url.TrimEnd('/') }

  $headers = @{}
  if ($env:SENTINEL_AUTH_TOKEN) { $headers['X-Mari-Auth'] = $env:SENTINEL_AUTH_TOKEN }

  Write-Host "Checking: $Url/ready"
  $ready = Get-Json ("$Url/ready")
  Write-Host ($ready | ConvertTo-Json -Depth 5)

  Write-Host "Posting sample to: $Url/inference"
  $payload = @{ coupon_hash='a7ff3e82b53cafe'; kid='a1b2c3d4'; expiry_ts=32503680000000; seal='8a2f3b91'; grid_id='grid-xyz'; amount=1.5 }
  $resp = Post-Json ("$Url/inference") $payload $headers
  Write-Host ("OK: score=" + $resp.score + " model_id=" + $resp.model_id)
  exit 0
} catch {
  Write-Host "Sentinel check failed: $($_.Exception.Message)" -ForegroundColor Red
  exit 1
}
