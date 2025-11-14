export class Validators {
  static validateBloodHash(hash: string): boolean {
    return /^[0-9a-f]{4}$/i.test(hash);
  }
  
  static validateLocationGrid(grid: string): boolean {
    return /^[0-9A-Z*]{8}$/.test(grid);
  }
  
  static validateAmount(amount: number): boolean {
    return amount >= 0 && amount <= 1000000; // $0 to $1,000,000 limit
  }
  
  static validateBioHash(hash: string): boolean {
    return this.validateBloodHash(hash);
  }
  
  static validateMariString(str: string): boolean {
    return str.startsWith('mari://');
  }
}
