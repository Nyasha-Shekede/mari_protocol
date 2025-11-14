export interface MariFunctionId {
  version: number;
  grid: string;
  bloodHash: string;
  readyCash: number;
  totalMoney: number;
  seal: string;
}

export interface MariTransferCoupon {
  senderBio: string;
  receiverBio: string;
  amount: number;
  grid: string;
  expiry: number;
  seal: string;
}

export class MariStringParser {
  static parseFunctionId(functionId: string): MariFunctionId {
    const regex = /mari:\/\/v(\d+)\?g=([^&]+)&b=([^&]+)&rc=([^&]+)&tm=([^&]+)&seal=([^&]+)/;
    const match = functionId.match(regex);
    
    if (!match) {
      throw new Error('Invalid Function ID format');
    }
    
    return {
      version: parseInt(match[1], 10),
      grid: match[2],
      bloodHash: match[3],
      readyCash: parseFloat(match[4]),
      totalMoney: parseFloat(match[5]),
      seal: match[6]
    };
  }
  
  static parseTransferCoupon(coupon: string): MariTransferCoupon {
    // Tolerate extra parameters (e.g., movementIntensity, lightLevel) and case-insensitive scheme
    const regex = /mari:\/\/xfer\?from=([^&]+)&to=([^&]+)&val=([^&]+)&g=([^&]+)&exp=([^&]+)&s=([^&]+)/i;
    const match = coupon.match(regex);
    
    if (!match) {
      throw new Error('Invalid Transfer Coupon format');
    }
    
    return {
      senderBio: match[1],
      receiverBio: match[2],
      amount: parseFloat(match[3]),
      grid: match[4],
      expiry: parseInt(match[5], 10),
      seal: match[6]
    };
  }
  
  static generateFunctionId(data: Omit<MariFunctionId, 'version'>): string {
    const rc = Number(data.readyCash).toFixed(2);
    const tm = Number(data.totalMoney).toFixed(2);
    return `mari://v1?g=${data.grid}&b=${data.bloodHash}&rc=${rc}&tm=${tm}&seal=${data.seal}`;
  }
  
  static generateTransferCoupon(data: MariTransferCoupon): string {
    return `mari://xfer?from=${data.senderBio}&to=${data.receiverBio}&val=${data.amount}&g=${data.grid}&exp=${data.expiry}&s=${data.seal}`;
  }
  
  static validateMariString(str: string): boolean {
    return str.startsWith('mari://');
  }
}
