import { Validators } from '../src/utils/validation/validators';

describe('Validators', () => {
  test('should validate blood hash correctly', () => {
    expect(Validators.validateBloodHash('9a3f')).toBe(true);
    expect(Validators.validateBloodHash('9A3F')).toBe(true);
    expect(Validators.validateBloodHash('1234')).toBe(true);
    expect(Validators.validateBloodHash('123')).toBe(false);
    expect(Validators.validateBloodHash('12345')).toBe(false);
    expect(Validators.validateBloodHash('xyz')).toBe(false);
  });

  test('should validate location grid correctly', () => {
    expect(Validators.validateLocationGrid('9X4F*Z5L')).toBe(true);
    expect(Validators.validateLocationGrid('12345678')).toBe(true);
    expect(Validators.validateLocationGrid('ABCDEFGH')).toBe(true);
    expect(Validators.validateLocationGrid('1234567')).toBe(false);
    expect(Validators.validateLocationGrid('123456789')).toBe(false);
    expect(Validators.validateLocationGrid('abcdefghi')).toBe(false);
  });

  test('should validate amount correctly', () => {
    expect(Validators.validateAmount(0)).toBe(true);
    expect(Validators.validateAmount(100)).toBe(true);
    expect(Validators.validateAmount(1000000)).toBe(true);
    expect(Validators.validateAmount(-1)).toBe(false);
    expect(Validators.validateAmount(1000001)).toBe(false);
  });

  test('should validate mari string correctly', () => {
    expect(Validators.validateMariString('mari://test')).toBe(true);
    expect(Validators.validateMariString('mari://')).toBe(true);
    expect(Validators.validateMariString('http://test')).toBe(false);
    expect(Validators.validateMariString('')).toBe(false);
  });
});
