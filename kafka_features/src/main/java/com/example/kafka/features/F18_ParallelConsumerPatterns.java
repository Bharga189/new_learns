package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Feature 18: Parallel consumer patterns — increasing throughput beyond one-record-at-a-time
 * processing while preserving correctness (ordering/commit semantics) where required.
 *
 * <h2>Why parallel consumption?</h2>
 * A standard Kafka consumer loop processes records sequentially per poll batch. Kafka already
 * provides <b>partition-level parallelism</b>: partitions of a topic are distributed across
 * consumers in the same group, and each partition is processed by only one member. However,
 * when a single partition's records are expensive to process (I/O, CPU, remote calls),
 * per-partition sequential processing can become a bottleneck. Parallel consumer patterns
 * increase in-partition or cross-task concurrency.
 *
 * <h2>Core constraint (deep dive)</h2>
 * Kafka's strongest ordering guarantee is <b>per partition</b>. Parallelism must be chosen
 * relative to ordering needs:
 * <ul>
 *   <li><b>Unordered / partition-independent:</b> maximum parallelism; safe when record order
 *       does not matter or work is idempotent/commutative.</li>
 *   <li><b>Key-ordered:</b> records with the same key must be processed in order; different keys
 *       may run in parallel. This preserves entity-level ordering (common for aggregates,
 *       account/order state) without sacrificing all throughput.</li>
 *   <li><b>Partition-ordered:</b> preserve full partition order (equivalent to sequential within
 *       partition). Parallelism can still come from multiple partitions/consumers.</li>
 * </ul>
 *
 * <h2>Offset-commit correctness (critical)</h2>
 * With parallel processing, <b>never commit offsets for work that has not completed successfully</b>.
 * Committing past an in-flight record can lose it on failure (at-most-once for that record).
 * Common safe patterns:
 * <ul>
 *   <li><b>Batch barrier:</b> dispatch a polled batch to workers, wait for all tasks to finish,
 *       then {@code commitSync()}. Simple and safe (at-least-once); throughput limited by the
 *       slowest record in the barrier.</li>
 *   <li><b>Per-partition completion tracking:</b> track the highest contiguous completed offset per
 *       partition and commit only that watermark ("largest offset + 1"). Allows out-of-order
 *       completion without skipping unprocessed records.</li>
 *   <li><b>Pause/resume:</b> {@code consumer.pause(partitions)} when in-flight work reaches a limit
 *       to apply backpressure without breaking group membership (keep polling/heartbeats alive via
 *       a heartbeat strategy as needed; modern clients separate heartbeats from poll timing to a
 *       degree, but {@code max.poll.interval.ms} still matters for processing gaps in some designs).</li>
 * </ul>
 * <b>At-least-once + idempotency:</b> parallel processing increases the chance of duplicates on
 * retry/rebalance; handlers should be idempotent or use deduplication.
 *
 * <h2>Library vs manual implementation</h2>
 * Production systems often use the <b>Confluent Parallel Consumer</b>
 * ({@code io.confluent.parallelconsumer:parallel-consumer-core}) which provides key-ordered,
 * unordered, and partition-ordered modes, offset/watermark management, retry/DLQ integration,
 * and metrics. This demo implements the core ideas manually with standard {@code kafka-clients}
 * to stay dependency-light and make the mechanics visible.
 *
 * <h2>Real-time demo modes</h2>
 * <ul>
 *   <li><b>BATCH_BARRIER</b> (default): parallel processing of each polled batch, commit after all
 *       tasks complete. Demonstrates throughput gain and safe commit.</li>
 *   <li><b>KEY_ORDERED</b>: a fixed worker per key-hash "lane" so the same key is processed
 *       sequentially while different keys run in parallel.</li>
 * </ul>
 *
 * <h2>Configuration knobs</h2>
 * <ul>
 *   <li>{@code -Dparallelism=N} (default 4): worker threads.</li>
 *   <li>{@code -Ddemo.mode=BATCH_BARRIER|KEY_ORDERED}.</li>
 *   <li>{@code -Ddemo.maxEmptyPolls=N} (default 12).</li>
 *   <li>{@code -Ddemo.produceCount=N} (default 40): records produced for the demo.</li>
 * </ul>
 *
 * <h2>Local test (Podman)</h2>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile
 * # bounded parallel batch processing + safe commit
 * mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F18_ParallelConsumerPatterns
 * # key-ordered parallelism (same key stays ordered)
 * mvn -q exec:java -Dexec.mainClass=com.example.kafka.features.F18_ParallelConsumerPatterns \
 *   -Ddemo.mode=KEY_ORDERED -Dparallelism=4
 * }</pre>
 *
 * <p><b>Observe:</b> multiple worker threads processing records, per-record completion logs with
 * thread names, and commits occurring only after work completes. In {@code KEY_ORDERED} mode,
 * records sharing a key are handled by the same lane in arrival order.</p>
 */
