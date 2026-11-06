package com.example.kafka.advance.features;

import com.example.kafka.advance.common.AdvancedKafkaConfig;
import com.example.kafka.advance.common.KafkaConfig;
import com.example.kafka.advance.common.TopicUtils;
import com.example.kafka.advance.model.EventEnvelope;
import com.example.kafka.advance.model.OrderEvent;
import com.example.kafka.advance.serde.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * A02 — Serialization/Deserialization variations: JSON (Jackson).
 *
 * <p><b>Variations demonstrated:</b>
 * <ol>
 *   <li><b>Typed JSON</b>: fixed POJO ({@link OrderEvent}) serializer/deserializer per topic.</li>
 *   <li><b>Envelope JSON</b>: {@link EventEnvelope} carries type/version/ids + a {@link JsonNode} payload (tolerant evolution).</li>
 *   <li><b>Header-based polymorphic</b>: producer sets {@code event-type} header; consumer routes via
 *       {@link MultiTypeJsonDeserializer} to a registered class or falls back to {@link JsonNode}.</li>
 * </ol>
 *
 * <p><b>Deep dive.</b> JSON is flexible and debuggable but lacks a strict broker-enforced contract.
 * For production across teams, pair with schema governance (JSON Schema/Avro/Protobuf + registry),
 * explicit versioning, compatibility tests, and DLQ handling for bad payloads.</p>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A02_SerDeJsonVariations
 * }</pre>
 */
public class A02_SerDeJsonVariations {
    private static final Logger log = LoggerFactory.getLogger(A02_SerDeJsonVariations.class);
    private static final String TYPED_TOPIC = "adv.serde.json.typed";
    private static final String ENVELOPE_TOPIC = "adv.serde.json.envelope";
    private static final String POLY_TOPIC = "adv.serde.json.poly";

    public static void main(String[] args) throws Exception {
        log.info("=== A02 SerDe JSON Variations (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        TopicUtils.ensureTopic(TYPED_TOPIC, 1, (short) 1);
        TopicUtils.ensureTopic(ENVELOPE_TOPIC, 1, (short) 1);
        TopicUtils.ensureTopic(POLY_TOPIC, 1, (short) 1);

        typedVariation();
        envelopeVariation();
        polymorphicVariation();
        log.info("A02 complete. Compared typed, envelope, and header-polymorphic JSON SerDe variations.");
    }

    private static void typedVariation() throws Exception {
        log.info("--- Variation 1: Typed JSON (OrderEvent) ---");
        Properties pProps = AdvancedKafkaConfig.durableIdempotentProducer("a02-typed-p");
        pProps.put("key.serializer", StringSerializer.class.getName());
        pProps.put("value.serializer", JacksonJsonSerializer.class.getName());
        try (KafkaProducer<String, OrderEvent> producer = new KafkaProducer<>(pProps);
             KafkaConsumer<String, OrderEvent> consumer = new KafkaConsumer<>(
                     AdvancedKafkaConfig.atLeastOnceConsumer("a02-typed-g-" + UUID.randomUUID(), "a02-typed-c"),
                     new StringDeserializer(), new JacksonJsonDeserializer<>(OrderEvent.class))) {
            consumer.subscribe(List.of(TYPED_TOPIC));
            OrderEvent evt = new OrderEvent("v1", UUID.randomUUID().toString(), "ORD-T1", "CREATED", 55.5, Instant.now());
            producer.send(new ProducerRecord<>(TYPED_TOPIC, evt.getOrderId(), evt)).get();
            producer.flush();
            pollOne(consumer, r -> log.info("[TYPED] key={} value={}", r.key(), r.value()));
        }
    }

    private static void envelopeVariation() throws Exception {
        log.info("--- Variation 2: Envelope JSON (type/version + JsonNode payload) ---");
        Properties pProps = AdvancedKafkaConfig.durableIdempotentProducer("a02-env-p");
        pProps.put("key.serializer", StringSerializer.class.getName());
        pProps.put("value.serializer", JacksonJsonSerializer.class.getName());
        try (KafkaProducer<String, EventEnvelope> producer = new KafkaProducer<>(pProps);
             KafkaConsumer<String, EventEnvelope> consumer = new KafkaConsumer<>(
                     AdvancedKafkaConfig.atLeastOnceConsumer("a02-env-g-" + UUID.randomUUID(), "a02-env-c"),
                     new StringDeserializer(), new JacksonJsonDeserializer<>(EventEnvelope.class))) {
            consumer.subscribe(List.of(ENVELOPE_TOPIC));
            JsonNode payload = JsonMapper.mapper().readTree("{\"orderId\":\"ORD-E1\",\"status\":\"PAID\",\"amount\":99.0,\"newOptionalField\":\"tolerated\"}");
            EventEnvelope env = new EventEnvelope("order.paid", "v2", UUID.randomUUID().toString(), "ORD-E1", Instant.now(), payload);
            producer.send(new ProducerRecord<>(ENVELOPE_TOPIC, env.getKey(), env)).get();
            producer.flush();
            pollOne(consumer, r -> {
                EventEnvelope e = r.value();
                log.info("[ENVELOPE] type={} version={} eventId={} payload={} (tolerant field={})",
                        e.getEventType(), SchemaEvolutionSupport.versionOf(e), e.getEventId(), e.getPayload(),
                        SchemaEvolutionSupport.textOrDefault(e.getPayload(), "newOptionalField", "<missing>"));
            });
        }
    }

    private static void polymorphicVariation() throws Exception {
        log.info("--- Variation 3: Header-based polymorphic JSON (event-type routing) ---");
        Properties pProps = AdvancedKafkaConfig.durableIdempotentProducer("a02-poly-p");
        pProps.put("key.serializer", StringSerializer.class.getName());
        pProps.put("value.serializer", JacksonJsonSerializer.class.getName());
        MultiTypeJsonDeserializer poly = new MultiTypeJsonDeserializer().register("order.event", OrderEvent.class);
        try (KafkaProducer<String, OrderEvent> producer = new KafkaProducer<>(pProps);
             KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(
                     AdvancedKafkaConfig.atLeastOnceConsumer("a02-poly-g-" + UUID.randomUUID(), "a02-poly-c"),
                     new StringDeserializer(), poly)) {
            consumer.subscribe(List.of(POLY_TOPIC));
            OrderEvent evt = new OrderEvent("v1", UUID.randomUUID().toString(), "ORD-P1", "SHIPPED", 10.0, Instant.now());
            ProducerRecord<String, OrderEvent> rec = new ProducerRecord<>(POLY_TOPIC, evt.getOrderId(), evt);
            rec.headers().add(new RecordHeader(MultiTypeJsonDeserializer.HEADER_EVENT_TYPE, "order.event".getBytes(StandardCharsets.UTF_8)));
            rec.headers().add(new RecordHeader("schema-version", "v1".getBytes(StandardCharsets.UTF_8)));
            producer.send(rec).get();
            producer.flush();
            pollOne(consumer, r -> log.info("[POLYMORPHIC] key={} resolvedType={} value={}", r.key(),
                    r.value() == null ? "null" : r.value().getClass().getSimpleName(), r.value()));
        }
    }

    private interface RecordHandler<V> { void handle(ConsumerRecord<String, V> r); }

    private static <V> void pollOne(KafkaConsumer<String, V> consumer, RecordHandler<V> handler) {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, V> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, V> r : records) {
                handler.handle(r);
                consumer.commitSync();
                return;
            }
        }
        log.warn("Timed out waiting for record");
    }
}
