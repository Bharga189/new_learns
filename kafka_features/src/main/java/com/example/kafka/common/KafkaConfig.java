package com.example.kafka.common;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.UUID;

/**
 * Shared Kafka client configuration helpers for the feature demos.
 *
 * <p>Deep dive: Kafka clients (producers, consumers, admin) are configured via plain
 * {@link Properties}. A few settings are fundamental:</p>
 * <ul>
 *   <li><b>bootstrap.servers</b>: initial contact points. The client discovers the full
 *       cluster metadata from them; you do not need to list every broker, but listing
 *       more than one improves resilience during startup.</li>
 *   <li><b>Serializers/Deserializers</b>: Kafka stores bytes. The producer serializer
 *       converts keys/values to bytes; the consumer deserializer reverses it. Mismatched
 *       SerDes cause runtime errors or corrupt reads.</li>
 *   <li><b>client.id</b>: a logical name that appears in broker logs/metrics, useful for
 *       debugging and quotas.</li>
 * </ul>
 *
 * <p>All demos default to the single-broker Podman setup at {@code localhost:9092}.
 * Override with {@code -Dbootstrap.servers=...} or the {@code BOOTSTRAP_SERVERS}
 * environment variable.</p>
 */
public final class KafkaConfig {

    /** Default bootstrap server for the single-broker Podman environment. */
    public static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";

    private KafkaConfig() {
    }

    /**
     * Resolves bootstrap servers from system property, environment, or default.
     *
     * @return bootstrap server list
     */
    public static String bootstrapServers() {
        String fromProp = System.getProperty("bootstrap.servers");
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp;
        }
        String fromEnv = System.getenv("BOOTSTRAP_SERVERS");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return DEFAULT_BOOTSTRAP_SERVERS;
    }

    /**
     * Base producer properties using String key/value serializers.
     *
     * @param clientId logical client id for logs/metrics
     * @return mutable properties
     */
    public static Properties baseProducerProps(String clientId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return props;
    }

    /**
     * Base consumer properties using String key/value deserializers.
     *
     * @param groupId consumer group id (required for group coordination)
     * @param clientId logical client id
     * @return mutable properties
     */
    public static Properties baseConsumerProps(String groupId, String clientId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId != null ? clientId : "consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }

    /**
     * Base AdminClient properties.
     *
     * @return mutable properties
     */
    public static Properties baseAdminProps() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        props.put(AdminClientConfig.CLIENT_ID_CONFIG, "admin-client");
        return props;
    }
}
