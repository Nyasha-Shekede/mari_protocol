import { createHash } from 'crypto';
import * as bitcoin from 'bitcoinjs-lib';

export interface FeatureVector {
  coupon_hash: string;
  kid: string;
  expiry_ts: number;
  seal: string;
  grid_id: string;
  amount: number;
  _metadata?: {
    chain: string;
    category?: string;
    is_from_darknet?: boolean;
    risk_factors: string[];
    tx_type?: string;
    fee_usd?: number;
    value_usd?: number;
    input_count?: number;
    output_count?: number;
    locktime?: number;
    version?: number;
    size_bytes?: number;
    weight?: number;
    fee_rate?: number;
    confirmations?: number;
    block_height?: number;
    timestamp?: number;
    sender_address?: string;
    receiver_address?: string;
    contract_address?: string;
    gas_used?: number;
    gas_price?: number;
    nonce?: number;
    data?: string;
  };
}

// Darknet address database (simplified)
const DARKNET_ADDRESSES = new Set([
  '1F1tAaz5x1HUXrCNLbtMDqcw6o5GNn4xqX',
  '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy',
]);

function isDarknetAddress(address: string): boolean {
  return DARKNET_ADDRESSES.has(address);
}

function createSeal(input: string): string {
  return createHash('sha256').update(input).digest('hex').substring(0, 8);
}

function convertBtcToUsd(btc: number): number {
  const btcPrice = parseFloat(process.env.BTC_USD_PRICE || '30000');
  return btc * btcPrice;
}

function convertEthToUsd(eth: number): number {
  const ethPrice = parseFloat(process.env.ETH_USD_PRICE || '2000');
  return eth * ethPrice;
}

function convertSolToUsd(sol: number): number {
  const solPrice = parseFloat(process.env.SOL_USD_PRICE || '100');
  return sol * solPrice;
}

export function featurizeBitcoinTx(tx: any): FeatureVector {
  const txid = tx.txid || tx.hash;
  const value = tx.value || tx.amount || 0;
  const fee = tx.fee || 0;
  const amount = convertBtcToUsd(value);
  const feeUsd = convertBtcToUsd(fee);

  const sealInput = `${txid}-${tx.time || Date.now()}-${value}`;
  const seal = createSeal(sealInput);

  const riskFactors: string[] = [];

  if (value > 10) riskFactors.push('large_value');
  if (fee > 0.001) riskFactors.push('high_fee');
  if (tx.locktime > 0) riskFactors.push('time_lock');
  if (tx.vin && tx.vin.length > 10) riskFactors.push('many_inputs');
  if (tx.vout && tx.vout.length > 10) riskFactors.push('many_outputs');

  let isFromDarknet = false;
  let senderAddress = 'unknown';

  if (tx.vin && tx.vin[0] && tx.vin[0].prevout && tx.vin[0].prevout.scriptpubkey_address) {
    senderAddress = tx.vin[0].prevout.scriptpubkey_address;
    if (isDarknetAddress(senderAddress)) {
      isFromDarknet = true;
      riskFactors.push('darknet_source');
    }
  }

  let receiverAddress = 'unknown';
  if (tx.vout && tx.vout[0] && tx.vout[0].scriptpubkey_address) {
    receiverAddress = tx.vout[0].scriptpubkey_address;
    if (isDarknetAddress(receiverAddress)) {
      riskFactors.push('darknet_destination');
    }
  }

  return {
    coupon_hash: txid,
    kid: `bitcoin:${senderAddress}`,
    expiry_ts: Date.now() + 10 * 60 * 1000,
    seal: seal,
    grid_id: 'crypto:bitcoin',
    amount: amount,
    _metadata: {
      chain: 'bitcoin',
      category: 'transaction',
      is_from_darknet: isFromDarknet,
      risk_factors: riskFactors,
      tx_type: 'p2p',
      fee_usd: feeUsd,
      value_usd: amount,
      input_count: tx.vin?.length || 0,
      output_count: tx.vout?.length || 0,
      locktime: tx.locktime || 0,
      version: tx.version || 1,
      size_bytes: tx.size || 0,
      weight: tx.weight || 0,
      fee_rate: fee > 0 && tx.size ? (fee * 100000000) / tx.size : 0,
      confirmations: tx.status?.confirmed ? 1 : 0,
      block_height: tx.status?.block_height,
      timestamp: tx.status?.block_time || tx.time || Date.now(),
      sender_address: senderAddress,
      receiver_address: receiverAddress
    }
  };
}

