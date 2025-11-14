# Render Mermaid diagrams for Mari Core docs
# Requires Node with npx OR Docker Desktop.
# Prefer local npx. If unavailable, attempt Docker mermaid-cli image.

$ErrorActionPreference = 'Stop'

function Render-Mermaid($input, $outPng, $outSvg) {
  try {
    npx -y @mermaid-js/mermaid-cli -i $input -o $outPng | Out-Null
    npx -y @mermaid-js/mermaid-cli -i $input -o $outSvg | Out-Null
  } catch {
    Write-Host "npx mermaid-cli not available; trying Docker..."
    docker run --rm -v "${PWD}:/data" minlag/mermaid-cli mmdc -i $input -o $outPng
    docker run --rm -v "${PWD}:/data" minlag/mermaid-cli mmdc -i $input -o $outSvg
  }
}

$dir = Join-Path (Get-Location) "docs/diagrams"
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }

$input = Join-Path $dir "core-architecture.mmd"
$png   = Join-Path $dir "core-architecture.png"
$svg   = Join-Path $dir "core-architecture.svg"
if (-not (Test-Path $input)) { throw "Diagram source not found: $input" }

Render-Mermaid -input $input -outPng $png -outSvg $svg

Write-Host "Rendered:" $png
Write-Host "Rendered:" $svg
