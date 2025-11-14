# Unified Dockerfile for Mari Core Server
# Supports both development and production builds via build args

FROM node:18-alpine AS base

# Build argument to determine target environment
ARG NODE_ENV=production
ARG BUILD_TARGET=production

WORKDIR /app

# Copy shared libs manifests and sources from parent context
COPY mari-shared-libs/package*.json ./mari-shared-libs/
COPY mari-shared-libs/ ./mari-shared-libs/

# Build shared libs first
RUN cd mari-shared-libs \
  && (npm ci || npm install) \
  && npm run build

# Copy core server manifests
COPY mari-server/package*.json ./mari-server/

# Install dependencies based on environment
RUN cd mari-server && \
  if [ "$BUILD_TARGET" = "development" ]; then \
    (npm ci || npm install) && npm install -g nodemon@^3; \
  else \
    (npm ci --omit=dev || npm install --omit=dev); \
  fi

# Copy core server source code
COPY mari-server/ ./mari-server/

# Set environment and working directory
ENV NODE_ENV=$NODE_ENV
WORKDIR /app/mari-server
EXPOSE 3000

# Use different commands based on build target
CMD if [ "$NODE_ENV" = "development" ]; then \
      npm run dev; \
    else \
      node server.js; \
    fi