public class F18_ParallelConsumerPatterns {
    private static final Logger log = LoggerFactory.getLogger(F18_ParallelConsumerPatterns.class);
    private static final String TOPIC = "demo.parallel-consumer";
    private static final String GROUP = "f18-parallel-group";

    enum Mode { BATCH_BARRIER, KEY_ORDERED }

    public static void main(String[] args) throws Exception {
        int parallelism = Math.max(1, Integer.getInteger("parallelism", 4));
        Mode mode = Mode.valueOf(System.getProperty("demo.mode", "BATCH_BARRIER").trim().toUpperCase());
        int maxEmptyPolls = Integer.getInteger("demo.maxEmptyPolls", 12);
        int produceCount = Integer.getInteger("demo.produceCount", 40);

        log.info("=== F18 Parallel Consumer Patterns (bootstrap={}, topic={}, mode={}, parallelism={}) ===",
                KafkaConfig.bootstrapServers(), TOPIC, mode, parallelism);

        ensureTopic(TOPIC, 3);
        produceWorkload(produceCount);

        if (mode == Mode.KEY_ORDERED) {
            runKeyOrdered(parallelism, maxEmptyPolls);
        } else {
            runBatchBarrier(parallelism, maxEmptyPolls);
        }

        log.info("=== Done === (also see Confluent Parallel Consumer for production-grade key/unordered/partition-ordered processing)");
    }

