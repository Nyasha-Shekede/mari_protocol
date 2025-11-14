import { createClient } from 'redis';
import { Dataset } from './dataset';

export class OnlineTrainer {
  private ds = new Dataset(5 * 60 * 1000); // 5 min TTL for PRE events
  private readonly batchSize = 500;
  private readonly listKey = 'labeled:examples';
  private readonly maxExamples = 50_000;

  constructor(private redis: ReturnType<typeof createClient>) {}

  async ingest(ev: import('./types').TransactionEvent) {
    this.ds.add(ev);
    const batch = this.ds.getBatch(this.batchSize);
    if (batch) await this.collect(batch);
  }

  private async collect(batch: Array<{ x: Float32Array; y: number }>) {
    // Push JSONL of labeled examples into Redis list for offline training
    const pipe = this.redis.multi();
    for (const b of batch) {
      const rec = {
        ts: Date.now(),
        x: Array.from(b.x),
        y: b.y,
      };
      pipe.rPush(this.listKey, JSON.stringify(rec));
    }
    // Trim to bounded size
    pipe.lTrim(this.listKey, -this.maxExamples, -1);
    await pipe.exec();
    console.log(`Collected ${batch.length} examples (total capped at ${this.maxExamples})`);
  }
}
