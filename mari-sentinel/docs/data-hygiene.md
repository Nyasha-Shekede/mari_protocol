# Data Hygiene

Guidance for inspecting, managing, and cleaning training data in Mari Sentinel.

## Where data lives
- Redis key `labeled:examples`: JSONL records used for offline training.
- Redis keys `model:<id>`, `model:current`: serialized ONNX artifacts (base64) with metadata.

## Inspect labeled examples
- Count examples
  ```bash
  docker run --rm --network mari-prod redis:7-alpine redis-cli -h redis LLEN labeled:examples
  ```
- View first example
  ```bash
  docker run --rm --network mari-prod redis:7-alpine redis-cli -h redis LINDEX labeled:examples 0
  ```

## Drain to JSONL
Use the helper script to export examples to a `.jsonl` file for audit or offline analysis.
```bash
node scripts/drain_labeled_examples.ts ./labeled.jsonl 10000 labeled:examples
```
Parameters:
- `./labeled.jsonl`: output file path
- `10000`: max number of records to drain
- `labeled:examples`: Redis list key (defaults to this value)

## Clear synthetic examples
Dangerous: deletes the list entirely. Useful when you want to reset after synthetic seeding.
```bash
docker run --rm --network mari-prod redis:7-alpine redis-cli -h redis DEL labeled:examples
```

## Best practices
- Keep synthetic and real datasets separate by using different keys (e.g., `labeled:examples:synthetic`).
- Implement retention by trimming the list after offline drains.
- Version models: archive `model:<id>` objects periodically into durable storage (S3/GCS).
- Monitor `LLEN labeled:examples` to ensure expected throughput and avoid unbounded growth.
