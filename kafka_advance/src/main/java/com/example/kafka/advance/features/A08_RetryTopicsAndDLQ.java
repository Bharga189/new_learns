package com.example.kafka.advance.features;

import com.example.kafka.advance.common.AdvancedKafkaConfig;
import com.example.kafka.advance.common.KafkaConfig;
import com.example.kafka.advance.common.TopicUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * A08 — Enhancement: retry topics and DLQ pattern (real-time resilient processing).
 *
 * <p><b>Pattern.</b> Transient failures (timeouts, dependency unavailable) can be retried; permanent
 * failures (validation, poison data) should not retry forever. A common design:
 * <pre>
 *   main topic  --(retriable fail)--> retry topic (delayed/backoff) --(max attempts)--> DLQ
 *               --(permanent fail)-----------------------------------------------> DLQ
 *               --(success)--------> commit / downstream
 * </pre>
 *
 * <p><b>What this demonstrates.</b> A consumer processes the main topic. One message is treated as
 * retriable (routed to retry with attempt header) and later "succeeds" on retry; another is routed to
 * DLQ after max attempts/context rules. Headers carry attempt count and error context.</p>
 *
 * <p><b>Best practices:</b> classify errors (retriable vs permanent), cap attempts, apply backoff
 * (delay queues / scheduled retry), preserve ordering implications (retries can reorder unless carefully handled),
 * make handlers idempotent, and monitor retry/DLQ rates. Frameworks (Spring Kafka, custom libs) provide
 * non-blocking retry topic implementations — this demo shows the core mechanics.</p>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A08_RetryTopicsAndDLQ
 * }</pre>
 */
public class A08_RetryTopicsAndDLQ {
    private static final Logger log = LoggerFactory.getLogger(A08_RetryTopicsAndDLQ.class);
    private static final String MAIN = "adv.retry.main";
    private static final String RETRY = "adv.retry.retry-1";
    private static final String DLQ = "adv.retry.dlq";
    private static final String H_ATTEMPT = "x-retry-attempt";
    private static final int MAX_ATTEMPTS = 2;

    public static void main(String[] args) throws Exception {
        log.info("=== A08 Retry Topics + DLQ (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        TopicUtils.ensureTopic(MAIN, 1, (short) 1);
        TopicUtils.ensureTopic(RETRY, 1, (short) 1);
        TopicUtils.ensureTopic(DLQ, 1, (short) 1);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(AdvancedKafkaConfig.durableIdempotentProducer("a08-p"))) {
            producer.send(new ProducerRecord<>(MAIN, "ok-1", "VALID:process-me")).get();
            producer.send(new ProducerRecord<>(MAIN, "retry-1", "TRANSIENT:temporary-db-timeout")).get();
            producer.send(new ProducerRecord<>(MAIN, "bad-1", "PERMANENT:invalid-business-rule")).get();
            producer.flush();

            // Pass 1: main
            processTopic(MAIN, "a08-main-" + UUID.randomUUID(), producer, true);
            // Pass 2: retry (simulate backoff by separate pass; real systems delay/schedule)
            Thread.sleep(1000);
            log.info("--- Processing retry topic (after simulated backoff) ---");
            processTopic(RETRY, "a08-retry-" + UUID.randomUUID(), producer, false);

            log.info("A08 complete. Transient errors retried; permanent/max-attempt failures landed in DLQ (inspect with a DLQ consumer in real ops).");
        }
    }

    private static void processTopic(String topic, String groupId, KafkaProducer<String, String> producer, boolean fromMain) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                AdvancedKafkaConfig.atLeastOnceConsumer(groupId, "a08-c"))) {
            consumer.subscribe(List.of(topic));
            int seen = 0;
            long deadline = System.currentTimeMillis() + 12_000;
            int expected = topic.equals(MAIN) ? 3 : 1;
            while (System.currentTimeMillis() < deadline && seen < expected) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : records) {
                    seen++;
                    String v = r.value();
                    int attempt = attemptOf(r);
                    try {
                        handle(r.key(), v, attempt, topic);
                        log.info("[SUCCESS] topic={} key={} attempt={}", topic, r.key(), attempt);
                    } catch (RetriableException e) {
                        int next = attempt + 1;
                        if (next <= MAX_ATTEMPTS) {
                            log.warn("[RETRY] key={} attempt={} -> {} (simulate backoff in real system)", r.key(), attempt, RETRY);
                            ProducerRecord<String, String> retry = new ProducerRecord<>(RETRY, r.key(), v);
                            retry.headers().add(new RecordHeader(H_ATTEMPT, String.valueOf(next).getBytes(StandardCharsets.UTF_8)));
                            retry.headers().add(new RecordHeader("x-error", e.getMessage().getBytes(StandardCharsets.UTF_8)));
                            producer.send(retry);
                        } else {
                            sendDlq(producer, r, "max-attempts-exceeded: " + e.getMessage());
                        }
                    } catch (PermanentException e) {
                        sendDlq(producer, r, "permanent: " + e.getMessage());
                    }
                }
                if (!records.isEmpty()) { producer.flush(); consumer.commitSync(); }
            }
        }
    }

    private static void handle(String key, String value, int attempt, String topic) {
        if (value.startsWith("PERMANENT:")) throw new PermanentException(value);
        if (value.startsWith("TRANSIENT:") && attempt == 0 && MAIN.equals(topic)) throw new RetriableException(value);
        // On retry pass, treat as recovered.
        if (value.startsWith("TRANSIENT:")) log.info("Recovered transient issue on retry for key={}", key);
    }

    private static void sendDlq(KafkaProducer<String, String> producer, ConsumerRecord<String, String> r, String reason) {
        log.error("[DLQ] key={} reason={}", r.key(), reason);
        ProducerRecord<String, String> dlq = new ProducerRecord<>(DLQ, r.key(), r.value());
        dlq.headers().add(new RecordHeader("x-dlq-reason", reason.getBytes(StandardCharsets.UTF_8)));
        dlq.headers().add(new RecordHeader("x-source-topic", r.topic().getBytes(StandardCharsets.UTF_8)));
        dlq.headers().add(new RecordHeader("x-source-offset", String.valueOf(r.offset()).getBytes(StandardCharsets.UTF_8)));
        producer.send(dlq);
    }

    private static int attemptOf(ConsumerRecord<String, String> r) {
        Header h = r.headers().lastHeader(H_ATTEMPT);
        if (h == null) return 0;
        try { return Integer.parseInt(new String(h.value(), StandardCharsets.UTF_8)); } catch (Exception e) { return 0; }
    }

    static class RetriableException extends RuntimeException { RetriableException(String m) { super(m); } }
    static class PermanentException extends RuntimeException { PermanentException(String m) { super(m); } }
}
