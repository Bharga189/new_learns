package io.buffy.cdc.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * One place for the connection details shared by every module.
 *
 * Everything is overridable with environment variables, which keeps the code
 * readable and lets the same artifact run against Podman (default) or a real
 * MySQL/Kafka installation.
 */
public final class CdcConfig {

    /** Prefix used for all Kafka topic names created by the connector. */
    public static final String TOPIC_PREFIX = "mysql-cdc";

    /** Database that contains the demo tables. */
    public static final String DATABASE = "test_db";

    private CdcConfig() {
    }

    public static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static int envInt(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static String mysqlHost() {
        return env("MYSQL_HOST", "localhost");
    }

    public static int mysqlPort() {
        return envInt("MYSQL_PORT", 3306);
    }

    public static String mysqlUser() {
        return env("MYSQL_USER", "cdc_user");
    }

    public static String mysqlPassword() {
        return env("MYSQL_PASSWORD", "cdc_pass");
    }

    public static String jdbcUrl() {
        return "jdbc:mysql://" + mysqlHost() + ":" + mysqlPort() + "/" + DATABASE
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    public static String kafkaBootstrap() {
        return env("KAFKA_BOOTSTRAP", "localhost:9092");
    }

    public static String connectUrl() {
        return env("CONNECT_URL", "http://localhost:8083");
    }

    /**
     * Debezium's embedded engine needs writable files for its offset and schema
     * history stores. Each demo gets its own directory so that running one
     * example does not silently resume another example's position.
     */
    public static Path dataDir(String demoName) {
        Path dir = Path.of(System.getProperty("user.dir"), "data", demoName);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create data directory " + dir, e);
        }
        return dir;
    }

    /**
     * The configuration the embedded engine and the Kafka Connect worker both
     * understand. The important teaching points are marked with comments.
     */
    public static Properties engineProperties(String demoName) {
        Path data = dataDir(demoName);

        Properties props = new Properties();
        props.setProperty("name", demoName);
        props.setProperty("connector.class", "io.debezium.connector.mysql.MySqlConnector");

        // --- Source database -------------------------------------------------
        props.setProperty("database.hostname", mysqlHost());
        props.setProperty("database.port", String.valueOf(mysqlPort()));
        props.setProperty("database.user", mysqlUser());
        props.setProperty("database.password", mysqlPassword());
        props.setProperty("database.include.list", DATABASE);
        props.setProperty("table.include.list", DATABASE + ".orders," + DATABASE + ".customers");
        props.setProperty("database.ssl.mode", "disabled");

        // A unique id in the MySQL replication topology. Never reuse it for two
        // connectors that point at the same MySQL server at the same time.
        props.setProperty("database.server.id", env("DEBEZIUM_SERVER_ID", "184054"));

        // The namespace for the topic names:
        //   mysql-cdc.test_db.orders
        //   mysql-cdc                     (schema change topic)
        props.setProperty("topic.prefix", TOPIC_PREFIX);

        // --- Snapshot / streaming -------------------------------------------
        // "initial" = snapshot the captured tables, then stream the binlog.
        props.setProperty("snapshot.mode", "initial");
        props.setProperty("include.schema.changes", "true");
        props.setProperty("tombstones.on.delete", "false");
        props.setProperty("heartbeat.interval.ms", "5000");

        // JSON output is easier to read than raw bytes. DECIMAL as string avoids
        // floating point surprises in a demo.
        props.setProperty("decimal.handling.mode", "string");
        props.setProperty("time.precision.mode", "connect");

        // --- Embedded-only storage ------------------------------------------
        // The Kafka Connect deployment stores offsets and history in Kafka
        // topics. The embedded engine writes them to local files instead.
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
        props.setProperty("offset.storage.file.filename", data.resolve("offsets.dat").toString());
        props.setProperty("offset.flush.interval.ms", "1000");

        props.setProperty("schema.history.internal",
                "io.debezium.storage.file.history.FileSchemaHistory");
        props.setProperty("schema.history.internal.file.filename",
                data.resolve("schema-history.dat").toString());

        props.setProperty("errors.log.enable", "true");
        props.setProperty("errors.log.include.messages", "true");
        return props;
    }
}
