import { Channel } from 'amqplib';
// Loosen Redis typing to avoid generics mismatch during build
type RedisLike = any;
import Web3 from 'web3';
import { fetchWithBackoff, nextIndex } from '../utils';
import { featurizeEthereumTx } from '../featurizer';
import { publishEvent, dedupeHits } from '../publisher';

const logger = {
  info: (msg: string, data?: any) => console.log(`[Ethereum Adapter] ${msg}`, data || ''),
  error: (msg: string, error?: any) => console.error(`[Ethereum Adapter] ${msg}`, error || ''),
  warn: (msg: string, data?: any) => console.warn(`[Ethereum Adapter] ${msg}`, data || ''),
};

export function startEthereumAdapter(channel: Channel, redisClient: RedisLike): void {
  const primary = process.env.ETHEREUM_RPC_URL || 'https://eth.llamarpc.com';
  const secondary = process.env.ETHEREUM_RPC_URL_ALT || '';
  let rpcUrl = primary;
  const pollInterval = parseInt(process.env.ETHEREUM_POLL_INTERVAL || '15000');

  logger.info('Starting Ethereum adapter', {
    rpcUrl: rpcUrl.replace(/\/\/.*@/, '//***@'),
    pollInterval
  });

  if (rpcUrl.startsWith('ws')) {
    startWebSocketListener(rpcUrl, redisClient).catch((e) => {
      logger.error('Failed to initialize WebSocket subscriptions; falling back to polling', e);
      startPollingListener(rpcUrl.replace('ws', 'http'), redisClient, pollInterval, [primary, secondary].filter(Boolean));
    });
  } else {
    startPollingListener(rpcUrl, redisClient, pollInterval, [primary, secondary].filter(Boolean));
  }
}

async function startWebSocketListener(rpcUrl: string, redisClient: RedisLike): Promise<void> {
  const web3 = new Web3(rpcUrl);

  // Subscribe to pending transactions
  const pendingSub = await (web3.eth as any).subscribe('pendingTransactions');
  pendingSub.on('data', async (transactionHash: string) => {
    try {
      const txKey = `eth:tx:${transactionHash}`;
      const seen = await redisClient.get(txKey);
      if (seen) {
        try { dedupeHits.inc({ chain: 'ethereum', adapter: 'ethereum' }); } catch {}
        return;
      }

      const tx = await web3.eth.getTransaction(transactionHash as any);
      if (!tx) return;

      const features = featurizeEthereumTx(tx);
      (features as any)._metadata = { ...((features as any)._metadata || {}), adapter: 'ethereum' };

      await publishEvent({
        event_type: 'PRE_SETTLEMENT',
        ...features,
        timestamp: Date.now()
      });

      logger.info(`Published Ethereum transaction: ${transactionHash}`, {
        from: tx.from,
        to: tx.to,
        value: features.amount,
        riskFactors: features._metadata?.risk_factors
      });

      await redisClient.setEx(txKey, 3600, '1');

    } catch (error) {
      logger.error(`Error processing Ethereum transaction ${transactionHash}`, error as any);
    }
  });
  pendingSub.on('error', (error: any) => {
    logger.error('WebSocket connection error (pendingTransactions)', error);
    setTimeout(() => {
      logger.info('Falling back to polling mode');
      startPollingListener(
        rpcUrl.replace('ws', 'http'),
        redisClient,
        10000,
        [
          process.env.ETHEREUM_RPC_URL || 'https://eth.llamarpc.com',
          process.env.ETHEREUM_RPC_URL_ALT || ''
        ].filter(Boolean)
      );
    }, 5000);
  });

  // Subscribe to new block headers
  const headerSub = await (web3.eth as any).subscribe('newBlockHeaders');
  headerSub.on('data', async (blockHeader: any) => {
    try {
      const block = await web3.eth.getBlock(blockHeader.number as any, true);
      if (block && block.transactions) {
        logger.info(`Processing Ethereum block ${block.number} with ${block.transactions.length} transactions`);

        for (const tx of (block.transactions as any[]).slice(0, 20)) {
          try {
            if (typeof tx === 'string') { continue; }
            const txKey = `eth:tx:${tx.hash}`;
            const seen = await redisClient.get(txKey);
            if (seen) { try { dedupeHits.inc({ chain: 'ethereum', adapter: 'ethereum' }); } catch {}; continue; }

            const features = featurizeEthereumTx({
              ...tx,
              blockNumber: Number(block.number),
              timestamp: Number(block.timestamp)
            });
            (features as any)._metadata = { ...((features as any)._metadata || {}), adapter: 'ethereum' };

            await publishEvent({
              event_type: 'PRE_SETTLEMENT',
              ...features,
              timestamp: Date.now()
            });

            logger.info(`Published confirmed Ethereum transaction: ${tx.hash}`);

            await redisClient.setEx(txKey, 3600, '1');

          } catch (txError) {
            logger.error(`Error processing confirmed transaction ${tx.hash}` as any, txError as any);
          }
        }
      }
    } catch (error) {
      logger.error(`Error processing Ethereum block ${blockHeader?.number}`, error as any);
    }
  });
  headerSub.on('error', (error: any) => {
    logger.error('WebSocket connection error (newBlockHeaders)', error);
    setTimeout(() => {
      logger.info('Falling back to polling mode');
      startPollingListener(rpcUrl.replace('ws', 'http'), redisClient, 10000, [process.env.ETHEREUM_RPC_URL || 'https://eth.llamarpc.com', process.env.ETHEREUM_RPC_URL_ALT || ''].filter(Boolean));
    }, 5000);
  });
}

function startPollingListener(rpcUrl: string, redisClient: RedisLike, pollInterval: number, rpcPool: string[]): void {
  let idx = 0;
  let web3 = new Web3(rpcUrl);
  let lastBlockNumber = 0;

  setInterval(async () => {
    try {
      const currentBlock = Number(await web3.eth.getBlockNumber());

      if (currentBlock > lastBlockNumber) {
        logger.info(`New Ethereum block detected: ${currentBlock}`);

        const block = await web3.eth.getBlock(currentBlock as any, true);
        if (block && block.transactions) {
          for (const tx of (block.transactions as any[]).slice(0, 15)) {
            try {
              if (typeof tx === 'string') { continue; }
              const txKey = `eth:tx:${tx.hash}`;
              const seen = await redisClient.get(txKey);
              if (seen) { try { dedupeHits.inc({ chain: 'ethereum', adapter: 'ethereum' }); } catch {}; continue; }

              const features = featurizeEthereumTx(tx as any);
              (features as any)._metadata = { ...((features as any)._metadata || {}), adapter: 'ethereum' };

              await publishEvent({
                event_type: 'PRE_SETTLEMENT',
                ...features,
                timestamp: Date.now()
              });

              logger.info(`Published Ethereum transaction: ${tx.hash}`);

              await redisClient.setEx(txKey, 3600, '1');

            } catch (txError) {
              logger.error(`Error processing transaction ${tx.hash}`, txError as any);
            }
          }
        }

        lastBlockNumber = currentBlock;
      }

    } catch (error) {
      logger.error('Error polling Ethereum', error as any);
      // Failover to next RPC if available
      if (rpcPool.length > 1) {
        idx = nextIndex(idx, rpcPool.length);
        const next = rpcPool[idx];
        logger.warn('Switching Ethereum RPC URL', { next });
        web3 = new Web3(next);
      }
    }
  }, pollInterval);
}

export { logger as ethereumLogger };
