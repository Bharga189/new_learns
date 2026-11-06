# Handoff Protocol (Agent Communication)

A **handoff protocol** standardizes how agents transfer work: a message envelope with IDs, sender/receiver, correlation, payload schema, status, and errors. This makes handoffs traceable and reliable.

## What it is

A typed, envelope-shaped message contract for transferring work between agents. It solves three problems at once: (1) **identity** (which message is this?), (2) **linkage** (which request does this response belong to?), and (3) **status** (pending/ack/done/error). Agents never hand raw unstructured data across a boundary.

## When to use

- Any multi-agent pipeline, delegation, or async workflow.
- You need tracing, retries, or auditing across agent boundaries.

## How it works

1. Sender builds an `Envelope` with `id`, `type`, `sender`, `receiver`, `correlation_id`, `payload`, `status`, `ts`, `error`.
2. Sender `.send()`-s it to the receiver.
3. Receiver validates the required fields (e.g., `goal` present for a `handoff.request`).
4. Receiver replies with `handoff.ack` (accepted) or `handoff.result` (`done`/`error` with the payload).
5. The `correlation_id` links request ↔ ack ↔ result so traces stay aligned.

## Code walkthrough (from `main.py`)

- `Envelope` — dataclass carrying every field; `status` is an explicit state machine: `pending` → `ack` → `done`/`error`.
- `Agent.send(receiver, type_, payload, correlation_id)` — builds the envelope and forwards to `receiver.receive(env)`.
- `Planner.receive(env)` — validates `"goal" in payload`; returns a `handoff.ack` on missing goal (`status=error`), else an ack + a `handoff.result` carrying `{"plan": [...]}` with the same `correlation_id`.
- `Coordinator.run(planner, goal)` — the client; sends `handoff.request`, prints the final status.

## Run

```bash
python 06-design-patterns/05-handoff-protocol/main.py
```

Expected output:

```text
[send] coordinator->planner handoff.request id=msg_... corr=None
[recv:planner] ack id=msg_... corr=msg_...
[recv:planner] done payload={'plan': ['analyze improve onboarding', 'execute safely']}
[coordinator] final status=done result={'plan': ['analyze improve onboarding', 'execute safely']}
```

## Tradeoffs

- Schema/versioning overhead.
- Still requires semantic agreement on payload fields.

## Production rules

- Define the envelope schema + version in one place; contract-test both senders and receivers.
- Use a **correlation id** for every request/response pair so tracing and retries align.
- Keep the **status machine explicit** (pending/ack/done/error) and never let an envelope live in `pending` forever — add timeouts.
- Make the handoff **idempotent** so a retried send doesn't double-execute.

## Anti-patterns

- Agents mutating a shared envelope across the boundary (origin corrupts the request).
- No `correlation_id` → retries and tracing become ambiguous.
- Silent drops (receiver never tells the sender the message was lost).

## Related patterns

- Mediator (the envelope's transport; adds authz/rate-limit/redaction)
- Request–response (the semantic use of correlation)
- Shared Blackboard (when agents observe shared state instead of handoffs)
