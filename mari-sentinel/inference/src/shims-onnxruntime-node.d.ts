declare module 'onnxruntime-node' {
  export type TensorType = 'float32' | 'int32' | 'int64' | 'float64';

  export class Tensor<T = any> {
    constructor(type: TensorType | string, data: T, dims?: number[]);
    readonly type: string;
    readonly data: T;
    readonly dims: number[];
  }

  export interface InferenceSessionOptions {
    executionProviders?: string[];
  }

  export class InferenceSession {
    static create(model: Buffer | Uint8Array, options?: InferenceSessionOptions): Promise<InferenceSession>;
    run(feeds: Record<string, Tensor>, fetches?: string[] | null): Promise<Record<string, Tensor>>;
  }
}
