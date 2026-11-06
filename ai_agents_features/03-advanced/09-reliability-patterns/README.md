# 09 — Reliability Patterns

Production agents fail in ordinary ways: timeouts, flaky tools, bad outputs, partial progress. Reliability comes from **bounded, observable control flow** around model calls.

## What you'll learn

- **Timeouts** and **retries with backoff** (for transient failures).
- **Fallbacks** when the primary path fails.
- **Idempotency** so retries don't duplicate side effects.
- **Eval hooks**: capture inputs/outputs so behavior can be tested.

## Pattern

```text
with timeout:
  for attempt in attempts:
    try: result = call()
         if valid(result): return result
    except transient: backoff and retry
fallback() or safe failure
record trace/eval event
```

## Run

```bash
python 03-advanced/09-reliability-patterns/main.py
```

## Key ideas / best practices

- Retry only transient, safe operations; never blindly retry non-idempotent actions.
- Use jittered backoff to avoid thundering herds.
- Fallbacks should be safe and clearly labeled (cached, simpler model, partial answer).
- Emit traces/metrics (latency, attempts, outcome) for every run.
- Write eval cases for failure modes, not just the happy path.
