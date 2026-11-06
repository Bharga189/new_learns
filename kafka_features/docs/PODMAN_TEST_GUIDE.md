# Podman Test Guide — Per-Feature Local Setup

This guide gives **explicit Podman local setup and test steps for every Kafka feature demo** in this project.

## How to use this guide

1. Pick a feature (F01–F18).
2. Start the **required Podman environment** for that feature.
3. Compile once: `mvn -q -DskipTests compile`
4. Run the feature’s command(s).
5. Observe the expected behavior.
6. Stop the environment when done: `podman compose down -v` (from the same compose directory).

> All commands assume you run them from the **project root** unless noted. On Windows, use **bash** (Git Bash/MSYS) — the same commands work. Default bootstrap: `localhost:9092` (single broker).

## Environments

| Environment | Path | Bootstrap servers | Start | Stop |
|---|---|---|---|---|
| Single broker (KRaft) | `podman/single-broker-kraft/` | `localhost:9092` | `cd podman/single-broker-kraft && podman compose up -d` | `podman compose down -v` |
| 3-broker cluster (KRaft) | `podman/cluster-3-broker/` | `localhost:19092,localhost:29092,localhost:39092` | `cd podman/cluster-3-broker && podman compose up -d` | `podman compose down -v` |

Verify readiness: `podman compose ps` (containers running). If a demo fails to connect, wait a few seconds for Kafka to finish starting and rerun.

## Per-feature setup and test

### F01 — Producer basics (`com.example.kafka.features.F01_ProducerBasics`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F01_ProducerBasics`
- **Observe:** sync send metadata (topic/partition/offset/timestamp) and async callback results; batching/compression/acks configured in code.
- **Cleanup:** `cd podman/single-broker-kraft && podman compose down -v`

### F02 — Consumer basics (`com.example.kafka.features.F02_ConsumerBasics`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Produce sample data (optional but recommended):** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F01_ProducerBasics`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F02_ConsumerBasics`
- **Realtime test:** run F01 in one terminal and F02 in another to see live consumption.
- **Observe:** poll loop receives records with coordinates; `auto.offset.reset=earliest` behavior on first run.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F03 — Consumer groups & rebalancing (`com.example.kafka.features.F03_ConsumerGroupsRebalancing`)
- **Podman setup:** **3-broker cluster** (best for observing distributed assignment; works on single broker too).
- **Start:** `cd podman/cluster-3-broker && podman compose up -d && cd ../..`
- **Run (2 members, separate terminals):**
  - Terminal 1: `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F03_ConsumerGroupsRebalancing -Dbootstrap.servers=localhost:19092,localhost:29092,localhost:39092 -Dclient.id=member-1`
  - Terminal 2: `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F03_ConsumerGroupsRebalancing -Dbootstrap.servers=localhost:19092,localhost:29092,localhost:39092 -Dclient.id=member-2`
- **Observe:** partition assignment and revocation callbacks during rebalance; cooperative-sticky behavior.
- **Cleanup:** `cd podman/cluster-3-broker && podman compose down -v`

