# Kafka Advance (Java + Podman)

An advanced, hands-on **Apache Kafka** project focused on:

- **Real-time examples** (runnable end-to-end pipeline demos)
- **Serialization / Deserialization (SerDe) variations** (JSON typed/envelope/polymorphic, String/bytes, schema evolution, safe error handling)
- **Enhancements** (retry topics + DLQ, idempotent consumer/dedup, exactly-once transactional refinements)
- **Best practices** (durable/idempotent producers, at-least-once consumers with correct commits, poll-loop health, rebalance safety, graceful shutdown, observability notes)

All demos run locally on **Podman** (KRaft single broker, no ZooKeeper).

## Prerequisites

- **Java 17+**
- **Maven 3.9+**
- **Podman 4+** with `podman compose`

```bash
java -version
mvn -version
podman --version
```

## Quick Start

```bash
# 1) Start local Kafka (single broker, KRaft)
cd podman/single-broker-kraft
podman compose up -d
cd ../..

# 2) Compile
mvn -q -DskipTests compile

# 3) Run a demo (example: real-time pipeline)
mvn -q exec:java -Dexec.mainClass=com.example.kafka.advance.features.A01_RealTimeEventPipeline

# 4) Stop Kafka (clean reset)
cd podman/single-broker-kraft && podman compose down -v
```

Default bootstrap: `localhost:9092` (override with `-Dbootstrap.servers=...` or `BOOTSTRAP_SERVERS`).

## Feature / Demo Index

| # | Area | Main class | What it demonstrates |
|---|---|---|---|
| A01 | Real-time | `com.example.kafka.advance.features.A01_RealTimeEventPipeline` | Real-time ingest → process/transform → emit status; keys, durable producer, manual commits |
| A02 | SerDe variations | `...A02_SerDeJsonVariations` | Typed JSON, envelope (type/version + JsonNode), header-based polymorphic routing |
| A03 | SerDe variations | `...A03_SerDeBytesAndStringVariations` | String (UTF-8), raw `byte[]`, binary-safe patterns, compression orthogonality |
| A04 | SerDe evolution | `...A04_SerDeSchemaEvolution` | Versioned envelopes, additive fields, tolerant reading with defaults (v1/v2) |
| A05 | SerDe + errors | `...A05_SerDeErrorHandling` | Safe deserializer wrapper, poison-pill handling, DLQ with error/context headers |
| A06 | Best practices | `...A06_BestPracticesProducer` | `acks=all`, idempotence, retries/timeouts, batching, compression, flush/close lifecycle |
| A07 | Best practices | `...A07_BestPracticesConsumer` | Manual commit after processing, rebalance commit on revoke, poll health, graceful shutdown (`wakeup`) |
| A08 | Enhancement | `...A08_RetryTopicsAndDLQ` | Retry topic pattern (main → retry → DLQ), attempt headers, retriable vs permanent classification |
| A09 | Enhancement | `...A09_IdempotentConsumerAndDedup` | Event-ID dedup for at-least-once duplicates (durable store required in production) |
| A10 | Enhancement | `...A10_ExactlyOnceEnhancements` | Transactional consume-transform-produce, `sendOffsetsToTransaction`, `read_committed`, abort/fencing notes |

(Use full class names in `exec:java -Dexec.mainClass=...`.)

## Serialization / Deserialization Variations (Summary)

| Variation | Where | Notes |
|---|---|---|
| Typed JSON (POJO) | A02, A01, A09 | Simple/explicit; one schema/type per topic is easiest |
| Envelope JSON (type/version + payload) | A02, A04 | Metadata for routing/versioning; tolerant `JsonNode` payload reading |
| Header-based polymorphic JSON | A02 | `event-type`/`schema-version` headers drive resolution |
| String (UTF-8) | A03 | Human-readable; define encoding/contract |
| Raw bytes (`byte[]`) | A03, A05 | Full control for binary formats, encryption, custom codecs |
| Schema evolution (additive + defaults) | A04 | Prefer additive changes; explicit versions; compatibility tests |
| Safe/error-handling deserializer | A05 | Prevent poison pills from stalling poll loop; route to DLQ with context |

See [`docs/SERDE_VARIATIONS.md`](docs/SERDE_VARIATIONS.md) for a deeper guide.

## Best Practices & Enhancements (Summary)

- **Producer:** `acks=all` + idempotence for safety; bound time with `delivery.timeout.ms`; batch/compress for throughput; always close/flush.
- **Consumer:** commit **after** processing for at-least-once; handle rebalances (commit on revoke); keep poll healthy; shutdown via `wakeup()`.
- **Delivery semantics:** at-least-once is common — make handlers idempotent or dedup (A09). Kafka transactions (A10) cover Kafka-to-Kafka atomicity, not external side effects.
- **Resilience:** classify retriable vs permanent errors; retry topics with backoff/limits; DLQ with context/metrics (A05, A08).
- **Contracts:** version events, test compatibility, and consider Schema Registry for multi-team production.

See [`docs/BEST_PRACTICES.md`](docs/BEST_PRACTICES.md) and [`docs/FEATURES_DEEP_DIVE.md`](docs/FEATURES_DEEP_DIVE.md).

## Podman Local Setup

- **Compose:** `podman/single-broker-kraft/docker-compose.yml`
- **Bootstrap:** `localhost:9092` (KRaft, RF=1 defaults suitable for local demos)
- **Guide:** [`docs/PODMAN_TEST_GUIDE.md`](docs/PODMAN_TEST_GUIDE.md)

```bash
cd podman/single-broker-kraft
podman compose up -d      # start
podman compose ps         # verify
podman compose logs -f kafka
podman compose down -v    # stop + clean data
```

## Project Structure

```text
kafka_advance/
├── pom.xml
├── README.md
├── docs/
│   ├── FEATURES_DEEP_DIVE.md
│   ├── SERDE_VARIATIONS.md
│   ├── BEST_PRACTICES.md
│   └── PODMAN_TEST_GUIDE.md
├── podman/
│   ├── README.md
│   └── single-broker-kraft/docker-compose.yml
├── scripts/
│   ├── podman-up.sh
│   ├── podman-down.sh
│   └── run-demo.sh
└── src/main/java/com/example/kafka/advance/
    ├── common/      (KafkaConfig, AdvancedKafkaConfig, TopicUtils)
    ├── model/       (OrderEvent, EventEnvelope)
    ├── serde/       (Jackson JSON serdes, Safe/MultiType deserializers, evolution helpers)
    └── features/    (A01–A10 runnable demos)
```

## Generate JavaDoc

```bash
mvn -q javadoc:javadoc
# open target/reports/apidocs/index.html
```

## Related Project

This repo also contains [`kafka_features/`](../kafka_features/README.md) with foundational Kafka feature demos (F01–F18). `kafka_advance` builds on those concepts with deeper SerDe variations, enhancements, and best-practice patterns.
