#!/bin/bash
set -e

echo "Publishing Mari Shared Libraries..."

# Build the library
./scripts/build.sh

# Run a final test
npm test

# Publish to npm
npm publish --access public

echo "Publish completed successfully!"
