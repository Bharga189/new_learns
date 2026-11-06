package com.example.kafka.advance.common;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;
import java.util.UUID;

/**
 * Best-practice-oriented Kafka client configuration presets (with rationale in JavaDoc).
 *
 * <p><b>Important:</b> "Best practice" depends on workload (latency vs throughput vs durability),
 * cluster topology, and failure model. These presets are safe, explainable starting points — tune
 * with metrics and load tests. Demos in this project use them to make trade-offs visible.</p>
 *
 * <h2>Producer best practices (summary)</h2>
 * <ul>
 *   <li><b>Durability:</b> {@code acks=all} + replication/min.insync.replicas on the broker/topic.</li>
 *   <li><b>Safety on retry:</b> {@code enable.idempotence=true} (default in modern clients) prevents duplicates from producer retries.</li>
 *   <li><b>Bounded time:</b> set {@code delivery.timeout.ms} and understand it bounds retries+linger; tune {@code request.timeout.ms}/{@code linger.ms} coherently.</li>
 *   <li><b>Throughput:</b> batching ({@code linger.ms}, {@code batch.size}) and {@code compression.type} (lz4/zstd common).</li>
 *   <li><b>Ordering:</b> idempotence preserves per-partition order with retries (within limits); keys pin ordering to a partition.</li>
 *   <li><b>Lifecycle:</b> always {@code close()} (flushes) on shutdown; use callbacks/metrics to observe errors.</li>
 * </ul>
 *
 * <h2>Consumer best practices (summary)</h2>
 * <ul>
 *   <li><b>Poll loop health:</b> keep polling; if processing is slow, reduce {@code max.poll.records}, increase {@code max.poll.interval.ms} carefully, or offload work with explicit backpressure.</li>
 *   <li><b>Commits:</b> for at-least-once, commit offsets <em>after</em> successful processing; use {@code enable.auto.commit=false} when you need control.</li>
 *   <li><b>Rebalances:</b> cooperative-sticky assignor reduces stop-the-world; commit processed offsets in {@code onPartitionsRevoked}.</li>
 *   <li><b>Reset policy:</b> set {@code auto.offset.reset} intentionally (earliest/latest/none) per use case.</li>
 *   <li><b>Lifecycle:</b> close consumer gracefully; use wakeup for shutdown from another thread.</li>
 * </ul>
 */
public final class AdvancedKafkaConfig {

    private AdvancedKafkaConfig() {
    }

    /**
     * Durable, idempotent producer preset (good default for correctness-sensitive workloads).
     * <p>Trade-off: {@code acks=all} + idempotence can add latency vs {@code acks=1}, but avoids many duplicate/loss scenarios.</p>
     */
    public static Properties durableIdempotentProducer(String clientId) {
        Properties props = KafkaConfig.baseProducerProps(clientId);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        // With idempotence, retries must be > 0 and max.in.flight.requests.per.connection <= 5 (client enforces safe defaults).
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "120000"); // upper bound for send+retries
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
        props.put(ProducerConfig.LINGER_MS_CONFIG, "10");   // small batching window
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, "32768");
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        return props;
    }

    /**
     * Manual-commit consumer preset for at-least-once processing (commit after work).
     */
    public static Properties atLeastOnceConsumer(String groupId, String clientId) {
        Properties props = KafkaConfig.baseConsumerProps(groupId, clientId != null ? clientId : "consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100"); // keep batches manageable
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000");
        // Cooperative rebalancing reduces unnecessary revocations (Kafka 2.4+ / modern brokers)
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");
        return props;
    }

    /**
     * Transactional producer preset for exactly-once consume-transform-produce pipelines.
     * <p>Requires a stable {@code transactional.id} per producer instance identity and
     * {@code read_committed} consumers on the output side.</p>
     */
    public static Properties transactionalProducer(String transactionalId, String clientId) {
        Properties props = durableIdempotentProducer(clientId);
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId);
        // Idempotence is implied/required for transactions in modern Kafka clients.
        return props;
    }
}
