package com.example.kafka.advance.common;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Small AdminClient helpers for demo setup (topic creation).
 *
 * <p><b>Deep dive.</b> In production, topics are usually managed via IaC/GitOps with explicit
 * partitions, replication factor, {@code min.insync.replicas}, retention/compaction, and quotas.
 * Demos create topics on-demand for convenience; {@code auto.create.topics.enable} may also create
 * them, but explicit creation makes config visible and deterministic.</p>
 */
public final class TopicUtils {

    private TopicUtils() {
    }

    /**
     * Ensures a topic exists with the given partition count and replication factor.
     * Safe to call if the topic already exists (idempotent for demos).
     */
    public static void ensureTopic(String topic, int partitions, short replicationFactor) {
        ensureTopic(topic, partitions, replicationFactor, Map.of());
    }

    /**
     * Ensures a topic exists with optional topic configs (e.g. cleanup.policy=compact).
     */
    public static void ensureTopic(String topic, int partitions, short replicationFactor, Map<String, String> configs) {
        try (AdminClient admin = AdminClient.create(KafkaConfig.baseAdminProps())) {
            NewTopic newTopic = new NewTopic(topic, partitions, replicationFactor).configs(configs);
            CreateTopicsResult result = admin.createTopics(List.of(newTopic));
            result.all().get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                return; // ok
            }
            throw new RuntimeException("Failed to create topic " + topic, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted creating topic " + topic, e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timed out creating topic " + topic, e);
        }
    }
}
