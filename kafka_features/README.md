# Kafka Features Deep Dive (Java + Podman)

A hands-on project that explores **Apache Kafka features** with:

- **Real-time Java examples** (runnable `main` classes, F01–F18)
- **Deep explanations in JavaDoc** and a **deep dive for each feature** (internals, trade-offs, failure modes)
- **Parallel consumer examples** (F18: bounded thread-pool processing, key-ordered lanes, safe offset-commit patterns)
- **Local Podman setup for testing each feature** (KRaft, no ZooKeeper)

Each feature explains *what it is*, *why it matters*, key configuration/trade-offs, and the exact **Podman + run commands** to test it locally.

## Prerequisites

- **Java 17+** (project targets Java 17 bytecode)
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

# 3) Run a feature demo (example: producer basics)
mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F01_ProducerBasics

# 4) Stop Kafka
cd podman/single-broker-kraft && podman compose down -v
```

Helper scripts (bash): `scripts/podman-up-single.sh`, `scripts/podman-up-cluster.sh`, `scripts/podman-down-all.sh`, `scripts/run-feature.sh <F01_ProducerBasics>`.

## Podman Environments (Local Test Setup)

| Environment | Compose file | Bootstrap servers (host) | Best for |
|---|---|---|---|
| Single broker (KRaft) | `podman/single-broker-kraft/docker-compose.yml` | `localhost:9092` | Most feature tests (producer/consumer, offsets, idempotence, transactions, headers, compaction config, interceptors, DLQ, Admin RF=1, Streams, SerDe, lag, EOSv2, security concepts) |
| 3-broker cluster (KRaft) | `podman/cluster-3-broker/docker-compose.yml` | `localhost:19092,localhost:29092,localhost:39092` | Replication, ISR, leader placement, consumer-group rebalancing/failover, Admin (RF=3) |
| Security overlay (template) | `podman/security-tls-sasl/README.md` | depends on overlay | TLS/SASL/ACL local testing guidance (no secrets committed) |

> **Every feature has explicit Podman test steps** in [`docs/PODMAN_TEST_GUIDE.md`](docs/PODMAN_TEST_GUIDE.md). Default bootstrap is `localhost:9092`; pass `-Dbootstrap.servers=...` for the 3-broker cluster.

## Feature Index (Real-time Examples + Podman Test per Feature)

| # | Kafka feature | Main class | Podman setup | Run command | What to observe |
|---|---|---|---|---|---|
| 01 | Producer basics (sync/async, batching, compression, acks) | `com.example.kafka.features.F01_ProducerBasics` | single | `mvn -q exec:java -Dexec.mainClass=...F01_ProducerBasics` | Metadata/callbacks; batching/compression/acks behavior |
| 02 | Consumer basics (poll loop, deserialization) | `com.example.kafka.features.F02_ConsumerBasics` | single | `mvn -q exec:java -Dexec.mainClass=...F02_ConsumerBasics` | Real-time poll/consume, offsets reset |
| 03 | Consumer groups & rebalancing | `com.example.kafka.features.F03_ConsumerGroupsRebalancing` | **cluster** | run 2 instances with `-Dbootstrap.servers=... -Dclient.id=member-N` | Assignment/revocation, cooperative-sticky |
| 04 | Offsets (manual commit, seek, replay) | `com.example.kafka.features.F04_OffsetsManagement` | single | `mvn -q exec:java -Dexec.mainClass=...F04_OffsetsManagement` | Commit after processing; replay via seek |
| 05 | Partitioning & keys (ordering) | `com.example.kafka.features.F05_PartitioningKeys` | single | `mvn -q exec:java -Dexec.mainClass=...F05_PartitioningKeys` | Same key → same partition/order |
| 06 | Headers & timestamps (metadata) | `com.example.kafka.features.F06_HeadersAndTimestamps` | single | `mvn -q exec:java -Dexec.mainClass=...F06_HeadersAndTimestamps` | Tracing/correlation headers, timestamps |
| 07 | Idempotent producer (safe retries) | `com.example.kafka.features.F07_IdempotentProducer` | single | `mvn -q exec:java -Dexec.mainClass=...F07_IdempotentProducer` | PID+sequence dedup on retries |
| 08 | Transactions / EOS (consume-transform-produce) | `com.example.kafka.features.F08_TransactionsEOS` | single | `mvn -q exec:java -Dexec.mainClass=...F08_TransactionsEOS` | Atomic offsets+writes, `read_committed` |
| 09 | Log compaction & retention | `com.example.kafka.features.F09_LogCompactionRetention` | single | `mvn -q exec:java -Dexec.mainClass=...F09_LogCompactionRetention` | Compacted topic config, versions/tombstone (async) |
| 10 | Interceptors (producer/consumer hooks) | `com.example.kafka.features.F10_Interceptors` | single | `mvn -q exec:java -Dexec.mainClass=...F10_Interceptors` | `onSend/onAck/onConsume/onCommit` hooks |
| 11 | Error handling, retries & DLQ | `com.example.kafka.features.F11_ErrorHandlingDLQ` | single | `mvn -q exec:java -Dexec.mainClass=...F11_ErrorHandlingDLQ` | Poison pill → DLQ with context |
| 12 | Admin API (topics, configs, cluster metadata) | `com.example.kafka.features.F12_AdminApi` | single/**cluster** | `mvn -q exec:java -Dexec.mainClass=...F12_AdminApi` | Topic/cluster metadata, leaders/replicas/ISR |
| 13 | Kafka Streams (real-time topology) | `com.example.kafka.features.F13_KafkaStreamsRealtime` | single | `mvn -q exec:java -Dexec.mainClass=...F13_KafkaStreamsRealtime` | Stateless filter/map stream processing |
| 14 | Custom JSON SerDe (typed events, schema/versioning notes) | `com.example.kafka.features.F14_CustomSerDeJson` | single | `mvn -q exec:java -Dexec.mainClass=...F14_CustomSerDeJson` | Typed domain events serialized/deserialized |
| 15 | Consumer lag & observability | `com.example.kafka.features.F15_ConsumerLagObservability` | single | `mvn -q exec:java -Dexec.mainClass=...F15_ConsumerLagObservability` | End offsets vs committed offsets (lag) |
| 16 | Stateful Streams + `exactly_once_v2` | `com.example.kafka.features.F16_StatefulStreamsExactlyOnce` | single | `mvn -q exec:java -Dexec.mainClass=...F16_StatefulStreamsExactlyOnce` | Aggregation, state/changelog, EOS config |
| 17 | Security concepts (TLS/SASL/ACL) | `com.example.kafka.features.F17_SecurityConcepts` | single (+ security overlay template) | `mvn -q exec:java -Dexec.mainClass=...F17_SecurityConcepts` | Security checklist + local connectivity check |
| 18 | Parallel consumer patterns (thread pool, key-ordered lanes, safe commits) | `com.example.kafka.features.F18_ParallelConsumerPatterns` | single | `mvn -q exec:java -Dexec.mainClass=...F18_ParallelConsumerPatterns` (add `-Ddemo.mode=KEY_ORDERED` for key ordering) | Parallel workers, key-ordered sequencing, commit-after-completion |

(Use full class names in commands as shown in the table header pattern, e.g. `com.example.kafka.features.F01_ProducerBasics`.)

## Documentation

- **Per-feature Podman test steps (all features):** [`docs/PODMAN_TEST_GUIDE.md`](docs/PODMAN_TEST_GUIDE.md)
- **Deep dive (architecture + per-feature deep dive F01–F18, trade-offs, failure modes):** [`docs/FEATURES_DEEP_DIVE.md`](docs/FEATURES_DEEP_DIVE.md)
- **Podman environments:** [`podman/README.md`](podman/README.md)
- **Java API docs (JavaDoc):** `mvn -q javadoc:javadoc` → `target/reports/apidocs/index.html`

## Project Structure

```text
.
├── pom.xml
├── README.md
├── docs/
│   ├── FEATURES_DEEP_DIVE.md
│   └── PODMAN_TEST_GUIDE.md
├── podman/
│   ├── README.md
│   ├── single-broker-kraft/docker-compose.yml
│   ├── cluster-3-broker/docker-compose.yml
│   └── security-tls-sasl/README.md
├── scripts/
│   ├── podman-up-single.sh
│   ├── podman-up-cluster.sh
│   ├── podman-down-all.sh
│   └── run-feature.sh
└── src/main/java/com/example/kafka/
    ├── common/KafkaConfig.java
    └── features/F01_...F18_*.java
