# Remote build script for Mari App (PowerShell/Windows)
# Usage: .\build-remote.ps1 -Server "your-server.com"

param(
    [string]$Server = "your-server.com",
    [string]$RemoteUser = "ubuntu",
    [string]$RemotePath = "/tmp/mari-app-build",
    [string]$LocalApkDir = ".\apk-downloads"
)

Write-Host "🚀 Mari App Remote Build Script" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Server: $RemoteUser@$Server"
Write-Host ""

# Create local download directory
New-Item -ItemType Directory -Force -Path $LocalApkDir | Out-Null

Write-Host "📦 Step 1: Uploading source code to remote server..." -ForegroundColor Yellow

# Using SCP (requires OpenSSH or PuTTY)
Write-Host "Creating archive..."
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$archiveName = "mari-app-$timestamp.tar.gz"

# Create tar.gz excluding build artifacts
tar -czf $archiveName --exclude='.gradle' --exclude='build' --exclude='app/build' `
    --exclude='.git' --exclude='*.apk' --exclude='*.aab' .

Write-Host "Uploading to server..."
scp $archiveName "${RemoteUser}@${Server}:${RemotePath}.tar.gz"

Write-Host ""
Write-Host "🐳 Step 2: Building APK on remote server..." -ForegroundColor Yellow

$buildScript = @"
cd /tmp
rm -rf $RemotePath
mkdir -p $RemotePath
cd $RemotePath
tar -xzf ${RemotePath}.tar.gz
echo 'Building Docker image...'
docker build -t mari-app-build .
echo 'Building APK (this may take 5-10 minutes)...'
docker run --rm -m 8g --memory-swap 10g \
  -v `$(pwd):/workspace \
  -w /workspace \
  mari-app-build \
  ./gradlew --no-daemon --stacktrace :app:assembleDebug
echo '✅ Build complete!'
"@

ssh "${RemoteUser}@${Server}" $buildScript

Write-Host ""
Write-Host "⬇️  Step 3: Downloading APK from remote server..." -ForegroundColor Yellow

$localApkPath = Join-Path $LocalApkDir "mari-app-debug-$timestamp.apk"
scp "${RemoteUser}@${Server}:${RemotePath}/app/build/outputs/apk/debug/app-debug.apk" $localApkPath

Write-Host ""
Write-Host "🧹 Step 4: Cleaning up remote server..." -ForegroundColor Yellow
ssh "${RemoteUser}@${Server}" "rm -rf $RemotePath ${RemotePath}.tar.gz"

# Clean up local archive
Remove-Item $archiveName

Write-Host ""
Write-Host "✅ Build complete!" -ForegroundColor Green
Write-Host "📱 APK downloaded to: $localApkPath" -ForegroundColor Green
Write-Host ""
Write-Host "To install on your phone:" -ForegroundColor Cyan
Write-Host "  adb install -r `"$localApkPath`"" -ForegroundColor White
Write-Host ""
Write-Host "Or use: make install-downloaded" -ForegroundColor Cyan
