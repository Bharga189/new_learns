# Apache Kafka Features — Deep Dive (F01–F18), Real-Time Examples, and Podman Testing

This document is the **deep-dive companion** to the runnable Java examples in `src/main/java/com/example/kafka/features/`. It explains *what each feature does, why it exists, how it behaves under failure, key configuration trade-offs, and how to test it locally with Podman*.

> **Per-feature, copy/paste Podman setup & run steps:** see [`PODMAN_TEST_GUIDE.md`](PODMAN_TEST_GUIDE.md).  
> **Local environments (KRaft, no ZooKeeper):**
> - Single broker: `podman/single-broker-kraft` → `localhost:9092` (most features)
> - 3-broker cluster: `podman/cluster-3-broker` → `localhost:19092,localhost:29092,localhost:39092` (replication/ISR, rebalancing, RF=3 Admin demos) — pass `-Dbootstrap.servers=...`

---

## 0. Kafka Architecture Fundamentals (applies to every feature)

### 0.1 Core model
- **Broker**: a Kafka server that stores partition data on disk (append-only log segments) and serves produce/fetch requests.
- **Topic**: a named event stream category.
- **Partition**: an ordered, immutable sequence of records identified by monotonically increasing **offsets**. Partitions are the unit of **parallelism** and the unit of **ordering** (ordering is guaranteed only within a partition).
- **Record**: `(key?, value, timestamp, headers, topic, partition, offset)` (offset/partition assigned by broker context; headers/metadata visible to clients).

### 0.2 Replication and durability
- `replication.factor` (RF) controls how many brokers store a partition replica.
- One replica is the **leader** (handles reads/writes for that partition, by default); others are **followers** that fetch from the leader.
- **ISR (in-sync replicas)**: replicas sufficiently caught up to the leader (tunable via lag/time settings on the broker).
- **Durability knobs**: producer `acks=all` + topic/broker `min.insync.replicas` (e.g., RF=3, `min.insync.replicas=2`, `acks=all`) means a write is acknowledged only when it reaches enough ISR replicas to survive a broker loss. If ISR shrinks below the minimum, producers with `acks=all` get `NotEnoughReplicas` (availability trade-off).

### 0.3 Consumer groups and coordination
- Consumers with the same `group.id` form a **consumer group**. Topic partitions are assigned so that **each partition is consumed by at most one member of the group at a time** (enabling parallel processing without duplicating work within the group).
- Different groups independently read the same topic (pub/sub fan-out by group).
- The group coordinator manages membership and **rebalances** (assignment changes) on join/leave/crash/topic-partition changes.

### 0.4 KRaft (metadata without ZooKeeper)
Modern Kafka uses **KRaft** (Raft quorum) for cluster metadata. This project’s Podman environments run KRaft mode.

### 0.5 Delivery semantics vocabulary
- **At-most-once**: offsets committed before/without guaranteed processing; failures can drop records.
- **At-least-once**: process then commit; failures can redeliver duplicates (handlers must be **idempotent**).
- **Exactly-once (Kafka scope)**: via idempotent producers and/or transactions so produced outputs and consumed offsets are committed atomically within Kafka. End-to-end exactly-once with external systems requires idempotent sinks/outbox/CDC patterns.

**Podman reality check (replication/ISR):** use `podman/cluster-3-broker`, run `F12_AdminApi` with the 3-broker bootstrap list, and inspect leader/replicas/ISR.

---

## 1. Producers — F01 `F01_ProducerBasics`

### What it demonstrates (real-time)
Synchronous vs asynchronous `send()`, callbacks, delivery metadata (topic/partition/offset/timestamp), batching/compression, and durable acknowledgments.

