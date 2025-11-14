import * as amqp from 'amqplib';
import client from 'prom-client';

// Relax types to avoid amqplib typing incompatibilities across environments
let channel: any = null;
let connection: any = null;

// Prometheus metrics
export const metricsRegister = new client.Registry();
client.collectDefaultMetrics({ register: metricsRegister });

export const labelPublishSuccess = new client.Counter({
  name: 'mari_label_oracle_publish_success_total',
  help: 'Number of successfully published label events',
  labelNames: ['chain', 'source'],
  registers: [metricsRegister],
});

export const labelPublishFailure = new client.Counter({
  name: 'mari_label_oracle_publish_failure_total',
  help: 'Number of failed attempts to publish label events',
  labelNames: ['chain', 'source'],
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

      if (!channel) { throw new Error('Failed to create RabbitMQ channel'); }
      await channel!.assertQueue('mari-tx-events', {
        durable: true,
        arguments: {
          'x-message-ttl': 86400000,
          'x-max-length': 1000000
        }
      });

      if (!connection) { throw new Error('Failed to create RabbitMQ connection'); }
      connection!.on('error', (err: any) => {
        console.error('RabbitMQ connection error:', err);
        channel = null;
        connection = null;
      });

      connection!.on('close', () => {
        console.log('RabbitMQ connection closed');
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

      await new Promise(resolve => setTimeout(resolve, Math.pow(2, retries) * 1000));
    }
  }

  throw new Error('Failed to connect to RabbitMQ');
}

export async function publishLabelEvent(txHash: string, result: string, chain: string, metadata?: any): Promise<void> {
  if (!channel) {
    throw new Error('RabbitMQ channel not initialized');
  }
  const source = metadata?.source || 'unknown';

  const event = {
    event_type: 'SETTLEMENT_OUTCOME',
    coupon_hash: txHash,
    result: result,
    chain: chain,
    ts: Date.now(),
    confidence: metadata?.confidence || 0.8,
    source: metadata?.source || 'unknown',
    severity: metadata?.severity || 'medium',
    description: metadata?.description || ''
  };

  const maxRetries = 3;
  let retries = 0;

  while (retries < maxRetries) {
    try {
      await new Promise<void>((resolve, reject) => {
        channel!.sendToQueue('mari-tx-events', Buffer.from(JSON.stringify(event)), {
          persistent: true,
          timestamp: Date.now(),
          messageId: `${txHash}-label-${Date.now()}`,
          headers: {
            'x-event-type': 'SETTLEMENT_OUTCOME',
            'x-chain': chain,
            'x-source': metadata?.source || 'unknown'
          }
        }, (err: any) => {
          if (err) {
            reject(err);
          } else {
            resolve();
          }
        });
      });
      labelPublishSuccess.inc({ chain, source });
      return;

    } catch (error) {
      retries++;
      labelPublishFailure.inc({ chain, source });
      console.error(`Failed to publish label event (attempt ${retries}/${maxRetries}):`, error);

      if (retries >= maxRetries) {
        throw new Error(`Failed to publish label event after ${maxRetries} attempts: ${error}`);
      }

      await new Promise(resolve => setTimeout(resolve, Math.pow(2, retries) * 100));
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
