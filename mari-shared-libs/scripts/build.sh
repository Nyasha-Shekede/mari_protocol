#!/bin/bash
set -e

echo "Building Mari Shared Libraries..."

# Clean previous build
rm -rf dist/

# Install dependencies
npm install

# Run tests
npm test

# Build the library
npm run build

# Generate documentation
npm run docs

echo "Build completed successfully!"
