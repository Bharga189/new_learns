package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Feature 08: Transactions and exactly-once semantics (EOS) for consume-transform-produce.
 *
 * <p><b>Deep dive: what "exactly once" means in Kafka.</b> Kafka provides exactly-once
 * <i>processing semantics</i> for read-process-write workflows when using transactions:</p>
 * <ol>
 *   <li>The producer starts a transaction ({@code initTransactions()}, {@code beginTransaction()}).</li>
 *   <li>It writes output records and sends the consumer's offsets to the transaction
 *       ({@code sendOffsetsToTransaction}).</li>
 *   <li>It commits or aborts the transaction atomically. Either all output records and the
 *       offset commit become visible together, or none do.</li>
 * </ol>
 *
 * <p><b>Isolation.</b> Consumers must use {@code isolation.level=read_committed} to avoid
 * seeing records from aborted/open transactions. {@code read_uncommitted} (default) sees all
 * records.</p>
 *
 * <p><b>Requirements:</b></p>
 * <ul>
 *   <li>Producer needs a stable {@code transactional.id} (enables fencing of zombie producers).</li>
 *   <li>Idempotence is implicitly enabled for transactional producers.</li>
 *   <li>Broker must have transaction log configured (the single-broker Podman compose sets
 *       replication/min ISR appropriately for local use).</li>
 * </ul>
 *
 * <p><b>Scope note:</b> EOS here covers Kafka-internal atomicity. Writing to an external
 * database requires an idempotent consumer or an outbox/change-data-capture pattern.</p>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F08_TransactionsEOS
 * }</pre>
 */
public class F08_TransactionsEOS {
    private static final Logger log = LoggerFactory.getLogger(F08_TransactionsEOS.class);
    private static final String INPUT = "demo.tx.input";
    private static final String OUTPUT = "demo.tx.output";

    public static void main(String[] args) throws Exception {
        seedInput();

        Properties producerProps = KafkaConfig.baseProducerProps("f08-tx-producer");
        producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "f08-tx-producer-1");
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        Properties consumerProps = KafkaConfig.baseConsumerProps("f08-tx-consumer-group", "f08-tx-consumer");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        // Critical for EOS: only read committed transactional records.
        consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        log.info("=== F08 Transactions / EOS (consume-transform-produce) ===");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
             KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {

            producer.initTransactions();
            consumer.subscribe(List.of(INPUT));

            int processed = 0;
            int attempts = 0;
            while (processed < 3 && attempts < 15) {
                attempts++;
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    continue;
                }
                try {
                    producer.beginTransaction();
                    Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();
                    for (var r : records) {
                        String transformed = r.value() == null ? null : r.value().toUpperCase() + "::PROCESSED";
                        producer.send(new ProducerRecord<>(OUTPUT, r.key(), transformed));
                        offsetsToCommit.put(new TopicPartition(r.topic(), r.partition()),
                                new OffsetAndMetadata(r.offset() + 1));
                        log.info("[TX] consumed {} -> produced {} (partition={} offset={})",
                                r.value(), transformed, r.partition(), r.offset());
                        processed++;
                        if (processed >= 3) break;
                    }
                    // Atomically commit produced records AND consumer offsets.
                    producer.sendOffsetsToTransaction(offsetsToCommit, consumer.groupMetadata());
                    producer.commitTransaction();
                    log.info("[TX] committed transaction (outputs + offsets visible atomically)");
                } catch (Exception e) {
                    log.error("[TX] aborting transaction due to error", e);
                    producer.abortTransaction();
                    throw e;
                }
            }
        }

        readCommittedOutput();
    }

    private static void seedInput() throws Exception {
        Properties p = KafkaConfig.baseProducerProps("f08-seeder");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(p)) {
            for (int i = 1; i <= 3; i++) {
                producer.send(new ProducerRecord<>(INPUT, "k" + i, "event-" + i)).get();
            }
        }
        log.info("Seeded input topic {}", INPUT);
    }

    private static void readCommittedOutput() {
        Properties props = KafkaConfig.baseConsumerProps("f08-verify-group-" + System.currentTimeMillis(), "f08-verifier");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(OUTPUT));
            int attempts = 0;
            while (attempts < 10) {
                attempts++;
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (!records.isEmpty()) {
                    records.forEach(r -> log.info("[READ_COMMITTED OUTPUT] key={} value={} offset={}",
                            r.key(), r.value(), r.offset()));
                    return;
                }
            }
            log.warn("No committed output records observed yet.");
        }
    }
}
