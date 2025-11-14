import { toByteArray, fromByteArray } from 'base64-js';

export class Converters {
  static hexToBytes(hex: string): Uint8Array {
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < hex.length; i += 2) {
      bytes[i / 2] = parseInt(hex.substr(i, 2), 16);
    }
    return bytes;
  }
  
  static bytesToHex(bytes: Uint8Array): string {
    return Array.from(bytes)
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');
  }
  
  static base64ToBytes(base64: string): Uint8Array {
    return toByteArray(base64);
  }
  
  static bytesToBase64(bytes: Uint8Array): string {
    return fromByteArray(bytes);
  }
  
  static stringToBytes(str: string): Uint8Array {
    return new TextEncoder().encode(str);
  }
  
  static bytesToString(bytes: Uint8Array): string {
    return new TextDecoder().decode(bytes);
  }
}
