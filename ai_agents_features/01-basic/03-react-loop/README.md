# 03 — ReAct Loop (Reason + Act)

**ReAct** interleaves reasoning with actions: the agent thinks, takes a tool action, observes the result, and repeats until it can answer.

## What you'll learn

- The **Thought → Action → Observation** cycle.
- Why observations must come from real tool results (not model guesses).
- **Stopping conditions**: final answer, max steps, or unrecoverable error.

## Pattern

```text
Thought: I need the order status.
Action: lookup_order(order_id="A100")
Observation: {"status": "shipped", "eta": "2026-11-08"}
Thought: I have enough information.
Answer: Order A100 shipped; ETA 2026-11-08.
```

## Run

```bash
python 01-basic/03-react-loop/main.py
```

The mock model emits scripted ReAct steps. A real implementation can use provider tool calling (structured) or careful text parsing (less robust).

## Key ideas / best practices

- Prefer structured tool calling over parsing free-form `Action:` text in production.
- Cap iterations; log each step for traceability.
- If an observation is an error, let the agent retry with corrected args or stop safely.
- Separate reasoning shown to users from internal traces as appropriate.
