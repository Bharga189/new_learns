package com.example.kafka.advance.serde;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * Typed Jackson-based Kafka {@link Deserializer} (UTF-8 JSON bytes → object).
 *
 * <p><b>Variation:</b> fixed target type per topic/consumer config. Simple and explicit; best when
 * one topic carries one schema/type. For polymorphic topics, see {@code MultiTypeJsonDeserializer}.</p>
 *
 * @param <T> target type
 */
public class JacksonJsonDeserializer<T> implements Deserializer<T> {

    private final Class<T> targetType;

    public JacksonJsonDeserializer(Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            return JsonMapper.mapper().readValue(data, targetType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize JSON from topic " + topic + " to " + targetType.getName(), e);
        }
    }

    @Override
    public T deserialize(String topic, Headers headers, byte[] data) {
        // Headers available for type/schema resolution if needed.
        return deserialize(topic, data);
    }

    @Override
    public void close() {
    }
}
