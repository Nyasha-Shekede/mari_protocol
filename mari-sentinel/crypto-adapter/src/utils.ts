// Shared utilities for adapters: backoff + jitter and resilient fetch

export type BackoffOptions = {
  retries?: number; // total attempts including the first, default 3
  baseDelayMs?: number; // initial backoff, default 500ms
  maxDelayMs?: number; // cap, default 5000ms
  timeoutMs?: number; // per request timeout, default 10000ms
};

function sleep(ms: number) {
  return new Promise((r) => setTimeout(r, ms));
}

export async function fetchWithBackoff(input: RequestInfo | URL, opts: BackoffOptions = {}, init?: RequestInit): Promise<Response> {
  const retries = Math.max(1, opts.retries ?? 3);
  const base = opts.baseDelayMs ?? 500;
  const cap = opts.maxDelayMs ?? 5000;
  const timeoutMs = opts.timeoutMs ?? 10000;

  let lastErr: any;
  for (let attempt = 0; attempt < retries; attempt++) {
    const ctrl = new AbortController();
    const t = setTimeout(() => ctrl.abort(), timeoutMs);
    try {
      const res = await fetch(input, { signal: ctrl.signal, ...(init || {}) });
      clearTimeout(t);
      return res;
    } catch (err) {
      clearTimeout(t);
      lastErr = err;
      if (attempt < retries - 1) {
        // exponential backoff with jitter
        const delay = Math.min(cap, base * Math.pow(2, attempt));
        const jitter = Math.floor(Math.random() * (delay / 2));
        await sleep(delay + jitter);
        continue;
      }
    }
  }
  throw lastErr;
}

export function nextIndex(current: number, length: number): number {
  return ((current + 1) % Math.max(1, length));
}
