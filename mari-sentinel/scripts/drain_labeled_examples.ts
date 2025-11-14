import { createClient } from 'redis';
import { writeFileSync, createWriteStream } from 'fs';

async function main() {
  const redisUrl = process.env.REDIS_URL || 'redis://localhost:6379';
  const outPath = process.argv[2] || `labeled_examples_${Date.now()}.jsonl`;
  const max = parseInt(process.argv[3] || '10000', 10);
  const listKey = process.argv[4] || 'labeled:examples';

  const client = createClient({ url: redisUrl });
  await client.connect();
  console.log(`Connected to ${redisUrl}. Draining up to ${max} records from '${listKey}' to ${outPath}`);

  const stream = createWriteStream(outPath, { flags: 'w' });
  let count = 0;

  while (count < max) {
    // Pop from head (oldest) using LPOP
    const item = await client.lPop(listKey);
    if (!item) break;
    stream.write(item + '\n');
    count++;
    if (count % 1000 === 0) console.log(`Drained ${count}...`);
  }

  stream.end();
  await client.quit();
  console.log(`Done. Drained ${count} records.`);
}

main().catch((err) => {
  console.error('Error draining labeled examples:', err);
  process.exit(1);
});
