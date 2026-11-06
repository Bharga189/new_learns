package com.example.kafka.advance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Objects;

/**
 * Example domain event used across SerDe and real-time demos.
 *
 * <p><b>Schema evolution note.</b> {@link JsonIgnoreProperties}(ignoreUnknown=true) helps consumers
 * tolerate <em>additive</em> producer changes (new fields). Removing/renaming fields or changing types
 * can still break consumers — use explicit versioning and compatibility policies in production.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEvent {
    private String schemaVersion = "v1";
    private String eventId;
    private String orderId;
    private String status;
    private double amount;
    private Instant occurredAt;

    public OrderEvent() {
        // for Jackson
    }

    public OrderEvent(String schemaVersion, String eventId, String orderId, String status, double amount, Instant occurredAt) {
        this.schemaVersion = schemaVersion;
        this.eventId = eventId;
        this.orderId = orderId;
        this.status = status;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }

    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    @Override
    public String toString() {
        return "OrderEvent{schemaVersion='" + schemaVersion + "', eventId='" + eventId + "', orderId='" + orderId
                + "', status='" + status + "', amount=" + amount + ", occurredAt=" + occurredAt + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderEvent that)) return false;
        return Double.compare(amount, that.amount) == 0 && Objects.equals(eventId, that.eventId)
                && Objects.equals(orderId, that.orderId) && Objects.equals(status, that.status)
                && Objects.equals(schemaVersion, that.schemaVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaVersion, eventId, orderId, status, amount);
    }
}
