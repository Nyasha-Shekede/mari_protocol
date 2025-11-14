import { Channel } from 'amqplib';
// Loosen type to avoid Redis generics mismatch across workspaces
type RedisLike = any;
import { publishLabelEvent } from '../publisher';

const logger = {
  info: (msg: string, data?: any) => console.log(`[Hack Monitor] ${msg}`, data || ''),
  error: (msg: string, error?: any) => console.error(`[Hack Monitor] ${msg}`, error || ''),
  warn: (msg: string, data?: any) => console.warn(`[Hack Monitor] ${msg}`, data || ''),
};

export function monitorHackAnnouncements(channel: Channel, redisClient: RedisLike): void {
  logger.info('Starting hack announcement monitor');

  setInterval(async () => {
    await checkTwitterAnnouncements(channel, redisClient);
    await checkTelegramChannels(channel, redisClient);
    await checkDiscordChannels(channel, redisClient);
    await checkBlockchainNews(channel, redisClient);
  }, 600000);

  setTimeout(() => {
    checkTwitterAnnouncements(channel, redisClient);
    checkTelegramChannels(channel, redisClient);
    checkDiscordChannels(channel, redisClient);
    checkBlockchainNews(channel, redisClient);
  }, 30000);
}

async function checkTwitterAnnouncements(channel: Channel, redisClient: RedisLike): Promise<void> {
  try {
    const mockTweets = [
      {
        id: '1234567890',
        text: '🚨 SECURITY ALERT: Protocol X exploited for $10M. Transaction: 0xabcdef1234567890abcdef1234567890abcdef12 #DeFiHack',
        author: 'PeckShieldAlert',
        timestamp: Date.now()
      },
      {
        id: '1234567891',
        text: '⚠️ Multiple suspicious transactions detected from contract 0x1234567890123456789012345678901234567890. Possible exploit in progress.',
        author: 'CertiKAlert',
        timestamp: Date.now()
      }
    ];

    for (const tweet of mockTweets) {
      await processAnnouncement(tweet, 'twitter', channel, redisClient);
    }

  } catch (error) {
    logger.error('Error checking Twitter announcements', error);
  }
}

async function checkTelegramChannels(channel: Channel, redisClient: RedisLike): Promise<void> {
  try {
    const mockMessages = [
      {
        id: 'tg_123',
        text: '🚨 URGENT: Flash loan attack detected on Protocol Y. Attacker address: 0xbadactor123456789012345678901234567890',
        channel: 'DeFi Security Alerts',
        timestamp: Date.now()
      }
    ];

    for (const message of mockMessages) {
      await processAnnouncement(message, 'telegram', channel, redisClient);
    }

  } catch (error) {
    logger.error('Error checking Telegram channels', error);
  }
}

async function checkDiscordChannels(channel: Channel, redisClient: RedisLike): Promise<void> {
  try {
    const mockMessages = [
      {
        id: 'dc_123',
        text: 'SECURITY BREACH: Bridge hack detected. Multiple chains affected. TX: 0xhack1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab',
        server: 'Blockchain Security',
        timestamp: Date.now()
      }
    ];

    for (const message of mockMessages) {
      await processAnnouncement(message, 'discord', channel, redisClient);
    }

  } catch (error) {
    logger.error('Error checking Discord channels', error);
  }
}

async function checkBlockchainNews(channel: Channel, redisClient: RedisLike): Promise<void> {
  try {
    const mockArticles = [
      {
        id: 'news_123',
        title: 'Major DeFi Protocol Suffers $50M Exploit',
        content: 'A major DeFi protocol was exploited today for over $50 million. The attacker used a flash loan attack vector. Transaction hashes: 0xnews1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab',
        source: 'CryptoNews',
        timestamp: Date.now()
      }
    ];

    for (const article of mockArticles) {
      await processAnnouncement(article, 'news', channel, redisClient);
    }

  } catch (error) {
    logger.error('Error checking blockchain news', error);
  }
}

