import { SensorSimulator } from '../src/physics/sensors/sensorSimulator';

describe('SensorSimulator', () => {
  test('should simulate consistent vein scan', () => {
    const scan1 = SensorSimulator.simulateVeinScan(true);
    const scan2 = SensorSimulator.simulateVeinScan(true);

    expect(scan1).toBe(scan2);
    expect(scan1).toMatch(/^[0-9a-f]{4}$/);
  });

  test('should simulate random vein scan', () => {
    const scan1 = SensorSimulator.simulateVeinScan(false);
    const scan2 = SensorSimulator.simulateVeinScan(false);

    expect(scan1).toMatch(/^[0-9a-f]{4}$/);
    expect(scan2).toMatch(/^[0-9a-f]{4}$/);
  });

  test('should simulate motion vector', () => {
    const motion = SensorSimulator.simulateMotion();

    expect(motion).toHaveProperty('x');
    expect(motion).toHaveProperty('y');
    expect(motion).toHaveProperty('z');
    expect(typeof motion.x).toBe('number');
    expect(typeof motion.y).toBe('number');
    expect(typeof motion.z).toBe('number');
    expect(motion.x).toBeGreaterThanOrEqual(-1);
    expect(motion.x).toBeLessThanOrEqual(1);
  });

  test('should simulate light level', () => {
    const lightLevel = SensorSimulator.simulateLightLevel();

    expect(typeof lightLevel).toBe('number');
    expect(lightLevel).toBeGreaterThanOrEqual(0);
    expect(lightLevel).toBeLessThanOrEqual(1000);
  });

  test('should generate location grid', () => {
    const grid = SensorSimulator.generateLocationGrid(37.7749, -122.4194);

    expect(grid).toMatch(/^[0-9A-Z*]{8}$/);
    expect(grid.length).toBe(8);
  });

  test('should capture physics data', () => {
    const data = SensorSimulator.capturePhysicsData();

    expect(data).toHaveProperty('bloodHash');
    expect(data).toHaveProperty('motion');
    expect(data).toHaveProperty('lightLevel');
    expect(data).toHaveProperty('locationGrid');
    expect(data).toHaveProperty('timestamp');
    expect(typeof data.bloodHash).toBe('string');
    expect(typeof data.lightLevel).toBe('number');
    expect(typeof data.timestamp).toBe('number');
    expect(data.motion).toHaveProperty('x');
    expect(data.motion).toHaveProperty('y');
    expect(data.motion).toHaveProperty('z');
  });
});
