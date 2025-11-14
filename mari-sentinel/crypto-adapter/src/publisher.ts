import * as amqp from 'amqplib';
import client from 'prom-client';

const logger = {
  info: (msg: string, data?: any) => console.log(`[Crypto Adapter Publisher] ${msg}`, data || ''),
  error: (msg: string, error?: any) => console.error(`[Crypto Adapter Publisher] ${msg}`, error || ''),
  warn: (msg: string, data?: any) => console.warn(`[Crypto Adapter Publisher] ${msg}`, data || ''),
};

// Relax types to avoid amqplib typing incompatibilities across environments
let channel: any = null;
let connection: any = null;

// Safely stringify objects that may contain BigInt values
function safeStringify(obj: any): string {
  return JSON.stringify(obj, (_key, value) =>
    typeof value === 'bigint' ? value.toString() : value
  );
}

// Prometheus metrics
export const metricsRegister = new client.Registry();
client.collectDefaultMetrics({ register: metricsRegister });

export const publishSuccess = new client.Counter({
  name: 'mari_crypto_adapter_publish_success_total',
  help: 'Number of successfully published events',
  labelNames: ['chain', 'adapter'],
  registers: [metricsRegister],
});

export const publishFailure = new client.Counter({
  name: 'mari_crypto_adapter_publish_failure_total',
  help: 'Number of failed publish attempts',
  labelNames: ['chain', 'adapter'],
  registers: [metricsRegister],
});

export const dlqSent = new client.Counter({
  name: 'mari_crypto_adapter_dlq_sent_total',
  help: 'Number of events sent to DLQ',
  labelNames: ['chain', 'adapter'],
  registers: [metricsRegister],
});

export const retryAttempts = new client.Counter({
  name: 'mari_crypto_adapter_retry_attempts_total',
  help: 'Total number of publish retry attempts',
  labelNames: ['chain', 'adapter'],
  registers: [metricsRegister],
});

export const dedupeHits = new client.Counter({
  name: 'mari_crypto_adapter_dedupe_hits_total',
  help: 'Number of transactions skipped due to deduplication',
  labelNames: ['chain', 'adapter'],
  registers: [metricsRegister],
});

// Optional deeper slicing
export const publishByTxType = new client.Counter({
  name: 'mari_crypto_adapter_publish_by_tx_type_total',
  help: 'Publish count labeled by tx_type',
  labelNames: ['chain', 'adapter', 'tx_type'],
  registers: [metricsRegister],
});

export const publishByRiskFactor = new client.Counter({
  name: 'mari_crypto_adapter_publish_by_risk_factor_total',
  help: 'Publish count labeled by individual risk_factor entries',
  labelNames: ['chain', 'adapter', 'risk_factor'],
  registers: [metricsRegister],
});

export async function connectToRabbitMQ(): Promise<any> {
  const maxRetries = 5;
  let retries = 0;

  while (retries < maxRetries) {
    try {
      connection = await amqp.connect(process.env.RABBITMQ_URL || 'amqp://rabbitmq:5672');
      channel = await connection!.createChannel();
      if (channel && typeof channel.confirmSelect === 'function') {
        await channel!.confirmSelect();
      }

      // Assert the main queue
      if (!channel) { throw new Error('Failed to create RabbitMQ channel'); }
      await channel!.assertQueue('mari-tx-events', {
        durable: true,
        arguments: {
          'x-message-ttl': 86400000, // 24 hour TTL
          'x-max-length': 1000000 // Max 1M messages
        }
      });

      // Assert dead letter queue for failed messages
      await channel!.assertQueue('mari-tx-events-dlq', {
        durable: true,
        arguments: {
          'x-message-ttl': 604800000, // 7 day TTL for DLQ
          'x-max-length': 100000
        }
      });

      // Handle connection errors
      if (!connection) { throw new Error('Failed to create RabbitMQ connection'); }
      connection!.on('error', (err: any) => {
        logger.error('Error in Mari Crypto Adapter Publisher:', err);
        channel = null;
        connection = null;
      });

      connection!.on('close', () => {
        logger.info('Mari Crypto Adapter Publisher connection closed');
        channel = null;
        connection = null;
      });

      console.log('Connected to RabbitMQ successfully');
      return channel as any;

    } catch (error) {
      retries++;
      console.error(`Failed to connect to RabbitMQ (attempt ${retries}/${maxRetries}):`, error);

      if (retries >= maxRetries) {
        throw new Error(`Failed to connect to RabbitMQ after ${maxRetries} attempts: ${error}`);
      }

      // Exponential backoff
      await new Promise(resolve => setTimeout(resolve, Math.pow(2, retries) * 1000));
    }
  }

  throw new Error('Failed to connect to RabbitMQ');
}

export async function publishEvent(event: any): Promise<void> {
  if (!channel) {
    throw new Error('RabbitMQ channel not initialized');
  }
  const chain = event?._metadata?.chain || 'unknown';
  const adapter = event?._metadata?.adapter || 'unknown';

  const maxRetries = 3;
  let retries = 0;

  while (retries < maxRetries) {
    try {
      const message = Buffer.from(safeStringify(event));

      // Use publisher confirms for reliability
      await new Promise<void>((resolve, reject) => {
        channel!.sendToQueue('mari-tx-events', message, {
          persistent: true,
          timestamp: Date.now(),
          messageId: `${event.coupon_hash || 'unknown'}-${Date.now()}`,
          headers: {
            'x-event-type': event.event_type,
            'x-chain': event._metadata?.chain || 'unknown',
            'x-retry-count': retries.toString()
          }
        }, (err: any) => {
          if (err) {
            reject(err);
          } else {
            resolve();
          }
        });
      });
      publishSuccess.inc({ chain, adapter });
      try {
        const txType = event?._metadata?.tx_type || 'unknown';
        publishByTxType.inc({ chain, adapter, tx_type: String(txType) });
        const rfs = Array.isArray(event?._metadata?.risk_factors) ? event._metadata.risk_factors : [];
        for (const rf of rfs) {
          publishByRiskFactor.inc({ chain, adapter, risk_factor: String(rf) });
        }
      } catch {}
      return; // Success

    } catch (error) {
      retries++;
      publishFailure.inc({ chain, adapter });
      console.error(`Failed to publish event (attempt ${retries}/${maxRetries}):`, error);

      if (retries >= maxRetries) {
        // Send to dead letter queue
        try {
          const dlqMessage = Buffer.from(safeStringify({
            ...event,
            error: error instanceof Error ? error.message : 'Unknown error',
            retry_count: retries,
            failed_at: new Date().toISOString()
          }));

          channel.sendToQueue('mari-tx-events-dlq', dlqMessage, { persistent: true });
          dlqSent.inc({ chain, adapter });
        } catch (dlqError) {
          console.error('Failed to send to DLQ:', dlqError);
        }

        throw new Error(`Failed to publish event after ${maxRetries} attempts: ${error}`);
      }

      // Exponential backoff
      await new Promise(resolve => setTimeout(resolve, Math.pow(2, retries) * 100));
      retryAttempts.inc({ chain, adapter });
    }
  }
}

export async function closeConnection(): Promise<void> {
  if (channel) {
    await channel.close();
    channel = null;
  }
  if (connection) {
    await connection.close();
    connection = null;
  }
}
