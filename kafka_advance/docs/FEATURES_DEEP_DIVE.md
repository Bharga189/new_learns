# Kafka Advance — Deep Dive (A01–A10)

This document provides a **deep dive for each advanced demo**: what it shows, why it matters, real-time behavior, trade-offs, failure modes, and how to test it locally with Podman.

**Local setup (used by all demos):** `podman/single-broker-kraft/docker-compose.yml`, bootstrap `localhost:9092`. Start with `cd podman/single-broker-kraft && podman compose up -d && cd ../..`.

---

## A01 — Real-Time Event Pipeline

**Class:** `com.example.kafka.advance.features.A01_RealTimeEventPipeline`  
**Topics:** `adv.orders.ingest` (input), `adv.orders.status` (output)

### What / why
A common real-time pattern: ingest domain events, process/transform them, and emit derived status events. Demonstrates end-to-end flow with keys (per-order ordering), typed JSON SerDe, and best-practice client presets.

### Key mechanics
- **Producer:** idempotent + `acks=all` preset; key = `orderId` so all events for an order route to the same partition (ordering per key).
- **Consumer:** manual commit after successful processing of a batch (at-least-once baseline).
- **Transformation:** consume → enrich/map → produce to output topic.

### Trade-offs / failure modes
- **At-least-once duplicates:** crash after producing output but before committing input offset can reprocess; make transforms/output idempotent or use transactions (A10).
- **Dual writes risk:** writing to Kafka + an external DB separately is not atomic — consider outbox or EOS patterns.
- **Hot keys:** skew partitions and throughput; choose keys with cardinality in mind.

### Test
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A01_RealTimeEventPipeline
```
Observe ingest metadata, processing logs, emitted status records, and commit-after-processing.

---

## A02 — SerDe JSON Variations

**Class:** `com.example.kafka.advance.features.A02_SerDeJsonVariations`

### Variations
1. **Typed JSON (POJO):** `OrderEvent` serialized/deserialized with a fixed target type. Simple and explicit; best when a topic has one contract.
2. **Envelope JSON:** `EventEnvelope` carries `eventType`, `schemaVersion`, `eventId`, timestamps, and a `JsonNode` payload. Enables routing/version metadata and tolerant payload inspection.
3. **Header-based polymorphic:** producer sets `event-type` (and optionally schema version) headers; `MultiTypeJsonDeserializer` resolves a registered class or falls back to `JsonNode`.

### Trade-offs
- JSON is flexible/readable but not self-describing with strong compatibility guarantees unless governed (Schema Registry / JSON Schema / explicit versioning + tests).
- Headers add explicit routing but require producer/consumer agreement; one-type-per-topic is operationally simpler when feasible.

### Test
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A02_SerDeJsonVariations
```

---

## A03 — SerDe Bytes and String Variations

**Class:** `com.example.kafka.advance.features.A03_SerDeBytesAndStringVariations`

### Variations
1. **String (UTF-8):** simplest text payloads; encoding and field delimiters/formats must be explicit.
2. **Raw `byte[]`:** format-agnostic — suitable for Protobuf/Avro bytes, custom binary, compressed/encrypted payloads. Brokers store bytes regardless of format.
3. **Binary-safe patterns:** Base64 is a text-wrapping display/transport workaround; prefer raw bytes when Kafka/client supports it. Compression (`compression.type`) is orthogonal to SerDe.

### Best practices
Define an explicit contract (format, encoding, version/magic bytes). Include type/schema metadata via headers or envelope. Avoid logging sensitive payloads.

### Test
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A03_SerDeBytesAndStringVariations
```

---

## A04 — SerDe Schema Evolution

**Class:** `com.example.kafka.advance.features.A04_SerDeSchemaEvolution`

### What / why
Schemas change. Safe evolution keeps producers and consumers compatible across deploy windows.

### Mechanics shown
- Produces **v1** and **v2** envelopes; v2 adds `currency`/`priority` (additive).
- Consumer reads both using tolerant access (`JsonNode` payload + defaults for missing fields).

### Compatibility guidance
- **Backward:** new consumer reads old data. **Forward:** old consumer reads new data. **Full:** both.
- Prefer additive optional fields with defaults. Treat renames/removals/type changes as breaking (new version/topic/migration).
- Test compatibility in CI; for multi-team scale, use Schema Registry + Avro/Protobuf/JSON Schema.

### Test
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A04_SerDeSchemaEvolution
```

---

## A05 — SerDe Error Handling (Safe Deserializer + DLQ)

**Class:** `com.example.kafka.advance.features.A05_SerDeErrorHandling`

### Problem
A malformed record can throw in deserialization and stall the poll loop, blocking partition progress.

