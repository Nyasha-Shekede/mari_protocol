import { 
  MariStringParser, 
  SensorSimulator, 
  PhysicsCrypto,
  Validators,
  Converters 
} from '@mari/shared-libs';

console.log('=== Mari Shared Libraries Demo ===\n');

// 1. Parse a transfer coupon
const coupon = 'mari://xfer?from=9a3f&to=384a&val=20.00&g=9X4F*Z5L&exp=173840000000000&s=7D4A';
console.log('1. Parsing transfer coupon:', coupon);
const parsed = MariStringParser.parseTransferCoupon(coupon);
console.log('Parsed coupon:', parsed);

// 2. Generate physics data
console.log('\n2. Generating physics data:');
const physicsData = SensorSimulator.capturePhysicsData();
console.log('Physics data:', physicsData);

// 3. Validate data
console.log('\n3. Validating data:');
const isValidBlood = Validators.validateBloodHash(physicsData.bloodHash);
const isValidGrid = Validators.validateLocationGrid(physicsData.locationGrid);
console.log('Blood hash valid:', isValidBlood);
console.log('Location grid valid:', isValidGrid);

// 4. Generate physics-based cryptography
console.log('\n4. Physics-based cryptography:');
const physicsSeed = PhysicsCrypto.generatePhysicsSeed(
  physicsData.bloodHash,
  physicsData.motion,
  physicsData.lightLevel,
  physicsData.locationGrid
);
const physicsKey = PhysicsCrypto.derivePhysicsKey(physicsSeed);
const seal = PhysicsCrypto.generateSeal(physicsData.motion, physicsData.lightLevel);
console.log('Physics seed (hex):', Converters.bytesToHex(physicsSeed));
console.log('Physics key (hex):', Converters.bytesToHex(physicsKey));
console.log('Generated seal:', seal);

// 5. Encrypt and decrypt data
console.log('\n5. Encryption/Decryption:');
const message = new TextEncoder().encode('Secret Mari message');
const iv = new Uint8Array(16).fill(42);
const { cipherText, authTag } = PhysicsCrypto.encryptWithPhysicsKey(message, physicsKey, iv);
const decrypted = PhysicsCrypto.decryptWithPhysicsKey(cipherText, physicsKey, iv, authTag);
console.log('Original message:', new TextDecoder().decode(message));
console.log('Encrypted (hex):', Converters.bytesToHex(cipherText));
console.log('Decrypted message:', new TextDecoder().decode(decrypted));

console.log('\n=== Demo completed successfully! ===');