```

## Kafka Features Covered (Conceptual Map)

- **Architecture:** brokers, topics, partitions, replication, ISR, leaders/followers, KRaft metadata
- **Producers:** serialization, partitioning, batching, compression, `acks`, callbacks, retries
- **Consumers:** poll loop, deserialization, `group.id`, assignment strategies, cooperative rebalancing
- **Parallel consumption:** partition-level scaling (groups) and in-consumer parallel patterns — unordered/key-ordered/partition-ordered trade-offs, completion barriers/watermarks, backpressure, idempotency (F03, F18)
- **Offsets & semantics:** committed offsets, auto/manual commit, seek, `auto.offset.reset`, delivery semantics
- **Data guarantees:** idempotent producer (PID/sequence), transactions, `read_committed`, `sendOffsetsToTransaction`
- **Storage:** retention vs compaction, tombstones, topic configs
- **Extensibility/ops:** headers, timestamps, interceptors, Admin API, DLQ/error handling
- **Processing:** Kafka Streams stateless/stateful topologies, state stores/changelogs, `exactly_once_v2`
- **SerDe & evolution:** custom JSON SerDe, schema/versioning considerations
- **Observability:** consumer lag (end vs committed offsets), operational health signals
- **Security:** TLS/SSL, SASL, ACLs, quotas/multi-tenancy concepts (local plaintext demos + security overlay template)
- **Ecosystem (documented):** Kafka Connect, Schema Registry, ksqlDB, MirrorMaker 2, tiered storage/quotas considerations

## Cleanup

```bash
cd podman/single-broker-kraft && podman compose down -v
cd ../cluster-3-broker && podman compose down -v
# or: ./scripts/podman-down-all.sh
```
