import os
import sys
import json
import time
import base64
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType
import redis

"""
Train a tiny baseline model from a JSONL file previously drained from Redis (labeled:examples).
Each line must be a JSON object like: { "ts": 123, "x": [..floats..], "y": 0|1 }
Writes a model artifact to Redis under model:<id> and model:current and publishes model-updates.

Usage:
  python train_from_jsonl.py <path_to_jsonl> [redis_url]
Example:
  python train_from_jsonl.py ./labeled.jsonl redis://localhost:6379
"""


def load_jsonl(path: str):
    X = []
    y = []
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
                X.append(obj['x'])
                y.append(obj['y'])
            except Exception:
                # skip malformed lines
                continue
    if not X:
        raise RuntimeError("No valid examples loaded from JSONL")
    return np.array(X, dtype=np.float32), np.array(y, dtype=np.int64)


def main():
    if len(sys.argv) < 2:
        print("Usage: python train_from_jsonl.py <path_to_jsonl> [redis_url]")
        sys.exit(2)

    path = sys.argv[1]
    redis_url = sys.argv[2] if len(sys.argv) > 2 else os.getenv("REDIS_URL", "redis://redis:6379")

    print(f"Loading data from {path}")
    X, y = load_jsonl(path)
    print(f"Loaded {len(X)} examples with dim {X.shape[1]}")

    clf = RandomForestClassifier(n_estimators=50, random_state=42)
    clf.fit(X, y)

    initial_types = [("float_input", FloatTensorType([None, X.shape[1]]))]
    onnx_model = convert_sklearn(clf, initial_types=initial_types)
    buf = onnx_model.SerializeToString()

    model_id = f"v{int(time.time())}"
    ms = {
        "model_id": model_id,
        "buffer": base64.b64encode(buf).decode("ascii"),
        "created_at": int(time.time() * 1000),
    }

    r = redis.from_url(redis_url, decode_responses=True)
    r.set(f"model:{model_id}", json.dumps(ms))
    r.set("model:current", json.dumps(ms))
    r.publish("model-updates", model_id)
    print("Published new model", model_id)


if __name__ == "__main__":
    main()
