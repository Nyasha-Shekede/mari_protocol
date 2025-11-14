#!/bin/bash
cd ..
npm run build
cd examples
npx ts-node basic-usage.ts
