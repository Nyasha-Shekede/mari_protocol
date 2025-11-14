import * as ort from 'onnxruntime-node';

let session: ort.InferenceSession;
let inputName: string = 'float_input';
let outputName: string | null = null;
let labelName: string | null = null;

export async function loadModel(buffer: Buffer) {
  session = await ort.InferenceSession.create(buffer);
  try {
    const names: string[] = (session as any).inputNames || [];
    if (Array.isArray(names) && names.length > 0) {
      inputName = names[0];
    }
    const outputs: string[] = (session as any).outputNames || [];
    // Capture names; we'll prefer probability tensor, with fallback to label tensor
    labelName = outputs.includes('output_label') ? 'output_label' : null;
    const probCandidates = ['output_probability', 'probabilities', 'probability'];
    outputName = outputs.find((n) => probCandidates.includes(n)) || labelName || outputs[0] || null;
    console.log(`Model IO -> inputs: ${names.join(', ') || 'unknown'} | outputs: ${outputs.join(', ') || 'unknown'} | selected output: ${outputName ?? 'auto'}`);
  } catch {
    // keep default
  }
}

export async function score(vec: Float32Array): Promise<number> {
  // Model expects tensor(float); always feed float32.
  const tensor = new ort.Tensor('float32', vec, [1, 8]);
  const feeds = { [inputName]: tensor } as Record<string, ort.Tensor>;

  // Try probability output first if selected; if the runtime rejects non-tensor, fallback to label tensor
  let ranFor: string | null = outputName;
  let res: Record<string, ort.Tensor>;
  try {
    res = await session.run(feeds, outputName ? [outputName] : undefined as any);
  } catch (e: any) {
    const msg = e && e.message ? e.message : String(e);
    if (msg.includes('Non tensor type is temporarily not supported') && labelName) {
      // fallback: run requesting label tensor only
      ranFor = labelName;
      res = await session.run(feeds, [labelName]);
    } else {
      throw new Error(`onnx_run_failed: input=${inputName} dtype=float32 len=${vec.length} output=${outputName ?? 'auto'} err=${msg}`);
    }
  }
  // Try common probability output names from skl2onnx
  const anyRes = res as any;
  let out = (ranFor && anyRes[ranFor]) ?? anyRes.output_probability ?? anyRes.probabilities ?? anyRes.probability ?? (labelName ? anyRes[labelName] : undefined);

  // If not found, pick the first tensor-like output
  if (!out) {
    for (const v of Object.values(anyRes)) {
      if (v && typeof v === 'object' && 'data' in v && (Array.isArray((v as any).data) || (v as any).data instanceof Float32Array)) {
        out = v;
        break;
      }
    }
  }

  // If still not found, some converters output a map/object: pick the first value
  if (!out) {
    for (const v of Object.values(anyRes)) {
      if (v instanceof Map) {
        const first = v.values().next().value;
        if (first && typeof first === 'object' && 'data' in first) { out = first; break; }
        if (Array.isArray(first) || first instanceof Float32Array) { return (first as number[])[0] as number; }
      } else if (v && typeof v === 'object' && !('data' in v)) {
        const first = Object.values(v as Record<string, unknown>)[0] as any;
        if (first && typeof first === 'object' && 'data' in first) { out = first; break; }
        if (Array.isArray(first) || first instanceof Float32Array) { return (first as number[])[0] as number; }
      }
    }
  }

  if (!out) {
    const keys = Object.keys(anyRes);
    throw new Error(`No tensor-like outputs found from ONNX session. Output keys: ${keys.join(', ')}`);
  }

  // If we ended up with label, convert class id (0/1) to 0.0/1.0
  if (ranFor === 'output_label' || (labelName && ranFor === labelName)) {
    const dataAny = (out as any).data as any;
    const val = Array.isArray(dataAny) ? dataAny[0] : dataAny[0];
    const cls = typeof val === 'bigint' ? Number(val) : Number(val);
    const prob = cls >= 1 ? 1.0 : 0.0;
    return prob;
  }

  const data = (out as any).data as Float32Array | number[];
  const prob = Array.isArray(data) ? (data[0] as number) : (data[0] as number);
  return prob;
}
