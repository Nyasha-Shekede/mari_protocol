export enum MariPacketType {
  HANDSHAKE = 0x01,
  TRANSFER_COUPON = 0x02,
  SETTLEMENT_REQUEST = 0x03,
  INCREMENT_KEY = 0x04
}

export interface MariPacketHeader {
  version: number;
  type: MariPacketType;
  timestamp: bigint;
  senderBioHash: string;
  receiverBioHash: string;
  length: number;
}

export interface MariPacket {
  header: MariPacketHeader;
  payload: Uint8Array;
  seal: string;
  authTag: string;
}

export class MariPacketBuilder {
  static createPacket(
    type: MariPacketType,
    senderBioHash: string,
    receiverBioHash: string,
    payload: Uint8Array,
    seal: string
  ): MariPacket {
    const header: MariPacketHeader = {
      version: 1,
      type,
      timestamp: BigInt(Date.now()) * BigInt(1000000), // Convert to nanoseconds
      senderBioHash,
      receiverBioHash,
      length: payload.length
    };
    
    return {
      header,
      payload,
      seal,
      authTag: '' // Will be set during encryption
    };
  }
  
  static packetToBuffer(packet: MariPacket): ArrayBuffer {
    // Convert packet to binary format for transmission
    const headerSize = 64; // Fixed header size in bytes
    const buffer = new ArrayBuffer(headerSize + packet.payload.length + 32); // +32 for seal and authTag
    
    const view = new DataView(buffer);
    let offset = 0;
    
    // Write header
    view.setUint8(offset++, packet.header.version);
    view.setUint8(offset++, packet.header.type);
    view.setBigUint64(offset, packet.header.timestamp);
    offset += 8;
    
    // Write bio hashes (4 bytes each)
    this.writeBioHash(view, offset, packet.header.senderBioHash);
    offset += 4;
    this.writeBioHash(view, offset, packet.header.receiverBioHash);
    offset += 4;
    
    view.setUint32(offset, packet.header.length, false);
    offset += 4;
    
    // Write payload
    const payloadArray = new Uint8Array(buffer, offset, packet.payload.length);
    payloadArray.set(packet.payload);
    offset += packet.payload.length;
    
    // Write seal and auth tag
    this.writeString(view, offset, packet.seal, 16);
    offset += 16;
    this.writeString(view, offset, packet.authTag, 16);
    
    return buffer;
  }
  
  private static writeBioHash(view: DataView, offset: number, bioHash: string): void {
    const hashValue = parseInt(bioHash, 16);
    view.setUint32(offset, hashValue, false);
  }
  
  private static writeString(view: DataView, offset: number, str: string, maxLength: number): void {
    for (let i = 0; i < Math.min(str.length, maxLength); i++) {
      view.setUint8(offset + i, str.charCodeAt(i));
    }
  }
}
