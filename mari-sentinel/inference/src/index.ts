import express from 'express';
import { createClient } from 'redis';
import { featurize } from './featurizer';
import { loadModel, score } from './model';
type InferenceRequest = {
  coupon_hash: string;
  kid: string;
  expiry_ts: number;
  seal: string;
  grid_id: string;
  amount: number;
};
type InferenceResponse = { score: number; model_id: string };
import client from 'prom-client';
const logger = {
  info: (msg: string, data?: any) => console.log(`[Inference] ${msg}`, data || ''),
  error: (msg: string, error?: any) => console.error(`[Inference] ${msg}`, error || ''),
  warn: (msg: string, data?: any) => console.warn(`[Inference] ${msg}`, data || ''),
};

const app = express();
app.use(express.json());

// Optional simple auth: if SENTINEL_AUTH_TOKEN is set, require it via X-Mari-Auth header
const requiredToken = process.env.SENTINEL_AUTH_TOKEN;
if (requiredToken) {
  app.use((req, res, next) => {
    const tok = req.header('X-Mari-Auth');
    if (tok !== requiredToken) return res.status(401).json({ error: 'unauthorized' });
    next();
  });
}

const redis = createClient({ url: process.env.REDIS_URL });
let modelId = '';
let modelReady = false;

// Metrics setup
const register = new client.Registry();
client.collectDefaultMetrics({ register });

const reqCounter = new client.Counter({
  name: 'inference_requests_total',
  help: 'Total number of inference requests',
  registers: [register],
});

const durationHist = new client.Histogram({
  name: 'inference_duration_seconds',
  help: 'Inference request duration in seconds',
  buckets: [0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2],
  registers: [register],
});

const scoreHist = new client.Histogram({
  name: 'inference_score',
  help: 'Distribution of inference score (0-999)',
  buckets: [0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000],
  registers: [register],
});

const modelVersionGauge = new client.Gauge({
  name: 'model_version_info',
  help: 'Current model version as a labeled gauge',
  labelNames: ['model_id'],
  registers: [register],
});

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

(async () => {
  await redis.connect();
  // Poll for model in Redis to avoid exiting when model is not yet seeded
  while (true) {
    try {
      const ms = await redis.get('model:current');
      if (!ms) {
        console.log('Waiting for model:current in Redis...');
        await sleep(1000);
        continue;
      }
      const { model_id, buffer } = JSON.parse(ms);
      await loadModel(Buffer.from(buffer, 'base64'));
      modelId = model_id;
      modelReady = true;
      logger.info(`Mari Inference Service ready with ${modelId}`);
      // Update metrics
      modelVersionGauge.reset();
      modelVersionGauge.labels(modelId).set(1);
      break;
    } catch (e) {
      console.error('Error loading model; retrying in 1s', e);
      await sleep(1000);
    }
  }
})();

// Use a dedicated subscriber connection for pub/sub
const sub = redis.duplicate();
(async () => {
  await sub.connect();
  await sub.subscribe('model-updates', async (msg: string) => {
    try {
      const key = `model:${msg}`;
      const ms = await redis.get(key);
      if (!ms) return;
      const { buffer, model_id } = JSON.parse(ms);
      await loadModel(Buffer.from(buffer, 'base64'));
      modelId = model_id;
      modelReady = true;
      logger.info(`Hot-swap to ${modelId}`);
      modelVersionGauge.reset();
      modelVersionGauge.labels(modelId).set(1);
    } catch (e) {
      console.error('Hot-swap failed, retaining previous model', e);
    }
  });
})();

app.post('/inference', async (req: express.Request, res: express.Response) => {
  try {
    reqCounter.inc();
    const endTimer = durationHist.startTimer();
    const body = req.body as InferenceRequest;
    const vec = featurize(body);
    const s = await score(vec);
    const resp: InferenceResponse = { score: Math.round(s * 999), model_id: modelId };
    scoreHist.observe(resp.score);
    endTimer();
    res.json(resp);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'inference_failed' });
  }
});

// Health and readiness endpoints
app.get('/health', (_req: express.Request, res: express.Response) => {
  res.json({ ok: true });
});

// Liveness alias for consistency with other services
app.get('/live', (_req: express.Request, res: express.Response) => {
  res.json({ ok: true });
});

app.get('/ready', (_req: express.Request, res: express.Response) => {
  const redisOk = (redis as any).isOpen === true;
  res.json({ ready: modelReady && redisOk, modelReady, redis: redisOk, model_id: modelId });
});

// Expose Prometheus metrics
app.get('/metrics', async (_req: express.Request, res: express.Response) => {
  try {
    res.set('Content-Type', register.contentType);
    res.end(await register.metrics());
  } catch (e) {
    res.status(500).end('metrics_error');
  }
});

const port = Number(process.env.PORT || 3002);
app.listen(port, () => logger.info('Mari Inference Service started on port', port));
