# Mari Shared Libraries

Shared libraries and utilities for the Mari protocol ecosystem.

## Installation

```bash
npm install @mari/shared-libs
```

## Usage

```typescript
import { 
  MariStringParser, 
  SensorSimulator, 
  PhysicsCrypto,
  Validators 
} from '@mari/shared-libs';

// Parse a transfer coupon
const coupon = 'mari://xfer?from=9a3f&to=384a&val=20.00&g=9X4F*Z5L&exp=173840000000000&s=7D4A';
const parsed = MariStringParser.parseTransferCoupon(coupon);

// Generate physics data
const physicsData = SensorSimulator.capturePhysicsData();

// Validate data
const isValid = Validators.validateBloodHash(physicsData.bloodHash);
```

## API Reference

### MariStringParser

- `parseFunctionId(functionId: string): MariFunctionId`
- `parseTransferCoupon(coupon: string): MariTransferCoupon`
- `generateFunctionId(data: Omit<MariFunctionId, 'version'>): string`
- `generateTransferCoupon(data: MariTransferCoupon): string`
- `validateMariString(str: string): boolean`

### PhysicsCrypto

- `generatePhysicsSeed(bloodHash: string, motionVector: MotionVector, lightLevel: number, locationGrid: string): Uint8Array`
- `derivePhysicsKey(physicsSeed: Uint8Array, context?: string): Uint8Array`
- `generateSeal(motionVector: MotionVector, lightLevel: number, timestamp?: number): string`
- `encryptWithPhysicsKey(data: Uint8Array, physicsKey: Uint8Array, iv: Uint8Array): { cipherText: Uint8Array; authTag: Uint8Array }`
- `decryptWithPhysicsKey(cipherText: Uint8Array, physicsKey: Uint8Array, iv: Uint8Array, authTag: Uint8Array): Uint8Array`

### SensorSimulator

- `simulateVeinScan(consistent?: boolean): string`
- `simulateMotion(): MotionVector`
- `simulateLightLevel(): number`
- `generateLocationGrid(latitude: number, longitude: number): string`
- `capturePhysicsData(): PhysicsData`

### Validators

- `validateBloodHash(hash: string): boolean`
- `validateLocationGrid(grid: string): boolean`
- `validateAmount(amount: number): boolean`
- `validateBioHash(hash: string): boolean`
- `validateMariString(str: string): boolean`

### Converters

- `hexToBytes(hex: string): Uint8Array`
- `bytesToHex(bytes: Uint8Array): string`
- `base64ToBytes(base64: string): Uint8Array`
- `bytesToBase64(bytes: Uint8Array): string`
- `stringToBytes(str: string): Uint8Array`
- `bytesToString(bytes: Uint8Array): string`
