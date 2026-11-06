package com.example.kafka.advance.features;

import com.example.kafka.advance.common.AdvancedKafkaConfig;
import com.example.kafka.advance.common.KafkaConfig;
import com.example.kafka.advance.common.TopicUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

/**
 * A10 — Exactly-once enhancements: transactional consume-transform-produce pipeline.
 *
 * <p><b>What EOS means (scope).</b> Kafka transactions provide atomicity for <em>Kafka-to-Kafka</em>
 * operations: writes to multiple partitions/topics and consumer offset commits can succeed or abort
 * together. Downstream consumers using {@code isolation.level=read_committed} ignore uncommitted/aborted data.
 * External side effects (DB writes, HTTP calls, emails) are <b>not</b> made exactly-once by Kafka alone —
 * use idempotency, outbox, or coordinated patterns.</p>
 *
 * <p><b>Enhancements/best practices shown:</b>
 * <ul>
 *   <li>Stable {@code transactional.id} (fences zombie producers with same id).</li>
 *   <li>{@code initTransactions()} once per producer instance lifecycle.</li>
 *   <li>{@code sendOffsetsToTransaction(offsets, consumer.group.metadata)} commits consumed offsets atomically with produced records.</li>
 *   <li>{@code read_committed} on the output consumer; input consumer reads source data (typically committed source) and participates via offset commit to transaction.</li>
 *   <li>Abort on processing failure (demo includes a guarded path) — prevents partial output visibility.</li>
 * </ul>
 *
 * <p><b>Operational notes:</b> transactions add coordinator overhead; tune for latency/throughput.
 * Requires broker support and correct replication settings for transaction/offset topics. On single-broker
 * Podman we use RF=1 (demo-safe, not production HA).</p>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A10_ExactlyOnceEnhancements
 * }</pre>
 */
public class A10_ExactlyOnceEnhancements {
    private static final Logger log = LoggerFactory.getLogger(A10_ExactlyOnceEnhancements.class);
    private static final String INPUT = "adv.eos.input";
    private static final String OUTPUT = "adv.eos.output";

    public static void main(String[] args) throws Exception {
        log.info("=== A10 Exactly-Once Enhancements (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        TopicUtils.ensureTopic(INPUT, 1, (short) 1);
        TopicUtils.ensureTopic(OUTPUT, 1, (short) 1);

        // Seed input
        try (KafkaProducer<String, String> seed = new KafkaProducer<>(AdvancedKafkaConfig.durableIdempotentProducer("a10-seed"))) {
            for (int i = 1; i <= 3; i++) seed.send(new ProducerRecord<>(INPUT, "k" + i, "value-" + i)).get();
            seed.flush();
        }

        String groupId = "a10-eos-group-" + UUID.randomUUID();
        Properties cProps = AdvancedKafkaConfig.atLeastOnceConsumer(groupId, "a10-eos-consumer");
        cProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.toString().toLowerCase(Locale.ROOT));

        Properties pProps = AdvancedKafkaConfig.transactionalProducer("a10-tx-producer-" + UUID.randomUUID(), "a10-tx-producer");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(cProps, new StringDeserializer(), new StringDeserializer());
             KafkaProducer<String, String> txProducer = new KafkaProducer<>(pProps, new StringSerializer(), new StringSerializer())) {

            txProducer.initTransactions();
            consumer.subscribe(List.of(INPUT));

            int processed = 0;
            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline && processed < 3) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                if (records.isEmpty()) continue;

                txProducer.beginTransaction();
                try {
                    Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
                    for (ConsumerRecord<String, String> r : records) {
                        String out = "TRANSFORMED(" + r.value() + ")@" + r.offset();
                        txProducer.send(new ProducerRecord<>(OUTPUT, r.key(), out));
                        offsets.put(new TopicPartition(r.topic(), r.partition()), new OffsetAndMetadata(r.offset() + 1));
                        log.info("[TX-IN] {}={} -> [TX-OUT] {}", r.key(), r.value(), out);
                        processed++;
                    }
                    // Atomic: output writes + consumed offsets
                    txProducer.sendOffsetsToTransaction(offsets, consumer.groupMetadata());
                    txProducer.commitTransaction();
                    log.info("[TX-COMMIT] committed {} record(s) atomically (outputs+offsets)", records.count());
                } catch (Exception e) {
                    log.error("[TX-ABORT] processing failed, aborting transaction (no partial read_committed visibility)", e);
                    txProducer.abortTransaction();
                    // Do not commit non-transactional offsets here; records will be reprocessed (handler must be idempotent if needed).
                }
            }

            // Verify read_committed visibility
            readCommittedVerify(groupId);
            log.info("A10 complete. EOS scope: atomic Kafka reads+writes+offset commits; external side effects still require idempotency/outbox.");
        }
    }

    private static void readCommittedVerify(String originalGroup) {
        Properties v = AdvancedKafkaConfig.atLeastOnceConsumer("a10-verify-" + UUID.randomUUID(), "a10-verify-c");
        v.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.toString().toLowerCase(Locale.ROOT));
        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(v, new StringDeserializer(), new StringDeserializer())) {
            c.subscribe(List.of(OUTPUT));
            long deadline = System.currentTimeMillis() + 10_000;
            int n = 0;
            while (System.currentTimeMillis() < deadline && n < 3) {
                ConsumerRecords<String, String> rs = c.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : rs) {
                    log.info("[READ_COMMITTED-VERIFY] partition={} offset={} key={} value={}", r.partition(), r.offset(), r.key(), r.value());
                    n++;
                }
                if (!rs.isEmpty()) c.commitSync();
            }
        }
    }
}
