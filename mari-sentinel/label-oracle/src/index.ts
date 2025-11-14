import { connectToRabbitMQ } from './publisher';
import { metricsRegister } from './publisher';
import { monitorCertik } from './sources/certik';
import { monitorChainalysis } from './sources/chainalysis';
import { monitorHackAnnouncements } from './sources/hack-monitor';
import { createClient } from 'redis';
import { CronJob } from 'cron';
import * as dotenv from 'dotenv';
import * as http from 'http';

dotenv.config();

const logger = {
  info: (msg: string, data?: any) => console.log(`[Label Oracle] ${msg}`, data || ''),
  error: (msg: string, error?: any) => console.error(`[Label Oracle] ${msg}`, error || ''),
  warn: (msg: string, data?: any) => console.warn(`[Label Oracle] ${msg}`, data || ''),
};

async function main() {
  try {
    logger.info('Starting Label Oracle Service...');

    // Connect to RabbitMQ
    const channel = await connectToRabbitMQ();
    logger.info('Connected to RabbitMQ');

    // Connect to Redis
    const redisClient = createClient({
      url: process.env.REDIS_URL || 'redis://redis:6379',
      socket: {
        reconnectStrategy: (retries) => Math.min(retries * 50, 500)
      }
    });

    redisClient.on('error', (err) => logger.error('Redis Client Error', err));
    await redisClient.connect();
    logger.info('Connected to Redis');

    // Start threat intelligence sources
    monitorCertik(channel, redisClient);
    monitorChainalysis(channel, redisClient);
    monitorHackAnnouncements(channel, redisClient);

    // Schedule periodic scans for historical data
    const historicalScanJob = new CronJob('0 */6 * * *', async () => {
      logger.info('Running historical threat intelligence scan');
      await runHistoricalScan(channel, redisClient);
    });

    historicalScanJob.start();

    logger.info('Label Oracle service fully started');

    // Health endpoint
    const HEALTH_PORT = parseInt(process.env.HEALTH_PORT || '8082', 10);
    const server = http.createServer((req, res) => {
      if (req.url === '/ready') {
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
      historicalScanJob.stop();
      await redisClient.quit();
      process.exit(0);
    });

    process.on('SIGINT', async () => {
      logger.info('SIGINT received, shutting down gracefully');
      historicalScanJob.stop();
      await redisClient.quit();
      process.exit(0);
    });

  } catch (error) {
    logger.error('Failed to start Label Oracle service', error);
    process.exit(1);
  }
}

async function runHistoricalScan(channel: any, redisClient: any): Promise<void> {
  // This would scan historical data for missed threats
  logger.info('Historical scan completed');
}

main().catch((error) => {
  logger.error('Unhandled error in main', error);
  process.exit(1);
});
