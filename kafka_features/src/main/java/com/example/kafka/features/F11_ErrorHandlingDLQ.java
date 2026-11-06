package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * Feature 11: Error handling, retries, and the Dead Letter Queue (DLQ) pattern.
 *
 * <p><b>Deep dive: failures are normal in distributed systems.</b> Kafka applications must
 * classify errors and choose a strategy:</p>
 * <ul>
 *   <li><b>Retriable producer errors</b>: transient network/broker issues (leader election,
 *       not-enough-replicas). Configure {@code retries}, {@code retry.backoff.ms}, and
 *       {@code delivery.timeout.ms}. With idempotence/transactions, retries are safer.</li>
 *   <li><b>Non-retriable errors</b>: serialization bugs, authorization failures, invalid topic
 *       config — fix the cause; retrying the same record won't help.</li>
 *   <li><b>Consumer processing errors</b>: a "poison pill" (bad payload) can block a
 *       partition forever if the consumer always retries it. Common strategies: log+skip,
 *       bounded retry, or publish to a <b>Dead Letter Queue</b> topic with metadata (original
 *       topic/partition/offset/error) for later inspection.</li>
 *   <li><b>Offset commit timing</b>: committing before processing risks data loss on crash;
 *       committing after risks duplicates. DLQ helps by making failures explicit without losing
 *       the record.</li>
 * </ul>
 *
 * <p><b>DLQ trade-offs:</b> DLQs preserve evidence but add operational burden (monitoring,
 * replay tooling, schema/compatibility of error metadata). Decide retention and access
 * controls for DLQ topics.</p>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F11_ErrorHandlingDLQ
 * }</pre>
 */
public class F11_ErrorHandlingDLQ {
    private static final Logger log = LoggerFactory.getLogger(F11_ErrorHandlingDLQ.class);
    private static final String INPUT = "demo.dlq.input";
    private static final String DLQ = "demo.dlq.dead-letter";

    public static void main(String[] args) throws Exception {
        seed();

        Properties producerProps = KafkaConfig.baseProducerProps("f11-dlq-producer");
        // Producer retry settings (deep dive): transient failures are retried within delivery timeout.
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, "5");
        producerProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "200");
        producerProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "30000");

        Properties consumerProps = KafkaConfig.baseConsumerProps("f11-dlq-group", "f11-consumer");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        log.info("=== F11 Error Handling + DLQ ===");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
             KafkaProducer<String, String> dlqProducer = new KafkaProducer<>(producerProps)) {
            consumer.subscribe(List.of(INPUT));
            int attempts = 0;
            int handled = 0;
            while (handled < 4 && attempts < 15) {
                attempts++;
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> r : records) {
                    try {
                        process(r);
                        log.info("[OK] processed key={} value={}", r.key(), r.value());
                    } catch (Exception ex) {
                        log.warn("[FAIL] sending to DLQ: key={} value={} error={}", r.key(), r.value(), ex.toString());
                        ProducerRecord<String, String> dead = new ProducerRecord<>(DLQ, r.key(), r.value());
                        dead.headers().add("x-original-topic", r.topic().getBytes());
                        dead.headers().add("x-original-partition", String.valueOf(r.partition()).getBytes());
                        dead.headers().add("x-original-offset", String.valueOf(r.offset()).getBytes());
                        dead.headers().add("x-error", ex.toString().getBytes());
                        dlqProducer.send(dead).get();
                    }
                    handled++;
                }
                if (!records.isEmpty()) {
                    consumer.commitSync(); // commit after handling (including DLQ routing)
                }
            }
            dlqProducer.flush();
        }
        log.info("Demo complete. Inspect topic '{}' for poison-pill records and error headers.", DLQ);
    }

    private static void process(ConsumerRecord<String, String> r) {
        // Simulate a non-retriable business/parse failure for a poison pill.
        if (r.value() != null && r.value().contains("POISON")) {
            throw new IllegalArgumentException("Invalid payload (simulated poison pill)");
        }
    }

    private static void seed() throws Exception {
        Properties p = KafkaConfig.baseProducerProps("f11-seeder");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(p)) {
            producer.send(new ProducerRecord<>(INPUT, "ok-1", "{\"amount\":10}")).get();
            producer.send(new ProducerRecord<>(INPUT, "bad-1", "POISON::{not-json}")).get();
            producer.send(new ProducerRecord<>(INPUT, "ok-2", "{\"amount\":20}")).get();
            producer.send(new ProducerRecord<>(INPUT, "ok-3", "{\"amount\":30}")).get();
        }
        log.info("Seeded input with a poison-pill record");
    }
}
