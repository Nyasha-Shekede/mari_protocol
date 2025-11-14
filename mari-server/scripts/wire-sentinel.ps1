# Wire Mari Server containers to the Sentinel network (mari-prod)
# - Creates the mari-prod network if missing
# - Connects core/bank containers to mari-prod
# - Sets optional env guidance for SENTINEL_URL=http://inference:3002
# Safe: does not remove or restart containers. Idempotent connects.

param(
  [string]$NetworkName = "mari-prod",
  [int]$CorePort = 3000,
  [int]$BankPort = 3001
)

$ErrorActionPreference = 'Stop'

function Ensure-Network($name) {
  $exists = (docker network ls --format '{{.Name}}' | Where-Object { $_ -eq $name })
  if (-not $exists) {
    Write-Host "Creating network $name"
    docker network create $name | Out-Null
  } else {
    Write-Host "Network $name already exists"
  }
}

function Get-ContainerByPublishedPort([int]$port) {
  $lines = docker ps --format '{{.ID}} {{.Ports}} {{.Names}}'
  foreach ($l in $lines) {
    $parts = $l -split ' '
    if ($parts.Length -lt 3) { continue }
    $id = $parts[0]
    $ports = $parts[1]
    $name = $parts[2]
    if ($ports -match ":$port->") { return $name }
  }
  return $null
}

function Connect-IfNeeded($container, $network) {
  if (-not $container) { return }
  $inspect = docker inspect $container --format '{{json .NetworkSettings.Networks}}'
  if ($inspect -like "*`"$network`":*") {
    Write-Host "$container already on $network"
    return
  }
  Write-Host "Connecting $container to $network"
  docker network connect $network $container | Out-Null
}

try {
  Ensure-Network $NetworkName
  $core = Get-ContainerByPublishedPort -port $CorePort
  $bank = Get-ContainerByPublishedPort -port $BankPort

  if (-not $core) { Write-Host "Warning: core (:$CorePort) not found, skipping connect" -ForegroundColor Yellow }
  if (-not $bank) { Write-Host "Warning: bank (:$BankPort) not found, skipping connect" -ForegroundColor Yellow }

  Connect-IfNeeded $core $NetworkName
  Connect-IfNeeded $bank $NetworkName

  Write-Host "Done. Set SENTINEL_URL=http://inference:3002 in mari-server services to call Sentinel via internal DNS."
  Write-Host "If Sentinel auth is enabled, also set SENTINEL_AUTH_TOKEN."
} catch {
  Write-Host "Failed to wire sentinel network: $($_.Exception.Message)" -ForegroundColor Red
  exit 1
}
