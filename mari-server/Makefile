# Mari Core Server Makefile

SHELL := /bin/sh
COMPOSE ?= docker compose
APP_SERVICE := app
DEV_SERVICE := app-dev
MONGO_SERVICE := mongo
NETWORK := mari-prod

# Default target
.DEFAULT_GOAL := help

.PHONY: help
help:
	@echo "Mari Core Server - Standardized Targets"
	@echo "======================================="
	@echo ""
	@echo "🚀 Quick Start:"
	@echo "  make dev            Start development server with live reload"
	@echo "  make up             Start production server"
	@echo "  make demo           Run complete end-to-end demo"
	@echo ""
	@echo "📦 Core Operations:"
	@echo "  make build          Build Docker images"
	@echo "  make up             Start service (production)"
	@echo "  make up-prod        Start on production network with Sentinel"
	@echo "  make down           Stop all services"
	@echo "  make restart        Restart services"
	@echo "  make logs           Show service logs"
	@echo "  make health         Check service health"
	@echo "  make clean          Clean containers and networks"
	@echo ""
	@echo "🔧 Development:"
	@echo "  make dev            Start with live reload"
	@echo "  make test           Run tests"
	@echo "  make lint           Run linting (if available)"
	@echo "  make shell          Open interactive shell"
	@echo ""
	@echo "🎯 Workflows:"
	@echo "  make install        Install dependencies locally"
	@echo "  make seed           Initialize/seed data"
	@echo "  make demo           Run end-to-end demo"

# ----------------------------------------------------------------------------
# Core Operations (Standardized)
# ----------------------------------------------------------------------------
.PHONY: install build up up-prod down restart logs health clean

install:
	npm install

build:
	$(COMPOSE) build

# Start in development mode (default)
up: setup-network
	@echo "🚀 Starting Mari Server (development mode)..."
	$(COMPOSE) up -d $(MONGO_SERVICE) $(DEV_SERVICE)
	@echo "✅ Server running at http://localhost:3003"

# Start in production mode with full network setup
up-prod: setup-prod-network ensure-sentinel
	@echo "🚀 Starting Mari Server (production mode)..."
	SENTINEL_URL=http://inference:3002 $(COMPOSE) up -d $(MONGO_SERVICE) $(APP_SERVICE)
	@echo "✅ Server running at http://localhost:3000"

down:
	$(COMPOSE) down

restart: down up

logs:
	$(COMPOSE) logs -f

health:
	curl -sf http://localhost:3000/health || echo "❌ Service not responding"

clean:
	@echo "🧹 Cleaning Mari Server containers and networks..."
	-@docker rm -f mari-core mari-core-dev mari-mongo 2>/dev/null || true
	-@docker network rm mari-net mari-prod 2>/dev/null || true
	@echo "✅ Cleanup complete"

# ----------------------------------------------------------------------------
# Development & Testing (Standardized)
# ----------------------------------------------------------------------------
.PHONY: dev test lint shell

# Start development server with live reload
dev: setup-network
	@echo "🔧 Starting Mari Server (development with live reload)..."
	$(COMPOSE) up -d $(MONGO_SERVICE) $(DEV_SERVICE)
	@echo "✅ Development server running at http://localhost:3003"
	@echo "📝 Logs: make logs-dev"

test:
	@echo "🧪 Running tests..."
	npm test 2>/dev/null || $(COMPOSE) run --rm $(DEV_SERVICE) npm test

lint:
	@echo "🔍 Running linting..."
	npm run lint 2>/dev/null || echo "ℹ️  No lint script configured"

shell:
	$(COMPOSE) exec $(DEV_SERVICE) sh || $(COMPOSE) run --rm $(DEV_SERVICE) sh

# Development-specific log viewing
logs-dev:
	$(COMPOSE) logs -f $(DEV_SERVICE)

# ----------------------------------------------------------------------------
# Workflows & Automation (Standardized)
# ----------------------------------------------------------------------------
.PHONY: demo seed diagrams

# Complete end-to-end demo workflow
demo: up-prod
	@echo "🎬 Running Mari Protocol end-to-end demo..."
	powershell -ExecutionPolicy Bypass -File scripts/demo-e2e.ps1
	@echo "✅ Demo completed! Check reports/ directory for results"

# Initialize/seed data (placeholder for future use)
seed:
	@echo "🌱 Seeding Mari Server data..."
	@echo "ℹ️  No seeding required for Mari Server"

# Generate documentation diagrams
diagrams:
	@echo "📊 Rendering Mermaid diagrams..."
	powershell -ExecutionPolicy Bypass -File scripts/render-diagrams.ps1

# ----------------------------------------------------------------------------
# Network & Infrastructure Setup (Internal)
# ----------------------------------------------------------------------------
.PHONY: setup-network setup-prod-network ensure-sentinel

# Setup basic development network
setup-network:
	@echo "🔗 Setting up development network..."
	-@docker network create mari-net 2>/dev/null || true
	-@docker network create mari-prod 2>/dev/null || true

# Setup production network and ensure Sentinel is available
setup-prod-network:
	@echo "🔗 Setting up production network..."
	-@docker network create $(NETWORK) 2>/dev/null || true

# Ensure Sentinel is running (for production mode)
ensure-sentinel:
	@echo "🤖 Checking Sentinel availability..."
	@(curl -sf http://localhost:3002/ready >/dev/null 2>&1) || \
	{ echo "⚠️  Sentinel not reachable at localhost:3002"; \
	  if [ -n "$(SENTINEL_DIR)" ] && [ -f "$(SENTINEL_DIR)/docker-compose.yml" ]; then \
	    echo "🚀 Starting Sentinel from $(SENTINEL_DIR)..."; \
	    (cd "$(SENTINEL_DIR)" && make up-prod); \
	  else \
	    echo "💡 Hint: Start Sentinel manually or set SENTINEL_DIR=../mari-sentinel"; \
	  fi; \
	}
 