export function featurizeEthereumTx(tx: any): FeatureVector {
  const txHash = tx.hash;
  const value = parseFloat(tx.value || '0') / 1e18;
  const amount = convertEthToUsd(value);
  const gasUsed = parseInt(tx.gas || '0');
  const gasPrice = parseFloat(tx.gasPrice || '0') / 1e9;
  const fee = (gasUsed * gasPrice) / 1e9;
  const feeUsd = convertEthToUsd(fee);

  const sealInput = `${txHash}-${tx.nonce || '0'}-${value}`;
  const seal = createSeal(sealInput);

  const riskFactors: string[] = [];

  if (value > 5) riskFactors.push('large_value');
  if (gasPrice > 100) riskFactors.push('high_gas_price');
  if (tx.to && isContractAddress(tx.to)) riskFactors.push('contract_interaction');
  if (tx.input && tx.input !== '0x' && tx.input !== '0x0') riskFactors.push('smart_contract');
  if (tx.nonce === 0) riskFactors.push('new_account');

  return {
    coupon_hash: txHash,
    kid: `ethereum:${tx.from || 'unknown'}`,
    expiry_ts: Date.now() + 10 * 60 * 1000,
    seal: seal,
    grid_id: 'crypto:ethereum',
    amount: amount,
    _metadata: {
      chain: 'ethereum',
      category: 'transaction',
      risk_factors: riskFactors,
      tx_type: tx.to && isContractAddress(tx.to) ? 'contract_call' : 'transfer',
      fee_usd: feeUsd,
      value_usd: amount,
      input_count: 1,
      output_count: 1,
      gas_used: gasUsed,
      gas_price: gasPrice,
      nonce: parseInt(tx.nonce || '0'),
      data: tx.input || '0x',
      confirmations: tx.blockNumber ? 1 : 0,
      block_height: tx.blockNumber,
      timestamp: tx.timestamp || Date.now(),
      sender_address: tx.from || 'unknown',
      receiver_address: tx.to || 'unknown',
      contract_address: tx.to && isContractAddress(tx.to) ? tx.to : undefined
    }
  };
}

export function featurizeSolanaTx(tx: any): FeatureVector {
  const transaction = tx?.transaction || {};
  const message = transaction?.message || {};
  const signatures: string[] = Array.isArray(transaction?.signatures) ? transaction.signatures : [];
  const txid = signatures[0] || (typeof tx === 'string' ? tx : 'unknown');
  const meta = tx?.meta || {};

  let value = 0;
  let fee = 0;

  if (meta && meta.postTokenBalances && meta.preTokenBalances) {
    value = extractTokenValue(meta);
  } else if (Array.isArray(message?.instructions)) {
    value = extractSolValue(message.instructions);
  }

  if (meta && meta.fee) {
    fee = meta.fee / 1e9;
  }

  const amount = convertSolToUsd(value);
  const feeUsd = convertSolToUsd(fee);

  const sealInput = `${txid}-${Date.now()}-${value}`;
  const seal = createSeal(sealInput);

  const riskFactors: string[] = [];

  if (value > 100) riskFactors.push('large_value');
  if (fee > 0.01) riskFactors.push('high_fee');
  if (meta && meta.err) riskFactors.push('transaction_error');
  if (Array.isArray(message?.instructions) && message.instructions.length > 5) riskFactors.push('complex_transaction');

  return {
    coupon_hash: txid,
    kid: `solana:${(Array.isArray(message?.accountKeys) && message.accountKeys[0]) || 'unknown'}`,
    expiry_ts: Date.now() + 10 * 60 * 1000,
    seal: seal,
    grid_id: 'crypto:solana',
    amount: amount,
    _metadata: {
      chain: 'solana',
      category: 'transaction',
      risk_factors: riskFactors,
      tx_type: meta && meta.postTokenBalances ? 'token_transfer' : 'transfer',
      fee_usd: feeUsd,
      value_usd: amount,
      input_count: Array.isArray(message?.accountKeys) ? message.accountKeys.length : 0,
      output_count: Array.isArray(message?.accountKeys) ? message.accountKeys.length : 0,
      confirmations: meta && meta.err === null ? 1 : 0,
      timestamp: Date.now(),
      sender_address: (Array.isArray(message?.accountKeys) && message.accountKeys[0]) || 'unknown',
      receiver_address: (Array.isArray(message?.accountKeys) && message.accountKeys[1]) || 'unknown'
    }
  };
}

function isContractAddress(address: string): boolean {
  return address.toLowerCase().startsWith('0x') && address.length === 42;
}

function extractTokenValue(meta: any): number {
  if (meta.postTokenBalances && meta.postTokenBalances[0]) {
    return parseFloat(meta.postTokenBalances[0].uiTokenAmount?.uiAmountString || '0');
  }
  return 0;
}

function extractSolValue(instructions: any[]): number {
  for (const instruction of instructions) {
    if (instruction.parsed && instruction.parsed.info && instruction.parsed.info.lamports) {
      return instruction.parsed.info.lamports / 1e9;
    }
  }
  return 0;
}
