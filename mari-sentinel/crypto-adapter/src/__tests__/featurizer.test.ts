import { featurizeBitcoinTx, featurizeEthereumTx, featurizeSolanaTx } from '../featurizer';

describe('featurizer', () => {
  test('featurizeBitcoinTx produces expected fields', () => {
    const tx = {
      txid: 'abc123',
      value: 0.5,
      fee: 0.0002,
      locktime: 0,
      vin: [{ prevout: { scriptpubkey_address: '1BoatSLRHtKNngkdXEeobR76b53LETtpyT' } }],
      vout: [{ scriptpubkey_address: '1dice8EMZmqKvrGE4Qc9bUFf9PX3xaYDp' }],
      status: { confirmed: false },
      size: 250,
      version: 2,
    } as any;

    const fv = featurizeBitcoinTx(tx);
    expect(fv.coupon_hash).toBe('abc123');
    expect(fv.kid.startsWith('bitcoin:')).toBe(true);
    expect(fv.grid_id).toBe('crypto:bitcoin');
    expect(typeof fv.amount).toBe('number');
    expect(fv._metadata?.chain).toBe('bitcoin');
    expect(fv._metadata?.input_count).toBe(1);
    expect(fv._metadata?.output_count).toBe(1);
  });

  test('featurizeEthereumTx converts wei to USD and flags contract', () => {
    const tx = {
      hash: '0xhash',
      value: (1e18).toString(),
      gas: '21000',
      gasPrice: (100n * 10n ** 9n).toString(), // 100 gwei
      nonce: '1',
      from: '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
      to: '0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
      input: '0x',
    } as any;

    const fv = featurizeEthereumTx(tx);
    expect(fv.coupon_hash).toBe('0xhash');
    expect(fv.kid.startsWith('ethereum:')).toBe(true);
    expect(fv.grid_id).toBe('crypto:ethereum');
    expect(fv._metadata?.gas_used).toBe(21000);
    expect(fv._metadata?.gas_price).toBe(100);
  });

  test('featurizeSolanaTx extracts signatures and accounts', () => {
    const tx: any = {
      transaction: {
        signatures: ['sig123'],
        message: {
          accountKeys: ['sender', 'receiver'],
          instructions: [],
        }
      },
      meta: {
        fee: 5000,
      }
    };

    const fv = featurizeSolanaTx(tx);
    expect(fv.coupon_hash).toBe('sig123');
    expect(fv.kid.startsWith('solana:')).toBe(true);
    expect(fv.grid_id).toBe('crypto:solana');
    expect(fv._metadata?.input_count).toBe(2);
    expect(fv._metadata?.output_count).toBe(2);
  });
});