    private static void produceWorkload(int count) {
        Properties props = KafkaConfig.baseProducerProps("f18-producer");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        String[] keys = {"account-A", "account-B", "account-C", "account-D"};
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 1; i <= count; i++) {
                String key = keys[i % keys.length];
                ProducerRecord<String, String> r = new ProducerRecord<>(
                        TOPIC, key, "event#" + i + " payload-for-" + key);
                producer.send(r, (md, ex) -> {
                    if (ex != null) {
                        log.error("Produce failed: {}", ex.toString());
                    }
                });
            }
            producer.flush();
            log.info("[PRODUCED] {} records with keys {} (same key => same partition/ordering domain)", count, List.of(keys));
        }
    }

    /**
     * BATCH_BARRIER: poll -> process all records in parallel -> wait for completion -> commitSync.
     * Safe at-least-once: offsets are committed only after the batch finishes. If a failure occurs
     * mid-batch, uncommitted records may be redelivered (handlers should be idempotent).
     */
    private static void runBatchBarrier(int parallelism, int maxEmptyPolls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(parallelism, r -> {
            Thread t = new Thread(r, "f18-worker");
            t.setDaemon(true);
            return t;
        });
        Properties props = consumerProps("f18-batch-" + System.currentTimeMillis());
        AtomicInteger processed = new AtomicInteger();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(TOPIC));
            int empty = 0;
            while (empty < maxEmptyPolls) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    empty++;
                    continue;
                }
                empty = 0;
                List<CompletableFuture<Void>> tasks = new ArrayList<>(records.count());
                for (ConsumerRecord<String, String> r : records) {
                    tasks.add(CompletableFuture.runAsync(() -> handle(r, processed), pool));
                }
                // Barrier: wait for every task before committing offsets.
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
                consumer.commitSync();
                log.info("[COMMIT] batchSize={} committed after parallel completion (at-least-once barrier)",
                        records.count());
            }
        } finally {
            shutdown(pool);
        }
        log.info("[SUMMARY] processed={} records in BATCH_BARRIER mode", processed.get());
    }

    /**
     * KEY_ORDERED: dispatch by key hash to a stable lane/worker queue so records with the same key
     * execute sequentially, while different keys execute in parallel. This preserves entity-level
     * ordering (e.g., per account/order) — a common real-world requirement.
     *
     * <p>Commits use a simple post-batch barrier for clarity. Production key-ordered engines (e.g.,
     * Confluent Parallel Consumer) also manage per-offset completion watermarks to commit safely
     * with finer granularity.</p>
     */
    private static void runKeyOrdered(int parallelism, int maxEmptyPolls) throws Exception {
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] laneTails = new CompletableFuture[parallelism];
        ExecutorService pool = Executors.newFixedThreadPool(parallelism, r -> {
            Thread t = new Thread(r, "f18-key-lane");
            t.setDaemon(true);
            return t;
        });
        for (int i = 0; i < parallelism; i++) laneTails[i] = CompletableFuture.completedFuture(null);
        // Track last processing sequence per key to make ordering visible in logs.
        Map<String, AtomicInteger> perKeySeq = new ConcurrentHashMap<>();

        Properties props = consumerProps("f18-key-" + System.currentTimeMillis());
        AtomicInteger processed = new AtomicInteger();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(TOPIC));
            int empty = 0;
            while (empty < maxEmptyPolls) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    empty++;
                    continue;
                }
                empty = 0;
                List<CompletableFuture<Void>> batchFutures = new ArrayList<>(records.count());
                for (ConsumerRecord<String, String> r : records) {
                    String key = r.key() == null ? "__null__" : r.key();
                    int lane = Math.floorMod(key.hashCode(), parallelism);
                    // Chain onto the lane tail: preserves order for the same lane; same key => same lane.
                    CompletableFuture<Void> next = laneTails[lane].thenRunAsync(() -> {
                        int seq = perKeySeq.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
                        handle(r, processed);
                        log.info("[KEY-ORDER] key={} lane={} keySeq={} (same key is sequential on its lane)",
                                key, lane, seq);
                    }, pool);
                    laneTails[lane] = next.exceptionally(ex -> {
                        log.error("Lane processing error key={} lane={}: {}", key, lane, ex.toString());
                        return null;
                    });
                    batchFutures.add(laneTails[lane]);
                }
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();
                // Simple commit after batch barrier. (Per-partition watermark commits are an advanced
                // optimization; see Confluent Parallel Consumer for production-grade handling.)
                Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
                records.partitions().forEach(tp -> {
                    long nextOffset = records.records(tp).get(records.records(tp).size() - 1).offset() + 1;
                    offsets.put(tp, new OffsetAndMetadata(nextOffset));
                });
                consumer.commitSync(offsets);
                log.info("[COMMIT] committed batch offsets after key-ordered parallel completion: {}", offsets);
            }
        } finally {
            shutdown(pool);
        }
        log.info("[SUMMARY] processed={} records in KEY_ORDERED mode", processed.get());
    }

    private static Properties consumerProps(String groupId) {
        Properties props = KafkaConfig.baseConsumerProps(groupId, "f18-client");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "20");
        return props;
    }

    private static void handle(ConsumerRecord<String, String> r, AtomicInteger processed) {
        try {
            // Simulate variable processing cost to make parallelism visible.
            TimeUnit.MILLISECONDS.sleep(60);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int n = processed.incrementAndGet();
        log.info("[WORKER {}] processed #{} partition={} offset={} key={} value={}",
                Thread.currentThread().getName(), n, r.partition(), r.offset(), r.key(), r.value());
        // IMPORTANT: make handlers idempotent in real systems; parallel/at-least-once can redeliver.
    }

    private static void ensureTopic(String topic, int partitions) {
        short rf = (short) Math.max(1, Integer.getInteger("demo.replication.factor", 1));
        try (AdminClient admin = AdminClient.create(KafkaConfig.baseAdminProps())) {
            admin.createTopics(List.of(new NewTopic(topic, partitions, rf))).all().get(15, TimeUnit.SECONDS);
            log.info("[TOPIC] created {} (partitions={}, rf={})", topic, partitions, rf);
        } catch (Exception e) {
            log.info("[TOPIC] '{}' may already exist ({}). Continuing.", topic, e.getMessage());
        }
    }

    private static void shutdown(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
