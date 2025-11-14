import os
import json
import base64
import time
import redis

REDIS_URL = os.getenv("REDIS_URL", "redis://redis:6379")
ONNX_PATH = os.getenv("ONNX_PATH", "./initial_model.onnx")
MODEL_ID = os.getenv("MODEL_ID", "v0")

r = redis.from_url(REDIS_URL)
with open(ONNX_PATH, "rb") as f:
    b64 = base64.b64encode(f.read()).decode("ascii")

payload = {
    "model_id": MODEL_ID,
    "buffer": b64,
    "created_at": int(time.time() * 1000),
}
js = json.dumps(payload)
r.set(f"model:{MODEL_ID}", js)
r.set("model:current", js)
r.publish("model-updates", MODEL_ID)
print("Seeding complete:", MODEL_ID)
