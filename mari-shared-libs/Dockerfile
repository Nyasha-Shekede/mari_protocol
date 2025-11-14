# syntax=docker/dockerfile:1

ARG NODE_VERSION=20-alpine
FROM node:${NODE_VERSION} AS base
WORKDIR /app
COPY package.json package-lock.json* yarn.lock* pnpm-lock.yaml* ./
RUN npm ci || npm install
COPY . .

FROM base AS test
RUN npm test

FROM base AS build
RUN npm run build && npm run docs

FROM node:${NODE_VERSION} AS runtime
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY package.json ./package.json
CMD ["node", "-e", "console.log('Mari shared-libs container. Use test/build stages for CI.')"]
