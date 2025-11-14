import { PhysicsCrypto } from '../src/crypto/physics/physicsCrypto';

describe('PhysicsCrypto', () => {
  test('should generate consistent physics seed', () => {
    const bloodHash = '9a3f';
    const motionVector = { x: 0.1, y: -0.2, z: 0.3 };
    const lightLevel = 500;
    const locationGrid = '9X4F*Z5L';

    const seed1 = PhysicsCrypto.generatePhysicsSeed(bloodHash, motionVector, lightLevel, locationGrid);
    const seed2 = PhysicsCrypto.generatePhysicsSeed(bloodHash, motionVector, lightLevel, locationGrid);

    expect(seed1).toEqual(seed2);
  });

  test('should derive physics key correctly', () => {
    const seed = new Uint8Array(32).fill(1);
    const key = PhysicsCrypto.derivePhysicsKey(seed);

    expect(key.length).toBe(32);
    expect(key).not.toEqual(seed);
  });

  test('should generate seal correctly', () => {
    const motionVector = { x: 0.1, y: -0.2, z: 0.3 };
    const lightLevel = 500;

    const seal = PhysicsCrypto.generateSeal(motionVector, lightLevel, 1234567890);

    expect(seal).toMatch(/^[0-9a-f]{8}$/);
  });

  test('should encrypt and decrypt data', () => {
    const data = new TextEncoder().encode('Hello, Mari!');
    const key = new Uint8Array(32).fill(1);
    const iv = new Uint8Array(16).fill(2);

    const { cipherText, authTag } = PhysicsCrypto.encryptWithPhysicsKey(data, key, iv);
    const decrypted = PhysicsCrypto.decryptWithPhysicsKey(cipherText, key, iv, authTag);

    expect(decrypted).toEqual(data);
    expect(new TextDecoder().decode(decrypted)).toBe('Hello, Mari!');
  });

  test('should fail decryption with wrong auth tag', () => {
    const data = new TextEncoder().encode('Hello, Mari!');
    const key = new Uint8Array(32).fill(1);
    const iv = new Uint8Array(16).fill(2);

    const { cipherText } = PhysicsCrypto.encryptWithPhysicsKey(data, key, iv);
    const wrongAuthTag = new Uint8Array(16).fill(99);

    expect(() => {
      PhysicsCrypto.decryptWithPhysicsKey(cipherText, key, iv, wrongAuthTag);
    }).toThrow('Authentication failed');
  });
});
