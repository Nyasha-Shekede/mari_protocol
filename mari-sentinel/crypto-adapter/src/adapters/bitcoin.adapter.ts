import { Channel } from 'amqplib';
// Loosen Redis typing to avoid generics mismatch during build
type RedisLike = any;
import { featurizeBitcoinTx } from '../featurizer';
import { fetchWithBackoff } from '../utils';
import { publishEvent, dedupeHits } from '../publisher';

const logger = {
  info: (msg: string, data?: any) => console.log(`[Bitcoin Adapter] ${msg}`, data || ''),
  error: (msg: string, error?: any) => console.error(`[Bitcoin Adapter] ${msg}`, error || ''),
  warn: (msg: string, data?: any) => console.warn(`[Bitcoin Adapter] ${msg}`, data || ''),
};

export function startBitcoinAdapter(channel: Channel, redisClient: RedisLike): void {
  const MEMPOOL_API = process.env.BITCOIN_MEMPOOL_API || 'https://mempool.space/api';
  const POLL_INTERVAL = parseInt(process.env.BITCOIN_POLL_INTERVAL || '15000');
  const MAX_TRANSACTIONS_PER_POLL = parseInt(process.env.BITCOIN_MAX_TX_PER_POLL || '10');
  const RETRIES = parseInt(process.env.BITCOIN_FETCH_RETRIES || '3');

  logger.info('Starting Bitcoin adapter', {
    api: MEMPOOL_API,
    pollInterval: POLL_INTERVAL,
    maxTxPerPoll: MAX_TRANSACTIONS_PER_POLL
  });

  setInterval(async () => {
    try {
      const response = await fetchWithBackoff(`${MEMPOOL_API}/mempool/recent`, { retries: RETRIES });
      if (!response.ok) {
        throw new Error(`Mempool API error: ${response.status}`);
      }

      const transactions = await response.json();
      logger.info(`Fetched ${transactions.length} recent transactions`);

      let processed = 0;
      for (const tx of transactions) {
        if (processed >= MAX_TRANSACTIONS_PER_POLL) break;

        try {
          const txKey = `btc:tx:${tx.txid}`;
          const seen = await redisClient.get(txKey);
          if (seen) {
            logger.info(`Skipping already processed transaction: ${tx.txid}`);
            try { dedupeHits.inc({ chain: 'bitcoin', adapter: 'bitcoin' }); } catch {}
            continue;
          }

          const txResponse = await fetchWithBackoff(`${MEMPOOL_API}/tx/${tx.txid}`, { retries: RETRIES });
          if (!txResponse.ok) {
            logger.warn(`Failed to fetch transaction details for ${tx.txid}`);
            continue;
          }

          const fullTx = await txResponse.json();

          const features = featurizeBitcoinTx(fullTx);
          (features as any)._metadata = { ...((features as any)._metadata || {}), adapter: 'bitcoin' };

          await publishEvent({
            event_type: 'PRE_SETTLEMENT',
            ...features,
            timestamp: Date.now()
          });

          logger.info(`Published Bitcoin transaction: ${tx.txid}`, {
            amount: features.amount,
            riskFactors: features._metadata?.risk_factors
          });

          await redisClient.setEx(txKey, 3600, '1');
          processed++;

          await new Promise(resolve => setTimeout(resolve, 100));

        } catch (txError) {
          logger.error(`Error processing transaction ${tx.txid}`, txError);
        }
      }

      logger.info(`Processed ${processed} Bitcoin transactions this poll`);

    } catch (error) {
      logger.error('Error fetching Bitcoin transactions', error);
    }
  }, POLL_INTERVAL);

  setInterval(async () => {
    try {
      const response = await fetchWithBackoff(`${MEMPOOL_API}/blocks`, { retries: RETRIES });
      if (!response.ok) {
        throw new Error(`Mempool API error: ${response.status}`);
      }

      const blocks = await response.json();
      const latestBlock = blocks[0];

      if (latestBlock) {
        const blockKey = `btc:block:${latestBlock.height}`;
        const seen = await redisClient.get(blockKey);

        if (!seen) {
          logger.info(`Processing new Bitcoin block: ${latestBlock.height}`);

          const blockTxResponse = await fetchWithBackoff(`${MEMPOOL_API}/block/${latestBlock.id}/txs`, { retries: RETRIES });
          if (blockTxResponse.ok) {
            const blockTransactions = await blockTxResponse.json();

            for (const tx of blockTransactions.slice(0, 50)) {
              try {
                const features = featurizeBitcoinTx({
                  ...tx,
                  status: {
                    confirmed: true,
                    block_height: latestBlock.height,
                    block_hash: latestBlock.id,
                    block_time: latestBlock.timestamp
                  }
                });

                await publishEvent({
                  event_type: 'PRE_SETTLEMENT',
                  ...features,
                  timestamp: Date.now()
                });

                logger.info(`Published confirmed Bitcoin transaction: ${tx.txid}`);

              } catch (txError) {
                logger.error(`Error processing confirmed transaction ${tx.txid}`, txError);
              }
            }
          }

          await redisClient.setEx(blockKey, 86400, '1');
        }
      }

    } catch (error) {
      logger.error('Error monitoring Bitcoin blocks', error);
    }
  }, 60000);
}

export { logger as bitcoinLogger };
