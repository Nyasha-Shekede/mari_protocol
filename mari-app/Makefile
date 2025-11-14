# Simple Makefile for Mari-mobile-app
# Works on Windows with make (e.g., MSYS2/Chocolatey/Git Bash) and on macOS/Linux

# Variables - Cross-platform support
ifeq ($(OS),Windows_NT)
    GRADLEW := gradlew.bat
    ADB ?= $(LOCALAPPDATA)/Android/Sdk/platform-tools/adb.exe
    RM := del /f /q
    RMDIR := rmdir /s /q
else
    GRADLEW := ./gradlew
    UNAME_S := $(shell uname -s)
    ifeq ($(UNAME_S),Darwin)
        ADB ?= $(HOME)/Library/Android/sdk/platform-tools/adb
    else
        ADB ?= $(HOME)/Android/Sdk/platform-tools/adb
    endif
    RM := rm -f
    RMDIR := rm -rf
endif

APP_MOD := :app
APK_DEBUG := app/build/outputs/apk/debug/app-debug.apk
AAB_RELEASE := app/build/outputs/bundle/release/app-release.aab
APK_RELEASE := app/build/outputs/apk/release/app-release.apk
PACKAGE ?= com.Mari.mobile

# Docker-based Android build
DOCKER_IMAGE ?= mari-app-android-build

# Default target
.DEFAULT_GOAL := help

.PHONY: help
help:
	@echo "Mari Mobile App - Standardized Targets"
	@echo "======================================"
	@echo ""
	@echo "🚀 Quick Start:"
	@echo "  make build          Build debug APK"
	@echo "  make install        Install and launch app"
	@echo "  make test           Run all tests"
	@echo ""
	@echo "📦 Core Operations:"
	@echo "  make build          Build debug APK (development)"
	@echo "  make build-prod     Build release APK (production)"
	@echo "  make install        Install debug APK and launch"
	@echo "  make uninstall      Remove app from device"
	@echo "  make clean          Clean build artifacts"
	@echo ""
	@echo "🔧 Development:"
	@echo "  make test           Run unit tests"
	@echo "  make test-ui        Run instrumented tests"
	@echo "  make lint           Run Android lint"
	@echo "  make logs           Show app logs"
	@echo ""
	@echo "📱 Device Management:"
	@echo "  make devices        List connected devices"
	@echo "  make run            Launch installed app"
	@echo "  make logs-errors    Show error logs only"
	@echo "  make clear-logs     Clear device logs"
	@echo ""
	@echo "🎯 Specialized Builds:"
	@echo "  make build-physical Build for physical device"
	@echo "  make bundle         Build release bundle (Play Store)"
	@echo ""
	@echo "💡 Examples:"
	@echo "  make install                        # Quick dev cycle"
	@echo "  make clean build install            # Full rebuild"
	@echo "  make test lint                      # Quality checks"
	@echo "  make build-prod                     # Production build"

# ----------------------------------------------------------------------------
# Core Operations (Standardized)
# ----------------------------------------------------------------------------
.PHONY: build build-prod build-physical build-docker install uninstall clean

# Build debug APK (default development build)
build:
	@echo "📦 Building Mari App (debug)..."
	$(GRADLEW) -p . $(APP_MOD):assembleDebug
	@echo "✅ Debug APK built: $(APK_DEBUG)"

# Build debug APK inside Docker (no local JDK/Android SDK required)
build-docker:
	@echo "🐳 Building Docker image $(DOCKER_IMAGE)..."
	docker build -t $(DOCKER_IMAGE) .
	@echo "📦 Building Mari App (debug) inside Docker..."
	docker run --rm -v $(PWD):/workspace -w /workspace $(DOCKER_IMAGE) ./gradlew -p . $(APP_MOD):assembleDebug
	@echo "✅ Debug APK built (Docker): $(APK_DEBUG)"

# Build release APK (production)
build-prod:
	@echo "📦 Building Mari App (release)..."
	$(GRADLEW) -p . $(APP_MOD):assembleRelease
	@echo "✅ Release APK built"

# Build for physical device with custom endpoints
# Usage: make build-physical CORE=http://192.168.1.10:3000 BANK=http://192.168.1.10:3001 ALLOWED="+15551234567"
build-physical:
	@echo "📦 Building Mari App (physical device)..."
	$(GRADLEW) -p . $(APP_MOD):assembleDebugPhysical \
	-PcoreBaseUrlPhysical="$(CORE)" \
	-PbankBaseUrlPhysical="$(BANK)" \
	-PallowedSmsSendersPhysical="$(ALLOWED)"
	@echo "✅ Physical device APK built"

# Build release bundle for Play Store
bundle:
	@echo "📦 Building release bundle..."
	$(GRADLEW) -p . $(APP_MOD):bundleRelease
	@echo "✅ Release bundle built"

# Install debug APK and launch app
install: build
	@echo "📦 Installing and launching Mari App..."
	"$(ADB)" install -r "$(APK_DEBUG)"
	"$(ADB)" shell monkey -p $(PACKAGE) -c android.intent.category.LAUNCHER 1
	@echo "✅ App installed and launched"

