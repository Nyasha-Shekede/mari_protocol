import { sha256 } from 'js-sha256';

export interface MotionVector {
  x: number;
  y: number;
  z: number;
}

export interface PhysicsData {
  bloodHash: string;
  motion: MotionVector;
  lightLevel: number;
  locationGrid: string;
  timestamp: number;
}

export class SensorSimulator {
  static simulateVeinScan(consistent: boolean = true): string {
    if (consistent) {
      // Generate a consistent hash based on a stable identifier
      const stableId = 'user-device-id'; // Would be a real device ID in production
      const hash = sha256.create();
      hash.update(stableId);
      return hash.hex().slice(0, 4); // 2-byte blood hash as hex string
    } else {
      // Generate a random hash for demo purposes
      return Math.floor(Math.random() * 0xFFFF).toString(16).padStart(4, '0');
    }
  }
  
  static simulateMotion(): MotionVector {
    // Simulate natural hand tremors with some randomness
    return {
      x: this.generateTremor(),
      y: this.generateTremor(),
      z: this.generateTremor()
    };
  }
  
  static simulateLightLevel(): number {
    // Simulate ambient light level between 0-1000 lux
    return Math.floor(Math.random() * 1000);
  }
  
  static generateLocationGrid(latitude: number, longitude: number): string {
    // Convert coordinates to an 8-character grid code
    const chars = '0123456789ABCDEFGHJKMNPQRSTVWXYZ*';
    let grid = '';
    
    // Simple grid encoding for demo
    const lat = Math.abs(Math.floor(latitude * 1000));
    const lon = Math.abs(Math.floor(longitude * 1000));
    
    for (let i = 0; i < 4; i++) {
      grid += chars[(lat >> (i * 5)) & 0x1F];
      grid += chars[(lon >> (i * 5)) & 0x1F];
    }
    
    return grid;
  }
  
  static capturePhysicsData(): PhysicsData {
    return {
      bloodHash: this.simulateVeinScan(),
      motion: this.simulateMotion(),
      lightLevel: this.simulateLightLevel(),
      locationGrid: this.generateLocationGrid(
        37.7749 + (Math.random() - 0.5) * 0.01, // SF with some variance
        -122.4194 + (Math.random() - 0.5) * 0.01
      ),
      timestamp: Date.now()
    };
  }
  
  private static generateTremor(): number {
    // Generate a realistic hand tremor value (-1 to 1 with bias toward 0)
    return (Math.random() - 0.5) * 2 * Math.random();
  }
}
