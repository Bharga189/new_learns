package io.buffy.cdc.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Small JSON helpers used by the command-line demos.
 *
 * Debezium can emit either a flat payload:
 *   {"before": ..., "after": ..., "op": "c"}
 * or a schema-enabled envelope:
 *   {"schema": ..., "payload": {"before": ..., "after": ..., "op": "c"}}
 *
 * The helpers below accept both shapes.
 */
public final class JsonSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonSupport() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static JsonNode parse(String json) {
        if (json == null || json.isBlank()) {
            return MAPPER.createObjectNode();
        }
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            return MAPPER.createObjectNode().put("unparseable", json);
        }
    }

    public static String pretty(String json) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(parse(json));
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    /** Returns the event body, unwrapping a schema-enabled JSON document. */
    public static JsonNode payload(JsonNode root) {
        if (root != null && root.has("payload") && root.get("payload").isObject()) {
            return root.get("payload");
        }
        return root;
    }

    public static JsonNode payload(String json) {
        return payload(parse(json));
    }

    public static String text(JsonNode node, String field, String fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText(fallback);
    }

    public static String shortSummary(JsonNode payload) {
        if (payload == null) {
            return "empty";
        }
        String op = text(payload, "op", "?");
        JsonNode source = payload.get("source");
        String table = source == null ? "?" : text(source, "table", "?");
        String database = source == null ? "?" : text(source, "db", "?");
        String ddl = text(payload, "ddl", "");
        if (!ddl.isBlank()) {
            return "op=DDL db=" + database + " table=" + table;
        }
        return "op=" + op + " db=" + database + " table=" + table;
    }
}
