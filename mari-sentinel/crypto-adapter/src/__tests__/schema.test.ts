import Ajv, { JSONSchemaType } from 'ajv';

const ajv = new Ajv({ allErrors: true });

// PRE_SETTLEMENT schema (simplified)
const preSchema: JSONSchemaType<any> = {
  type: 'object',
  properties: {
    event_type: { type: 'string', const: 'PRE_SETTLEMENT' },
    coupon_hash: { type: 'string' },
    kid: { type: 'string' },
    expiry_ts: { type: 'number' },
    seal: { type: 'string' },
    grid_id: { type: 'string' },
    amount: { type: 'number' },
    _metadata: {
      type: 'object',
      nullable: true,
      properties: {
        chain: { type: 'string' },
        category: { type: 'string', nullable: true },
        risk_factors: {
          type: 'array',
          items: { type: 'string' },
          nullable: true,
        },
      },
      required: ['chain'],
      additionalProperties: true,
    },
    timestamp: { type: 'number', nullable: true },
  },
  required: ['event_type', 'coupon_hash', 'kid', 'expiry_ts', 'seal', 'grid_id', 'amount'],
  additionalProperties: true,
};

// SETTLEMENT_OUTCOME schema (simplified)
const outcomeSchema: JSONSchemaType<any> = {
  type: 'object',
  properties: {
    event_type: { type: 'string', const: 'SETTLEMENT_OUTCOME' },
    coupon_hash: { type: 'string' },
    result: { type: 'string' },
    chain: { type: 'string' },
    ts: { type: 'number' },
    confidence: { type: 'number', nullable: true },
    source: { type: 'string', nullable: true },
    severity: { type: 'string', nullable: true },
    description: { type: 'string', nullable: true },
  },
  required: ['event_type', 'coupon_hash', 'result', 'chain', 'ts'],
  additionalProperties: true,
};

const validatePre = ajv.compile(preSchema);
const validateOutcome = ajv.compile(outcomeSchema);

describe('event schema validation', () => {
  test('valid PRE_SETTLEMENT passes', () => {
    const ev = {
      event_type: 'PRE_SETTLEMENT',
      coupon_hash: '0xabc',
      kid: 'ethereum:0xsender',
      expiry_ts: Date.now() + 60000,
      seal: 'deadbeef',
      grid_id: 'crypto:ethereum',
      amount: 123.45,
      _metadata: {
        chain: 'ethereum',
        risk_factors: ['large_value']
      },
      timestamp: Date.now(),
    };
    const ok = validatePre(ev);
    if (!ok) console.error(validatePre.errors);
    expect(ok).toBe(true);
  });

  test('valid SETTLEMENT_OUTCOME passes', () => {
    const ev = {
      event_type: 'SETTLEMENT_OUTCOME',
      coupon_hash: '0xabc',
      result: 'MALICIOUS',
      chain: 'ethereum',
      ts: Date.now(),
      confidence: 0.95,
      source: 'certik',
      severity: 'high',
      description: 'Test event',
    };
    const ok = validateOutcome(ev);
    if (!ok) console.error(validateOutcome.errors);
    expect(ok).toBe(true);
  });

  test('PRE_SETTLEMENT missing fields fails', () => {
    const ev: any = { event_type: 'PRE_SETTLEMENT' };
    const ok = validatePre(ev);
    expect(ok).toBe(false);
  });
});
