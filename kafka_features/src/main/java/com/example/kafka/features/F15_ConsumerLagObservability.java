package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Feature 15: Consumer lag and observability — measuring backlog and understanding health signals.
 *
 * <p><b>Deep dive: consumer lag.</b> Lag is the gap between the latest records in a partition
 * (log-end offset) and the consumer group's committed position. It indicates how far behind
 * consumers are. Lag is normal in bursts, but sustained growth usually means consumers cannot keep
 * up (slow processing, insufficient instances/partitions, rebalances, errors) or are stuck.</p>
 *
 * <ul>
 *   <li><b>Log-end offset (LEO):</b> offset of the next record to be written (high watermark/end
 *       from a producer perspective varies by API; admin/consumer end offsets are commonly used).</li>
 *   <li><b>Committed offset:</b> the group's checkpoint in {@code __consumer_offsets}. Lag is
 *       typically computed as {@code endOffset - committedOffset} per partition.</li>
 *   <li><b>Operational signals:</b> lag, commit rate, poll/processing latency, rebalance frequency,
 *       error/DLQ rates, broker/controller health, ISR shrinkage, under-replicated partitions.</li>
 *   <li><b>Tooling:</b> Kafka metrics (JMX), AdminClient, {@code kafka-consumer-groups.sh}, and
 *       observability platforms. This demo uses AdminClient APIs for a local, code-driven check.</li>
 * </ul>
 *
 * <p><b>Realtime behavior.</b> The demo produces a burst of records, consumes/commits part of them,
 * and prints per-partition lag so you can see the backlog decrease as commits advance.</p>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F15_ConsumerLagObservability
 * podman compose down -v
 * }</pre>
 */
public class F15_ConsumerLagObservability {
    private static final Logger log = LoggerFactory.getLogger(F15_ConsumerLagObservability.class);
    private static final String TOPIC = "demo.observability.lag";
    private static final String GROUP_ID = "f15-lag-group-" + UUID.randomUUID();

    public static void main(String[] args) throws Exception {
        log.info("=== F15 Consumer Lag & Observability (bootstrap={}) ===", KafkaConfig.bootstrapServers());

        Properties producerProps = KafkaConfig.baseProducerProps("f15-lag-producer");
        Properties consumerProps = KafkaConfig.baseConsumerProps(GROUP_ID, "f15-lag-consumer");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
             KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
             AdminClient admin = AdminClient.create(KafkaConfig.baseAdminProps())) {

            // Produce a small backlog.
            for (int i = 1; i <= 12; i++) {
                producer.send(new ProducerRecord<>(TOPIC, "k" + (i % 3), "event-" + i)).get();
            }
            producer.flush();
            log.info("Produced 12 records to {}", TOPIC);

            consumer.subscribe(List.of(TOPIC));
            // Ensure assignment.
            consumer.poll(Duration.ofMillis(1000));
            consumer.poll(Duration.ofMillis(1000));
            List<TopicPartition> partitions = consumer.assignment().stream().toList();
            if (partitions.isEmpty()) {
                // Fallback: assume partition 0 for demo visibility.
                partitions = List.of(new TopicPartition(TOPIC, 0));
                consumer.assign(partitions);
            }
            log.info("Assigned partitions: {}", partitions);

            printLag(admin, consumer, partitions, "before consuming");

            // Consume and commit only part of the backlog to leave visible lag.
            int processed = 0;
            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline && processed < 5) {
                var records = consumer.poll(Duration.ofMillis(500));
                for (var r : records) {
                    log.info("[PROCESS] partition={} offset={} value={}", r.partition(), r.offset(), r.value());
                    processed++;
                    if (processed >= 5) break;
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
            printLag(admin, consumer, partitions, "after partial commit");

            log.info("Observability takeaway: track lag per partition, alert on sustained growth, and correlate with throughput/errors/rebalances.");
        }
    }

    private static void printLag(AdminClient admin, KafkaConsumer<String, String> consumer,
                                 List<TopicPartition> partitions, String label) throws Exception {
        Map<TopicPartition, OffsetSpec> endSpecs = new HashMap<>();
        for (TopicPartition tp : partitions) {
            endSpecs.put(tp, OffsetSpec.latest());
        }
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> ends =
                admin.listOffsets(endSpecs).all().get(10, TimeUnit.SECONDS);

        log.info("--- Lag snapshot ({}) ---", label);
        for (TopicPartition tp : partitions) {
            long end = ends.containsKey(tp) ? ends.get(tp).offset() : -1L;
            OffsetAndMetadata committed = consumer.committed(tp);
            long committedOffset = committed != null ? committed.offset() : -1L;
            long lag = (end >= 0 && committedOffset >= 0) ? Math.max(0, end - committedOffset)
                    : (end >= 0 ? end : -1L);
            log.info("partition={} endOffset={} committedOffset={} lag={}", tp.partition(), end, committedOffset, lag);
        }
    }
}
