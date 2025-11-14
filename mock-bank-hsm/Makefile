# Makefile for Mari Mock Bank
# Usage: make <target>

# Mock Bank HSM Makefile

DOCKER_COMPOSE ?= docker compose
IMAGE ?= mari-mock-bank:latest
SERVICE ?= mari-mock-bank
PORT ?= 3001
NETWORK := mari-prod

# Default target
.DEFAULT_GOAL := help

.PHONY: help build up up-prod down logs health clean test shell

help:
	@echo "Mock Bank HSM - Standardized Targets"
	@echo "===================================="
	@echo ""
	@echo "🚀 Quick Start:"
	@echo "  make up             Start Mock Bank service"
	@echo "  make test           Run all tests"
	@echo "  make demo           Run end-to-end demo"
	@echo ""
	@echo "📦 Core Operations:"
	@echo "  make build          Build Docker image"
	@echo "  make up             Start service (development)"
	@echo "  make up-prod        Start on production network"
	@echo "  make down           Stop service"
	@echo "  make restart        Restart service"
	@echo "  make logs           Show service logs"
	@echo "  make health         Check service health"
	@echo "  make clean          Clean containers and networks"
	@echo ""
	@echo "🔧 Development:"
	@echo "  make test           Run tests"
	@echo "  make shell          Open interactive shell"
	@echo "  make lint           Run linting (if available)"
	@echo ""
	@echo "🎯 Workflows:"
	@echo "  make demo           Run end-to-end demo"

# ----------------------------------------------------------------------------
# Core Operations (Standardized)
# ----------------------------------------------------------------------------

build:
	@echo "📦 Building Mock Bank HSM image..."
	$(DOCKER_COMPOSE) build
	@echo "✅ Build complete"

# Start in development mode
up: setup-network
	@echo "🚀 Starting Mock Bank HSM (development mode)..."
	$(DOCKER_COMPOSE) up -d
	@echo "✅ Mock Bank running at http://localhost:$(PORT)"

# Start in production mode
up-prod: setup-prod-network
	@echo "🚀 Starting Mock Bank HSM (production mode)..."
	$(DOCKER_COMPOSE) up -d
	@echo "✅ Mock Bank running on production network"

down:
	@echo "🛑 Stopping Mock Bank HSM..."
	$(DOCKER_COMPOSE) down
	@echo "✅ Service stopped"

restart: down up

logs:
	$(DOCKER_COMPOSE) logs -f $(SERVICE)

health:
	@echo "🔍 Checking Mock Bank health..."
	curl -sf http://localhost:$(PORT)/health && echo "✅ Service healthy" || echo "❌ Service not responding"

clean:
	@echo "🧹 Cleaning Mock Bank containers and networks..."
	-@docker rm -f mari-mock-bank 2>/dev/null || true
	-@docker network rm mari-net mari-prod 2>/dev/null || true
	-@docker image rm $(IMAGE) 2>/dev/null || true
	-@powershell -NoProfile -Command "if (Test-Path reports) { Remove-Item -Recurse -Force reports }" 2>/dev/null || true
	@echo "✅ Cleanup complete"

# ----------------------------------------------------------------------------
# Development & Testing (Standardized)
# ----------------------------------------------------------------------------
.PHONY: test shell lint demo

# Run tests in containerized environment
test:
	@echo "🧪 Running Mock Bank tests..."
	docker run --rm -v "${PWD}:/work" -w /work node:20-alpine sh -c "npm install && npm test"
	@echo "✅ Tests completed"

# Open interactive shell
shell:
	$(DOCKER_COMPOSE) exec $(SERVICE) sh || $(DOCKER_COMPOSE) run --rm $(SERVICE) sh

# Placeholder for linting
lint:
	@echo "🔍 Running linting..."
	@echo "ℹ️  No lint configuration available"

# Run end-to-end demo
demo: up
	@echo "🎬 Running Mock Bank end-to-end demo..."
	powershell -ExecutionPolicy Bypass -File scripts/demo-e2e.ps1
	@echo "✅ Demo completed! Check reports/ directory for results"

# ----------------------------------------------------------------------------
# Network & Infrastructure Setup (Internal)
# ----------------------------------------------------------------------------
.PHONY: setup-network setup-prod-network

# Setup development network
setup-network:
	@echo "🔗 Setting up development network..."
	-@docker network create mari-net 2>/dev/null || true
	-@docker network create mari-prod 2>/dev/null || true

# Setup production network
setup-prod-network:
	@echo "🔗 Setting up production network..."
	-@docker network create $(NETWORK) 2>/dev/null || true

