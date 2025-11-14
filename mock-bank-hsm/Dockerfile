# Production-ready Dockerfile for Mari Mock Bank
FROM node:18-alpine AS base

# Create root workdir
WORKDIR /app

# Copy package manifests for bank and shared libs
COPY mock-bank-hsm/package*.json ./mock-bank-hsm/
COPY mari-shared-libs/package*.json ./mari-shared-libs/

# Copy and build shared libs (needs dev deps for build)
COPY mari-shared-libs/ ./mari-shared-libs/
RUN cd mari-shared-libs \
  && (npm ci || npm install) \
  && npm run build

# Copy bank source
COPY mock-bank-hsm/ ./mock-bank-hsm/

# Install production deps for bank (resolves local file dependency)
RUN cd mock-bank-hsm \
  && (npm ci --omit=dev || npm install --omit=dev)

ENV NODE_ENV=production
WORKDIR /app/mock-bank-hsm

# Expose service port
EXPOSE 3001

# Start the server
CMD ["node", "src/app.js"]
