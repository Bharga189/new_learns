package com.example.kafka.advance.features;

import com.example.kafka.advance.common.AdvancedKafkaConfig;
import com.example.kafka.advance.common.KafkaConfig;
import com.example.kafka.advance.common.TopicUtils;
import com.example.kafka.advance.model.OrderEvent;
import com.example.kafka.advance.serde.JacksonJsonDeserializer;
import com.example.kafka.advance.serde.JacksonJsonSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A01 — Real-time event pipeline (produce → consume → transform output) with best-practice configs.
 *
 * <p><b>What this demonstrates.</b> A small real-time flow: an order-ingest topic receives typed
 * {@link OrderEvent}s; a consumer processes them and emits enriched status events to an output topic.
 * This mirrors common event-driven microservice patterns (ingest, validate/enrich, publish).</p>
 *
 * <p><b>Enhancements / best practices shown:</b>
 * <ul>
 *   <li>Durable idempotent producer ({@code acks=all}, idempotence, compression, bounded delivery timeout).</li>
 *   <li>Manual commits after successful processing (at-least-once baseline).</li>
 *   <li>Keys ({@code orderId}) preserve per-order ordering on a partition.</li>
 *   <li>Bounded poll/record counts for poll-loop health; cooperative-sticky assignor preset.</li>
 *   <li>Explicit topic creation for deterministic demos.</li>
 * </ul>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A01_RealTimeEventPipeline
 * }</pre>
 */
public class A01_RealTimeEventPipeline {
    private static final Logger log = LoggerFactory.getLogger(A01_RealTimeEventPipeline.class);
    private static final String INPUT = "adv.orders.ingest";
    private static final String OUTPUT = "adv.orders.status";

    public static void main(String[] args) throws Exception {
        log.info("=== A01 Real-Time Event Pipeline (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        TopicUtils.ensureTopic(INPUT, 3, (short) 1);
        TopicUtils.ensureTopic(OUTPUT, 3, (short) 1);

        Properties producerProps = AdvancedKafkaConfig.durableIdempotentProducer("a01-pipeline-producer");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class.getName());

        Properties consumerProps = AdvancedKafkaConfig.atLeastOnceConsumer("a01-pipeline-group-" + UUID.randomUUID(), "a01-pipeline-consumer");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // Use explicit instance for typed deserialization
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // placeholder, overridden below

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        AtomicInteger processed = new AtomicInteger();

        try (KafkaProducer<String, OrderEvent> producer = new KafkaProducer<>(producerProps);
             KafkaProducer<String, String> statusProducer = new KafkaProducer<>(AdvancedKafkaConfig.durableIdempotentProducer("a01-status-producer"));
             KafkaConsumer<String, OrderEvent> consumer = new KafkaConsumer<>(
                     AdvancedKafkaConfig.atLeastOnceConsumer("a01-pipeline-group-" + UUID.randomUUID(), "a01-pipeline-consumer"),
                     new StringDeserializer(), new JacksonJsonDeserializer<>(OrderEvent.class))) {

            consumer.subscribe(List.of(INPUT));

            // 1) Produce real-time ingest events
            List<OrderEvent> events = List.of(
                    new OrderEvent("v1", UUID.randomUUID().toString(), orderId, "CREATED", 120.00, Instant.now()),
                    new OrderEvent("v1", UUID.randomUUID().toString(), orderId, "PAYMENT_AUTHORIZED", 120.00, Instant.now()),
                    new OrderEvent("v1", UUID.randomUUID().toString(), orderId, "SHIPPED", 120.00, Instant.now())
            );
            for (OrderEvent e : events) {
                RecordMetadata md = producer.send(new ProducerRecord<>(INPUT, e.getOrderId(), e)).get();
                log.info("[INGEST] key={} status={} -> partition={} offset={}", e.getOrderId(), e.getStatus(), md.partition(), md.offset());
            }
            producer.flush();

            // 2) Consume, transform, emit status, commit after processing
            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline && processed.get() < events.size()) {
                ConsumerRecords<String, OrderEvent> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, OrderEvent> r : records) {
                    OrderEvent in = r.value();
                    log.info("[PROCESS] partition={} offset={} key={} event={}", r.partition(), r.offset(), r.key(), in);
                    // Transform/enrich (real-time processing step)
                    String statusMsg = "orderId=" + in.getOrderId() + " advancedTo=" + in.getStatus() + " at=" + Instant.now();
                    statusProducer.send(new ProducerRecord<>(OUTPUT, in.getOrderId(), statusMsg)).get();
                    log.info("[EMIT-STATUS] {}", statusMsg);
                    processed.incrementAndGet();
                }
                if (!records.isEmpty()) {
                    consumer.commitSync(); // commit after successful processing of the batch
                    log.info("[COMMIT] committed after processing {} record(s) in poll", records.count());
                }
            }
            statusProducer.flush();
            log.info("Pipeline complete. processed={} (at-least-once: commit after processing; idempotent producer reduces duplicates on retry).", processed.get());
        }
    }
}
