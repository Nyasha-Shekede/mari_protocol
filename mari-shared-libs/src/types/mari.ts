export interface MariUser {
  id: string;
  bloodHash: string;
  locationGrid: string;
  readyCash: number;
  totalMoney: number;
  createdAt: number;
  updatedAt: number;
}

export interface MariTransaction {
  id: string;
  senderBioHash: string;
  receiverBioHash: string;
  amount: number;
  locationGrid: string;
  timestamp: number;
  status: TransactionStatus;
  type: TransactionType;
  coupon: string;
  transportMethod: TransportMethod;
}

export enum TransactionStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

export enum TransactionType {
  SEND = 'SEND',
  RECEIVE = 'RECEIVE'
}

export enum TransportMethod {
  SMS = 'SMS'
}

export enum ValidationError {
  BLOOD_MISMATCH = 'BLOOD_MISMATCH',
  LOCATION_MISMATCH = 'LOCATION_MISMATCH',
  TIME_EXPIRED = 'TIME_EXPIRED',
  SEAL_MISMATCH = 'SEAL_MISMATCH'
}
