#!/bin/bash
# Remote build script for Mari App
# Usage: ./build-remote.sh [server-address]

set -e

SERVER=${1:-"your-server.com"}
REMOTE_USER=${REMOTE_USER:-"ubuntu"}
REMOTE_PATH="/tmp/mari-app-build"
LOCAL_APK_DIR="./apk-downloads"

echo "🚀 Mari App Remote Build Script"
echo "================================"
echo "Server: $REMOTE_USER@$SERVER"
echo ""

# Create local download directory
mkdir -p "$LOCAL_APK_DIR"

echo "📦 Step 1: Uploading source code to remote server..."
rsync -avz --exclude='.gradle' --exclude='build' --exclude='app/build' \
  --exclude='.git' --exclude='*.apk' --exclude='*.aab' \
  ./ "$REMOTE_USER@$SERVER:$REMOTE_PATH/"

echo ""
echo "🐳 Step 2: Building APK on remote server..."
ssh "$REMOTE_USER@$SERVER" << 'ENDSSH'
cd /tmp/mari-app-build
echo "Building Docker image..."
docker build -t mari-app-build .
echo "Building APK (this may take 5-10 minutes)..."
docker run --rm -m 8g --memory-swap 10g \
  -v "$(pwd)":/workspace \
  -w /workspace \
  mari-app-build \
  ./gradlew --no-daemon --stacktrace :app:assembleDebug
echo "✅ Build complete!"
ENDSSH

echo ""
echo "⬇️  Step 3: Downloading APK from remote server..."
scp "$REMOTE_USER@$SERVER:$REMOTE_PATH/app/build/outputs/apk/debug/app-debug.apk" \
  "$LOCAL_APK_DIR/mari-app-debug-$(date +%Y%m%d-%H%M%S).apk"

echo ""
echo "🧹 Step 4: Cleaning up remote server..."
ssh "$REMOTE_USER@$SERVER" "rm -rf $REMOTE_PATH"

echo ""
echo "✅ Build complete!"
echo "📱 APK downloaded to: $LOCAL_APK_DIR/"
echo ""
echo "To install on your phone:"
echo "  adb install -r $LOCAL_APK_DIR/mari-app-debug-*.apk"
echo ""
echo "Or use: make install-downloaded"
