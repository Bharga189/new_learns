package com.example.kafka.advance.serde;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared Jackson {@link ObjectMapper} configuration for SerDe demos.
 *
 * <p><b>Best practices:</b> reuse a configured mapper (thread-safe after config), register Java time
 * module for {@code Instant}/JSR-310 types, write dates as ISO strings (not timestamps) for
 * interoperability, and consider {@code FAIL_ON_UNKNOWN_PROPERTIES=false} for tolerant consumers
 * (balanced against catching contract drift).</p>
 */
public final class JsonMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    private JsonMapper() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