### F04 — Offsets management (`com.example.kafka.features.F04_OffsetsManagement`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F04_OffsetsManagement`
- **Observe:** manual `commitSync`, committed offsets, and `seek` replaying records.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F05 — Partitioning & keys (`com.example.kafka.features.F05_PartitioningKeys`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F05_PartitioningKeys`
- **Observe:** same key -> same partition (ordering per key/partition); null-key sticky partitioning; explicit partition if demonstrated.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F06 — Headers & timestamps (`com.example.kafka.features.F06_HeadersAndTimestamps`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F06_HeadersAndTimestamps`
- **Observe:** headers (correlation/tracing metadata) and timestamps on consumed records.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F07 — Idempotent producer (`com.example.kafka.features.F07_IdempotentProducer`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F07_IdempotentProducer`
- **Observe:** idempotence enabled (`acks=all`, retries) preventing duplicates on retry scenarios (PID+sequence semantics explained in logs/Javadoc).
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F08 — Transactions / EOS (`com.example.kafka.features.F08_TransactionsEOS`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F08_TransactionsEOS`
- **Observe:** atomic consume-transform-produce; `read_committed` consumer sees committed transactional output; abort/commit semantics described.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F09 — Log compaction & retention (`com.example.kafka.features.F09_LogCompactionRetention`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F09_LogCompactionRetention`
- **Observe:** compacted topic config, multiple versions and tombstone written. Compaction is asynchronous — immediate reads may show all versions; Javadoc explains eventual behavior.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F10 — Interceptors (`com.example.kafka.features.F10_Interceptors`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F10_Interceptors`
- **Observe:** producer/consumer interceptor callbacks (`onSend`/`onAcknowledgement`, `onConsume`/`onCommit`).
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F11 — Error handling, retries & DLQ (`com.example.kafka.features.F11_ErrorHandlingDLQ`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F11_ErrorHandlingDLQ`
- **Observe:** poison-pill/invalid records routed to DLQ topic with context headers; valid records processed.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F12 — Admin API (`com.example.kafka.features.F12_AdminApi`)
- **Podman setup:** single broker (basic) or **3-broker cluster** (replication/ISR/leader placement).
- **Start (single):** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run (single):** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F12_AdminApi`
- **Run (cluster):** `cd podman/cluster-3-broker && podman compose up -d && cd ../..` then `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F12_AdminApi -Dbootstrap.servers=localhost:19092,localhost:29092,localhost:39092`
- **Observe:** cluster metadata, topic creation/describe/list, partition leaders/replicas/ISR.
- **Cleanup:** `podman compose down -v` in the compose directory used.

### F13 — Kafka Streams realtime (`com.example.kafka.features.F13_KafkaStreamsRealtime`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F13_KafkaStreamsRealtime`
- **Observe:** topology logs and realtime stream processing (input -> filter/map -> output topic).
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F14 — Custom JSON SerDe (`com.example.kafka.features.F14_CustomSerDeJson`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F14_CustomSerDeJson`
- **Observe:** typed domain objects serialized/deserialized as JSON; schema/versioning considerations explained in Javadoc.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F15 — Consumer lag & observability (`com.example.kafka.features.F15_ConsumerLagObservability`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F15_ConsumerLagObservability`
- **Observe:** end offsets vs committed offsets (lag) via AdminClient/consumer metrics concepts.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F16 — Stateful Streams with exactly_once_v2 (`com.example.kafka.features.F16_StatefulStreamsExactlyOnce`)
- **Podman setup:** single broker.
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F16_StatefulStreamsExactlyOnce`
- **Observe:** stateful aggregation (count/sum) with `processing.guarantee=exactly_once_v2` (where supported) and changelog-topic concepts.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F17 — Security & ACL concepts (`com.example.kafka.features.F17_SecurityConcepts`)
- **Podman setup:** single broker (concept demo connects to plaintext local broker; security overlay documented).
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Run:** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F17_SecurityConcepts`
- **Observe:** prints TLS/SASL/ACL configuration checklist and validates local connectivity assumptions; see `podman/security-tls-sasl/README.md` for a hardening overlay template.
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

### F18 — Parallel consumer patterns (`com.example.kafka.features.F18_ParallelConsumerPatterns`)
- **Podman setup:** single broker (topic with 3 partitions created by the demo).
- **Start:** `cd podman/single-broker-kraft && podman compose up -d && cd ../..`
- **Compile:** `mvn -q -DskipTests compile`
- **Run (batch barrier, parallel processing + safe commit):** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F18_ParallelConsumerPatterns`
- **Run (key-ordered parallelism — same key stays ordered):** `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F18_ParallelConsumerPatterns -Ddemo.mode=KEY_ORDERED -Dparallelism=4`
- **Optional knobs:** `-Dparallelism=N`, `-Ddemo.maxEmptyPolls=N`, `-Ddemo.produceCount=N`.
- **Observe:** records processed by multiple worker threads/lanes, key-ordered lane sequencing in `KEY_ORDERED` mode, and offsets committed only after work completes (at-least-once barrier). The demo produces its own workload for a real-time parallel-processing example.
- **Deep dive:** ordering (unordered/key-ordered/partition-ordered), completion watermarks vs batch barriers, backpressure/pause-resume, idempotency, and the Confluent Parallel Consumer library approach (see JavaDoc and `docs/FEATURES_DEEP_DIVE.md` F18).
- **Cleanup:** `podman compose down -v` in `podman/single-broker-kraft`.

## Tips
- Use `-Dbootstrap.servers=...` to target a different cluster.
- Use `podman compose down -v` to reset topics/state between clean test runs.
- If ports conflict, edit the compose file’s published ports and pass the matching `bootstrap.servers`.
- Generate Java API docs: `mvn -q javadoc:javadoc` → `target/reports/apidocs/index.html`.
