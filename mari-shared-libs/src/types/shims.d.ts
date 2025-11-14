declare module 'js-sha256' {
  export const sha256: {
    (message: string | ArrayBuffer | Uint8Array): string;
    create(): {
      update(data: string | ArrayBuffer | Uint8Array): void;
      hex(): string;
      array(): number[];
    };
  };
}

declare module 'base64-js' {
  export function toByteArray(b64: string): Uint8Array;
  export function fromByteArray(bytes: Uint8Array): string;
}
