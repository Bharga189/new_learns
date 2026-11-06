# Event-Driven Pub/Sub (Agent Communication)

Agents **publish events** to topics and **subscribe** to topics they care about. Producers and consumers are decoupled: they don't call each other directly.

## What it is

A mediator-free, topic-based broadcast plane. A producer publishes an event to a topic; every registered subscriber for that topic receives a copy. Producers and consumers know nothing about each other — only about topics and event schemas. This is the agent-message analogue of a message bus.

## When to use

- Reactive workflows (alerts, status changes, pipelines).
- Multiple consumers for the same event.
- Loose coupling between components/agents.

## How it works

1. Define topics and an event schema (topic, type, payload, id, timestamp).
2. Subscribers register handlers for the topics they care about.
3. Producers publish events to topics.
4. The bus delivers the event to every registered handler for that topic.
5. Subscribers process and (optionally) publish new events, forming a pipeline.

## Code walkthrough (from `main.py`)

- `Event` dataclass: `topic`, `type`, `payload`, `id`, `ts` (UTC ISO-8601).
- `EventBus.subscribe(topic, handler)` registers a handler; on publish, it iterates `subs[topic]` and calls each handler.
- `make_triage_agent` subscribes to `alerts`; on an alert it dedupes by `id` and publishes an `action.proposed` event to `actions`.
- `make_audit_agent` subscribes to `actions` and records every action — regardless of its source. It never talks to the triage agent directly.

## Run

```bash
python 06-design-patterns/03-event-driven-pubsub/main.py
```

Expected output:

```text
[bus] subscribed handler to 'alerts'
[bus] subscribed handler to 'actions'
[bus] publish alerts/alert.raised id=e1
[bus] publish actions/action.proposed id=act-e1
[audit] recorded action.proposed: {'action': 'page_oncall', 'source': 'e1'}
[bus] publish alerts/alert.raised id=e1   # duplicate
```

Note that `e1` appears **twice** in the log but the audit handler only records one action — the triage agent dedupes by event id.

## Tradeoffs

- Harder debugging (async flow), ordering/delivery semantics matter.
- Needs idempotent handlers and clear event schemas.

## Production rules

- **Idempotent handlers**: use an event `id` (and dedup/seq number) so re-delivery doesn't double-act.
- Durable event log + at-least-once/at-most-once guarantees depending on the workflow.
- Define ordering guarantees per topic; cross-topic pipeline ordering needs explicit sequencing.
- Schema registry for events; contract tests for `publish` shape.

## Anti-patterns

- Publishing and consuming across unrelated topics (leaks coupling).
- Handlers that assume in-order delivery without an ordering scheme.
- No idempotent handling → duplicated side effects.

## Related patterns

- Handoff protocol / message envelope (structured event payloads with correlation)
- Mediator (policy enforcement point; pub/sub has none)
- Shared Blackboard (when agents need to read the latest state, not just events)
