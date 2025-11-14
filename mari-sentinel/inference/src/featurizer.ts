// Deterministic ultra-light numerical encoding – no PII
function hashCode(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return Math.abs(h);
}

export function featurize(r: {
  coupon_hash: string;
  kid: string;
  expiry_ts: number;
  seal: string;
  grid_id: string;
  amount: number;
}): Float32Array {
  const now = Date.now();
  const kid = hashCode(r.kid) % 10000;
  const seal = hashCode(r.seal) % 10000;
  const grid = hashCode(r.grid_id) % 1000;
  const hashSeen = 0; // simplified (could be fed from Redis metrics later)
  const timeToExpiry = r.expiry_ts - now;

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
