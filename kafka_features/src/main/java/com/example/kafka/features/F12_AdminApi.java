package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Feature 12: Kafka Admin API — programmatic topic/cluster management.
 *
 * <p><b>Deep dive: operational control plane.</b> The AdminClient talks to the Kafka
 * controller/brokers to manage metadata and configuration. Common uses:</p>
 * <ul>
 *   <li><b>Topic lifecycle</b>: create, delete, list, describe topics; choose partition count
 *       and replication factor at creation. Partition count can usually be increased later, but
 *       decreasing is not supported and key-based ordering stability is affected by increases.</li>
 *   <li><b>Configuration</b>: inspect/alter topic and broker configs (retention, compaction,
 *       min ISR, etc.). Some configs are dynamic; others require broker-level changes.</li>
 *   <li><b>Cluster inspection</b>: describe cluster, brokers, consumer groups, offsets/lag
 *       (via relevant admin calls) — essential for observability and automation.</li>
 *   <li><b>Safety</b>: admin operations affect production behavior. Use least privilege,
 *       change control, and idempotent automation (handle "topic already exists").</li>
 * </ul>
 *
 * <p><b>Replication factor & ISR (context):</b> replication factor N means N copies of each
 * partition across brokers. The leader serves reads/writes (by default); followers replicate.
 * <i>In-sync replicas</i> (ISR) are replicas caught up enough to be eligible for leader
 * election and to satisfy {@code acks=all} when {@code min.insync.replicas} is met. Use the
 * 3-broker Podman cluster to observe real replication behavior.</p>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * # single broker (replication factor 1)
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F12_AdminApi
 *
 * # multi-broker: see RF=3 placement
 * cd podman/cluster-3-broker && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F12_AdminApi \
 *   -Dbootstrap.servers=localhost:19092,localhost:29092,localhost:39092
 * }</pre>
 */
public class F12_AdminApi {
    private static final Logger log = LoggerFactory.getLogger(F12_AdminApi.class);

    public static void main(String[] args) throws Exception {
        log.info("=== F12 Admin API (bootstrap={}) ===", KafkaConfig.bootstrapServers());

        try (AdminClient admin = AdminClient.create(KafkaConfig.baseAdminProps())) {
            logCluster(admin);
            createAndDescribeTopic(admin);
            listTopics(admin);
        }
    }

    private static void logCluster(AdminClient admin) throws Exception {
        var cluster = admin.describeCluster();
        String clusterId = cluster.clusterId().get(10, TimeUnit.SECONDS);
        Node controller = cluster.controller().get(10, TimeUnit.SECONDS);
        var nodes = cluster.nodes().get(10, TimeUnit.SECONDS);
        log.info("ClusterId={} controller={} nodeCount={}", clusterId, controller, nodes.size());
        nodes.forEach(n -> log.info("  node id={} host={} port={} rack={}", n.id(), n.host(), n.port(), n.rack()));
    }

    private static void createAndDescribeTopic(AdminClient admin) throws Exception {
        String topicName = "demo.admin-managed";
        short rf = KafkaConfig.bootstrapServers().contains(",") ? (short) 3 : (short) 1;
        NewTopic newTopic = new NewTopic(topicName, 3, rf)
                .configs(Map.of(
                        TopicConfig.RETENTION_MS_CONFIG, "600000",
                        TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE
                ));
        log.info("Creating topic {} partitions=3 replicationFactor={} (RF limited by broker count/cluster)", topicName, rf);
        CreateTopicsResult result = admin.createTopics(List.of(newTopic));
        try {
            result.all().get(15, TimeUnit.SECONDS);
            log.info("Topic created.");
        } catch (Exception e) {
            log.info("Create topic result: {} (often means it already exists)", e.getMessage());
        }

        DescribeTopicsResult desc = admin.describeTopics(List.of(topicName));
        TopicDescription td = desc.allTopicNames().get(15, TimeUnit.SECONDS).get(topicName);
        log.info("Describe topic='{}' partitions={} internal={}", td.name(), td.partitions().size(), td.isInternal());
        td.partitions().forEach(p -> log.info(
                "  partition={} leader={} replicas={} isr={}", p.partition(), p.leader(), p.replicas(), p.isr()));
    }

    private static void listTopics(AdminClient admin) throws Exception {
        var names = admin.listTopics(new ListTopicsOptions().listInternal(false)).names().get(10, TimeUnit.SECONDS);
        log.info("User topics (sample up to 15): {}", names.stream().limit(15).toList());
    }
}
