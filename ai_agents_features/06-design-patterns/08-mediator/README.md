# Mediator (Agent Communication)

Peers never call each other directly. Every message goes through a **mediator**, which owns name resolution, routing, delivery, and error handling. Adding a peer means registering with the mediator — not editing the other peers.

## What it is

A central broker that turns N direct peer connections into N registration + N×1 routing. It is the single place where **cross-cutting policy** (auth, authz, rate limits, redaction, schema validation) can be enforced without a peer calling another peer directly and bypassing it.

## When to use

- Many peers that must collaborate (N×N growth) and you want **N integrations instead of N²**.
- One place to enforce policy: auth, rate limits, logging, redaction, schema validation.
- Peers are heterogeneous, swapped at runtime, or discovered dynamically.
- You need to change wiring without redeploying every agent.

## How it works

1. Peers register their handler with the mediator.
2. A peer sends a `Message` (sender, to, type, payload, id, reply_to, correlation_id) through the mediator.
3. The mediator routes point-to-point (`to="peer"`) or broadcasts (`to="*"`).
4. On an unknown recipient it increments `errors` and sends a `delivery.error` back to the sender — it never drops silently.
5. Peers only know the mediator, so adding a peer at runtime works with no existing peer edits.

## Code walkthrough (from `main.py`)

- `Message` — envelope with `sender`, `to` (peer or `"*"` broadcast), `type`, `payload`, `id`, `reply_to`, `correlation_id`.
- `Mediator.send(msg)` — broadcast or `_deliver`.
- `_deliver(name, msg)` — looks up the handler; on miss, sends a `delivery.error` to `reply_to` if registered, and increments `errors`.
- `Peer` — base peer that registers itself with the mediator on construction and never holds references to other peers.

## Run

```bash
python 06-design-patterns/08-mediator/main.py
```

Expected output:

```text
[mediator] registered 'researcher-1' (peers=['researcher-1'])
...
=== 1. point-to-point with correlated reply ===
[mediator] route msg_... researcher-1->researcher-2 (research.request)
[researcher-2] researching 'retry policy'
[mediator] route msg_... researcher-2->researcher-1 (research.result)
[researcher-1] got result corr=job-42: retry policy: retries must be idempotent
=== 2. error path: unknown recipient ===
[mediator] ERROR msg_...: no such peer 'publisher' (from writer)
[writer] delivery failed: unknown recipient 'publisher' (original=msg_...)
=== 3. new peer joins at runtime, no peer edits ===
[mediator] registered 'editor' (peers=[...,'editor'])
[mediator] broadcast draft.published from writer -> ['researcher-1', 'researcher-2', 'editor']
[editor] received draft.published from writer: {'draft': 'v1'}
[stats] delivered=5 errors=1 editor_saw=1
```

## Tradeoffs

- The mediator becomes a **single point of failure** and a potential throughput bottleneck — replicate/scale deliberately.
- Extra hop adds latency.
- Easy to let it grow into a "god object"; keep it to routing + cross-cutting policy, not business logic.
- Not a substitute for clear message contracts (pair with the Handoff Protocol).

## Production rules

- Keep the mediator to **routing + cross-cutting policy**, never business logic.
- **Denials are answered, not dropped** — a `policy.denied` message carries `reason`, `stage`, `original_id`.
- **Redaction is non-destructive** — build a new message with a redacted payload copy; the sender's object still holds real values.
- Every decision is audited; `denied` and `unknown` counters are separate from `delivered`.
- The clock is **injected** so rate-limit behavior is deterministic in tests.

## Anti-patterns

- Putting business logic in the mediator.
- No message schema/versioning — the envelope shape becomes an implicit contract.
- Ignoring delivery errors (silent drops).
- Treating the mediator as a decision engine (policy belongs in code, not in the router).

## Related patterns

- Handoff protocol (the message contract the mediator carries)
- Request–response (correlation via `correlation_id`)
- Policy enforcement demo: `08-mediator/policy_demo.py`
