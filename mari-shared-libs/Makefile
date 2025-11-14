SHELL := /bin/sh
PACKAGE_NAME := mari-shared-libs
IMAGE := $(PACKAGE_NAME):latest

# Default target
.DEFAULT_GOAL := help

.PHONY: help build up test clean shell lint docs

help:
	@echo "Mari Shared Libraries - Standardized Targets"
	@echo "============================================"
	@echo ""
	@echo "🚀 Quick Start:"
	@echo "  make build          Build library"
	@echo "  make test           Run all tests"
	@echo "  make docs           Generate documentation"
	@echo ""
	@echo "📦 Core Operations:"
	@echo "  make build          Build Docker image and library"
	@echo "  make test           Run tests in container"
	@echo "  make clean          Clean build artifacts"
	@echo ""
	@echo "🔧 Development:"
	@echo "  make shell          Open interactive shell"
	@echo "  make lint           Run linting"
	@echo "  make dev            Development mode (use shell)"
	@echo ""
	@echo "📚 Documentation:"
	@echo "  make docs           Generate TypeDoc documentation"
	@echo "  make example        Run example code"

all: build test

# ----------------------------------------------------------------------------
# Core Operations (Standardized)
# ----------------------------------------------------------------------------

build:
	@echo "📦 Building Mari Shared Libraries..."
	docker build -t $(IMAGE) .
	@echo "✅ Build complete"

# Alias for consistency (shared libs don't "start" but we provide build)
up: build

test:
	@echo "🧪 Running tests in container..."
	docker build --target=test -t $(IMAGE):test .
	@echo "✅ Tests completed"

clean:
	@echo "🧹 Cleaning build artifacts..."
	rm -rf dist coverage node_modules
	@echo "✅ Cleanup complete"

# ----------------------------------------------------------------------------
# Development & Testing (Standardized)
# ----------------------------------------------------------------------------

# Open interactive development shell
shell:
	@echo "🔧 Opening development shell..."
	docker run --rm -it -v $(PWD):/app -w /app node:20-alpine sh

# Development mode (use shell for interactive development)
dev:
	@echo "🔧 Starting development mode..."
	@echo "💡 Use 'make shell' to open interactive development environment"
	@echo "📝 Inside shell: npm run dev, npm run build, npm test"

test-host:
	@echo "🧪 Running tests on host (outputs coverage)..."
	docker run --rm -v $(PWD):/app -w /app node:20-alpine sh -lc "npm i && npm test"

lint:
	@echo "🔍 Running ESLint..."
	docker run --rm -v $(PWD):/app -w /app node:20-alpine sh -lc "npm i -D eslint @typescript-eslint/parser @typescript-eslint/eslint-plugin >/dev/null 2>&1 || true; npx eslint src/"

# ----------------------------------------------------------------------------
# Documentation & Examples (Standardized)
# ----------------------------------------------------------------------------

docs:
	@echo "📚 Generating TypeDoc documentation..."
	docker run --rm -v $(PWD):/app -w /app node:20-alpine sh -lc "npm i >/dev/null 2>&1 || true; npx typedoc --options typedoc.json"
	@echo "✅ Documentation generated in docs/"

example:
	@echo "📝 Running example code..."
	docker run --rm -v $(PWD):/app -w /app node:20-alpine sh -lc "npm i >/dev/null 2>&1 || true; npm i -D ts-node tsconfig-paths >/dev/null 2>&1 || true; node -r ts-node/register -r tsconfig-paths/register examples/basic-usage.ts"
	@echo "✅ Example completed"

# Build library and output to host
build-host:
	@echo "📦 Building library on host..."
	docker run --rm -v $(PWD):/app -w /app node:20-alpine sh -lc "npm i && npm run build && npm run docs"
	@echo "✅ Build artifacts written to host"

# ----------------------------------------------------------------------------
# Legacy Compatibility (Deprecated)
# ----------------------------------------------------------------------------
.PHONY: install docker-build docker-test docker-shell docker-example docker-docs docker-build-host docker-test-host

# Legacy install
install:
	@echo "⚠️  'make install' is deprecated. Use 'make build' instead"
	$(MAKE) build

# Legacy Docker targets
docker-build:
	@echo "⚠️  'make docker-build' is deprecated. Use 'make build' instead"
	$(MAKE) build

docker-test:
	@echo "⚠️  'make docker-test' is deprecated. Use 'make test' instead"
	$(MAKE) test

docker-shell:
	@echo "⚠️  'make docker-shell' is deprecated. Use 'make shell' instead"
	$(MAKE) shell

docker-example:
	@echo "⚠️  'make docker-example' is deprecated. Use 'make example' instead"
	$(MAKE) example

docker-docs:
	@echo "⚠️  'make docker-docs' is deprecated. Use 'make docs' instead"
	$(MAKE) docs

docker-build-host:
	@echo "⚠️  'make docker-build-host' is deprecated. Use 'make build-host' instead"
	$(MAKE) build-host

docker-test-host:
	@echo "⚠️  'make docker-test-host' is deprecated. Use 'make test-host' instead"
	$(MAKE) test-host
