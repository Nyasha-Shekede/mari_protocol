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

export interface InferenceRequest {
  coupon_hash: string;
  kid: string;
  expiry_ts: number;
  seal: string;
  grid_id: string;
  amount: number;
}

export interface InferenceResponse {
  score: number; // 0-999
  model_id: string;
}

export interface ModelStorage {
  model_id: string;
  buffer: string; // base64 ONNX
  created_at: number;
}
