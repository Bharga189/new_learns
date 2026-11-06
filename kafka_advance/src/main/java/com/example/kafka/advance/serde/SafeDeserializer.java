package com.example.kafka.advance.serde;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A defensive {@link Deserializer} wrapper that catches deserialization failures ("poison pills").
 *
 * <p><b>Best practice context.</b> A throwing deserializer can crash the poll loop and stall the
 * consumer. Wrapping allows the application to route bad records to a DLQ, record metrics, and
 * continue processing. The wrapper returns {@code null} on failure; callers must check for null and
 * handle accordingly (inspect raw bytes, headers, offsets).</p>
 *
 * <p><b>Trade-off:</b> returning null can hide data quality issues if not monitored. Always log,
 * metric, and DLQ poison pills. Some teams use {@code ErrorHandlingDeserializer} (Spring Kafka) or
 * Confluent's error-aware serdes in those ecosystems.</p>
 *
 * @param <T> target type
 */
public class SafeDeserializer<T> implements Deserializer<T> {
    private static final Logger log = LoggerFactory.getLogger(SafeDeserializer.class);

    private final Deserializer<T> delegate;

    public SafeDeserializer(Deserializer<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        delegate.configure(configs, isKey);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            return delegate.deserialize(topic, data);
        } catch (Exception e) {
            String preview = safePreview(data);
            log.warn("Poison pill / deserialization failure topic={} bytes={} preview='{}'. Returning null; route to DLQ with context.",
                    topic, data.length, preview, e);
            return null;
        }
    }

    @Override
    public T deserialize(String topic, Headers headers, byte[] data) {
        if (data == null) return null;
        try {
            return delegate.deserialize(topic, headers, data);
        } catch (Exception e) {
            log.warn("Poison pill / deserialization failure topic={} with headers. Returning null.", topic, e);
            return null;
        }
    }

    private static String safePreview(byte[] data) {
        int n = Math.min(data.length, 200);
        String s = new String(data, 0, n, StandardCharsets.UTF_8).replaceAll("[\\r\\n\\t]", " ");
        return data.length > n ? s + "..." : s;
    }

    @Override
    public void close() {
        delegate.close();
    }
}
