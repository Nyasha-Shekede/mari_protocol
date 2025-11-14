import type { TransactionEvent } from './types';

export class Dataset {
  private pre = new Map<string, { ev: TransactionEvent; ts: number }>();
  private labeled: Array<{ x: Float32Array; y: number }> = [];
  private readonly ttlMs: number;

  constructor(ttlMs = 5 * 60 * 1000) {
    this.ttlMs = ttlMs;
  }

  add(ev: TransactionEvent) {
    const now = Date.now();
    this.prune(now);
    if (ev.event_type === 'PRE_SETTLEMENT') {
      this.pre.set(ev.coupon_hash, { ev, ts: now });
    } else if (ev.event_type === 'SETTLEMENT_OUTCOME' && ev.result) {
      const stored = this.pre.get(ev.coupon_hash);
      if (!stored) return;
      this.pre.delete(ev.coupon_hash);
      const x = this.featurize(stored.ev);
      const y = ev.result === 'SUCCESS' ? 1 : 0;
      this.labeled.push({ x, y });
    }
  }

  getBatch(size: number) {
    if (this.labeled.length < size) return null;
    return this.labeled.splice(0, size);
  }

  private prune(now: number) {
    for (const [k, v] of this.pre.entries()) {
      if (now - v.ts > this.ttlMs) this.pre.delete(k);
    }
  }

  private featurize(r: TransactionEvent): Float32Array {
    // identical to inference featurizer – keep in sync
    const kid = hashCode(r.kid) % 10000;
    const seal = hashCode(r.seal) % 10000;
    const grid = hashCode(r.grid_id) % 1000;
    const hashSeen = 0; // simplified
    const timeToExpiry = r.expiry_ts - Date.now();
    return new Float32Array([
      kid,
      seal,
      grid,
      r.amount,
      timeToExpiry,
      hashSeen,
      parseInt(r.coupon_hash.slice(0, 2), 16),
      parseInt(r.coupon_hash.slice(-2), 16),
    ]);
  }
}

function hashCode(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return Math.abs(h);
}
