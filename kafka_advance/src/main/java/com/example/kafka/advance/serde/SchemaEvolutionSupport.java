package com.example.kafka.advance.serde;

import com.example.kafka.advance.model.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Utilities demonstrating schema-evolution-friendly reading patterns (JSON variation).
 *
 * <p><b>Best practices:</b>
 * <ul>
 *   <li>Prefer <b>additive</b> changes (new optional fields). Keep consumers tolerant of unknown fields.</li>
 *   <li>Use explicit {@code schemaVersion}/{@code eventType} and compatibility tests.</li>
 *   <li>Provide defaults when reading missing fields; avoid breaking renames/removals.</li>
 *   <li>For strong contracts at scale, consider Schema Registry + Avro/Protobuf/JSON Schema.</li>
 * </ul>
 */
public final class SchemaEvolutionSupport {

    private SchemaEvolutionSupport() {
    }

    /** Reads an optional text field from payload with a default (forward/backward tolerant). */
    public static String textOrDefault(JsonNode payload, String field, String defaultValue) {
        if (payload == null || !payload.has(field) || payload.get(field).isNull()) return defaultValue;
        return payload.get(field).asText(defaultValue);
    }

    /** Reads an optional numeric field with a default. */
    public static double doubleOrDefault(JsonNode payload, String field, double defaultValue) {
        if (payload == null || !payload.has(field) || payload.get(field).isNull()) return defaultValue;
        return payload.get(field).asDouble(defaultValue);
    }

    /** Resolves schema version from envelope, defaulting if absent. */
    public static String versionOf(EventEnvelope envelope) {
        return Optional.ofNullable(envelope).map(EventEnvelope::getSchemaVersion).filter(s -> !s.isBlank()).orElse("unknown");
    }
}