### Enhancement
`SafeDeserializer` catches failures, logs a preview, and returns `null`. The app routes failures to a DLQ with context headers (`x-source-topic/partition/offset`, error). Valid records continue.

### Best practices
- Preserve enough context (and raw bytes where policy allows) for forensics/replay.
- Metric/alert on DLQ rate; DLQ is an operational signal, not a sink to ignore.
- Keep handlers idempotent; redrives can re-deliver.

### Test
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A05_SerDeErrorHandling
```

---

## A06 — Producer Best Practices

**Class:** `com.example.kafka.advance.features.A06_BestPracticesProducer`

### Practices demonstrated
- **Durability/safety:** `acks=all`, `enable.idempotence=true` (safe retries without duplicates per partition/producer session).
- **Bounded time:** `delivery.timeout.ms` caps total send+retry time; coordinate with `request.timeout.ms`/`linger.ms`.
- **Throughput:** `linger.ms`/`batch.size` + `compression.type` (lz4/zstd).
- **Ordering:** keys pin records to partitions; idempotence preserves order with retries.
- **Lifecycle:** callbacks/futures for errors; `flush()`/`close()` prevents buffered loss.

### Observability
Monitor record send rate, error/retry rates, request latency, batch size, buffer exhaustion.

### Test
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A06_BestPracticesProducer
```

---

## A07 — Consumer Best Practices

**Class:** `com.example.kafka.advance.features.A07_BestPracticesConsumer`

### Practices demonstrated
- **At-least-once commits:** `enable.auto.commit=false`; process then `commitSync` (or commit tracked offsets).
- **Rebalance safety:** commit pending processed offsets in `onPartitionsRevoked`; cooperative-sticky assignor reduces churn.
- **Poll health:** bounded `max.poll.records`; process within `max.poll.interval.ms` or offload carefully.
- **Graceful shutdown:** `consumer.wakeup()` triggers `WakeupException`; close in `finally`.

### Failure modes
- Committing before processing risks data loss on crash. Committing after risks duplicates — pair with idempotency (A09).
- Long processing without polling causes rebalance (`max.poll.interval.ms` exceeded).

### Test
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A07_BestPracticesConsumer
```

---

## A08 — Retry Topics and DLQ (Enhancement)

**Class:** `com.example.kafka.advance.features.A08_RetryTopicsAndDLQ`

### Pattern
`main --(retriable)--> retry (backoff/delay) --(max attempts)--> DLQ`; permanent failures go directly to DLQ.

### Mechanics shown
- Error classification (retriable vs permanent), attempt header (`x-retry-attempt`), max attempts, DLQ context headers.
- Demo simulates backoff via a separate pass; production uses delayed/scheduled retry topics or framework non-blocking retry.

### Trade-offs
Retries can reorder records; design for ordering requirements. Cap attempts and alert on DLQ/retry rates. Make handlers idempotent.

### Test
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A08_RetryTopicsAndDLQ
```

---

## A09 — Idempotent Consumer / Dedup (Enhancement)

**Class:** `com.example.kafka.advance.features.A09_IdempotentConsumerAndDedup`

### Why
At-least-once delivery can duplicate logical events. Non-idempotent side effects (charges, counters) require dedup or stronger coordination.

### Mechanics shown
Producer sends a duplicate `eventId`; consumer skips duplicates using a processed-ID set (demo in-memory). Production needs **durable, bounded** storage (DB unique key, Redis set+TTL, compacted topic/state store) and careful atomicity with side effects.

### Alternatives
Natural idempotency (upserts/conditional updates), transactional outbox, Kafka EOS for Kafka-internal pipelines (A10) — but external systems still need idempotency.

### Test
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A09_IdempotentConsumerAndDedup
```

---

## A10 — Exactly-Once Enhancements (Transactions)

**Class:** `com.example.kafka.advance.features.A10_ExactlyOnceEnhancements`

### Scope
Kafka transactions provide atomicity for **Kafka-to-Kafka** operations: output writes + consumed offset commits (`sendOffsetsToTransaction`) commit/abort together. `read_committed` consumers hide uncommitted/aborted data. **External** side effects are not covered.

### Mechanics shown
- Stable `transactional.id` (fencing), `initTransactions()`, `begin/commit/abort`.
- Atomic offsets + produced records via `sendOffsetsToTransaction(..., consumer.groupMetadata())`.
- `read_committed` verification consumer.

### Operational notes
Adds coordinator overhead; tune latency/throughput. Requires correct broker/topic replication for transaction/offset topics (Podman demo uses RF=1). On abort, records may be reprocessed — handlers should be idempotent where applicable.

### Test
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A10_ExactlyOnceEnhancements
```
