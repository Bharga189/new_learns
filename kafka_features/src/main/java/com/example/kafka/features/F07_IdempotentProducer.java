package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Feature 07: Idempotent producer — safe retries without duplicate writes.
 *
 * <p><b>Deep dive: the duplicate-write problem.</b> If a producer sends a record, the broker
 * appends it, but the acknowledgment is lost (network error), the producer may retry and
 * create a duplicate. Idempotence solves this for producer-to-broker writes.</p>
 *
 * <p><b>How it works.</b> With {@code enable.idempotence=true} (default in modern Kafka
 * clients when compatible settings are used), the producer is assigned a Producer ID (PID)
 * and each record batch carries a sequence number per partition. The broker tracks the last
 * committed sequence per (PID, partition) and rejects duplicates/out-of-order retries.</p>
 *
 * <p><b>Requirements/implications:</b></p>
 * <ul>
 *   <li>{@code acks} must be {@code all} (idempotence implies strong acking).</li>
 *   <li>{@code retries} should be &gt; 0; {@code max.in.flight.requests.per.connection} must be
 *       &lt;= 5 (and ordering is preserved by the broker with idempotence).</li>
 *   <li>Idempotence guarantees <i>no duplicates per producer session for a partition</i> on
 *       retries. It does not span producer restarts with a new PID, and it does not make
 *       end-to-end consume-process-produce atomic — that requires transactions (F08).</li>
 * </ul>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F07_IdempotentProducer
 * }</pre>
 */
public class F07_IdempotentProducer {
    private static final Logger log = LoggerFactory.getLogger(F07_IdempotentProducer.class);
    private static final String TOPIC = "demo.idempotent";

    public static void main(String[] args) throws Exception {
        Properties props = KafkaConfig.baseProducerProps("f07-idempotent-producer");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // Allow retries; idempotence makes retries safe from duplicate appends.
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        log.info("=== F07 Idempotent Producer ===");
        log.info("idempotence={} acks={} (broker deduplicates retried batches via PID+sequence)",
                props.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG), props.get(ProducerConfig.ACKS_CONFIG));

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 1; i <= 10; i++) {
                var md = producer.send(new ProducerRecord<>(TOPIC, "payment-" + i, "charge-" + i)).get();
                log.info("[SEND] key=payment-{} -> partition={} offset={}", i, md.partition(), md.offset());
            }
            producer.flush();
            log.info("Sent 10 records with idempotence enabled. Retries (if any transport errors occurred) would not create duplicates.");
        }
    }
}
