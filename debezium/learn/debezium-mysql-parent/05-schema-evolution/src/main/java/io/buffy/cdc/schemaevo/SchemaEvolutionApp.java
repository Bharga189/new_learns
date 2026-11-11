package io.buffy.cdc.schemaevo;

import com.fasterxml.jackson.databind.JsonNode;
import io.buffy.cdc.common.CdcConfig;
import io.buffy.cdc.common.Jdbc;
import io.buffy.cdc.common.JsonSupport;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ADVANCED: schema changes are events too.
 *
 * Debezium reads DDL from the MySQL binlog and writes a record to the schema
 * change topic named after topic.prefix (here: "mysql-cdc-schema"). Data events
 * continue to use mysql-cdc-schema.test_db.orders.
 *
 * This demo subscribes to both topics. With --apply-ddl it also runs an ALTER
 * TABLE, so you can watch the schema change event arrive and then see the new
 * column in the next data change.
 *
 * Run from learn/debezium-mysql-parent:
 *   mvn -q -pl 05-schema-evolution exec:java -Dexec.args="--apply-ddl"
 */
public final class SchemaEvolutionApp {

    private static final String PREFIX = "mysql-cdc-schema";

    private SchemaEvolutionApp() {
    }

    public static void main(String[] args) throws Exception {
        boolean applyDdl = List.of(args).contains("--apply-ddl");

        Properties props = new Properties();
        props.put("bootstrap.servers", CdcConfig.kafkaBootstrap());
        props.put("group.id", "cdc-schema-evolution");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");

        AtomicBoolean running = new AtomicBoolean(true);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            consumer.wakeup();
        }, "schema-evolution-shutdown"));

        String schemaTopic = PREFIX;
        String dataTopic = PREFIX + "." + CdcConfig.DATABASE + ".orders";
        consumer.subscribe(List.of(schemaTopic, dataTopic));

        System.out.println("Watching schema changes on : " + schemaTopic);
        System.out.println("Watching data changes on   : " + dataTopic);
        System.out.println("Ctrl+C to stop.\n");

        if (applyDdl) {
            Thread ddl = new Thread(() -> {
                sleep(2500);
                applySchemaChange();
            }, "ddl-writer");
            ddl.setDaemon(true);
            ddl.start();
        }

        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    print(record);
                }
            }
        } catch (WakeupException e) {
            if (running.get()) {
                throw e;
            }
        } finally {
            consumer.close();
        }
    }

    private static void print(ConsumerRecord<String, String> record) {
        JsonNode payload = JsonSupport.payload(record.value());

        System.out.println();
        System.out.println("============================================================");
        System.out.println("topic=" + record.topic() + " offset=" + record.offset()
                + " key=" + record.key());
        System.out.println("============================================================");

        if (payload.has("ddl")) {
            System.out.println("SCHEMA CHANGE EVENT");
            System.out.println("database: " + payload.path("databaseName").asText("-"));
            System.out.println("ddl     : " + payload.path("ddl").asText());
            System.out.println("tables  : " + payload.path("tableChanges").size() + " table change(s)");

            JsonNode tableChanges = payload.path("tableChanges");
            for (JsonNode change : tableChanges) {
                System.out.println("  - " + change.path("id").asText("-")
                        + " type=" + change.path("type").asText("-"));
            }
        } else {
            System.out.println("DATA CHANGE EVENT: " + JsonSupport.shortSummary(payload));
            System.out.println("op=" + JsonSupport.text(payload, "op", "?")
                    + "  (c=insert, u=update, d=delete, r=snapshot read)");
            System.out.println("before: " + JsonSupport.pretty(payload.path("before").toString()));
            System.out.println("after : " + JsonSupport.pretty(payload.path("after").toString()));
        }
    }

    private static void applySchemaChange() {
        try (Connection connection = Jdbc.connect()) {
            if (!columnExists(connection, "orders", "loyalty_tier")) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("""
                            ALTER TABLE orders
                            ADD COLUMN loyalty_tier VARCHAR(20) NOT NULL DEFAULT 'STANDARD'
                            """);
                }
                System.out.println("\n[DDL] Added orders.loyalty_tier.");
            } else {
                System.out.println("\n[DDL] orders.loyalty_tier already exists; skipping ALTER.");
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        UPDATE orders
                        SET loyalty_tier = 'GOLD'
                        WHERE id = (SELECT id FROM (SELECT MIN(id) AS id FROM orders) AS first_order)
                        """);
            }
            System.out.println("[DML] Updated the first order; the next event should contain loyalty_tier.\n");
        } catch (SQLException e) {
            System.err.println("Could not apply the schema change: " + e.getMessage());
        }
    }

    private static boolean columnExists(Connection connection, String table, String column)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ? AND column_name = ?
                """)) {
            statement.setString(1, CdcConfig.DATABASE);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