### Deep dive
- **Serialization**: keys/values are converted to bytes. Incompatible schema changes break consumers; prefer governed schemas in production (see F14, ecosystem).
- **Partition selection**: explicit partition, or key hash (stable mapping while partition count is stable), or sticky partitioning for null keys to improve batching.
- **Batching**: `linger.ms` (wait to accumulate) and `batch.size` trade latency for throughput. `buffer.memory` bounds queued unsent records.
- **Compression** (`compression.type=lz4|snappy|zstd|gzip`): reduces network/storage; CPU cost. Compression applies to batches.
- **Acknowledgments**: `acks=0` (fire-and-forget, fastest, least safe), `acks=1` (leader only), `acks=all` (ISR durability with `min.insync.replicas`).
- **Retries & ordering**: retries can reorder unless idempotence/EOS constraints are used (see F07/F08). `max.in.flight.requests.per.connection` interacts with ordering.

### Failure modes / ops notes
- `TimeoutException`/buffer exhaustion if broker unreachable or buffer full. Transient leader changes cause retriable errors. Monitor record-error rate, request latency, batch size, and buffer usage.

### Podman test
Env: `podman/single-broker-kraft` (`localhost:9092`).  
Run: `mvn -q -DskipTests compile && mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F01_ProducerBasics`  
Observe: sync metadata and async callbacks. Verify with `kafka-console-consumer.sh` if desired.

---

## 2. Consumers (poll loop) — F02 `F02_ConsumerBasics`

### What it demonstrates
The pull-based `poll()` loop, deserialization, record metadata, and `auto.offset.reset`.

### Deep dive
- **Pull model** gives consumer-controlled backpressure. `fetch.min.bytes`/`fetch.max.wait.ms` tune fetch behavior; `max.poll.records` bounds work per poll.
- **Group liveness**: consumers send heartbeats; `session.timeout.ms` and `heartbeat.interval.ms` govern failure detection. Long processing without timely polling/healthy heartbeats can trigger rebalances; `max.poll.interval.ms` bounds time between polls.
- **`auto.offset.reset`** (`earliest|latest|none`) applies only when no committed offset exists for the group/partition.
- **Concurrency**: a single `KafkaConsumer` instance is **not thread-safe**; use one consumer per thread or confine to one thread (see F18 for parallel processing patterns).

### Failure modes
Poison-pill deserialization/processing errors (see F11), rebalance churn, commit/processing races (see F04).

### Podman test
Env: single broker. Produce with F01, consume with:  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F02_ConsumerBasics`  
Real-time: run producer and consumer in separate terminals.

---

## 3. Consumer Groups & Rebalancing — F03 `F03_ConsumerGroupsRebalancing`

### What it demonstrates
Partition assignment across group members, `ConsumerRebalanceListener` callbacks, and cooperative-sticky assignment.

### Deep dive
- **Scaling**: max useful consumers in a group ≤ number of partitions for that topic (extra consumers sit idle).
- **Rebalance triggers**: member join/leave/crash, subscription changes, partition count changes. Rebalances can pause consumption (“stop-the-world” in eager protocols) or reduce disruption with **cooperative** protocols.
- **Assignment strategies**: range, round-robin, sticky, cooperative-sticky. Cooperative-sticky (`CooperativeStickyAssignor`) minimizes partition movement and allows incremental revocation.
- **Static membership** (`group.instance.id`) can reduce unnecessary rebalances for transient restarts (when configured appropriately).

### Failure modes
Rebalance storms (unstable members, slow polls), duplicate processing around revocation if commits/processing aren’t coordinated (commit offsets for processed records before or as defined by your semantics during `onPartitionsRevoked`).

### Podman test
Env: **3-broker cluster** recommended. Run two members in separate terminals with different client IDs:  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F03_ConsumerGroupsRebalancing -Dclient.id=member-1 -Dbootstrap.servers=localhost:19092,localhost:29092,localhost:39092`  
Observe assignment/revocation logs.

---

## 4. Offsets — F04 `F04_OffsetsManagement`

### What it demonstrates
Manual commits (`commitSync`/specific offsets) and `seek` for replay.

