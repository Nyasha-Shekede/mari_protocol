import { Channel } from 'amqplib';
// Loosen type to avoid Redis generics mismatch across workspaces
type RedisLike = any;
import { publishLabelEvent } from '../publisher';

const logger = {
  info: (msg: string, data?: any) => console.log(`[Chainalysis Monitor] ${msg}`, data || ''),
  error: (msg: string, error?: any) => console.error(`[Chainalysis Monitor] ${msg}`, error || ''),
  warn: (msg: string, data?: any) => console.warn(`[Chainalysis Monitor] ${msg}`, data || ''),
};

export function monitorChainalysis(channel: Channel, redisClient: RedisLike): void {
  const API_KEY = process.env.CHAINALYSIS_API_KEY;

  if (!API_KEY) {
    logger.warn('CHAINALYSIS_API_KEY not set, using mock data');
    setInterval(() => generateMockData(channel, redisClient), 600000); // Every 10 minutes
    return;
  }

  logger.info('Starting Chainalysis monitor');

  setInterval(async () => {
    await checkSanctionedAddresses(channel, redisClient);
    await checkHighRiskEntities(channel, redisClient);
  }, 300000);

  setTimeout(() => {
    checkSanctionedAddresses(channel, redisClient);
    checkHighRiskEntities(channel, redisClient);
  }, 10000);
}

async function checkSanctionedAddresses(channel: Channel, redisClient: RedisLike): Promise<void> {
  try {
    const sanctionedAddresses = await fetchSanctionedAddresses();

    logger.info(`Checking ${sanctionedAddresses.length} sanctioned addresses`);

    for (const address of sanctionedAddresses) {
      try {
        const recentTxs = await findRecentTransactions(address);

        for (const tx of recentTxs) {
          await publishLabelEvent(
            tx.hash,
            'MALICIOUS',
            tx.chain || 'bitcoin',
            {
              confidence: 1.0,
              source: 'chainalysis-ofac',
              severity: 'critical',
              description: `Transaction involving OFAC sanctioned address: ${address.address}`
            }
          );

          logger.info(`Published sanctioned address label for ${tx.hash}`);
        }

      } catch (addressError) {
        logger.error(`Error processing sanctioned address ${address.address}`, addressError);
      }
    }

  } catch (error) {
    logger.error('Error checking sanctioned addresses', error);
  }
}

async function checkHighRiskEntities(channel: Channel, redisClient: RedisLike): Promise<void> {
  try {
    const highRiskEntities = await fetchHighRiskEntities();

    logger.info(`Checking ${highRiskEntities.length} high-risk entities`);

    for (const entity of highRiskEntities) {
      try {
        const recentTxs = await findRecentTransactions(entity);

        for (const tx of recentTxs) {
          const confidence = entity.riskScore > 80 ? 0.9 : 0.7;

          await publishLabelEvent(
            tx.hash,
            'SUSPICIOUS',
            tx.chain || 'bitcoin',
            {
              confidence: confidence,
              source: 'chainalysis-risk',
              severity: mapRiskScore(entity.riskScore),
              description: `Transaction involving high-risk entity: ${entity.name} (risk score: ${entity.riskScore})`
            }
          );

          logger.info(`Published high-risk entity label for ${tx.hash}`);
        }

      } catch (entityError) {
        logger.error(`Error processing high-risk entity ${entity.name}`, entityError);
      }
    }

  } catch (error) {
    logger.error('Error checking high-risk entities', error);
  }
}

async function fetchSanctionedAddresses(): Promise<Array<{ address: string, chain: string, name: string }>> {
  if (!process.env.CHAINALYSIS_API_KEY) {
    return [
      { address: '1F1tAaz5x1HUXrCNLbtMDqcw6o5GNn4xqX', chain: 'bitcoin', name: 'OFAC Sanctioned Address 1' },
      { address: '0x1234567890123456789012345678901234567890', chain: 'ethereum', name: 'OFAC Sanctioned Address 2' }
    ];
  }

  try {
    const response = await fetch('https://api.chainalysis.com/api/kyt/v1/sanctions/addresses', {
      headers: {
        'Authorization': `Bearer ${process.env.CHAINALYSIS_API_KEY}`,
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error(`Chainalysis API error: ${response.status}`);
    }

    const data = await response.json();
    return data.addresses || [];

  } catch (error) {
    logger.error('Error fetching sanctioned addresses from Chainalysis', error);
    return [];
  }
}

async function fetchHighRiskEntities(): Promise<Array<{ name: string, addresses: string[], riskScore: number, chain: string }>> {
  if (!process.env.CHAINALYSIS_API_KEY) {
    return [
      { name: 'Suspicious Exchange 1', addresses: ['1A2B3C4D5E6F7G8H9I0J1K2L3M4N5O6P7Q8R9S0T', '0xabcdef1234567890abcdef1234567890abcdef12'], riskScore: 85, chain: 'bitcoin' },
      { name: 'Darknet Market Wallet', addresses: ['3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy'], riskScore: 95, chain: 'bitcoin' }
    ];
  }

  try {
    const response = await fetch('https://api.chainalysis.com/api/kyt/v1/entities/high-risk', {
      headers: {
        'Authorization': `Bearer ${process.env.CHAINALYSIS_API_KEY}`,
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error(`Chainalysis API error: ${response.status}`);
    }

    const data = await response.json();
    return data.entities || [];

  } catch (error) {
    logger.error('Error fetching high-risk entities from Chainalysis', error);
    return [];
  }
}

async function findRecentTransactions(entity: any): Promise<Array<{ hash: string, chain: string, timestamp: number }>> {
  const mockTransactions: Array<{ hash: string, chain: string, timestamp: number }> = [];

  if (entity.addresses) {
    for (const address of entity.addresses) {
      for (let i = 0; i < 3; i++) {
        mockTransactions.push({ hash: `0x${generateMockHash()}`, chain: entity.chain || 'bitcoin', timestamp: Date.now() - (i * 3600000) });
      }
    }
  } else if (entity.address) {
    for (let i = 0; i < 2; i++) {
      mockTransactions.push({ hash: `0x${generateMockHash()}`, chain: entity.chain || 'bitcoin', timestamp: Date.now() - (i * 7200000) });
    }
  }

  return mockTransactions;
}

function generateMockHash(): string {
  return Array.from({ length: 64 }, () => Math.floor(Math.random() * 16).toString(16)).join('');
}

function mapRiskScore(score: number): string {
  if (score >= 90) return 'critical';
  if (score >= 70) return 'high';
  if (score >= 50) return 'medium';
  return 'low';
}

async function generateMockData(channel: Channel, redisClient: RedisLike): Promise<void> {
  const mockEntities = [
    { name: 'Suspicious Mixer', addresses: ['1MixerAddress123456789012345678901234567890'], riskScore: 90, chain: 'bitcoin' },
    { name: 'Ransomware Wallet', addresses: ['0xRansomwareAddress123456789012345678901234'], riskScore: 95, chain: 'ethereum' }
  ];

  for (const entity of mockEntities) {
    const recentTxs = await findRecentTransactions(entity);

    for (const tx of recentTxs) {
      await publishLabelEvent(
        tx.hash,
        'MALICIOUS',
        tx.chain,
        {
          confidence: 0.9,
          source: 'chainalysis-mock',
          severity: mapRiskScore(entity.riskScore),
          description: `Transaction involving ${entity.name} (risk score: ${entity.riskScore})`
        }
      );
    }
  }
}

export { logger as chainalysisLogger };
