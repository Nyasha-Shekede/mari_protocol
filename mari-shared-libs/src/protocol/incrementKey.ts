import { sha256 } from 'js-sha256';
// Use global Buffer (Node). TypeScript hint to avoid missing type errors in this package build.
declare const Buffer: any;

export interface IncrementKeyPayload {
  version: string;
  merchantId: string;
  newBalance: number;
  timestamp: number;
  locationGrid?: string;
  keyId: string;
}

// HMAC-SHA256 signature over the JSON payload (demo only)
function signPayload(payload: IncrementKeyPayload, privateKey: string): string {
  // js-sha256.hmac(key, message)
  return (sha256 as any).hmac(privateKey, JSON.stringify(payload));
}

export function generateIncrementKey(payload: IncrementKeyPayload, privateKey: string): string {
  const signature = signPayload(payload, privateKey);
  const base64Payload = Buffer.from(JSON.stringify(payload)).toString('base64');
  return `MARI_INC:${base64Payload}:${signature}`;
}

export function verifyIncrementKey(key: string, publicKey: string): boolean {
  // Demo verifier: HMAC is symmetric, so we treat publicKey as the verifying secret
  try {
    const parts = key.split(':');
    if (parts[0] !== 'MARI_INC' || parts.length !== 3) return false;
    const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString());
    const signature = parts[2];
    const expected = (sha256 as any).hmac(publicKey, JSON.stringify(payload));
    return expected === signature;
  } catch {
    return false;
  }
}
