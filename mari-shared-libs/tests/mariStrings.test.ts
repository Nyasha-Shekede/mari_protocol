import { MariStringParser } from '../src/protocols/strings/mariStrings';

describe('MariStringParser', () => {
  test('should parse function ID correctly', () => {
    const functionId = 'mari://v1?g=9X4F*Z5L&b=9a3f&rc=100.00&tm=100.00&seal=G3K8';
    const parsed = MariStringParser.parseFunctionId(functionId);
    
    expect(parsed.version).toBe(1);
    expect(parsed.grid).toBe('9X4F*Z5L');
    expect(parsed.bloodHash).toBe('9a3f');
    expect(parsed.readyCash).toBe(100.00);
    expect(parsed.totalMoney).toBe(100.00);
    expect(parsed.seal).toBe('G3K8');
  });
  
  test('should generate function ID correctly', () => {
    const data = {
      grid: '9X4F*Z5L',
      bloodHash: '9a3f',
      readyCash: 100.00,
      totalMoney: 100.00,
      seal: 'G3K8'
    };
    
    const functionId = MariStringParser.generateFunctionId(data);
    expect(functionId).toBe('mari://v1?g=9X4F*Z5L&b=9a3f&rc=100.00&tm=100.00&seal=G3K8');
  });
  
  test('should parse transfer coupon correctly', () => {
    const coupon = 'mari://xfer?from=9a3f&to=384a&val=20.00&g=9X4F*Z5L&exp=173840000000000&s=7D4A';
    const parsed = MariStringParser.parseTransferCoupon(coupon);
    
    expect(parsed.senderBio).toBe('9a3f');
    expect(parsed.receiverBio).toBe('384a');
    expect(parsed.amount).toBe(20.00);
    expect(parsed.grid).toBe('9X4F*Z5L');
    expect(parsed.expiry).toBe(173840000000000);
    expect(parsed.seal).toBe('7D4A');
  });
  
  test('should validate mari strings', () => {
    expect(MariStringParser.validateMariString('mari://test')).toBe(true);
    expect(MariStringParser.validateMariString('http://test')).toBe(false);
    expect(MariStringParser.validateMariString('')).toBe(false);
  });
});
