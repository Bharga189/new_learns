package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Feature 03: Consumer groups and rebalancing.
 *
 * <p><b>Deep dive: consumer groups.</b> A consumer group is a set of consumers sharing a
 * {@code group.id} that cooperatively consume a topic. Kafka assigns each partition to at
 * most one consumer in the group, enabling horizontal scaling:</p>
 * <ul>
 *   <li>More partitions than consumers: some consumers read multiple partitions.</li>
 *   <li>Equal partitions and consumers: one partition per consumer (ideal parallelism).</li>
 *   <li>More consumers than partitions: extra consumers sit idle (no partition assigned).</li>
 * </ul>
 *
 * <p><b>Rebalancing.</b> When group membership or the subscribed partition set changes, the
 * group coordinator triggers a rebalance to redistribute partitions. During an eager
 * rebalance, consumers may briefly stop reading. The cooperative-sticky assignor (default in
 * modern Kafka clients) reduces disruption by revoking/moving only the partitions that must
 * change.</p>
 *
 * <p><b>Partition assignment strategies</b> ({@code partition.assignment.strategy}):</p>
 * <ul>
 *   <li>{@code CooperativeStickyAssignor}: incremental, minimizes movement (recommended).</li>
 *   <li>{@code RangeAssignor}: assigns contiguous partition ranges per topic.</li>
 *   <li>{@code RoundRobinAssignor}: distributes partitions evenly across members.</li>
 * </ul>
 *
 * <p><b>Real-time test (Podman, 3-broker cluster recommended for visibility):</b></p>
 * <pre>{@code
 * cd podman/cluster-3-broker && podman compose up -d
 * # create a topic with several partitions (auto-create may use defaults; Admin API demo shows explicit creation)
 * # terminal 1
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F03_ConsumerGroupsRebalancing \
 *   -Dbootstrap.servers=localhost:19092,localhost:29092,localhost:39092 -Dclient.id=member-1
 * # terminal 2 (same group) - observe rebalance callbacks and partition redistribution
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F03_ConsumerGroupsRebalancing \
 *   -Dbootstrap.servers=localhost:19092,localhost:29092,localhost:39092 -Dclient.id=member-2
 * }</pre>
 */
public class F03_ConsumerGroupsRebalancing {
    private static final Logger log = LoggerFactory.getLogger(F03_ConsumerGroupsRebalancing.class);
    private static final String TOPIC = "demo.groups";

    public static void main(String[] args) {
        String bootstrap = KafkaConfig.bootstrapServers();
        String clientId = System.getProperty("client.id", "member-" + UUID.randomUUID().toString().substring(0, 8));
        Properties props = KafkaConfig.baseConsumerProps("f03-shared-group", clientId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Cooperative sticky assignor minimizes stop-the-world rebalances.
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");

        log.info("=== F03 Consumer Groups (bootstrap={}, clientId={}, group=f03-shared-group) ===",
                bootstrap, clientId);
        log.info("Start another instance with a different -Dclient.id to observe rebalancing.");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(TOPIC), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    // Called before partitions are taken away. Commit offsets here if using manual commits
                    // to avoid reprocessing after rebalance.
                    log.info("[REBALANCE] partitions revoked: {}", partitions);
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    log.info("[REBALANCE] partitions assigned: {}", partitions);
                }

                @Override
                public void onPartitionsLost(Collection<TopicPartition> partitions) {
                    // With cooperative rebalancing, lost partitions were not cleanly revoked in time.
                    log.warn("[REBALANCE] partitions lost: {}", partitions);
                }
            });

            int emptyPolls = 0;
            while (emptyPolls < 30) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    emptyPolls++;
                    if (emptyPolls % 5 == 0) {
                        log.info("Polling... assignment={} (produce to '{}' to see records)",
                                consumer.assignment(), TOPIC);
                    }
                } else {
                    emptyPolls = 0;
                    records.forEach(r -> log.info("[{}] recv partition={} offset={} value={}",
                            clientId, r.partition(), r.offset(), r.value()));
                }
            }
        }
        log.info("Exited after idle period.");
    }
}
