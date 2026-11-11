package io.buffy.cdc.outbox;

import io.buffy.cdc.common.Jdbc;
import io.buffy.cdc.common.JsonSupport;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ADVANCED: the transactional outbox pattern.
 *
 * The business row and the event row are written in the same local MySQL
 * transaction. Either both commit or neither does, so the database can never
 * contain an order without its event (or an event without its order).
 *
 * Debezium tails the outbox table and the EventRouter SMT turns each row into
 * a normal Kafka message, keyed by aggregate id and routed to a topic.
 *
 * Run from learn/debezium-mysql-parent:
 *   mvn -q -pl 07-outbox exec:java -Dexec.args="Dana 99.50"
 *
 * Then consume the routed topic:
 *   mvn -q -pl 03-consumer exec:java -Dexec.args="--topic outbox.Order"
 */
public final class OutboxWriter {

    private OutboxWriter() {
    }

    public static void main(String[] args) throws Exception {
        String customer = args.length > 0 ? args[0] : "Dana";
        BigDecimal amount = args.length > 1 ? new BigDecimal(args[1]) : new BigDecimal("99.50");

        try (Connection connection = Jdbc.connect()) {
            connection.setAutoCommit(false);
            try {
                long orderId = insertOrder(connection, customer, amount);
                String payload = orderCreatedPayload(orderId, customer, amount);
                insertOutboxEvent(connection, orderId, "OrderCreated", payload);
                connection.commit();

                System.out.println("Committed order + outbox event in one transaction.");
                System.out.println("  order id : " + orderId);
                System.out.println("  event    : OrderCreated");
                System.out.println("  topic    : outbox.Order");
                System.out.println("  payload  : " + payload);
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static long insertOrder(Connection connection, String customer, BigDecimal amount)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO orders (customer_name, amount, status)
                VALUES (?, ?, 'PENDING')
                """, Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, customer);
            insert.setBigDecimal(2, amount);
            insert.executeUpdate();

            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL did not return the generated order id");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void insertOutboxEvent(Connection connection, long orderId,
                                          String type, String payload) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO outbox (aggregate_type, aggregate_id, type, payload)
                VALUES ('Order', ?, ?, ?)
                """)) {
            insert.setString(1, String.valueOf(orderId));
            insert.setString(2, type);
            insert.setString(3, payload);
            insert.executeUpdate();
        }
    }

    private static String orderCreatedPayload(long orderId, String customer, BigDecimal amount)
            throws Exception {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("orderId", orderId);
        event.put("customer", customer);
        event.put("amount", amount);
        event.put("status", "PENDING");
        event.put("eventType", "OrderCreated");
        return JsonSupport.mapper().writeValueAsString(event);
    }
}
