package com.example.kafka.advance.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Generic Jackson-based Kafka {@link Serializer} (object → UTF-8 JSON bytes).
 *
 * <p><b>Variation:</b> typed/generic JSON serialization. Works with POJOs, records-like classes,
 * maps, and envelopes. Produces human-readable payloads; consider compression ({@code compression.type})
 * and schema governance for production.</p>
 *
 * @param <T> value type
 */
public class JacksonJsonSerializer<T> implements Serializer<T> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Hook: schema registry URL, type headers, etc.
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) return null;
        try {
            return JsonMapper.mapper().writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize JSON for topic " + topic + " type=" + data.getClass().getName(), e);
        }
    }

    @Override
    public byte[] serialize(String topic, Headers headers, T data) {
        // Optional enhancement point: add headers like event-type/schema-version here.
        return serialize(topic, data);
    }

    @Override
    public void close() {
    }
}
