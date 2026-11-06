package com.example.kafka.advance.features;

import com.example.kafka.advance.common.AdvancedKafkaConfig;
import com.example.kafka.advance.common.KafkaConfig;
import com.example.kafka.advance.common.TopicUtils;
import com.example.kafka.advance.model.EventEnvelope;
import com.example.kafka.advance.serde.JacksonJsonDeserializer;
import com.example.kafka.advance.serde.JacksonJsonSerializer;
import com.example.kafka.advance.serde.JsonMapper;
import com.example.kafka.advance.serde.SchemaEvolutionSupport;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.UUID;

/**
 * A04 — Schema evolution and versioned SerDe (real-time compatible reading).
 *
 * <p><b>What this demonstrates.</b> Producers emit v1 and v2 envelopes; a single consumer reads both
 * using tolerant patterns (version field, optional fields with defaults, {@link com.fasterxml.jackson.databind.JsonNode}
 * payload inspection). v2 adds {@code currency} and {@code priority} — a backward-compatible additive change.</p>
 *
 * <p><b>Best practices / deep dive:</b>
 * <ul>
 *   <li><b>Compatibility direction:</b> backward (new consumer reads old data), forward (old consumer reads new data),
 *       full (both). Additive optional fields + defaults are the easiest path.</li>
 *   <li><b>Breaking changes:</b> renames, removals, type changes — require new topic/event type/version migration.</li>
 *   <li><b>Contracts:</b> version every event, test compatibility in CI, and use a schema registry where possible.</li>
 *   <li><b>Operational:</b> consumers must handle unknown versions gracefully (log/metric/DLQ), not crash the poll loop.</li>
 * </ul>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A04_SerDeSchemaEvolution
 * }</pre>
 */
public class A04_SerDeSchemaEvolution {
    private static final Logger log = LoggerFactory.getLogger(A04_SerDeSchemaEvolution.class);
    private static final String TOPIC = "adv.serde.evolution";

    public static void main(String[] args) throws Exception {
        log.info("=== A04 SerDe Schema Evolution (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        TopicUtils.ensureTopic(TOPIC, 1, (short) 1);

        Properties pProps = AdvancedKafkaConfig.durableIdempotentProducer("a04-evo-p");
        pProps.put("key.serializer", StringSerializer.class.getName());
        pProps.put("value.serializer", JacksonJsonSerializer.class.getName());

        try (KafkaProducer<String, EventEnvelope> producer = new KafkaProducer<>(pProps);
             KafkaConsumer<String, EventEnvelope> consumer = new KafkaConsumer<>(
                     AdvancedKafkaConfig.atLeastOnceConsumer("a04-evo-g-" + UUID.randomUUID(), "a04-evo-c"),
                     new StringDeserializer(), new JacksonJsonDeserializer<>(EventEnvelope.class))) {

            consumer.subscribe(List.of(TOPIC));

            // v1 payload (no currency/priority)
            ObjectNode p1 = JsonMapper.mapper().createObjectNode()
                    .put("orderId", "ORD-V1").put("status", "CREATED").put("amount", 30.0);
            producer.send(new ProducerRecord<>(TOPIC, "ORD-V1",
                    new EventEnvelope("order.created", "v1", UUID.randomUUID().toString(), "ORD-V1", Instant.now(), p1))).get();

            // v2 payload (additive fields) - older readers should still work if tolerant
            ObjectNode p2 = JsonMapper.mapper().createObjectNode()
                    .put("orderId", "ORD-V2").put("status", "CREATED").put("amount", 45.0)
                    .put("currency", "USD").put("priority", "HIGH");
            producer.send(new ProducerRecord<>(TOPIC, "ORD-V2",
                    new EventEnvelope("order.created", "v2", UUID.randomUUID().toString(), "ORD-V2", Instant.now(), p2))).get();
            producer.flush();
            log.info("Produced v1 and v2 events (v2 adds currency/priority).");

            int seen = 0;
            long deadline = System.currentTimeMillis() + 15_000;
            while (System.currentTimeMillis() < deadline && seen < 2) {
                ConsumerRecords<String, EventEnvelope> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, EventEnvelope> r : records) {
                    EventEnvelope env = r.value();
                    String currency = SchemaEvolutionSupport.textOrDefault(env.getPayload(), "currency", "UNKNOWN(default)");
                    String priority = SchemaEvolutionSupport.textOrDefault(env.getPayload(), "priority", "NORMAL(default)");
                    double amount = SchemaEvolutionSupport.doubleOrDefault(env.getPayload(), "amount", -1);
                    log.info("[READ] version={} type={} key={} amount={} currency={} priority={} (defaults applied when fields missing)",
                            SchemaEvolutionSupport.versionOf(env), env.getEventType(), r.key(), amount, currency, priority);
                    seen++;
                }
                if (!records.isEmpty()) consumer.commitSync();
            }
            log.info("Schema evolution demo complete. Additive changes + tolerant readers + defaults = safer evolution.");
        }
    }
}
