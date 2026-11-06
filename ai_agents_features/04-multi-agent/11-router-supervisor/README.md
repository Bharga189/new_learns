# 11 — Router / Supervisor

A **router** (or supervisor) classifies the request and delegates to the right specialist. It's a star topology: one coordinator, multiple workers.

## What you'll learn

- **Intent routing**: choose a specialist based on the task.
- **Delegation contracts**: what the supervisor sends and expects back.
- **Aggregation**: combine or normalize worker results.
- **Fallback routing** when intent is unclear.

## Pattern

```text
request -> [Supervisor routes] -> billing agent  -> result
                               -> technical agent -> result
                               -> fallback agent  -> result
         -> [Supervisor aggregates/formats] -> response
```

## Run

```bash
python 04-multi-agent/11-router-supervisor/main.py
```

## Key ideas / best practices

- Use deterministic rules for obvious routing; use a model only where judgment helps.
- Define a clear "unknown/needs clarification" route.
- Keep worker interfaces uniform so the supervisor can aggregate reliably.
- Cap fan-out/turns; supervisor loops can become expensive.
- Log routing decisions for evals (was the right agent chosen?).
