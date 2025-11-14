import { createClient } from 'redis';
import { readFileSync } from 'fs';
import path from 'path';

const redis = createClient({ url: process.env.REDIS_URL });
(async () => {
  await redis.connect();
  const onnxPath = path.resolve(__dirname, 'initial_model.onnx');
  const buf = readFileSync(onnxPath);
  const ms = {
    model_id: 'v0',
    buffer: buf.toString('base64'),
    created_at: Date.now(),
  };
  await redis.set('model:current', JSON.stringify(ms));
  await redis.set(`model:v0`, JSON.stringify(ms));
  await redis.disconnect();
  console.log('Redis seeded with v0');
})();
