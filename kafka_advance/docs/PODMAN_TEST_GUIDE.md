# Podman Test Guide — kafka_advance (A01–A10)

Explicit local **Podman** setup and test steps for every advanced demo.

## Prerequisites
- Java 17+, Maven 3.9+, Podman 4+ with `podman compose`.

## Environment (all demos)

| Path | Bootstrap (host) | Start | Stop (clean) |
|---|---|---|---|
| `podman/single-broker-kraft/` | `localhost:9092` | `cd podman/single-broker-kraft && podman compose up -d && cd ../..` | `cd podman/single-broker-kraft && podman compose down -v` |

Verify: `podman compose ps` (broker running). If connection fails, wait a few seconds and retry.

Compile once from project root (`kafka_advance/`):
```bash
mvn -q -DskipTests compile
```

> Use `podman compose down -v` for a clean topic/state reset between runs. Override bootstrap with `-Dbootstrap.servers=...`.

## Per-demo test steps

### A01 — Real-time event pipeline
```bash
cd podman/single-broker-kraft && podman compose up -d && cd ../..
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A01_RealTimeEventPipeline
```
**Observe:** ingest records produced with keys, consume→transform→emit to status topic, commits after processing. Durable/idempotent producer preset.

### A02 — SerDe JSON variations (typed, envelope, polymorphic)
```bash
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A02_SerDeJsonVariations
```
**Observe:** typed POJO round-trip, envelope with `type/version/payload` (tolerant field), header-based `event-type` routing.

### A03 — SerDe bytes and String variations
```bash
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A03_SerDeBytesAndStringVariations
```
**Observe:** UTF-8 String payload; raw `byte[]` payload with Base64/decoded view; compression noted as orthogonal.

### A04 — SerDe schema evolution (v1/v2 + defaults)
```bash
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A04_SerDeSchemaEvolution
```
**Observe:** v1/v2 additive fields; consumer applies defaults for missing fields (tolerant evolution).

### A05 — SerDe error handling (safe deser + DLQ)
```bash
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A05_SerDeErrorHandling
```
**Observe:** one valid record processed; poison pill logged and routed to DLQ with context headers; consumer continues.

### A06 — Producer best practices
```bash
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A06_BestPracticesProducer
```
**Observe:** `acks=all`/idempotence/compression/linger configs logged; async callbacks + sync ack; flush/close lifecycle.

### A07 — Consumer best practices
```bash
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A07_BestPracticesConsumer
```
**Observe:** manual commits after processing, rebalance listener commit-on-revoke, graceful shutdown via `wakeup()`.

### A08 — Retry topics and DLQ enhancement
```bash
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A08_RetryTopicsAndDLQ
```
**Observe:** valid message succeeds; transient failure retried with attempt header; permanent failure sent to DLQ.

### A09 — Idempotent consumer / dedup enhancement
```bash
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A09_IdempotentConsumerAndDedup
```
**Observe:** duplicate `eventId` delivered twice but applied once (`APPLY` vs `DEDUP-SKIP`). Demo uses in-memory store; production needs durable dedup.

### A10 — Exactly-once enhancements (transactions)
```bash
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A10_ExactlyOnceEnhancements
```
**Observe:** transactional consume-transform-produce with `sendOffsetsToTransaction`, commit/abort semantics, and `read_committed` verification of output. (Single-broker demo uses RF=1.)

## Cleanup
```bash
cd podman/single-broker-kraft && podman compose down -v
```

## Troubleshooting
- **Connection refused:** broker not ready — `podman compose ps` / `podman compose logs kafka`, wait, retry.
- **Port conflict:** change published `9092` in compose and pass `-Dbootstrap.servers=localhost:<port>`.
- **Stale state/offset surprises:** `podman compose down -v` for a clean reset (demos also use unique groups where helpful).
- **Transactional errors:** ensure broker started with transaction settings (compose sets single-broker-safe defaults).
