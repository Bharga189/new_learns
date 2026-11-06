package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

/**
 * Feature 06: Record headers, timestamps, and metadata.
 *
 * <p><b>Deep dive: record metadata beyond key/value.</b></p>
 * <ul>
 *   <li><b>Headers</b>: ordered key/value pairs (key is a string, value is bytes) carried with
 *       the record. They are ideal for cross-cutting metadata — correlation IDs, tracing
 *       context, schema version, source system — without polluting the business payload or
 *       requiring consumers to deserialize the value just to route/filter.</li>
 *   <li><b>Timestamps</b>: each record has a timestamp. With {@code CreateTime} (default),
 *       the producer sets it (or Kafka uses broker receive time if unset). With
 *       {@code LogAppendTime} (topic config), the broker overwrites it on append — useful when
 *       broker time is the source of truth. Timestamps enable time-based retention and
 *       offsets-for-times lookups.</li>
 *   <li><b>Record coordinates</b>: topic, partition, offset uniquely identify a record within
 *       a partition. Combined with the committed offset, they define consumer progress.</li>
 * </ul>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F06_HeadersAndTimestamps
 * }</pre>
 */
public class F06_HeadersAndTimestamps {
    private static final Logger log = LoggerFactory.getLogger(F06_HeadersAndTimestamps.class);
    private static final String TOPIC = "demo.headers";

    public static void main(String[] args) throws Exception {
        produceWithHeaders();
        consumeAndInspectHeaders();
    }

    private static void produceWithHeaders() throws Exception {
        Properties props = KafkaConfig.baseProducerProps("f06-producer");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            log.info("=== F06 Produce with headers/timestamp ===");
            long eventTime = Instant.now().toEpochMilli();
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    TOPIC, null, eventTime, "order-777", "{\"status\":\"CREATED\",\"amount\":49.99}");
            record.headers()
                    .add(new RecordHeader("correlation-id", "corr-abc-123".getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("schema-version", "v1".getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("source", "checkout-service".getBytes(StandardCharsets.UTF_8)));
            var md = producer.send(record).get();
            log.info("Sent to partition={} offset={} producerTimestamp={}", md.partition(), md.offset(), eventTime);
        }
    }

    private static void consumeAndInspectHeaders() {
        Properties props = KafkaConfig.baseConsumerProps("f06-header-group-" + System.currentTimeMillis(), "f06-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(TOPIC));
            int attempts = 0;
            while (attempts < 10) {
                attempts++;
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (!records.isEmpty()) {
                    records.forEach(r -> {
                        log.info("[RECV] key={} value={} partition={} offset={} timestamp={} timestampType={}",
                                r.key(), r.value(), r.partition(), r.offset(), r.timestamp(), r.timestampType());
                        for (Header h : r.headers()) {
                            String val = h.value() == null ? null : new String(h.value(), StandardCharsets.UTF_8);
                            log.info("       header {}={}", h.key(), val);
                        }
                    });
                    return;
                }
            }
            log.warn("No records received; verify Podman Kafka is running.");
        }
    }
}
