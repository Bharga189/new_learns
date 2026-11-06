package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Feature 10: Producer and consumer interceptors.
 *
 * <p><b>Deep dive: interceptors.</b> Interceptors are pluggable hooks for cross-cutting
 * concerns without changing business logic in every call site. They run inside the client.</p>
 * <ul>
 *   <li><b>{@link ProducerInterceptor}</b>: {@code onSend} can inspect/mutate a record before
 *       it is serialized into the batch (e.g., inject tracing headers, enforce naming,
 *       audit). {@code onAcknowledgement} is called when the broker acknowledges or send fails —
 *       useful for metrics. Exceptions in interceptors should be handled carefully; they can
 *       affect the send path.</li>
 *   <li><b>{@link ConsumerInterceptor}</b>: {@code onConsume} sees records returned by
 *       {@code poll()} before application code processes them; {@code onCommit} observes offset
 *       commits. Useful for metrics, audit, or sampling.</li>
 * </ul>
 *
 * <p><b>Trade-offs:</b> interceptors add latency/complexity in the client hot path. Keep them
 * fast and side-effect aware. For distributed tracing in production, prefer mature
 * instrumentation libraries; this demo shows the mechanism.</p>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F10_Interceptors
 * }</pre>
 */
public class F10_Interceptors {
    private static final Logger log = LoggerFactory.getLogger(F10_Interceptors.class);
    private static final String TOPIC = "demo.interceptors";

    public static class AuditProducerInterceptor implements ProducerInterceptor<String, String> {
        @Override
        public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
            // Non-invasive enrichment: add an audit header before serialization/batching.
            record.headers().add("x-audit", ("producer-interceptor@" + System.currentTimeMillis()).getBytes());
            log.info("[PRODUCER-INTERCEPTOR onSend] topic={} key={}", record.topic(), record.key());
            return record;
        }

        @Override
        public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
            if (exception == null && metadata != null) {
                log.info("[PRODUCER-INTERCEPTOR ack] partition={} offset={}", metadata.partition(), metadata.offset());
            } else {
                log.warn("[PRODUCER-INTERCEPTOR ack] send failed", exception);
            }
        }

        @Override public void close() { }
        @Override public void configure(Map<String, ?> configs) { }
    }

    public static class AuditConsumerInterceptor implements ConsumerInterceptor<String, String> {
        @Override
        public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
            log.info("[CONSUMER-INTERCEPTOR onConsume] count={}", records.count());
            return records;
        }

        @Override
        public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
            log.info("[CONSUMER-INTERCEPTOR onCommit] offsets={}", offsets);
        }

        @Override public void close() { }
        @Override public void configure(Map<String, ?> configs) { }
    }

    public static void main(String[] args) throws Exception {
        Properties pProps = KafkaConfig.baseProducerProps("f10-producer");
        pProps.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, AuditProducerInterceptor.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(pProps)) {
            log.info("=== F10 Interceptors: producing ===");
            producer.send(new ProducerRecord<>(TOPIC, "k1", "intercepted-message")).get();
            producer.flush();
        }

        Properties cProps = KafkaConfig.baseConsumerProps("f10-group-" + System.currentTimeMillis(), "f10-consumer");
        cProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        cProps.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, AuditConsumerInterceptor.class.getName());
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(cProps)) {
            consumer.subscribe(List.of(TOPIC));
            int attempts = 0;
            while (attempts < 10) {
                attempts++;
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (!records.isEmpty()) {
                    records.forEach(r -> {
                        var header = r.headers().lastHeader("x-audit");
                        String audit = header == null ? null : new String(header.value());
                        log.info("[APP] received key={} value={} auditHeader={}", r.key(), r.value(), audit);
                    });
                    consumer.commitSync();
                    return;
                }
            }
        }
    }
}
