package com.example.kafka.advance.features;

import com.example.kafka.advance.common.AdvancedKafkaConfig;
import com.example.kafka.advance.common.KafkaConfig;
import com.example.kafka.advance.common.TopicUtils;
import com.example.kafka.advance.model.OrderEvent;
import com.example.kafka.advance.serde.JacksonJsonDeserializer;
import com.example.kafka.advance.serde.JacksonJsonSerializer;
import com.example.kafka.advance.serde.SafeDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
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
 * A05 — SerDe error handling: safe deserialization, poison pills, and DLQ routing.
 *
 * <p><b>Problem.</b> A malformed record ("poison pill") can throw during deserialization and break
 * the consumer poll loop, blocking progress on a partition.</p>
 *
 * <p><b>Enhancement shown.</b> {@link SafeDeserializer} wraps the real deserializer and returns
 * {@code null} on failure (logging a preview). The application detects null/bad records and publishes
 * the raw payload + error context to a DLQ topic, then commits past the poison pill.</p>
 *
 * <p><b>Best practices:</b>
 * <ul>
 *   <li>Capture context in DLQ headers: source topic/partition/offset, key, error, timestamp, consumer group.</li>
 *   <li>Keep raw bytes for replay/forensics (mind PII/secrets policies).</li>
 *   <li>Metric and alert on DLQ volume; treat DLQ as an operational signal.</li>
 *   <li>Make downstream processing idempotent where possible (at-least-once delivery applies).</li>
 * </ul>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A05_SerDeErrorHandling
 * }</pre>
 */
public class A05_SerDeErrorHandling {
    private static final Logger log = LoggerFactory.getLogger(A05_SerDeErrorHandling.class);
    private static final String TOPIC = "adv.serde.errors.input";
    private static final String DLQ = "adv.serde.errors.dlq";

    public static void main(String[] args) throws Exception {
        log.info("=== A05 SerDe Error Handling + DLQ (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        TopicUtils.ensureTopic(TOPIC, 1, (short) 1);
        TopicUtils.ensureTopic(DLQ, 1, (short) 1);

        // Produce one valid JSON event and one poison pill (invalid JSON) as raw bytes.
        Properties rawP = AdvancedKafkaConfig.durableIdempotentProducer("a05-raw-p");
        rawP.put("key.serializer", StringSerializer.class.getName());
        rawP.put("value.serializer", ByteArraySerializer.class.getName());
        try (KafkaProducer<String, byte[]> rawProducer = new KafkaProducer<>(rawP);
             KafkaProducer<String, byte[]> dlqProducer = new KafkaProducer<>(rawP)) {

            byte[] valid = new JacksonJsonSerializer<OrderEvent>().serialize(TOPIC,
                    new OrderEvent("v1", UUID.randomUUID().toString(), "ORD-OK", "CREATED", 12.0, Instant.now()));
            byte[] poison = "THIS IS NOT JSON {{{".getBytes(StandardCharsets.UTF_8);
            rawProducer.send(new ProducerRecord<>(TOPIC, "ORD-OK", valid)).get();
            rawProducer.send(new ProducerRecord<>(TOPIC, "ORD-BAD", poison)).get();
            rawProducer.flush();
            log.info("Produced 1 valid record and 1 poison pill.");

            SafeDeserializer<OrderEvent> safe = new SafeDeserializer<>(new JacksonJsonDeserializer<>(OrderEvent.class));
            try (KafkaConsumer<String, OrderEvent> consumer = new KafkaConsumer<>(
                    AdvancedKafkaConfig.atLeastOnceConsumer("a05-err-g-" + UUID.randomUUID(), "a05-err-c"),
                    new StringDeserializer(), safe)) {
                consumer.subscribe(List.of(TOPIC));

                // We also need raw bytes for DLQ on null; in this demo we re-send a marker/preview by tracking keys.
                // (Production: use a byte[] consumer or ErrorHandlingDeserializer patterns that preserve raw bytes.)
                int seen = 0;
                long deadline = System.currentTimeMillis() + 15_000;
                while (System.currentTimeMillis() < deadline && seen < 2) {
                    ConsumerRecords<String, OrderEvent> records = consumer.poll(Duration.ofMillis(500));
                    for (ConsumerRecord<String, OrderEvent> r : records) {
                        seen++;
                        if (r.value() == null) {
                            log.warn("[POISON->DLQ] key={} partition={} offset={} (value failed deserialization)", r.key(), r.partition(), r.offset());
                            ProducerRecord<String, byte[]> dlqRec = new ProducerRecord<>(DLQ, r.key(),
                                    ("poison-pill:key=" + r.key() + ",partition=" + r.partition() + ",offset=" + r.offset()).getBytes(StandardCharsets.UTF_8));
                            dlqRec.headers().add(new RecordHeader("x-source-topic", TOPIC.getBytes(StandardCharsets.UTF_8)));
                            dlqRec.headers().add(new RecordHeader("x-source-partition", String.valueOf(r.partition()).getBytes(StandardCharsets.UTF_8)));
                            dlqRec.headers().add(new RecordHeader("x-source-offset", String.valueOf(r.offset()).getBytes(StandardCharsets.UTF_8)));
                            dlqRec.headers().add(new RecordHeader("x-error", "deserialization-failed".getBytes(StandardCharsets.UTF_8)));
                            dlqProducer.send(dlqRec).get();
                        } else {
                            log.info("[OK] key={} value={}", r.key(), r.value());
                        }
                    }
                    if (!records.isEmpty()) consumer.commitSync();
                }
                dlqProducer.flush();
                log.info("Error-handling demo complete. Poison pills routed to DLQ with context headers; consumer kept moving.");
            }
        }
    }
}
