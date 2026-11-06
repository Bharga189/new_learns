package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Feature 05: Partitioning, message keys, and ordering guarantees.
 *
 * <p><b>Deep dive: partitions are the unit of parallelism and ordering.</b> A topic is split
 * into partitions distributed across brokers. Key facts:</p>
 * <ul>
 *   <li><b>Ordering is guaranteed only within a single partition</b>, not across partitions.
 *       If total ordering matters, use one partition (limits throughput) or design keys so
 *       related events share a partition.</li>
 *   <li><b>Keys</b>: when a record has a non-null key, the default partitioner hashes the key
 *       and maps it to a partition. The same key always routes to the same partition (as long
 *       as the partition count does not change), preserving order for that key (e.g., all
 *       events for {@code orderId=42}).</li>
 *   <li><b>Null keys</b>: modern clients use a "sticky" partitioner that fills one batch/
 *       partition for a period before switching, improving batching efficiency while spreading
 *       load over time.</li>
 *   <li><b>Explicit partition</b>: the producer can set a partition number directly, bypassing
 *       the partitioner — useful but couples the producer to topic layout.</li>
 *   <li><b>Changing partition count</b> breaks key-to-partition stability for hash-based
 *       partitioning; plan partition counts carefully.</li>
 * </ul>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F05_PartitioningKeys
 * }</pre>
 */
public class F05_PartitioningKeys {
    private static final Logger log = LoggerFactory.getLogger(F05_PartitioningKeys.class);
    private static final String TOPIC = "demo.partitioning";

    public static void main(String[] args) throws Exception {
        Properties props = KafkaConfig.baseProducerProps("f05-partitioner");
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            log.info("=== F05 Partitioning & Keys ===");
            log.info("Same key -> same partition (stable ordering per key). Null key -> sticky/hashed distribution.");

            String[] orderIds = {"order-100", "order-200", "order-100", "order-200", "order-100"};
            for (int i = 0; i < orderIds.length; i++) {
                String key = orderIds[i];
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        TOPIC, key, "event-" + i + "-for-" + key);
                RecordMetadata md = producer.send(record).get();
                log.info("[KEYED] key={} -> partition={} offset={} (same key should keep same partition)",
                        key, md.partition(), md.offset());
            }

            for (int i = 1; i <= 4; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, null, "null-key-event-" + i);
                RecordMetadata md = producer.send(record).get();
                log.info("[NULL-KEY] partition={} offset={} (sticky partitioner may batch to one partition)",
                        md.partition(), md.offset());
            }

            // Explicit partition selection (only meaningful if the topic has enough partitions).
            ProducerRecord<String, String> explicit = new ProducerRecord<>(TOPIC, 0, "explicit-key", "pinned-to-partition-0");
            RecordMetadata md = producer.send(explicit).get();
            log.info("[EXPLICIT] requested partition=0 -> actual partition={} offset={}", md.partition(), md.offset());

            producer.flush();
        }
    }
}