### Deep dive
- **Committed offset** is the consumer group’s checkpoint stored in `__consumer_offsets`. It represents “next offset to read” semantics when committing `record.offset()+1`.
- **Auto-commit** (`enable.auto.commit`) is simple but can commit before processing completes (risking loss) or create duplicate windows; manual commit gives explicit control.
- **Commit timing** defines semantics: commit after durable/idempotent processing => at-least-once. Synchronous commits are simpler/clearer; async commits have lower latency but require careful error handling.
- **Seeking**: `seek`, `seekToBeginning/End`, and time-based offset lookup patterns enable replay, backfill, or skipping. Replaying requires downstream idempotency.

### Failure modes
Committing wrong offsets, committing during rebalance without coordination, unbounded replay load.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F04_OffsetsManagement`

---

## 5. Partitioning & Keys — F05 `F05_PartitioningKeys`

### Deep dive
- **Ordering**: only within a partition. Choose keys (e.g., `orderId`, `accountId`, `deviceId`) to colocate causally related events.
- **Partitioner behavior**: keyed records map to a partition via hashing (default murmur-based in Java client) — stable for a fixed partition count. **Increasing partitions changes the mapping** for some keys; plan key strategy before scaling partitions, or use custom partitioning/consistent-hashing approaches when needed.
- **Null keys**: use sticky partitioning for batching efficiency; no key-based ordering/affinity.
- **Hot keys** (skew): a single key/partition can become a bottleneck; mitigate by key design, salting (with downstream reassembly), or scaling consumers/partitions carefully.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F05_PartitioningKeys`  
Observe same key → same partition.

---

## 6. Headers & Timestamps — F06 `F06_HeadersAndTimestamps`

### Deep dive
- **Headers**: key/value byte metadata (correlation/trace IDs, schema hints, routing flags) without changing the payload contract. Keep them small and stable.
- **Timestamps**: `CreateTime` (producer/set time) vs `LogAppendTime` (broker append time, topic config `message.timestamp.type`). Timestamps affect time-based retention and `offsetsForTimes` lookups.
- **Event-time vs processing-time**: Kafka timestamps are not a full event-time processing framework, but they are important for retention, debugging, and stream time semantics.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F06_HeadersAndTimestamps`

---

## 7. Idempotent Producer — F07 `F07_IdempotentProducer`

### Deep dive
- Prevents **duplicate appends caused by producer retries** within a producer session using a **Producer ID (PID)** and per-partition **sequence numbers**. The broker deduplicates retried batches.
- Enable with `enable.idempotence=true`. Kafka enforces safe constraints (e.g., `acks=all`, retries > 0, bounded in-flight with ordering guarantees as required by version semantics).
- **Scope**: protects the write path from retry-induced duplicates; it does **not** deduplicate application-level resubmissions with new producer instances/logic, nor make consume-transform-produce atomic (see F08).

### Failure modes
Fenced/closed producer errors, misconfiguration; application retries that create *new* sends (different sequence context) are a separate concern.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F07_IdempotentProducer`

---

## 8. Transactions / EOS — F08 `F08_TransactionsEOS`

### Deep dive
- **Goal**: make a consume → transform → produce workflow atomic in Kafka: output records and the corresponding consumed offsets are committed together, visible as a unit.
- **Mechanisms**: `transactional.id` (stable identity; enables fencing of zombie producers), idempotence, transaction coordinator/markers, and consumer isolation level `read_committed` to hide aborted/uncommitted transactional data.
- **Pattern**: `initTransactions()` → `beginTransaction()` → `send()` outputs → `sendOffsetsToTransaction(...)` → `commitTransaction()` (or `abortTransaction()` on failure).
- **Scope/limits**: “exactly-once” applies to Kafka reads/writes within this model. External side effects (DB writes, emails, HTTP calls) require **idempotent consumers/sinks, transactional outbox, or CDC** patterns.

