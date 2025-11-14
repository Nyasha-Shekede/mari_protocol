PROJECT_NAME := mari-sentinel
NETWORK := mari-prod
PROD_NETWORK := mari-prod

# Default target
.DEFAULT_GOAL := help

.PHONY: help build up up-prod down logs health clean seed test

help:
	@echo "Mari Sentinel - Standardized Targets"
	@echo "===================================="
	@echo ""
	@echo "🚀 Quick Start:"
	@echo "  make up             Start all Sentinel services"
	@echo "  make seed           Initialize model and data"
	@echo "  make test           Test inference endpoint"
	@echo ""
	@echo "📦 Core Operations:"
	@echo "  make build          Build all Docker images"
	@echo "  make up             Start services (development)"
	@echo "  make up-prod        Start on production network"
	@echo "  make down           Stop all services"
	@echo "  make restart        Restart services"
	@echo "  make logs           Show service logs"
	@echo "  make health         Check service health"
	@echo "  make clean          Clean containers and images"
	@echo ""
	@echo "🤖 AI/ML Operations:"
	@echo "  make seed           Initialize model and Redis"
	@echo "  make train          Train new model from data"
	@echo "  make test           Test inference endpoint"
	@echo "  make seed-data      Add synthetic training data"
	@echo ""
	@echo "🔧 Development:"
	@echo "  make shell          Open interactive shell"
	@echo "  make lint           Run linting (if available)"

# ----------------------------------------------------------------------------
# Core Operations (Standardized)
# ----------------------------------------------------------------------------

build:
	@echo "📦 Building Mari Sentinel images..."
	docker compose build
	@echo "✅ Build complete"

# Start in development mode
up: setup-network
	@echo "🚀 Starting Mari Sentinel (development mode)..."
	docker compose up -d
	$(MAKE) seed
	@echo "✅ Sentinel running - Inference: http://localhost:3002"
	@echo "📊 Monitoring: http://localhost:3005 (Grafana)"

# Start in production mode with network setup
up-prod: setup-prod-network ensure-core
	@echo "🚀 Starting Mari Sentinel (production mode)..."
	docker compose up -d
	@echo "✅ Sentinel running on production network"

down:
	@echo "🛑 Stopping Mari Sentinel..."
	docker compose down
	@echo "✅ Services stopped"

restart: down up

logs:
	docker compose logs -f --tail=200

health:
	@echo "🔍 Checking Sentinel health..."
	@curl -sf http://localhost:3002/ready >/dev/null && echo "✅ Inference service healthy" || echo "❌ Inference service not responding"
	@curl -sf http://localhost:9095/-/healthy >/dev/null && echo "✅ Prometheus healthy" || echo "❌ Prometheus not responding"

clean:
	@echo "🧹 Cleaning Mari Sentinel containers and images..."
	-@docker image prune -f
	-@docker container prune -f
	@echo "✅ Cleanup complete"


# ----------------------------------------------------------------------------
# AI/ML Operations (Standardized)
# ----------------------------------------------------------------------------
.PHONY: seed train test seed-data

# Complete initialization: model + Redis seeding
seed: seed-model seed-redis
	@echo "✅ Mari Sentinel initialization complete!"

# Generate initial ONNX model
seed-model:
	@echo "🧠 Generating initial ONNX model..."
	MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*" docker run --rm -v "$(CURDIR)/scripts:/work" -w /work python:3.11-slim \
		sh -c "pip install --no-cache-dir numpy==1.26.4 scikit-learn==1.4.2 onnx==1.14.1 skl2onnx==1.16.0 packaging && python create_initial_model.py"
	@echo "✅ Model generated: scripts/initial_model.onnx"

# Seed Redis with initial model
seed-redis:
	@echo "💾 Seeding Redis with initial model..."
	MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*" docker run --rm -v "$(CURDIR)/scripts:/work" -w /work --network $(NETWORK) \
		-e REDIS_URL=redis://redis:6379 python:3.11-slim \
		sh -c "pip install --no-cache-dir redis && python seed_redis.py"
	@echo "✅ Redis seeded with model"

# Add synthetic training data
seed-data:
	@echo "🌱 Adding synthetic training data ($(or $(SEED_SYNTH_COUNT),200) examples)..."
	MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*" docker run --rm --network $(NETWORK) \
		-e REDIS_URL=redis://redis:6379 -e SEED_SYNTH_COUNT=$(or $(SEED_SYNTH_COUNT),200) \
		-v "$(CURDIR)/scripts:/work" -w /work python:3.11-slim \
		sh -c "pip install --no-cache-dir redis && python seed_synth.py"
	@echo "✅ Synthetic data added"

# Train new model from accumulated data
train:
	@echo "🎯 Training new model from labeled data..."
	docker compose run --rm model-builder
	@echo "✅ Model training complete"

# Test inference endpoint
test:
	@echo "🧪 Testing inference endpoint..."
	docker run --rm curlimages/curl:8.7.1 \
		-sS -X POST http://host.docker.internal:3002/inference \
		-H "Content-Type: application/json" \
		-d '{"coupon_hash":"a7ff3e82b53cafe","kid":"a1b2c3d4","expiry_ts":32503680000000,"seal":"8a2f3b91","grid_id":"grid-xyz","amount":1.5}' \
	| docker run --rm -i imega/jq .
	@echo "✅ Inference test complete"

# ----------------------------------------------------------------------------
# Development & Infrastructure (Standardized)
# ----------------------------------------------------------------------------
.PHONY: shell lint setup-network setup-prod-network ensure-core

# Open interactive shell in inference container
shell:
	docker compose exec inference sh || docker compose run --rm inference sh

# Placeholder for linting
lint:
	@echo "🔍 Running linting..."
	@echo "ℹ️  No lint configuration available"

# Setup development network
setup-network:
	@echo "🔗 Setting up development network..."
	-@docker network create $(NETWORK) 2>/dev/null || true
	-@docker network create $(PROD_NETWORK) 2>/dev/null || true

# Setup production network
setup-prod-network:
	@echo "🔗 Setting up production network..."
	-@docker network create $(PROD_NETWORK) 2>/dev/null || true

# Ensure Core server is available (for production mode)
ensure-core:
	@echo "🔍 Checking Core server availability..."
	@(curl -sf http://localhost:3000/health >/dev/null 2>&1) || \
	{ echo "⚠️  Core not reachable at localhost:3000"; \
	  if [ -n "$(CORE_DIR)" ] && [ -f "$(CORE_DIR)/docker-compose.yml" ]; then \
	    echo "🚀 Starting Core from $(CORE_DIR)..."; \
	    (cd "$(CORE_DIR)" && make up-prod); \
	  else \
	    echo "💡 Hint: Start Core manually or set CORE_DIR=../mari-server"; \
	  fi; \
	}
