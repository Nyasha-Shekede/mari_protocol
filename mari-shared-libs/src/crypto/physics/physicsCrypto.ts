import { sha256 } from 'js-sha256';

export class PhysicsCrypto {
  static generatePhysicsSeed(
    bloodHash: string,
    motionVector: { x: number; y: number; z: number },
    lightLevel: number,
    locationGrid: string
  ): Uint8Array {
    const combinedData = `${bloodHash}:${motionVector.x},${motionVector.y},${motionVector.z}:${lightLevel}:${locationGrid}`;
    const hash = sha256.create();
    hash.update(combinedData);
    return new Uint8Array(hash.array());
  }
  
  static derivePhysicsKey(physicsSeed: Uint8Array, context: string = 'MARI_KDF'): Uint8Array {
    const kdfInput = new Uint8Array(physicsSeed.length + context.length);
    kdfInput.set(physicsSeed);
    kdfInput.set(new TextEncoder().encode(context), physicsSeed.length);
    
    const hash = sha256.create();
    hash.update(kdfInput);
    return new Uint8Array(hash.array()).slice(0, 32); // 256-bit key
  }
  
  static generateSeal(
    motionVector: { x: number; y: number; z: number },
    lightLevel: number,
    timestamp: number = Date.now()
  ): string {
    const sealData = `${motionVector.x}:${motionVector.y}:${motionVector.z}:${lightLevel}:${timestamp}`;
    const hash = sha256.create();
    hash.update(sealData);
    return hash.hex().slice(0, 8); // 4-byte seal as hex string
  }
  
  static encryptWithPhysicsKey(
    data: Uint8Array,
    physicsKey: Uint8Array,
    iv: Uint8Array
  ): { cipherText: Uint8Array; authTag: Uint8Array } {
    // Simplified encryption for demo purposes
    // In a real implementation, this would use AES-GCM
    
    // XOR "encryption" for demo (not secure for production)
    const cipherText = new Uint8Array(data.length);
    for (let i = 0; i < data.length; i++) {
      cipherText[i] = data[i] ^ physicsKey[i % physicsKey.length];
    }
    
    // Generate a simple auth tag
    const authData = new Uint8Array([...physicsKey.slice(0, 16), ...iv]);
    const authHash = sha256.create();
    authHash.update(authData);
    const authTag = new Uint8Array(authHash.array()).slice(0, 16);
    
    return { cipherText, authTag };
  }
  
  static decryptWithPhysicsKey(
    cipherText: Uint8Array,
    physicsKey: Uint8Array,
    iv: Uint8Array,
    authTag: Uint8Array
  ): Uint8Array {
    // Verify auth tag first
    const authData = new Uint8Array([...physicsKey.slice(0, 16), ...iv]);
    const authHash = sha256.create();
    authHash.update(authData);
    const expectedAuthTag = new Uint8Array(authHash.array()).slice(0, 16);
    
    if (!this.constantTimeCompare(authTag, expectedAuthTag)) {
      throw new Error('Authentication failed');
    }
    
    // XOR "decryption" for demo
    const plainText = new Uint8Array(cipherText.length);
    for (let i = 0; i < cipherText.length; i++) {
      plainText[i] = cipherText[i] ^ physicsKey[i % physicsKey.length];
    }
    
    return plainText;
  }
  
  private static constantTimeCompare(a: Uint8Array, b: Uint8Array): boolean {
    if (a.length !== b.length) return false;
    
    let result = 0;
    for (let i = 0; i < a.length; i++) {
      result |= a[i] ^ b[i];
    }
    
    return result === 0;
  }
}
