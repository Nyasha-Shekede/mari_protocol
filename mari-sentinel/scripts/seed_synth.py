import os
import json
import time
import random
import redis

REDIS_URL = os.getenv("REDIS_URL", "redis://redis:6379")
COUNT = int(os.getenv("SEED_SYNTH_COUNT", "200"))
LIST_KEY = os.getenv("LABELED_LIST_KEY", "labeled:examples")

r = redis.from_url(REDIS_URL, decode_responses=True)

for _ in range(COUNT):
    rec = {
        "ts": int(time.time() * 1000),
        "x": [random.random() for _ in range(8)],
        "y": random.randint(0, 1),
    }
    r.rpush(LIST_KEY, json.dumps(rec))

print("Seeded examples:", r.llen(LIST_KEY))
