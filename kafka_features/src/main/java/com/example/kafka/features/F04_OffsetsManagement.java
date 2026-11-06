package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Feature 04: Offsets — auto vs manual commit, seek, and reset semantics.
 *
 * <p><b>Deep dive: offsets.</b> An offset is a monotonically increasing position of a record
 * within a partition. Kafka itself is an append-only log; consumers track progress by
 * committing offsets (stored in the internal {@code __consumer_offsets} topic for groups).</p>
 *
 * <ul>
 *   <li><b>Auto commit</b> ({@code enable.auto.commit=true}): the client commits offsets
 *       periodically. Simple, but if processing happens after the commit point is advanced and
 *       the consumer crashes, records may be skipped (at-most-once risk for processing).
 *       Conversely, committing too infrequently can cause reprocessing (at-least-once).</li>
 *   <li><b>Manual commit</b>: commit after processing. {@code commitSync()} blocks until the
 *       broker confirms; {@code commitAsync()} does not block but needs careful error handling.
 *       Committing specific offsets (e.g., {@code record.offset()+1}) gives fine-grained
 *       control.</li>
 *   <li><b>Seek</b>: {@code seek(partition, offset)} moves the fetch position — useful for
 *       replay, skipping bad records, or time-based reprocessing (with offsets-for-times).</li>
 *   <li><b>auto.offset.reset</b>: applies only when no committed offset exists:
 *       {@code earliest}, {@code latest}, or {@code none}.</li>
 * </ul>
 *
 * <p><b>Delivery semantics tie-in:</b> manual commit after successful processing is the basis
 * for at-least-once processing. Exactly-once requires transactions or idempotent sinks (see
 * F07/F08).</p>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F04_OffsetsManagement
 * }</pre>
 */
public class F04_OffsetsManagement {
    private static final Logger log = LoggerFactory.getLogger(F04_OffsetsManagement.class);
    private static final String TOPIC = "demo.offsets";

    public static void main(String[] args) throws Exception {
        seedData();

        Properties props = KafkaConfig.baseConsumerProps("f04-manual-offset-group", "f04-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Disable auto commit to demonstrate explicit control.
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5");

        log.info("=== F04 Offsets Management (manual commit + seek) ===");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(TOPIC));

            // First pass: process a batch and manually commit the next offset to read.
            boolean processedBatch = false;
            TopicPartition firstPartition = null;
            long firstOffsetSeen = -1;
            int attempts = 0;
            while (!processedBatch && attempts < 20) {
                attempts++;
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> r : records) {
                    if (firstPartition == null) {
                        firstPartition = new TopicPartition(r.topic(), r.partition());
                        firstOffsetSeen = r.offset();
                    }
                    log.info("[PROCESS] partition={} offset={} value={}", r.partition(), r.offset(), r.value());
                    // Commit the offset AFTER processing this record: next read starts at offset+1.
                    consumer.commitSync(Map.of(
                            new TopicPartition(r.topic(), r.partition()),
                            new OffsetAndMetadata(r.offset() + 1, "processed")));
                    processedBatch = true;
                    break; // handle one record to keep the demo deterministic
                }
            }

            if (firstPartition != null) {
                log.info("Committed past offset {}. Now seeking back to {} to replay (demo of seek).",
                        firstOffsetSeen, firstOffsetSeen);
                // Seek repositions the consumer without changing committed offsets by itself.
                consumer.seek(firstPartition, firstOffsetSeen);
                ConsumerRecords<String, String> replayed = consumer.poll(Duration.ofMillis(2000));
                replayed.forEach(r -> log.info("[REPLAY] partition={} offset={} value={}",
                        r.partition(), r.offset(), r.value()));
            } else {
                log.warn("No records received. Ensure the single-broker Podman setup is running.");
            }
        }
    }

    private static void seedData() throws Exception {
        Properties p = KafkaConfig.baseProducerProps("f04-seeder");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(p)) {
            for (int i = 1; i <= 5; i++) {
                producer.send(new ProducerRecord<>(TOPIC, "k" + i, "offset-demo-" + i)).get();
            }
            producer.flush();
            log.info("Seeded {} records to {}", 5, TOPIC);
        }
    }
}
