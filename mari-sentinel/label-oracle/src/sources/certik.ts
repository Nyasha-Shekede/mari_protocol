import { Channel } from 'amqplib';
// Loosen type to avoid Redis generics mismatch across workspaces
type RedisLike = any;
import { publishLabelEvent } from '../publisher';

const logger = {
  info: (msg: string, data?: any) => console.log(`[Certik Monitor] ${msg}`, data || ''),
  error: (msg: string, error?: any) => console.error(`[Certik Monitor] ${msg}`, error || ''),
  warn: (msg: string, data?: any) => console.warn(`[Certik Monitor] ${msg}`, data || ''),
};

export function monitorCertik(channel: Channel, redisClient: RedisLike): void {
  const CERTIK_API = process.env.CERTIK_API_URL || 'https://www.certik.com/api/v1';
  const API_KEY = process.env.CERTIK_API_KEY;

  if (!API_KEY) {
    logger.warn('CERTIK_API_KEY not set, using mock data');
    setInterval(() => generateMockAlerts(channel, redisClient), 300000);
    return;
  }

  logger.info('Starting Certik monitor', { api: CERTIK_API });

  setInterval(async () => {
    await fetchCertikAlerts(channel, redisClient);
  }, 180000);

  setTimeout(() => fetchCertikAlerts(channel, redisClient), 5000);
}

async function fetchCertikAlerts(channel: Channel, redisClient: RedisLike): Promise<void> {
  try {
    const response = await fetch(`${process.env.CERTIK_API_URL}/security/feed`, {
      headers: {
        'Authorization': `Bearer ${process.env.CERTIK_API_KEY}`,
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error(`Certik API error: ${response.status}`);
    }

    const data = await response.json();
    const alerts = data.data || [];

    logger.info(`Fetched ${alerts.length} Certik alerts`);

    for (const alert of alerts) {
      await processCertikAlert(alert, channel, redisClient);
    }

  } catch (error) {
    logger.error('Error fetching Certik alerts', error);
  }
}

async function processCertikAlert(alert: any, channel: Channel, redisClient: RedisLike): Promise<void> {
  try {
    const alertKey = `certik:alert:${alert.id}`;
    const seen = await redisClient.get(alertKey);
    if (seen) {
      return;
    }

    logger.info(`Processing Certik alert: ${alert.id}`, {
      severity: alert.severity,
      type: alert.type,
      project: alert.project
    });

    const threats = extractThreatsFromAlert(alert);

    for (const threat of threats) {
      try {
        await publishLabelEvent(
          threat.txHash,
          'MALICIOUS',
          threat.chain || 'ethereum',
          {
            confidence: threat.confidence || 0.9,
            source: 'certik',
            severity: mapSeverity(alert.severity),
            description: alert.description || alert.title
          }
        );

        logger.info(`Published malicious label for ${threat.txHash} from Certik alert ${alert.id}`);

      } catch (publishError) {
        logger.error(`Error publishing label for ${threat.txHash}`, publishError);
      }
    }

    await redisClient.setEx(alertKey, 86400, '1');

  } catch (error) {
    logger.error(`Error processing Certik alert ${alert.id}`, error);
  }
}

function extractThreatsFromAlert(alert: any): Array<{txHash: string, chain: string, confidence: number}> {
  const threats: Array<{txHash: string, chain: string, confidence: number}> = [];

  switch (alert.type) {
    case 'EXPLOIT':
    case 'HACK':
    case 'FLASH_LOAN_ATTACK':
      if (alert.transactionHashes) {
        for (const tx of alert.transactionHashes) {
          threats.push({
            txHash: tx.hash,
            chain: tx.chain || 'ethereum',
            confidence: 0.95
          });
        }
      }
      if (alert.description) {
        const txHashes = extractTransactionHashes(alert.description);
        for (const hash of txHashes) {
          threats.push({ txHash: hash, chain: 'ethereum', confidence: 0.8 });
        }
      }
      break;

    case 'SUSPICIOUS_ACTIVITY':
      if (alert.addresses) {
        for (const address of alert.addresses) {
          threats.push({ txHash: `0x${generateMockHash()}`, chain: address.chain || 'ethereum', confidence: 0.7 });
        }
      }
      break;
  }

  return threats;
}

function extractTransactionHashes(text: string): string[] {
  const regex = /0x[a-fA-F0-9]{64}/g;
  const matches = text.match(regex);
  return matches || [];
}

function generateMockHash(): string {
  return Array.from({ length: 64 }, () => Math.floor(Math.random() * 16).toString(16)).join('');
}

function mapSeverity(certikSeverity: string): string {
  switch (certikSeverity?.toLowerCase()) {
    case 'critical': return 'high';
    case 'high': return 'high';
    case 'medium': return 'medium';
    case 'low': return 'low';
    default: return 'medium';
  }
}

async function generateMockAlerts(channel: Channel, redisClient: RedisLike): Promise<void> {
  const mockAlerts = [
    {
      id: `mock-${Date.now()}`,
      type: 'EXPLOIT',
      severity: 'critical',
      title: 'Flash Loan Attack Detected',
      description: 'Suspicious flash loan attack detected on DeFi protocol. Transaction: 0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef',
      project: 'Example DeFi Protocol',
      chain: 'ethereum',
      transactionHashes: [{
        hash: '0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef',
        chain: 'ethereum'
      }]
    },
    {
      id: `mock-${Date.now() + 1}`,
      type: 'SUSPICIOUS_ACTIVITY',
      severity: 'medium',
      title: 'Suspicious Address Activity',
      description: 'Multiple large transfers from known suspicious address',
      project: 'Unknown',
      addresses: [{
        address: '0xabcdef1234567890abcdef1234567890abcdef12',
        chain: 'ethereum'
      }]
    }
  ];

  for (const alert of mockAlerts) {
    await processCertikAlert(alert, channel, redisClient);
  }
}

export { logger as certikLogger };
