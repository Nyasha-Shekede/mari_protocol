import os
import json
import time
import base64
import redis
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

REDIS_URL = os.getenv("REDIS_URL", "redis://redis:6379")
LIST_KEY = os.getenv("LABELED_LIST_KEY", "labeled:examples")

# Deterministic featurization identical to TS code
# Input example expects fields: { ts, x: [..8 floats..], y }
# We trust x is already the exact feature vector from Dataset; otherwise,
# implement the hashCode-based featurizer here to recompute.

def load_examples(r: redis.Redis):
    raw = r.lrange(LIST_KEY, 0, -1)
    examples = []
    for item in raw:
        try:
            examples.append(json.loads(item))
        except Exception:
            continue
    return examples


def main():
    r = redis.from_url(REDIS_URL, decode_responses=True)
    examples = load_examples(r)
    if not examples:
        print("No labeled examples found; exiting")
        return

    # Prepare dataset
    X = np.array([e["x"] for e in examples], dtype=np.float32)
    y = np.array([e["y"] for e in examples], dtype=np.int64)

    # Train model (example: RandomForest)
    clf = RandomForestClassifier(n_estimators=100, random_state=42)
    clf.fit(X, y)

    # Convert to ONNX
    initial_types = [("float_input", FloatTensorType([None, X.shape[1]]))]
    # Disable ZipMap so the output is a plain tensor, not a map.
    # Scope the option to the classifier instance for reliability across skl2onnx versions.
    onnx_model = convert_sklearn(
        clf,
        initial_types=initial_types,
        options={id(clf): {"zipmap": False, "output_class_labels": False}},
        target_opset=12,
    )
    buf = onnx_model.SerializeToString()

    model_id = f"v{int(time.time())}"
    ms = {
        "model_id": model_id,
        "buffer": base64.b64encode(buf).decode("ascii"),
        "created_at": int(time.time() * 1000),
    }

    # Publish to Redis
    r.set(f"model:{model_id}", json.dumps(ms))
    r.set("model:current", json.dumps(ms))
    r.publish("model-updates", model_id)
    print("Published new model", model_id)


if __name__ == "__main__":
    main()
