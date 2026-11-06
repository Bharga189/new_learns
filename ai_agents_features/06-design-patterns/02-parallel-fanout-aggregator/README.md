# Parallel Fan-out / Aggregator (Map-Reduce)

A coordinator **fans out** independent subtasks to workers (or tool calls), runs them in parallel where safe, then **aggregates** results (merge, vote, rank, or synthesize) with conflict handling.

## What it is

The pattern splits a problem into **independent** sub-problems, dispatches them concurrently, and then combines the results into a single answer. The aggregation step is where the pattern earns its keep: it must merge evidence, rank candidates, vote, or synthesize while explicitly surfacing failures.

## When to use

- Subtasks are independent (multiple sources, checks, perspectives).
- Latency matters and parallelism is safe.
- You want ensembling or redundancy.

## How it works

1. The coordinator splits the request into independent sources/sub-queries.
2. Each subtask is submitted to a worker pool (threads here; processes or a real async runtime in production).
3. Results arrive as they complete (`as_completed`), so the coordinator does not block on the slowest one.
4. The `aggregate()` function merges the OK evidence and keeps `sources_failed` explicit rather than hiding it.

## Code walkthrough (from `main.py`)

- `check_source(name, query)` simulates an independent lookup (docs / runbook / alerts), returning `WorkerResult(ok, data)`.
- `fanout(query, sources, max_workers)` submits all sources to a `ThreadPoolExecutor` and collects results as they complete.
- `aggregate(results)` returns `summary` (joined OK evidence), `sources_ok`, and `sources_failed` — the aggregation policy is visible in one place.

## Run

```bash
python 06-design-patterns/02-parallel-fanout-aggregator/main.py
```

Expected output:

```text
[worker:docs] ok=True
[worker:runbook] ok=True
[worker:alerts] ok=True

[aggregated] {'summary': "docs say: retry idempotent calls for 'API failures' | runbook says: use backoff and tracing for 'API failures' | alerts: no active incident related to 'API failures'", 'sources_ok': ['docs', 'runbook', 'alerts'], 'sources_failed': []}
```

## Tradeoffs

- Aggregation can be hard (conflicts, different formats).
- More cost; must cap fan-out and handle partial failures.

## Production rules

- Only parallelize **truly independent** work (dependency → serial).
- **Cap fan-out**: max_workers, per-task budget, and a total-timeout.
- Treat **partial failure as a first-class signal** — merge and report failed sources, do not drop them silently.
- Use idempotency on the sub-tasks themselves if a task can be re-run safely.

## Anti-patterns

- Fanning out dependent work (serializes the pipeline incorrectly).
- Silently dropping failed sources (the summary loses evidence).
- Unbounded fan-out (a large fan-out silently queues).

## Related patterns

- Evaluator–Optimizer (merge via scoring/voting)
- Debate & Consensus (competing candidates aggregated by an arbiter)
- Shared Blackboard (watch for events instead of polling)
