package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Feature 09: Log retention and log compaction.
 *
 * <p><b>Deep dive: how Kafka manages log data over time.</b> Kafka stores records in
 * partition logs on disk. Cleanup policy controls what happens when data ages or the log
 * grows:</p>
 * <ul>
 *   <li><b>Retention (delete policy)</b>: segments older than {@code retention.ms} or beyond
 *       {@code retention.bytes} are deleted. This fits event streams where history beyond a
 *       window is not needed.</li>
 *   <li><b>Compaction ({@code cleanup.policy=compact})</b>: Kafka keeps at least the latest
 *       value for each key within a partition (like a changelog/table snapshot). Older values
 *       for the same key become eligible for removal during compaction. A record with a
 *       {@code null} value is a <i>tombstone</i> — it marks the key for deletion (after
 *       {@code delete.retention.ms}).</li>
 *   <li><b>Compact,delete</b>: combines both behaviors.</li>
 * </ul>
 *
 * <p><b>Important compaction details:</b> compaction runs in the background on inactive log
 * segments and is not immediate; the active segment is never compacted. Ordering within a
 * partition is preserved. Compaction requires keys — records without keys cannot be compacted
 * meaningfully.</p>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F09_LogCompactionRetention
 * }</pre>
 * <p>Note: the demo creates a compacted topic and writes multiple versions per key plus a
 * tombstone. Immediate post-write reads may still show older values because compaction is
 * asynchronous; the deep-dive lesson is the configuration and semantics.</p>
 */
public class F09_LogCompactionRetention {
    private static final Logger log = LoggerFactory.getLogger(F09_LogCompactionRetention.class);
    private static final String COMPACTED_TOPIC = "demo.compacted";

    public static void main(String[] args) throws Exception {
        createCompactedTopic();
        produceVersionsAndTombstone();
        consumeSnapshotStyle();
    }

    private static void createCompactedTopic() throws Exception {
        Map<String, String> config = Map.of(
                TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT,
                // Aggressive settings to make compaction eligibility easier in demos (still async).
                TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.01",
                TopicConfig.SEGMENT_MS_CONFIG, "1000",
                TopicConfig.DELETE_RETENTION_MS_CONFIG, "100"
        );
        NewTopic topic = new NewTopic(COMPACTED_TOPIC, 1, (short) 1).configs(config);
        try (AdminClient admin = AdminClient.create(KafkaConfig.baseAdminProps())) {
            try {
                admin.createTopics(List.of(topic)).all().get(15, TimeUnit.SECONDS);
                log.info("Created compacted topic '{}' with cleanup.policy=compact", COMPACTED_TOPIC);
            } catch (Exception e) {
                log.info("Topic '{}' may already exist ({}). Continuing.", COMPACTED_TOPIC, e.getMessage());
            }
            log.info("Deep dive: compaction keeps the latest record per key; null values act as tombstones.");
        }
    }

    private static void produceVersionsAndTombstone() throws Exception {
        Properties props = KafkaConfig.baseProducerProps("f09-compaction-producer");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            log.info("=== Writing multiple versions per key (changelog style) ===");
            send(producer, "user:1", "{\"name\":\"Alice\",\"city\":\"NY\"}");
            send(producer, "user:2", "{\"name\":\"Bob\",\"city\":\"SF\"}");
            send(producer, "user:1", "{\"name\":\"Alice\",\"city\":\"LA\"}"); // update same key
            send(producer, "user:2", "{\"name\":\"Bob\",\"city\":\"Austin\"}");
            // Tombstone: null value means "delete user:1" after compaction/tombstone retention.
            producer.send(new ProducerRecord<>(COMPACTED_TOPIC, "user:1", null)).get();
            log.info("[TOMBSTONE] key=user:1 value=null (marks key for removal by compaction)");
            producer.flush();
        }
    }

    private static void send(KafkaProducer<String, String> producer, String key, String value) throws Exception {
        var md = producer.send(new ProducerRecord<>(COMPACTED_TOPIC, key, value)).get();
        log.info("[WRITE] key={} value={} offset={}", key, value, md.offset());
    }

    private static void consumeSnapshotStyle() {
        Properties props = KafkaConfig.baseConsumerProps("f09-compact-read-" + System.currentTimeMillis(), "f09-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        log.info("=== Reading topic from earliest (current log state; compaction is async) ===");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(COMPACTED_TOPIC));
            int attempts = 0;
            while (attempts < 10) {
                attempts++;
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (!records.isEmpty()) {
                    records.forEach(r -> log.info("[READ] key={} value={} offset={}", r.key(), r.value(), r.offset()));
                    log.info("Interpretation: before compaction you may see all versions; after compaction, latest per key remains (tombstones expire later).");
                    return;
                }
            }
            log.warn("No records read.");
        }
    }
}
