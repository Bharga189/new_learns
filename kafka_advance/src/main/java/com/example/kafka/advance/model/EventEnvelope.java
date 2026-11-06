package com.example.kafka.advance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Generic event envelope for schema evolution and polymorphic/event-type routing demos.
 *
 * <p><b>Deep dive.</b> An envelope separates transport metadata (type, version, ids, timestamps)
 * from the payload. This supports:
 * <ul>
 *   <li><b>Versioning:</b> consumers can branch on {@code schemaVersion}/{@code eventType}.</li>
 *   <li><b>Routing:</b> headers and/or envelope fields drive deserialization/handling.</li>
 *   <li><b>Evolution:</b> payload kept as {@link JsonNode} allows tolerant reading; typed binding can be applied after inspecting metadata.</li>
 *   <li><b>Observability/correlation:</b> eventId/occurredAt/source aid tracing and dedup.</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventEnvelope {
    private String eventType;
    private String schemaVersion;
    private String eventId;
    private String key;
    private Instant occurredAt;
    private JsonNode payload;

    public EventEnvelope() {
    }

    public EventEnvelope(String eventType, String schemaVersion, String eventId, String key, Instant occurredAt, JsonNode payload) {
        this.eventType = eventType;
        this.schemaVersion = schemaVersion;
        this.eventId = eventId;
        this.key = key;
        this.occurredAt = occurredAt;
        this.payload = payload;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public JsonNode getPayload() { return payload; }
    public void setPayload(JsonNode payload) { this.payload = payload; }

    @Override
    public String toString() {
        return "EventEnvelope{eventType='" + eventType + "', schemaVersion='" + schemaVersion + "', eventId='" + eventId
                + "', key='" + key + "', occurredAt=" + occurredAt + ", payload=" + payload + '}';
    }
}
