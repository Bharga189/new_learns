package com.example.kafka.advance.features;

import com.example.kafka.advance.common.AdvancedKafkaConfig;
import com.example.kafka.advance.common.KafkaConfig;
import com.example.kafka.advance.common.TopicUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * A03 — Serialization/Deserialization variations: String and raw bytes.
 *
 * <p><b>Variations demonstrated:</b>
 * <ol>
 *   <li><b>String SerDe (UTF-8)</b>: simplest human-readable payloads; explicit encoding matters.</li>
 *   <li><b>Byte array SerDe</b>: full control over format (custom binary, compressed blobs, encrypted
 *       payloads, Protobuf/Avro bytes, etc.). Kafka is format-agnostic at the broker.</li>
 *   <li><b>Binary-safe transport pattern</b>: Base64 text wrapping (useful for text-only channels) vs
 *       sending raw bytes directly (preferred when the transport supports bytes).</li>
 * </ol>
 *
 * <p><b>Best practices:</b> define an explicit contract (format + encoding + version). For binary
 * formats, include magic bytes/version or headers. Use {@code compression.type} (lz4/zstd/snappy) for
 * larger payloads — compression is orthogonal to SerDe. Avoid logging sensitive raw payloads.</p>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A03_SerDeBytesAndStringVariations
 * }</pre>
 */
public class A03_SerDeBytesAndStringVariations {
    private static final Logger log = LoggerFactory.getLogger(A03_SerDeBytesAndStringVariations.class);
    private static final String STRING_TOPIC = "adv.serde.string";
    private static final String BYTES_TOPIC = "adv.serde.bytes";

    public static void main(String[] args) throws Exception {
        log.info("=== A03 SerDe String/Bytes Variations (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        TopicUtils.ensureTopic(STRING_TOPIC, 1, (short) 1);
        TopicUtils.ensureTopic(BYTES_TOPIC, 1, (short) 1);

        stringVariation();
        bytesVariation();
        log.info("A03 complete. String = simple/text; Bytes = full control (binary formats, encryption, custom codecs).");
    }

    private static void stringVariation() throws Exception {
        log.info("--- Variation 1: String <-> UTF-8 bytes ---");
        Properties p = AdvancedKafkaConfig.durableIdempotentProducer("a03-str-p");
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(p);
             KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                     AdvancedKafkaConfig.atLeastOnceConsumer("a03-str-g-" + UUID.randomUUID(), "a03-str-c"),
                     new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(STRING_TOPIC));
            String payload = "order=ORD-S1;status=CREATED;amount=42.00;encoding=UTF-8";
            producer.send(new ProducerRecord<>(STRING_TOPIC, "ORD-S1", payload)).get();
            producer.flush();
            pollOnce(consumer, r -> log.info("[STRING] key={} value={} (len={} chars)", r.key(), r.value(), r.value().length()));
        }
    }

    private static void bytesVariation() throws Exception {
        log.info("--- Variation 2: Raw bytes (binary-safe) + Base64 display pattern ---");
        Properties p = AdvancedKafkaConfig.durableIdempotentProducer("a03-bytes-p");
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        p.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4"); // orthogonal to payload format
        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(p);
             KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(
                     AdvancedKafkaConfig.atLeastOnceConsumer("a03-bytes-g-" + UUID.randomUUID(), "a03-bytes-c"),
                     new StringDeserializer(), new ByteArrayDeserializer())) {
            consumer.subscribe(List.of(BYTES_TOPIC));
            // Example: a small custom binary-ish payload (could be Protobuf/Avro/encrypted bytes in real use)
            byte[] binary = ("BINv1|ORD-B1|amount=7.5|" + Instantish.now()).getBytes(StandardCharsets.UTF_8);
            producer.send(new ProducerRecord<>(BYTES_TOPIC, "ORD-B1", binary)).get();
            producer.flush();
            pollOnce(consumer, r -> {
                byte[] v = r.value();
                log.info("[BYTES] key={} size={} bytes base64={} decoded='{}'", r.key(), v.length,
                        Base64.getEncoder().encodeToString(v), new String(v, StandardCharsets.UTF_8));
            });
        }
    }

    private interface Handler<V> { void handle(ConsumerRecord<String, V> r); }
    private static <V> void pollOnce(KafkaConsumer<String, V> consumer, Handler<V> h) {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, V> rs = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, V> r : rs) { h.handle(r); consumer.commitSync(); return; }
        }
        log.warn("Timed out waiting for record");
    }

    /** Tiny helper to avoid extra imports noise in the demo payload. */
    static final class Instantish { static String now() { return java.time.Instant.now().toString(); } }
}
