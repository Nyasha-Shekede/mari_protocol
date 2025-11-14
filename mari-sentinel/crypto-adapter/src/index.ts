import { connectToRabbitMQ } from './publisher';
import { metricsRegister } from './publisher';
import { startBitcoinAdapter } from './adapters/bitcoin.adapter';
import { startEthereumAdapter } from './adapters/ethereum.adapter';
import { startSolanaAdapter } from './adapters/solana.adapter';
import { createClient } from 'redis';
import * as dotenv from 'dotenv';
import * as http from 'http';

dotenv.config();

const logger = {
  info: (msg: string, data?: any) => console.log(`[INFO] ${msg}`, data || ''),
  error: (msg: string, error?: any) => console.error(`[ERROR] ${msg}`, error || ''),
  warn: (msg: string, data?: any) => console.warn(`[WARN] ${msg}`, data || ''),
};

async function main() {
  try {
    logger.info('Starting Crypto Adapter Service...');

    // Connect to RabbitMQ
    const rabbitMQChannel = await connectToRabbitMQ();
    logger.info('Connected to RabbitMQ');

    // Connect to Redis for rate limiting and deduplication
    const redisClient = createClient({
      url: process.env.REDIS_URL || 'redis://redis:6379',
      socket: {
        reconnectStrategy: (retries) => Math.min(retries * 50, 500)
      }
    });

    redisClient.on('error', (err) => logger.error('Redis Client Error', err));
    await redisClient.connect();
    logger.info('Connected to Redis');

    // Start all blockchain adapters
    const adapters = [
      { name: 'Bitcoin', start: startBitcoinAdapter },
      { name: 'Ethereum', start: startEthereumAdapter },
      { name: 'Solana', start: startSolanaAdapter }
    ];

    for (const adapter of adapters) {
      try {
        adapter.start(rabbitMQChannel, redisClient);
        logger.info(`${adapter.name} adapter started`);
      } catch (error) {
        logger.error(`Failed to start ${adapter.name} adapter`, error);
      }
    }

    logger.info('Crypto Adapter service fully started');

    // Health endpoint
    const HEALTH_PORT = parseInt(process.env.HEALTH_PORT || '8081', 10);
    const server = http.createServer((req, res) => {
      if (req.url === '/ready') {
        // Basic readiness: if process is running, consider ready (Rabbit/Redis failures would exit)
        res.statusCode = 200;
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify({ status: 'ok' }));
        return;
      }
      if (req.url === '/metrics') {
        res.statusCode = 200;
        res.setHeader('Content-Type', metricsRegister.contentType);
        metricsRegister.metrics().then((m) => res.end(m)).catch(() => {
          res.statusCode = 500;
          res.end('metrics_error');
        });
        return;
      }
      if (req.url === '/live') {
        res.statusCode = 200;
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify({ status: 'alive' }));
        return;
      }
      res.statusCode = 404;
      res.end();
    });
    server.listen(HEALTH_PORT, () => logger.info(`Health server listening on ${HEALTH_PORT}`));

    // Graceful shutdown
    process.on('SIGTERM', async () => {
      logger.info('SIGTERM received, shutting down gracefully');
      await redisClient.quit();
      process.exit(0);
    });

    process.on('SIGINT', async () => {
      logger.info('SIGINT received, shutting down gracefully');
      await redisClient.quit();
      process.exit(0);
    });

  } catch (error) {
    logger.error('Failed to start Crypto Adapter service', error);
    process.exit(1);
  }
}

main().catch((error) => {
  logger.error('Unhandled error in main', error);
  process.exit(1);
});
