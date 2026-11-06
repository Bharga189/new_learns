# Kafka Best Practices & Enhancements

Practical guidance reflected in the `kafka_advance` demos. Tune for your workload, SLOs, and cluster topology.

## 1. Producer best practices (A06, A01)

| Goal | Practice | Notes |
|---|---|---|
| Durability | `acks=all` + broker/topic `min.insync.replicas` | Stronger with RF≥3 and ISR health |
| No duplicates on retry | `enable.idempotence=true` | Per partition/producer session; preserves order with retries (within limits) |
| Bounded latency/time | `delivery.timeout.ms`, `request.timeout.ms`, `linger.ms` | `delivery.timeout.ms` bounds total send+retries |
| Throughput | `linger.ms`, `batch.size`, `compression.type` (lz4/zstd) | Balance latency vs efficiency |
| Ordering | Use stable keys; understand partitioning | Hot keys can skew partitions |
| Reliability | Handle callbacks/futures; retry retriable errors appropriately | Fatal/config/auth errors need intervention |
| Lifecycle | `flush()` / `close()` on shutdown | Prevents buffered record loss |

## 2. Consumer best practices (A07)

| Goal | Practice | Notes |
|---|---|---|
| At-least-once control | `enable.auto.commit=false`; commit **after** processing | Prevents loss-on-crash-before-process; duplicates possible |
| Rebalance safety | Commit pending offsets in `onPartitionsRevoked`; cooperative-sticky assignor | Reduces reprocessing and churn |
| Poll health | Keep polling; tune `max.poll.records` / `max.poll.interval.ms` | Long processing can trigger rebalance |
| Predictable resets | Set `auto.offset.reset` explicitly | `earliest`/`latest`/`none` per use case |
| Shutdown | `wakeup()` + catch `WakeupException` + `close()` | Graceful exit from other thread |
| Isolation (tx outputs) | `isolation.level=read_committed` when consuming transactional topics | Hides aborted/uncommitted data |

## 3. Delivery semantics & idempotency (A07, A09, A10)

- **At-most-once:** commit before processing (risk loss) — rare.
- **At-least-once:** commit after processing (risk duplicates) — common; requires idempotent handlers or dedup.
- **Exactly-once (Kafka scope):** transactions atomically commit Kafka outputs + consumed offsets; `read_committed` consumers read committed results. **External side effects** (DB/HTTP/email) still need idempotency, outbox, or coordination.
- **Idempotent consumer pattern:** stable `eventId`, durable dedup store (DB unique constraint, Redis+TTL, state store), or natural idempotency (upserts/conditional writes). Demo A09 uses in-memory dedup (not production-durable).

## 4. Error handling & resilience enhancements (A05, A08)

- **Poison pills:** use safe/error-handling deserialization; route failures to **DLQ** with context headers; do not stall the poll loop.
- **Retry topics:** classify **retriable** vs **permanent** errors; apply backoff/delay; cap attempts; then DLQ.
- **Ordering:** retries/DLQ can affect ordering — design per key/partition requirements.
- **Observability:** alert on DLQ volume, retry rates, consumer lag, commit failures, send errors.

## 5. SerDe & schema governance (A02–A05)

- Make contracts explicit (format, encoding, version). Prefer additive evolution with defaults.
- One type per topic simplifies operations; envelopes/headers support polymorphic needs.
- For multi-team scale: Schema Registry + compatibility checks (Avro/Protobuf/JSON Schema).
- Never crash-loop on bad data; handle and DLQ with forensics.

## 6. Operations & security (awareness)

- **Topics:** explicit partitions/RF/`min.insync.replicas`, retention/compaction, quotas — manage via IaC.
- **Monitoring:** broker health, ISR/URP, consumer lag, request latency, error rates, DLQ/retry metrics.
- **Security:** TLS, SASL, ACLs (least privilege), secrets management; avoid plaintext in production.
- **Testing:** validate rebalances, failures, retries, duplicates, and schema compatibility; test on representative topologies.

## 7. Quick checklist

- [ ] Producer: `acks=all`, idempotence on, bounded `delivery.timeout.ms`, compression, graceful close.
- [ ] Consumer: manual commit after processing (if needed), rebalance commit-on-revoke, poll healthy, graceful shutdown.
- [ ] Handlers: idempotent/dedup for at-least-once; classify retriable/permanent errors.
- [ ] SerDe: explicit contract/version; safe deserialization + DLQ; evolution tested.
- [ ] Observability: lag, errors, retries, DLQ, latency metrics and alerts.
- [ ] Ops: RF/ISR/`min.insync.replicas` match durability goals; secure cluster.
