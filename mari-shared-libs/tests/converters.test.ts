import { Converters } from '../src/utils/conversion/converters';

describe('Converters', () => {
  test('should convert hex to bytes and back', () => {
    const hex = '9a3f';
    const bytes = Converters.hexToBytes(hex);
    const backToHex = Converters.bytesToHex(bytes);

    expect(bytes).toBeInstanceOf(Uint8Array);
    expect(bytes.length).toBe(2);
    expect(backToHex).toBe(hex);
  });

  test('should handle longer hex strings', () => {
    const hex = 'deadbeefcafebabe';
    const bytes = Converters.hexToBytes(hex);
    const backToHex = Converters.bytesToHex(bytes);

    expect(bytes.length).toBe(8);
    expect(backToHex).toBe(hex);
  });

  test('should convert string to bytes and back', () => {
    const str = 'Hello, Mari!';
    const bytes = Converters.stringToBytes(str);
    const backToStr = Converters.bytesToString(bytes);

    expect(bytes).toBeInstanceOf(Uint8Array);
    expect(backToStr).toBe(str);
  });

  test('should convert bytes to base64 and back', () => {
    const bytes = new Uint8Array([1, 2, 3, 4, 5]);
    const base64 = Converters.bytesToBase64(bytes);
    const backToBytes = Converters.base64ToBytes(base64);

    expect(typeof base64).toBe('string');
    expect(backToBytes).toEqual(bytes);
  });
});
