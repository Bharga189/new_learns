package com.example.kafka.advance.features;

import com.example.kafka.advance.common.AdvancedKafkaConfig;
import com.example.kafka.advance.common.KafkaConfig;
import com.example.kafka.advance.common.TopicUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A07 — Consumer best practices (at-least-once, rebalance safety, poll health, shutdown).
 *
 * <p><b>Practices demonstrated:</b>
 * <ul>
 *   <li><b>Manual commits after processing:</b> {@code enable.auto.commit=false} + {@code commitSync} after the app has handled records.</li>
 *   <li><b>Rebalance listener:</b> commit processed offsets in {@code onPartitionsRevoked} to avoid reprocessing after revocation (cooperative-sticky minimizes churn).</li>
 *   <li><b>Poll loop health:</b> bounded {@code max.poll.records}; keep processing within {@code max.poll.interval.ms} or offload with care.</li>
 *   <li><b>Graceful shutdown:</b> {@code wakeup()} from another thread breaks {@code poll()}, catch {@link WakeupException}, close consumer.</li>
 *   <li><b>Offset reset:</b> explicit {@code auto.offset.reset}.</li>
 * </ul>
 *
 * <p><b>Enhancement note.</b> At-least-once means duplicates are possible (crash after process, before commit).
 * Make handlers <b>idempotent</b> or use dedup (see A09) / transactions (A10) depending on the pipeline.</p>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A07_BestPracticesConsumer
 * }</pre>
 */
public class A07_BestPracticesConsumer {
    private static final Logger log = LoggerFactory.getLogger(A07_BestPracticesConsumer.class);
    private static final String TOPIC = "adv.best.consumer";

    public static void main(String[] args) throws Exception {
        log.info("=== A07 Consumer Best Practices (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        TopicUtils.ensureTopic(TOPIC, 2, (short) 1);

        // Seed some records
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(AdvancedKafkaConfig.durableIdempotentProducer("a07-seed"))) {
            for (int i = 1; i <= 6; i++) {
                producer.send(new ProducerRecord<>(TOPIC, "k" + (i % 2), "msg-" + i)).get();
            }
            producer.flush();
        }

        AtomicBoolean running = new AtomicBoolean(true);
        Map<TopicPartition, OffsetAndMetadata> pendingOffsets = new HashMap<>();

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                AdvancedKafkaConfig.atLeastOnceConsumer("a07-best-group-" + java.util.UUID.randomUUID(), "a07-best-consumer"));

        // Demo graceful shutdown trigger (self-contained): stop after processing target
        Thread stopper = new Thread(() -> {
            try { Thread.sleep(18_000); } catch (InterruptedException ignored) {}
            if (running.getAndSet(false)) {
                log.info("Shutdown requested -> consumer.wakeup()");
                consumer.wakeup();
            }
        }, "a07-stopper");
        stopper.setDaemon(true);
        stopper.start();

        int processed = 0;
        try {
            consumer.subscribe(List.of(TOPIC), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    log.info("[REBALANCE] revoked={} -> committing pending offsets before losing ownership", partitions);
                    if (!pendingOffsets.isEmpty()) {
                        consumer.commitSync(pendingOffsets);
                        pendingOffsets.clear();
                    }
                }
                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    log.info("[REBALANCE] assigned={}", partitions);
                }
            });

            while (running.get() && processed < 6) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : records) {
                    // Process first, then track offset (at-least-once)
                    log.info("[PROCESS] partition={} offset={} key={} value={}", r.partition(), r.offset(), r.key(), r.value());
                    pendingOffsets.put(new TopicPartition(r.topic(), r.partition()),
                            new OffsetAndMetadata(r.offset() + 1, "processed"));
                    processed++;
                }
                if (!records.isEmpty()) {
                    consumer.commitSync(pendingOffsets);
                    pendingOffsets.clear();
                    log.info("[COMMIT] synced after batch; processed={}", processed);
                }
            }
        } catch (WakeupException e) {
            if (running.get()) throw e; // unexpected
            log.info("Woke up for graceful shutdown.");
        } finally {
            try {
                if (!pendingOffsets.isEmpty()) consumer.commitSync(pendingOffsets);
            } finally {
                consumer.close();
                log.info("Consumer closed cleanly. Best practices: process->commit, commit on revoke, wakeup+close on shutdown.");
            }
        }
    }
}
