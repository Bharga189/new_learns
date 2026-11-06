package com.example.kafka.advance.features;

import com.example.kafka.advance.common.AdvancedKafkaConfig;
import com.example.kafka.advance.common.KafkaConfig;
import com.example.kafka.advance.common.TopicUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A06 — Producer best practices (durability, safety, throughput, lifecycle).
 *
 * <p><b>Practices demonstrated/explained:</b>
 * <ul>
 *   <li><b>Idempotence + acks=all:</b> safe retries without duplicates (per partition/producer session) and strong ack durability (with proper broker/topic ISR config).</li>
 *   <li><b>Timeouts:</b> {@code delivery.timeout.ms} bounds total send time; coordinate with {@code request.timeout.ms} and {@code linger.ms}.</li>
 *   <li><b>Batching/compression:</b> {@code linger.ms}/{@code batch.size} + {@code compression.type=lz4} improve throughput/efficiency.</li>
 *   <li><b>Keys:</b> stable keys preserve per-entity ordering; choose keys deliberately (hot keys can skew partitions).</li>
 *   <li><b>Error handling:</b> inspect callback/future exceptions; treat fatal/auth/config errors differently from retriable ones.</li>
 *   <li><b>Lifecycle:</b> {@code flush()} / {@code close()} to avoid losing buffered records on shutdown.</li>
 *   <li><b>Observability:</b> monitor record-send rate/errors/retries, request latency, batch size, buffer availability.</li>
 * </ul>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d && cd ../..
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.example.kafka.advance.features.A06_BestPracticesProducer
 * }</pre>
 */
public class A06_BestPracticesProducer {
    private static final Logger log = LoggerFactory.getLogger(A06_BestPracticesProducer.class);
    private static final String TOPIC = "adv.best.producer";

    public static void main(String[] args) throws Exception {
        log.info("=== A06 Producer Best Practices (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        TopicUtils.ensureTopic(TOPIC, 3, (short) 1);

        Properties props = AdvancedKafkaConfig.durableIdempotentProducer("a06-best-producer");
        log.info("Key configs: acks={}, idempotence={}, compression={}, linger.ms={}, delivery.timeout.ms={}",
                props.get(ProducerConfig.ACKS_CONFIG), props.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG),
                props.get(ProducerConfig.COMPRESSION_TYPE_CONFIG), props.get(ProducerConfig.LINGER_MS_CONFIG),
                props.get(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG));

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            // Demonstrate async sends with callbacks + one sync send; keys for ordering.
            for (int i = 1; i <= 10; i++) {
                String key = "entity-" + (i % 3); // stable key -> stable partition (modulo partitioner behavior)
                String value = "event-" + i + " payload for best-practice demo";
                producer.send(new ProducerRecord<>(TOPIC, key, value), (md, ex) -> {
                    if (ex != null) {
                        log.error("Send failed key={} (check retriable vs fatal; metrics/alerts in production)", key, ex);
                    } else {
                        log.info("[ACK] key={} partition={} offset={} timestamp={}", key, md.partition(), md.offset(), md.timestamp());
                    }
                });
            }
            // Sync example: get() surfaces exceptions (use judiciously; can limit throughput)
            Future<RecordMetadata> f = producer.send(new ProducerRecord<>(TOPIC, "entity-sync", "sync-check-event"));
            RecordMetadata md = f.get(30, TimeUnit.SECONDS);
            log.info("[SYNC-ACK] partition={} offset={}", md.partition(), md.offset());

            producer.flush(); // ensure buffered records sent before close
            log.info("Best-practice producer demo complete. close()/try-with-resources flushes remaining records.");
        }
    }
}