### Failure modes
Zombie fencing (good — prevents split-brain writers), transaction timeouts (`transaction.timeout.ms`), increased latency/overhead, broker version/config requirements.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F08_TransactionsEOS`

---

## 9. Log Compaction & Retention — F09 `F09_LogCompactionRetention`

### Deep dive
- **Delete retention** (`cleanup.policy=delete`): removes old segments by time (`retention.ms`) or size (`retention.bytes`).
- **Compaction** (`cleanup.policy=compact` or `compact,delete`): retains at least the **latest value per key** (changelog/table semantics). Requires meaningful keys. Compaction runs in the background and is **eventual**; active segments are not compacted.
- **Tombstones**: a record with a `null` value marks a key for deletion; the tombstone is retained for `delete.retention.ms` so consumers can observe the delete before it is removed.
- **Use cases**: CDC changelogs, KTable/state restore topics, latest-state snapshots by key.

### Failure modes
Assuming immediate compaction, missing keys, unbounded unique keys (compaction still keeps latest per key but storage can grow).

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F09_LogCompactionRetention`  
Immediate reads may show all versions; compaction is asynchronous/eventual.

---

## 10. Interceptors — F10 `F10_Interceptors`

### Deep dive
- `ProducerInterceptor` and `ConsumerInterceptor` hook into send/ack and consume/commit paths for **observability, tracing, auditing, and lightweight decoration**.
- **Constraints**: run on hot paths — keep them fast, non-blocking, and safe (handle exceptions carefully; don’t break produce/consume). Avoid heavy I/O inside interceptors.
- Pair with metrics (client JMX), distributed tracing, and structured logging for production.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F10_Interceptors`

---

## 11. Error Handling, Retries & DLQ — F11 `F11_ErrorHandlingDLQ`

### Deep dive
- **Classify errors**: retriable (transient network/broker/leader issues) vs non-retriable (bad payload, validation/auth/serialization issues). Retrying non-retriable “poison pills” can stall a partition.
- **Retry strategy**: bounded retries, exponential backoff, jitter, and circuit-breaking at the app level when appropriate. Kafka producer retries handle some transient cases; consumer-side processing retries are your responsibility.
- **DLQ (dead-letter queue/topic)**: publish failed records with context (original topic/partition/offset, key/value/headers, exception, timestamp). DLQs require **monitoring, triage, and replay tooling** — they are not a trash can.
- **Commit semantics**: decide whether to commit past a DLQ’d record (common, to unblock progress) and ensure replay/idempotency are defined.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F11_ErrorHandlingDLQ`  
Observe poison pill routed to DLQ and normal records processed.

---

## 12. Admin API — F12 `F12_AdminApi`

### Deep dive
- **Control plane** operations: create/delete topics, describe cluster/topics/configs, manage partitions (increase only), inspect consumer groups/offsets (and more). Prefer automation with **idempotency** and **least privilege**.
- **Operational impacts**: increasing partitions affects key→partition mapping (F05). RF/ISR/min.insync.replicas determine durability/availability (F0). Some config changes require care/rolling processes depending on the setting.

### Podman test
- Single broker: `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F12_AdminApi`
- 3-broker RF/ISR view: add `-Dbootstrap.servers=localhost:19092,localhost:29092,localhost:39092`

---

## 13. Kafka Streams (stateless real-time) — F13 `F13_KafkaStreamsRealtime`

