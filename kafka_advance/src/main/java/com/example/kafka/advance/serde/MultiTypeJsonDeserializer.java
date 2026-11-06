package com.example.kafka.advance.serde;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Polymorphic/header-based JSON deserializer (variation for multi-type topics).
 *
 * <p><b>Deep dive.</b> A single topic may carry multiple event types. Common resolution strategies:
 * <ul>
 *   <li><b>Header-driven:</b> producer sets {@code event-type} (and optionally schema/version/id); deserializer routes by header — explicit and efficient.</li>
 *   <li><b>Envelope-driven:</b> parse a small envelope field (type/version) then bind payload.</li>
 *   <li><b>Subject/topic-per-type:</b> simpler contracts (one type per topic) — often preferred.</li>
 * </ul>
 *
 * <p>This implementation resolves by the {@value #HEADER_EVENT_TYPE} header and maps to registered
 * classes; unknown types are returned as a {@link JsonNode} for tolerant handling.</p>
 */
public class MultiTypeJsonDeserializer implements Deserializer<Object> {
    private static final Logger log = LoggerFactory.getLogger(MultiTypeJsonDeserializer.class);
    public static final String HEADER_EVENT_TYPE = "event-type";

    private final Map<String, Class<?>> registry = new HashMap<>();

    public MultiTypeJsonDeserializer register(String eventType, Class<?> type) {
        registry.put(eventType, type);
        return this;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        // Without headers we can only do a best-effort generic parse.
        if (data == null) return null;
        try {
            return JsonMapper.mapper().readTree(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize JSON without type headers", e);
        }
    }

    @Override
    public Object deserialize(String topic, Headers headers, byte[] data) {
        if (data == null) return null;
        String eventType = headerValue(headers, HEADER_EVENT_TYPE);
        Class<?> target = eventType != null ? registry.get(eventType) : null;
        try {
            if (target != null) {
                return JsonMapper.mapper().readValue(data, target);
            }
            if (eventType != null) {
                log.debug("No registered type for event-type='{}' on topic {}; returning JsonNode", eventType, topic);
            }
            return JsonMapper.mapper().readTree(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize polymorphic JSON eventType=" + eventType, e);
        }
    }

    private static String headerValue(Headers headers, String key) {
        if (headers == null) return null;
        Header h = headers.lastHeader(key);
        return h == null || h.value() == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
    }
}
