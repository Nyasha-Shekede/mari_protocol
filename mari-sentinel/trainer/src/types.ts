export interface TransactionEvent {
  event_id: string;
  event_type: 'PRE_SETTLEMENT' | 'SETTLEMENT_OUTCOME';
  coupon_hash: string;
  kid: string;
  expiry_ts: number;
  seal: string;
  grid_id: string;
  amount: number;
  result?: 'SUCCESS' | 'DUPLICATE' | 'INVALID_SIG';
}