### Deep dive
- **Topology** is a DAG of sources, processors, and sinks. `KafkaStreams` manages tasks, threads, and (for stateful apps) state restoration.
- **KStream vs KTable**: record stream vs changelog/table view (latest per key). Semantics matter for joins/aggregations.
- **SerDes** are required for keys/values (and internal repartition/changelog topics). Schema evolution discipline applies (F14).
- **Processing guarantees**: `at_least_once` (default historically common) vs `exactly_once_v2` for EOS within Streams (see F16). Stateful apps create internal topics; ensure broker permissions/configs allow them.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F13_KafkaStreamsRealtime`  
Observe topology and real-time transformed output.

---

## 14. Custom SerDe / JSON & Schema Evolution — F14 `F14_CustomSerDeJson`

### Deep dive
- Kafka transports bytes; **serialization/deserialization compatibility** is a production-critical contract. JSON is human-friendly but schema-flexible — uncoordinated changes can break consumers.
- **Recommended production approach**: Avro/Protobuf/JSON Schema with a **Schema Registry** and explicit compatibility rules (backward/forward/full), plus CI checks.
- **Versioning tactics**: include `schemaVersion`, prefer additive/backward-compatible changes, ignore unknown fields where safe, and route malformed/unsupported payloads to a DLQ (F11).
- **Performance/ops**: SerDe cost, payload size, compression (F01), and observability of deserialization errors.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F14_CustomSerDeJson`

---

## 15. Consumer Lag & Observability — F15 `F15_ConsumerLagObservability`

### Deep dive
- **Lag per partition** = `log-end offset (LEO) - committed offset` (with care for committed-vs-consumed-position nuances). It measures how far a group is behind available data.
- **Interpretation**: transient lag during bursts can be normal; **sustained growth** indicates insufficient consumer capacity, slow processing, rebalances, errors/DLQ loops, or downstream bottlenecks.
- **How to measure**: AdminClient (`listConsumerGroupOffsets`, `listOffsets`), `kafka-consumer-groups.sh`, client metrics/JMX, and monitoring platforms. Alert on sustained lag, lag growth rate, and time-to-recover SLOs.
- **Correlate with**: throughput, processing latency, rebalance rate, poll/commit behavior, error rates, under-replicated partitions/ISR, and broker resource saturation.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F15_ConsumerLagObservability`  
Observe lag before/after partial consumption and commits.

---

## 16. Stateful Streams & `exactly_once_v2` — F16 `F16_StatefulStreamsExactlyOnce`

### Deep dive
- **Stateful operations** (aggregations, joins, windowing, dedup patterns) maintain local **state stores** (e.g., RocksDB/in-memory) backed by internal **changelog topics** for fault tolerance and task restoration.
- **Repartitioning**: key-changing operations may create internal repartition topics; plan topic permissions, retention/cleanup settings (often compaction for changelogs), and capacity.
- **`exactly_once_v2`** (Kafka Streams `processing.guarantee`): uses transactions to provide stronger EOS within the Streams topology (atomic state/changelog/output commits per processed batch scope as defined by Streams). Requires compatible brokers/config and adds overhead (latency/throughput/cost).
- **Ops**: monitor task state, restoration time, commit latency, state store size, and internal topic health.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F16_StatefulStreamsExactlyOnce`

---

## 17. Security — TLS/SASL/ACLs — F17 `F17_SecurityConcepts`

### Deep dive
- **Encryption in transit (TLS/SSL)**: protects data on the wire; verifies broker identity via certificates; optional **mTLS** authenticates clients with certs. Configure `security.protocol=SSL` or `SASL_SSL`, truststores/keystores (never commit secrets).
- **Authentication (SASL)**: who the client is (e.g., `SCRAM-SHA-*` over TLS, `PLAIN` only over TLS, Kerberos/GSSAPI, `OAUTHBEARER`). Use least-privilege identities per service.
- **Authorization (ACLs)**: what an authenticated principal can do (read/write/create/describe on topics, group access, cluster operations, `transactional.id`/idempotent write permissions as needed). Default-deny in production when feasible; audit changes.
- **Multi-tenancy controls**: quotas, network segmentation, and separate listeners. **At rest** encryption and secret management are platform responsibilities.
- **Verification**: test authorized and unauthorized paths; rotate credentials/certs; monitor auth failures.

