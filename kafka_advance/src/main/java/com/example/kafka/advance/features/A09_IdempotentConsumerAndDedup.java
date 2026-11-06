package com.example.kafka.advance.features;

import com.example.kafka.advance.common.AdvancedKafkaConfig;
import com.example.kafka.advance.common.KafkaConfig;
import com.example.kafka.advance.common.TopicUtils;
import com.example.kafka.advance.model.OrderEvent;
import com.example.kafka.advance.serde.JacksonJsonDeserializer;
import com.example.kafka.advance.serde.JacksonJsonSerializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A09 — Enhancement: idempotent consumer / deduplication (handle at-least-once duplicates).
 *
 * <p><b>Why.</b> With at-least-once delivery (or producer retries/consumer rebalances), the same
 * logical event can be delivered more than once. If the side effect is not naturally idempotent
 * (e.g., charging a card, incrementing a counter), you must deduplicate or use transactional/outbox patterns.</p>
 *
 * <p><b>Patterns (deep dive):</b>
 * <ul>
 *   <li><b>Event ID dedup:</b> require a stable unique {@code eventId}; store processed IDs (DB unique constraint,
 *       Redis/set with TTL, compacted topic/state store) and skip duplicates.</li>
 *   <li><b>Natural idempotency:</b> upserts by business key, "set status to PAID if not already", etc.</li>
 *   <li><b>Offset+side-effect atomicity:</b> store consumer offset and effect in the same transaction (where possible)
 *       or use the transactional outbox pattern.</li>
 *   <li><b>EOS pipelines:</b> Kafka transactions (A10) help within Kafka consume-transform-produce, but external systems still need idempotency.</li>
 * </ul>
 *
 * <p><b>What this demonstrates.</b> The producer intentionally sends a duplicate {@code eventId}. The
 * consumer uses an in-memory processed-ID set (demo-only; production needs durable/bounded storage)
 * to process the event only once.</p>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A09_IdempotentConsumerAndDedup
 * }</pre>
 */
public class A09_IdempotentConsumerAndDedup {
    private static final Logger log = LoggerFactory.getLogger(A09_IdempotentConsumerAndDedup.class);
    private static final String TOPIC = "adv.dedup.events";

    public static void main(String[] args) throws Exception {
        log.info("=== A09 Idempotent Consumer / Dedup (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        TopicUtils.ensureTopic(TOPIC, 1, (short) 1);

        Properties pProps = AdvancedKafkaConfig.durableIdempotentProducer("a09-p");
        pProps.put("key.serializer", StringSerializer.class.getName());
        pProps.put("value.serializer", JacksonJsonSerializer.class.getName());

        String dupEventId = "EVT-DUP-" + UUID.randomUUID().toString().substring(0, 8);
        try (KafkaProducer<String, OrderEvent> producer = new KafkaProducer<>(pProps);
             KafkaConsumer<String, OrderEvent> consumer = new KafkaConsumer<>(
                     AdvancedKafkaConfig.atLeastOnceConsumer("a09-dedup-g-" + UUID.randomUUID(), "a09-dedup-c"),
                     new StringDeserializer(), new JacksonJsonDeserializer<>(OrderEvent.class))) {

            consumer.subscribe(List.of(TOPIC));

            OrderEvent e1 = new OrderEvent("v1", dupEventId, "ORD-D1", "CHARGED", 77.0, Instant.now());
            OrderEvent e2 = new OrderEvent("v1", UUID.randomUUID().toString(), "ORD-D2", "CREATED", 10.0, Instant.now());
            // Intentional duplicate delivery of the same logical event
            producer.send(new ProducerRecord<>(TOPIC, e1.getOrderId(), e1)).get();
            producer.send(new ProducerRecord<>(TOPIC, e1.getOrderId(), e1)).get(); // duplicate eventId
            producer.send(new ProducerRecord<>(TOPIC, e2.getOrderId(), e2)).get();
            producer.flush();
            log.info("Produced 3 records including a duplicate eventId={} (simulating at-least-once duplicate).", dupEventId);

            // Demo-only dedup store: in production use durable storage (DB unique key, Redis SET+TTL, state store)
            Set<String> processedEventIds = ConcurrentHashMap.newKeySet();
            int applied = 0, skipped = 0, seen = 0;
            long deadline = System.currentTimeMillis() + 15_000;
            while (System.currentTimeMillis() < deadline && seen < 3) {
                ConsumerRecords<String, OrderEvent> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, OrderEvent> r : records) {
                    seen++;
                    OrderEvent evt = r.value();
                    if (evt == null || evt.getEventId() == null) {
                        log.warn("Missing event/value -> route to DLQ in production");
                        continue;
                    }
                    if (!processedEventIds.add(evt.getEventId())) {
                        skipped++;
                        log.warn("[DEDUP-SKIP] duplicate eventId={} orderId={} status={} offset={}", evt.getEventId(), evt.getOrderId(), evt.getStatus(), r.offset());
                        continue;
                    }
                    // Apply side effect exactly once per eventId
                    applied++;
                    log.info("[APPLY] eventId={} orderId={} status={} amount={} (side effect applied once)", evt.getEventId(), evt.getOrderId(), evt.getStatus(), evt.getAmount());
                }
                if (!records.isEmpty()) consumer.commitSync(); // commit after handling; dedup store must be durable for strict guarantees
            }
            log.info("Dedup complete. applied={} skippedDuplicates={}. Note: make dedup store durable/atomic with side effects for strong guarantees.", applied, skipped);
        }
    }
}
