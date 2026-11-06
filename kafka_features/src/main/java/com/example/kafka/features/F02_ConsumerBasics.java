package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * Feature 02: Consumer basics — the poll loop, deserialization, and record metadata.
 *
 * <p><b>Deep dive: how consumers work.</b> Kafka consumers pull data from brokers using a
 * {@code poll()} loop. This design is intentional:</p>
 * <ul>
 *   <li><b>Pull model</b> lets consumers control their rate and batch size, avoiding being
 *       overwhelmed (backpressure by simply polling less often).</li>
 *   <li><b>Consumer groups</b> (see F03): partitions of a topic are distributed across group
 *       members so each partition is processed by exactly one member at a time.</li>
 *   <li><b>Offsets</b> (see F04): the consumer's position in each partition. Kafka tracks
 *       committed offsets so a consumer can resume after restart.</li>
 *   <li><b>Rebalancing</b>: when members join/leave or partitions change, the group
 *       coordinator redistributes partitions. The poll loop is also the consumer's heartbeat
 *       to the group; long processing without polling can trigger a rebalance
 *       ({@code max.poll.interval.ms}).</li>
 * </ul>
 *
 * <p><b>auto.offset.reset</b>: what to do when there is no committed offset for a partition.
 * {@code earliest} reads from the beginning; {@code latest} reads only new records;
 * {@code none} throws if no offset exists.</p>
 *
 * <p><b>Real-time demo:</b> run the producer (F01) in one terminal and this consumer in
 * another to see records flow in near real time.</p>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * # terminal 1 (producer)
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F01_ProducerBasics
 * # terminal 2 (consumer) - reads from earliest for demo visibility
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F02_ConsumerBasics
 * }</pre>
 */
public class F02_ConsumerBasics {
    private static final Logger log = LoggerFactory.getLogger(F02_ConsumerBasics.class);
    private static final String TOPIC = "demo.producer-basics";

    public static void main(String[] args) {
        Properties props = KafkaConfig.baseConsumerProps("f02-consumer-group", "f02-consumer");
        // For a first-time demo, read existing messages too.
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Auto-commit is convenient but has delivery-semantics implications (see F04/F06-delivery docs).
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        int maxEmptyPolls = Integer.getInteger("demo.maxEmptyPolls", 15);
        log.info("=== F02 Consumer Basics (bootstrap={}, topic={}, maxEmptyPolls={}) ===",
                KafkaConfig.bootstrapServers(), TOPIC, maxEmptyPolls);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(TOPIC));
            log.info("Subscribed. Poll loop started — Ctrl+C to stop, or it exits after idle polls.");

            int emptyPolls = 0;
            while (emptyPolls < maxEmptyPolls) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    emptyPolls++;
                    continue;
                }
                emptyPolls = 0;
                for (ConsumerRecord<String, String> r : records) {
                    // Each record carries its position (partition/offset) and producer metadata.
                    log.info("[RECV] topic={} partition={} offset={} key={} timestamp={} value={}",
                            r.topic(), r.partition(), r.offset(), r.key(), r.timestamp(), r.value());
                }
                // With auto-commit enabled, offsets are committed periodically in the background.
            }
            log.info("No new records after {} empty polls; exiting.", maxEmptyPolls);
        }
    }
}
