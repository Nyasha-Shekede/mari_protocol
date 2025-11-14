// Label publisher for Mari Core.
// Always logs to stdout; when RABBITMQ_URL is set and reachable, also publishes
// TransactionEvent JSON into the mari-tx-events queue for Sentinel training.

const amqp = require('amqplib');

let rabbitChannel = null;
let rabbitConn = null;
let connecting = null;

async function getRabbitChannel() {
  if (rabbitChannel) return rabbitChannel;
  if (connecting) return connecting;

  const url = process.env.RABBITMQ_URL;
  if (!url) return null;

  connecting = (async () => {
    try {
      const conn = await amqp.connect(url);
      rabbitConn = conn;
      conn.on('error', () => {
        rabbitChannel = null;
        rabbitConn = null;
        connecting = null;
      });
      conn.on('close', () => {
        rabbitChannel = null;
        rabbitConn = null;
        connecting = null;
      });

      const ch = await conn.createChannel();
      await ch.assertQueue('mari-tx-events', {
        durable: true,
        arguments: {
          'x-message-ttl': 86400000,
          'x-max-length': 1000000
        }
      });
      rabbitChannel = ch;
      return ch;
    } catch (e) {
      console.error('[LABEL] Failed to connect to RabbitMQ for label publishing', e.message || e);
      rabbitChannel = null;
      rabbitConn = null;
      connecting = null;
      return null;
    }
  })();

  return connecting;
}

async function publishToRabbit(msg) {
  try {
    const ch = await getRabbitChannel();
    if (!ch) return;
    const payload = Buffer.from(JSON.stringify(msg));
    ch.sendToQueue('mari-tx-events', payload, {
      persistent: true,
      timestamp: Date.now(),
      headers: {
        'x-event-type': msg.event_type || 'SETTLEMENT_OUTCOME'
      }
    });
  } catch (e) {
    console.error('[LABEL] Failed to publish label message to RabbitMQ', e.message || e);
  }
}

async function publishOutcome(msg) {
  try { console.log('[LABEL][SETTLEMENT_OUTCOME]', JSON.stringify(msg)); } catch {}
  await publishToRabbit(msg);
}

async function publishPre(msg) {
  try { console.log('[LABEL][PRE_SETTLEMENT]', JSON.stringify(msg)); } catch {}
  await publishToRabbit(msg);
}

module.exports = { publishOutcome, publishPre };