# Remove app from device
uninstall:
	@echo "🗑️ Uninstalling Mari App..."
	"$(ADB)" uninstall $(PACKAGE) || true
	@echo "✅ App uninstalled"

# Clean build artifacts
clean:
	@echo "🧹 Cleaning build artifacts..."
	$(GRADLEW) -p . clean
	@echo "✅ Cleanup complete"

# ----------------------------------------------------------------------------
# Development & Testing (Standardized)
# ----------------------------------------------------------------------------
.PHONY: test test-ui lint logs run

# Run unit tests
test:
	@echo "🧪 Running unit tests..."
	$(GRADLEW) -p . $(APP_MOD):testDebugUnitTest
	@echo "✅ Unit tests completed"

# Run instrumented UI tests
test-ui:
	@echo "🧪 Running instrumented tests..."
	$(GRADLEW) -p . $(APP_MOD):connectedDebugAndroidTest
	@echo "✅ UI tests completed"

# Run Android lint
lint:
	@echo "🔍 Running Android lint..."
	$(GRADLEW) -p . $(APP_MOD):lintDebug
	@echo "✅ Lint completed"

# Show app logs
logs:
	@echo "📝 Showing Mari App logs..."
	"$(ADB)" logcat | grep -E "$(PACKAGE)"

# Launch installed app
run:
	@echo "🚀 Launching Mari App..."
	"$(ADB)" shell monkey -p $(PACKAGE) -c android.intent.category.LAUNCHER 1

# ----------------------------------------------------------------------------
# Device Management & Utilities (Standardized)
# ----------------------------------------------------------------------------
.PHONY: devices logs-errors clear-logs adb-start adb-kill internet-panel

# List connected devices
devices:
	@echo "📱 Connected devices:"
	"$(ADB)" devices

# Show error logs only
logs-errors:
	@echo "❌ Showing error logs..."
	"$(ADB)" logcat *:E | grep -E "AndroidRuntime|FATAL EXCEPTION|$(PACKAGE)"

# Clear device logs
clear-logs:
	@echo "🧹 Clearing device logs..."
	"$(ADB)" logcat -c
	@echo "✅ Logs cleared"

# Start ADB server
adb-start:
	@echo "🔄 Starting ADB server..."
	"$(ADB)" start-server

# Kill ADB server
adb-kill:
	@echo "🛑 Killing ADB server..."
	"$(ADB)" kill-server

# Open device internet settings
internet-panel:
	@echo "🌐 Opening internet settings..."
	"$(ADB)" shell am start -a "android.settings.panel.action.INTERNET_CONNECTIVITY"

# Deep clean (including Gradle cache)
clean-all: clean
	@echo "🧹 Deep cleaning (including Gradle cache)..."
	$(RMDIR) .gradle 2>nul || true
	$(RMDIR) app/build 2>nul || true
	$(RMDIR) build 2>nul || true
	@echo "✅ Deep cleanup complete"

# Clean repository files
cleanup-repo:
	@echo "🧹 Cleaning repository files..."
	$(RM) *.hprof 2>nul || true
	$(RMDIR) .idea 2>nul || true
	$(RM) *.iml 2>nul || true
	$(RM) .DS_Store 2>nul || true
	$(RM) Thumbs.db 2>nul || true
	@echo "✅ Repository cleanup complete"

# ----------------------------------------------------------------------------
# Legacy Compatibility (Deprecated)
# ----------------------------------------------------------------------------
.PHONY: debug debug_physical release install_debug install_debug_physical install_run connected logs_all clearlog logs_errors

# Legacy build targets
debug:
	@echo "⚠️  'make debug' is deprecated. Use 'make build' instead"
	$(MAKE) build

debug_physical:
	@echo "⚠️  'make debug_physical' is deprecated. Use 'make build-physical' instead"
	$(MAKE) build-physical CORE=$(CORE) BANK=$(BANK) ALLOWED=$(ALLOWED)

release:
	@echo "⚠️  'make release' is deprecated. Use 'make build-prod' instead"
	$(MAKE) build-prod

# Legacy install targets
install_debug:
	@echo "⚠️  'make install_debug' is deprecated. Use 'make install' instead"
	$(MAKE) install

install_debug_physical:
	@echo "⚠️  'make install_debug_physical' is deprecated. Use 'make build-physical && make install' instead"
	$(MAKE) build-physical CORE=$(CORE) BANK=$(BANK) ALLOWED=$(ALLOWED)
	"$(ADB)" install -r "app/build/outputs/apk/debugPhysical/app-debugPhysical.apk"

install_run:
	@echo "⚠️  'make install_run' is deprecated. Use 'make install' instead"
	$(MAKE) install

# Legacy test targets
connected:
	@echo "⚠️  'make connected' is deprecated. Use 'make test-ui' instead"
	$(MAKE) test-ui

# Legacy log targets
logs_all:
	@echo "⚠️  'make logs_all' is deprecated. Use 'make logs' instead"
	$(MAKE) logs

clearlog:
	@echo "⚠️  'make clearlog' is deprecated. Use 'make clear-logs' instead"
	$(MAKE) clear-logs

logs_errors:
	@echo "⚠️  'make logs_errors' is deprecated. Use 'make logs-errors' instead"
	$(MAKE) logs-errors
