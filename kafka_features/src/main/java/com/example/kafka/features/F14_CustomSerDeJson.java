package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Feature 14: Custom serialization/deserialization (SerDe) with JSON — real-time typed events.
 *
 * <p><b>Deep dive: why SerDe matters.</b> Kafka brokers store records as raw bytes. Producers must
 * serialize keys/values; consumers must deserialize them. SerDe choice affects interoperability,
 * schema evolution, payload size, and correctness:</p>
 * <ul>
 *   <li><b>Built-in SerDes</b> (String, Integer, bytes) are simple but provide no structure or
 *       evolution guarantees.</li>
 *   <li><b>JSON</b> is human-readable and flexible, but without a schema registry/compatibility
 *       rules you can break consumers by renaming/removing fields or changing types.</li>
 *   <li><b>Schema-based formats</b> (Avro/Protobuf/JSON Schema with Schema Registry) add explicit
 *       contracts and compatibility checks — recommended for multi-team production systems.</li>
 *   <li><b>Versioning patterns</b> (even with JSON): include a {@code schemaVersion} or event type,
 *       make additive changes when possible, and handle unknown fields/defaults defensively.</li>
 * </ul>
 *
 * <p><b>What this demo shows.</b> A small domain event ({@link OrderEvent}) is serialized to JSON
 * bytes via a custom {@link Serializer} and deserialized via a custom {@link Deserializer}, then
 * produced and consumed in real time. This illustrates the Kafka SerDe contract and where to plug in
 * validation, headers (e.g., schema id/version), or compression decisions.</p>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F14_CustomSerDeJson
 * podman compose down -v
 * }</pre>
 *
 * @see org.apache.kafka.common.serialization.Serializer
 * @see org.apache.kafka.common.serialization.Deserializer
 */
public class F14_CustomSerDeJson {
    private static final Logger log = LoggerFactory.getLogger(F14_CustomSerDeJson.class);
    private static final String TOPIC = "demo.serde.json";

    /**
     * Example domain event. In production, consider explicit versioning and backward-compatible changes.
     */
    public static class OrderEvent {
        public final String schemaVersion;
        public final String orderId;
        public final String status;
        public final double amount;
        public final String createdAt;

        @JsonCreator
        public OrderEvent(
                @JsonProperty("schemaVersion") String schemaVersion,
                @JsonProperty("orderId") String orderId,
                @JsonProperty("status") String status,
                @JsonProperty("amount") double amount,
                @JsonProperty("createdAt") String createdAt) {
            this.schemaVersion = schemaVersion;
            this.orderId = orderId;
            this.status = status;
            this.amount = amount;
            this.createdAt = createdAt;
        }

        @Override
        public String toString() {
            return "OrderEvent{schemaVersion='" + schemaVersion + "', orderId='" + orderId
                    + "', status='" + status + "', amount=" + amount + ", createdAt='" + createdAt + "'}";
        }
    }

    /** Jackson-based serializer: converts objects to UTF-8 JSON bytes. */
    public static class JsonSerializer<T> implements Serializer<T> {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public byte[] serialize(String topic, T data) {
            if (data == null) return null;
            try {
                return mapper.writeValueAsBytes(data);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize to JSON for topic " + topic, e);
            }
        }
    }

    /** Jackson-based deserializer: converts UTF-8 JSON bytes to a typed object. */
    public static class OrderEventDeserializer implements Deserializer<OrderEvent> {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public OrderEvent deserialize(String topic, byte[] data) {
            if (data == null) return null;
            try {
                return mapper.readValue(data, OrderEvent.class);
            } catch (Exception e) {
                // In real systems, route poison pills to a DLQ with the raw payload and error context.
                log.error("Failed to deserialize JSON from topic {}: {}", topic, new String(data, StandardCharsets.UTF_8), e);
                throw new IllegalArgumentException("Invalid JSON payload", e);
            }
        }

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            // Hook for schema registry URLs, subject naming, etc.
        }
    }

    public static void main(String[] args) throws Exception {
        log.info("=== F14 Custom JSON SerDe (bootstrap={}) ===", KafkaConfig.bootstrapServers());

        Properties producerProps = KafkaConfig.baseProducerProps("f14-json-producer");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        Properties consumerProps = KafkaConfig.baseConsumerProps("f14-json-group-" + UUID.randomUUID(), "f14-json-consumer");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, OrderEventDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        String orderId = "order-" + UUID.randomUUID().toString().substring(0, 8);
        try (KafkaProducer<String, OrderEvent> producer = new KafkaProducer<>(producerProps);
             KafkaConsumer<String, OrderEvent> consumer = new KafkaConsumer<>(consumerProps)) {

            consumer.subscribe(List.of(TOPIC));

            // Produce a small realtime sequence of typed events.
            List<OrderEvent> events = List.of(
                    new OrderEvent("v1", orderId, "CREATED", 49.99, Instant.now().toString()),
                    new OrderEvent("v1", orderId, "PAID", 49.99, Instant.now().toString()),
                    new OrderEvent("v1", orderId, "SHIPPED", 49.99, Instant.now().toString())
            );
            for (OrderEvent event : events) {
                producer.send(new ProducerRecord<>(TOPIC, event.orderId, event),
                        (md, ex) -> {
                            if (ex != null) log.error("Send failed", ex);
                            else log.info("[PRODUCED-JSON] key={} partition={} offset={} status={}",
                                    event.orderId, md.partition(), md.offset(), event.status);
                        });
            }
            producer.flush();

            long deadline = System.currentTimeMillis() + 15_000;
            int seen = 0;
            while (System.currentTimeMillis() < deadline && seen < events.size()) {
                ConsumerRecords<String, OrderEvent> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, OrderEvent> r : records) {
                    log.info("[CONSUMED-JSON] partition={} offset={} key={} value={}",
                            r.partition(), r.offset(), r.key(), r.value());
                    seen++;
                }
            }
            log.info("Done. Custom SerDe converted typed events <-> bytes. For multi-team production, prefer schema-governed formats and compatibility policies.");
        }
    }
}
