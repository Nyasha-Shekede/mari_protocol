import { Channel } from 'amqplib';
// Loosen Redis typing to avoid generics mismatch during build
type RedisLike = any;
import { Connection, PublicKey } from '@solana/web3.js';
import { featurizeSolanaTx } from '../featurizer';
import { publishEvent, dedupeHits } from '../publisher';

const logger = {
  info: (msg: string, data?: any) => console.log(`[Solana Adapter] ${msg}`, data || ''),
  error: (msg: string, error?: any) => console.error(`[Solana Adapter] ${msg}`, error || ''),
  warn: (msg: string, data?: any) => console.warn(`[Solana Adapter] ${msg}`, data || ''),
};

export function startSolanaAdapter(channel: Channel, redisClient: RedisLike): void {
  const primary = process.env.SOLANA_RPC_URL || 'https://api.mainnet-beta.solana.com';
  const secondary = process.env.SOLANA_RPC_URL_ALT || '';
  let rpcUrl = primary;
  const pollInterval = parseInt(process.env.SOLANA_POLL_INTERVAL || '20000');

  logger.info('Starting Solana adapter', {
    rpcUrl,
    pollInterval
  });

  let connection = new Connection(rpcUrl, 'confirmed');

  setInterval(async () => {
    try {
      const signatures = await connection.getSignaturesForAddress(
        new PublicKey('11111111111111111111111111111111'),
        { limit: 20 }
      );

      logger.info(`Fetched ${signatures.length} recent Solana signatures`);

      for (const sigInfo of signatures) {
        try {
          const txKey = `sol:tx:${sigInfo.signature}`;
          const seen = await redisClient.get(txKey);
          if (seen) { try { dedupeHits.inc(); } catch {}; continue; }

          if (sigInfo.err) {
            logger.info(`Skipping failed transaction: ${sigInfo.signature}`);
            continue;
          }

          const tx = await connection.getTransaction(sigInfo.signature, {
            commitment: 'confirmed',
            maxSupportedTransactionVersion: 0
          });

          if (!tx) continue;

          const features = featurizeSolanaTx(tx);
          (features as any)._metadata = { ...((features as any)._metadata || {}), adapter: 'solana' };

          await publishEvent({
            event_type: 'PRE_SETTLEMENT',
            ...features,
            timestamp: Date.now()
          });

          logger.info(`Published Solana transaction: ${sigInfo.signature}`, {
            slot: tx.slot,
            riskFactors: features._metadata?.risk_factors
          });

          await redisClient.setEx(txKey, 3600, '1');

          await new Promise(resolve => setTimeout(resolve, 50));

        } catch (txError) {
          logger.error(`Error processing Solana transaction ${sigInfo.signature}` as any, txError as any);
        }
      }

    } catch (error) {
      logger.error('Error fetching Solana transactions', error as any);
      // Failover to alternate RPC if provided
      const pool = [primary, secondary].filter(Boolean);
      if (pool.length > 1) {
        const next = rpcUrl === primary ? secondary : primary;
        logger.warn('Switching Solana RPC URL', { next });
        rpcUrl = next;
        connection = new Connection(rpcUrl, 'confirmed');
      }
    }
  }, pollInterval);

  setInterval(async () => {
    try {
      const slot = await connection.getSlot('confirmed');
      const blockKey = `sol:block:${slot}`;
      const seen = await redisClient.get(blockKey);

      if (!seen) {
        logger.info(`Processing new Solana slot: ${slot}`);

        const block = await connection.getBlock(slot, {
          commitment: 'confirmed',
          maxSupportedTransactionVersion: 0
        });

        if (block && block.transactions) {
          for (const tx of block.transactions.slice(0, 10)) {
            try {
              const txid = tx.transaction.signatures[0];
              const txKey = `sol:tx:${txid}`;
              const seenTx = await redisClient.get(txKey);
              if (seenTx) { try { dedupeHits.inc({ chain: 'solana', adapter: 'solana' }); } catch {}; continue; }

              const features = featurizeSolanaTx({
                ...tx,
                slot: slot,
                blockTime: block.blockTime
              });
              (features as any)._metadata = { ...((features as any)._metadata || {}), adapter: 'solana' };

              await publishEvent({
                event_type: 'PRE_SETTLEMENT',
                ...features,
                timestamp: Date.now()
              });

              logger.info(`Published confirmed Solana transaction: ${txid}`);

              await redisClient.setEx(txKey, 3600, '1');

            } catch (txError) {
              logger.error(`Error processing confirmed transaction` as any, txError as any);
            }
          }
        }

        await redisClient.setEx(blockKey, 3600, '1');
      }

    } catch (error) {
      logger.error('Error monitoring Solana slots', error as any);
      const pool = [primary, secondary].filter(Boolean);
      if (pool.length > 1) {
        const next = rpcUrl === primary ? secondary : primary;
        logger.warn('Switching Solana RPC URL', { next });
        rpcUrl = next;
        connection = new Connection(rpcUrl, 'confirmed');
      }
    }
  }, Math.max(30000, pollInterval));
}

export { logger as solanaLogger };
