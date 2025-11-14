import { sha256 } from 'js-sha256';

export function generateBatchSeal(transactions: Array<{ id: string; amount: number }>): string {
  const data = transactions
    .map(t => `${t.id}${t.amount}`)
    .sort()
    .join('');
  return sha256(data);
}

export function verifyBatchSeal(transactions: Array<{ id: string; amount: number }>, seal: string): boolean {
  return generateBatchSeal(transactions) === seal;
}
