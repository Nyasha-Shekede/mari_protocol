# Crypto Adapter Dockerfile
FROM node:20-bullseye-slim AS build
WORKDIR /app

# Install deps
COPY package.json tsconfig.json ./
RUN npm install --production=false

# Copy source
COPY src ./src

# Build
RUN npm run build

# Runtime image
FROM node:20-bullseye-slim
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
CMD ["node", "dist/index.js"]