async function processAnnouncement(announcement: any, source: string, channel: Channel, redisClient: RedisLike): Promise<void> {
  try {
    const announcementKey = `announcement:${source}:${announcement.id}`;
    const seen = await redisClient.get(announcementKey);
    if (seen) {
      return;
    }

    logger.info(`Processing ${source} announcement: ${announcement.id}`, {
      title: announcement.title || 'N/A',
      author: announcement.author || announcement.channel || announcement.server || 'Unknown'
    });

    const txHashes = extractTransactionHashes(announcement.text || announcement.content || '');
    const addresses = extractAddresses(announcement.text || announcement.content || '');

    for (const address of addresses) {
      const recentTxs = await findRecentTransactionsByAddress(address);

      for (const tx of recentTxs) {
        const severity = determineSeverity(announcement);
        const confidence = determineConfidence(source, announcement);

        await publishLabelEvent(
          tx.hash,
          'MALICIOUS',
          tx.chain,
          {
            confidence: confidence,
            source: `hack-monitor-${source}`,
            severity: severity,
            description: `Transaction related to hack announcement: ${announcement.title || announcement.text?.substring(0, 100)}`
          }
        );

        logger.info(`Published hack-related label for ${tx.hash} from ${source}`);
      }
    }

    for (const txHash of txHashes) {
      const severity = determineSeverity(announcement);
      const confidence = determineConfidence(source, announcement);

      await publishLabelEvent(
        txHash,
        'MALICIOUS',
        'ethereum',
        {
          confidence: confidence,
          source: `hack-monitor-${source}`,
          severity: severity,
          description: `Transaction directly mentioned in hack announcement: ${announcement.title || announcement.text?.substring(0, 100)}`
        }
      );

      logger.info(`Published hack-related label for directly mentioned transaction ${txHash} from ${source}`);
    }

    await redisClient.setEx(announcementKey, 86400, '1');

  } catch (error) {
    logger.error(`Error processing ${source} announcement ${announcement.id}`, error);
  }
}

function extractTransactionHashes(text: string): string[] {
  const regex = /0x[a-fA-F0-9]{64}/g;
  const matches = text.match(regex);
  return matches ? [...new Set(matches)] : [];
}

function extractAddresses(text: string): Array<{ address: string, chain: string }> {
  const addresses: Array<{ address: string, chain: string }> = [];

  const btcRegex = /[13][a-km-zA-HJ-NP-Z1-9]{25,34}|bc1[a-z0-9]{39,59}/g;
  const btcMatches = text.match(btcRegex);
  if (btcMatches) {
    for (const match of btcMatches) {
      addresses.push({ address: match, chain: 'bitcoin' });
    }
  }

  const ethRegex = /0x[a-fA-F0-9]{40}/g;
  const ethMatches = text.match(ethRegex);
  if (ethMatches) {
    for (const match of ethMatches) {
      addresses.push({ address: match, chain: 'ethereum' });
    }
  }

  return addresses;
}

async function findRecentTransactionsByAddress(address: { address: string, chain: string }): Promise<Array<{ hash: string, chain: string, timestamp: number }>> {
  const mockTxs: Array<{ hash: string, chain: string, timestamp: number }> = [];

  for (let i = 0; i < 2; i++) {
    mockTxs.push({ hash: `0x${generateMockHash()}`, chain: address.chain, timestamp: Date.now() - (i * 3600000) });
  }

  return mockTxs;
}

function generateMockHash(): string {
  return Array.from({ length: 64 }, () => Math.floor(Math.random() * 16).toString(16)).join('');
}

function determineSeverity(announcement: any): string {
  const text = (announcement.text || announcement.content || announcement.title || '').toLowerCase();

  if (text.includes('urgent') || text.includes('critical') || text.includes('emergency')) {
    return 'critical';
  }
  if (text.includes('hack') || text.includes('exploit') || text.includes('attack') || text.includes('breach')) {
    return 'high';
  }
  if (text.includes('suspicious') || text.includes('alert') || text.includes('warning')) {
    return 'medium';
  }

  return 'low';
}

function determineConfidence(source: string, announcement: any): number {
  const sourceConfidence: Record<string, number> = {
    'twitter': 0.7,
    'telegram': 0.6,
    'discord': 0.5,
    'news': 0.8
  };

  let baseConfidence = sourceConfidence[source] || 0.5;

  const text = (announcement.text || announcement.content || announcement.title || '').toLowerCase();

  if (text.includes('transaction:') || text.includes('tx:') || text.includes('0x')) {
    baseConfidence += 0.1;
  }

  if (announcement.author === 'PeckShieldAlert' || announcement.author === 'CertiKAlert') {
    baseConfidence += 0.2;
  }

  return Math.min(baseConfidence, 1.0);
}

export { logger as hackMonitorLogger };