### Podman test
Env: single broker (plaintext connectivity check).  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F17_SecurityConcepts`  
Real TLS/SASL/ACL testing: follow `podman/security-tls-sasl/README.md` (template only; inject secrets/keystores locally).

---

## 18. Parallel Consumer Patterns — F18 `F18_ParallelConsumerPatterns`

### What it demonstrates (real-time)
Bounded thread-pool processing of consumed records with **safe commit barriers**, plus a **key-ordered** dispatch mode (same key processed sequentially, different keys in parallel).

### Deep dive
- **Why**: partition-level parallelism (F03) scales across partitions/consumers, but expensive per-record work can bottleneck a partition. Parallel consumer patterns add controlled concurrency.
- **Ordering choices**:
  - *Unordered*: max throughput when order doesn’t matter (or work is commutative/idempotent).
  - *Key-ordered*: preserves per-key (entity) ordering — common for stateful business logic — while parallelizing across keys.
  - *Partition-ordered*: equivalent to sequential per partition (use more partitions/consumers instead).
- **Offset safety (critical)**: commit only offsets whose records have **completed successfully**. Unsafe commits can skip records (at-most-once) or, with naive handling, create gaps. Demo uses a **batch barrier** (process batch in parallel → wait → `commitSync`) for clarity. Production systems may use **completion watermarks** (highest contiguous completed offset per partition) to commit more granularly.
- **Backpressure & liveness**: bound in-flight tasks (queue limits), consider `pause()`/`resume()` on partitions, and ensure polling/heartbeats respect `max.poll.interval.ms` and session timeouts under load.
- **Idempotency**: at-least-once + parallelism/retries/rebalances can produce duplicates; make handlers idempotent or deduplicate.
- **Production library**: **Confluent Parallel Consumer** (`io.confluent.parallelconsumer:parallel-consumer-core`) supports key-ordered/unordered/partition-ordered modes, offset management, retries/DLQ hooks, and metrics. This repo’s F18 implements the mechanics manually with standard `kafka-clients` (no extra dependency).

### Failure modes
Committing before completion, breaking key order with naive threading, unbounded concurrency causing OOM/overload, duplicate side effects without idempotency.

### Podman test
Env: single broker.  
`mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F18_ParallelConsumerPatterns`  
Key-ordered: `mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F18_ParallelConsumerPatterns -Ddemo.mode=KEY_ORDERED -Dparallelism=4`

---

## 19. Ecosystem (adjacent, for completeness)

| Component | Role | Key considerations |
|---|---|---|
| **Kafka Connect** | Source/sink integration framework | Scales via tasks; handle DLQ, retries, idempotent sinks, schema evolution |
| **Schema Registry** (Avro/Protobuf/JSON Schema) | Governed schemas & compatibility | Stronger contracts than ad-hoc JSON; integrates with SerDes (F14) |
| **ksqlDB** | SQL-like stream processing on Kafka | Built on Streams concepts (streams/tables, push/pull queries) |
| **MirrorMaker 2** | Cross-cluster replication | DR/multi-region; watch ordering/EOS/config/topic parity and cycles |
| **Tiered storage / quotas / metrics** | Cost, scale, multi-tenancy, ops | Broker/platform features; monitor and enforce governance |

---

## 20. Design Choices in This Project
- **KRaft Podman setups** (single + 3-broker cluster) for reproducible local testing.
- **One runnable `main` class per feature (F01–F18)** with deep JavaDoc, trade-offs, and Podman notes.
- **Per-feature Podman test guide** ([`PODMAN_TEST_GUIDE.md`](PODMAN_TEST_GUIDE.md)) so every feature has explicit local setup/test steps.
- **Minimal dependencies** (Kafka clients/streams, Jackson, SLF4J) — F18 uses standard concurrency APIs.
- **No committed secrets**; security testing is templated in `podman/security-tls-sasl/README.md`.
- **Parallel consumer coverage** includes both partition-level scaling concepts (F03) and in-consumer parallel patterns with ordering/commit trade-offs (F18).
