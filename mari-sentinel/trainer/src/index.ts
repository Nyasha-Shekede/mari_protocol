import amqp from 'amqplib';
import { createClient } from 'redis';
import type { TransactionEvent } from './types';
import { OnlineTrainer } from './trainer';
import * as http from 'http';
import client from 'prom-client';

const redis = createClient({ url: process.env.REDIS_URL });
const rabbitUrl = process.env.RABBITMQ_URL || 'amqp://rabbitmq:5672';

let redisReady = false;
let rabbitReady = false;

// Prometheus metrics
const register = new client.Registry();
client.collectDefaultMetrics({ register });
const consumedCounter = new client.Counter({
  name: 'trainer_consumed_total',
  help: 'Total number of events consumed by trainer',
  registers: [register],
});
const readyGauge = new client.Gauge({
  name: 'trainer_ready',
  help: '1 if trainer is ready (redis+rabbit), else 0',
  registers: [register],
});

(async () => {
  await redis.connect();
  redisReady = true;
  readyGauge.set(redisReady && rabbitReady ? 1 : 0);
  const conn = await amqp.connect(rabbitUrl);
  rabbitReady = true;
  readyGauge.set(redisReady && rabbitReady ? 1 : 0);
  const ch = await conn.createChannel();
  await ch.assertQueue('mari-tx-events', {
    durable: true,
    arguments: {
      'x-message-ttl': 86400000, // 24 hours, must match publisher
      'x-max-length': 1000000    // must match publisher
    }
  });
  await ch.prefetch(1000);
  const trainer = new OnlineTrainer(redis as any);

  console.log('Trainer consuming...');
  ch.consume('mari-tx-events', async (msg: amqp.ConsumeMessage | null) => {
    if (!msg) return;
    let ev: TransactionEvent;
    try {
      ev = JSON.parse(msg.content.toString()) as TransactionEvent;
    } catch (e) {
      console.error('Invalid event JSON, dropping', e);
      ch.ack(msg);
      return;
    }
    await trainer.ingest(ev);
    consumedCounter.inc();
    ch.ack(msg);
  });
})();

// Health endpoints
const HEALTH_PORT = parseInt(process.env.HEALTH_PORT || '8083', 10);
const server = http.createServer((req, res) => {
  if (req.url === '/ready') {
    const ready = redisReady && rabbitReady;
    res.statusCode = ready ? 200 : 503;
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify({ ready, redis: redisReady, rabbitmq: rabbitReady }));
    return;
  }
  if (req.url === '/live') {
    res.statusCode = 200;
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify({ status: 'alive' }));
    return;
  }
  if (req.url === '/metrics') {
    res.statusCode = 200;
    res.setHeader('Content-Type', register.contentType);
    register.metrics().then((m) => res.end(m)).catch(() => {
      res.statusCode = 500;
      console.info('Mari Trainer started');
    });
    return;
  }
  res.statusCode = 404;
  res.end();
});
server.listen(HEALTH_PORT, () => console.log(`Trainer health server on ${HEALTH_PORT}`));
