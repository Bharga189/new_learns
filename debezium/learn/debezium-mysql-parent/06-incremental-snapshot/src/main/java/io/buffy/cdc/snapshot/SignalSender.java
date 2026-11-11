package io.buffy.cdc.snapshot;

import io.buffy.cdc.common.CdcConfig;
import io.buffy.cdc.common.Jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * ADVANCED: ask Debezium to snapshot a table while it is already streaming.
 *
 * Debezium watches a normal table (the signal table) inside MySQL. A row in
 * that table is a command:
 *
 *   type = 'execute-snapshot'
 *   data = {"data-collections":["test_db.orders"],"type":"incremental"}
 *
 * The incremental snapshot is chunked and does not block the ongoing binlog
 * stream. The connector records its progress, so a restart can resume it.
 *
 * Run from learn/debezium-mysql-parent:
 *   mvn -q -pl 06-incremental-snapshot exec:java -Dexec.args="incremental test_db.orders"
 *   mvn -q -pl 06-incremental-snapshot exec:java -Dexec.args="stop test_db.orders"
 *   mvn -q -pl 06-incremental-snapshot exec:java -Dexec.args="log hello"
 */
public final class SignalSender {

    private static final String SIGNAL_TABLE = "test_db.debezium_signal";

    private SignalSender() {
    }

    public static void main(String[] args) throws SQLException {
        String action = args.length > 0 ? args[0].toLowerCase() : "incremental";
        String argument = args.length > 1 ? args[1] : CdcConfig.DATABASE + ".orders";

        String type;
        String data;

        switch (action) {
            case "incremental", "snapshot" -> {
                type = "execute-snapshot";
                data = "{\"data-collections\":[\"" + argument
                        + "\"],\"type\":\"incremental\"}";
            }
            case "stop" -> {
                type = "stop-snapshot";
                data = "{\"data-collections\":[\"" + argument
                        + "\"],\"type\":\"incremental\"}";
            }
            case "log" -> {
                type = "log";
                data = "{\"message\":\"" + argument.replace("\"", "\\\"") + "\"}";
            }
            default -> {
                System.err.println("Usage: SignalSender [incremental|stop|log] [table-or-message]");
                System.exit(2);
                return;
            }
        }

        try (Connection connection = Jdbc.connect()) {
            createSignalTableIfMissing(connection);

            String id = UUID.randomUUID().toString();
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO debezium_signal (id, type, data)
                    VALUES (?, ?, ?)
                    """)) {
                insert.setString(1, id);
                insert.setString(2, type);
                insert.setString(3, data);
                insert.executeUpdate();
            }

            System.out.println("Inserted signal into " + SIGNAL_TABLE);
            System.out.println("  id  : " + id);
            System.out.println("  type: " + type);
            System.out.println("  data: " + data);
            System.out.println();
            System.out.println("The running connector will pick this up on its next poll.");
            System.out.println("Watch the connector log:");
            System.out.println("  podman logs -f cdc-connect");
        }
    }

    private static void createSignalTableIfMissing(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS debezium_signal (
                        id   VARCHAR(42)  NOT NULL PRIMARY KEY,
                        type VARCHAR(32)  NOT NULL,
                        data VARCHAR(2048) NULL
                    ) ENGINE=InnoDB
                    """);
        }
    }
}
