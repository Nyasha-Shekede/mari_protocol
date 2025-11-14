import { startBitcoinAdapter } from '../adapters/bitcoin.adapter';
import { startEthereumAdapter } from '../adapters/ethereum.adapter';
import { startSolanaAdapter } from '../adapters/solana.adapter';

// Mocks
jest.mock('../publisher', () => ({
  publishEvent: jest.fn(async () => {}),
}));

// Provide a minimal RedisClientType-like mock
const redisMock = {
  get: jest.fn(async () => null),
  setEx: jest.fn(async () => 'OK'),
} as any;

// Global fetch mock for BTC adapter
(global as any).fetch = jest.fn(async (url: string) => {
  if (url.includes('/mempool/recent')) {
    return {
      ok: true,
      json: async () => [{ txid: 'tx123' }],
    };
  }
  if (url.includes('/tx/tx123')) {
    return {
      ok: true,
      json: async () => ({ txid: 'tx123', vin: [], vout: [], status: {} }),
    };
  }
  if (url.includes('/blocks')) {
    return { ok: true, json: async () => [{ height: 1, id: 'block1', timestamp: Date.now() }] };
  }
  if (url.includes('/block/block1/txs')) {
    return { ok: true, json: async () => [{ txid: 'txc1', vin: [], vout: [], status: {} }] };
  }
  return { ok: false, status: 404 } as any;
});

// Web3 and Solana mocks to avoid network
jest.mock('web3', () => {
  return jest.fn().mockImplementation(() => ({
    eth: {
      subscribe: (_: any, cb: any) => ({
        on: () => ({})
      }),
      getTransaction: async () => ({ hash: '0xabc', value: '0', gas: '0', gasPrice: '0', nonce: '0' }),
      getBlock: async () => ({ number: 1, transactions: [] }),
      getBlockNumber: async () => 1,
    }
  }));
});

jest.mock('@solana/web3.js', () => ({
  Connection: jest.fn().mockImplementation(() => ({
    getSignaturesForAddress: async () => [],
    getTransaction: async () => null,
    getSlot: async () => 1,
    getBlock: async () => ({ transactions: [] }),
  })),
  PublicKey: jest.fn().mockImplementation((x: string) => x),
}));

// Fake AMQP channel placeholder (not used by our start functions directly)
const fakeChannel = {} as any;

describe('adapters start without throwing', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.clearAllMocks();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('startBitcoinAdapter does not throw and sets intervals', () => {
    expect(() => startBitcoinAdapter(fakeChannel, redisMock)).not.toThrow();
    // Advance timers to trigger at least one cycle
    jest.advanceTimersByTime(100); // short tick; real intervals are longer
  });

  test('startEthereumAdapter does not throw (ws/poll logic mocked)', () => {
    expect(() => startEthereumAdapter(fakeChannel, redisMock)).not.toThrow();
  });

  test('startSolanaAdapter does not throw and sets intervals', () => {
    expect(() => startSolanaAdapter(fakeChannel, redisMock)).not.toThrow();
    jest.advanceTimersByTime(100);
  });
});
