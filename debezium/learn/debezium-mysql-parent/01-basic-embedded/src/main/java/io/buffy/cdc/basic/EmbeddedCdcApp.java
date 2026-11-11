package io.buffy.cdc.basic;

import com.fasterxml.jackson.databind.JsonNode;
import io.buffy.cdc.common.CdcConfig;
import io.buffy.cdc.common.JsonSupport;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * BASIC: run Debezium inside this JVM.
 *
 * There is no Kafka and no Kafka Connect here. The embedded engine talks to
 * MySQL's binlog directly, converts every change to JSON, and calls
 * {@link #printEvent(ChangeEvent)}. Offsets and schema history are written to
 * local files under ./data/basic-embedded.
 *
 * Run from learn/debezium-mysql-parent:
 *   mvn -q -pl 01-basic-embedded exec:java
 *
 * Then, in another terminal, change a row:
 *   podman exec -it cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
 *     -e "INSERT INTO orders(customer_name, amount, status) VALUES ('Carol', 42.00, 'PENDING');"
 */
public final class EmbeddedCdcApp {

    private EmbeddedCdcApp() {
    }

    public static void main(String[] args) throws Exception {
        Properties props = CdcConfig.engineProperties("basic-embedded");

        System.out.println("Starting embedded Debezium engine...");
        System.out.println("  MySQL      : " + CdcConfig.mysqlHost() + ":" + CdcConfig.mysqlPort());
        System.out.println("  topic prefix: " + CdcConfig.TOPIC_PREFIX);
        System.out.println("  data dir    : " + CdcConfig.dataDir("basic-embedded").toAbsolutePath());
        System.out.println("Press Ctrl+C to stop.\n");

        DebeziumEngine<ChangeEvent<String, String>> engine =
                DebeziumEngine.create(Json.class)
                        .using(props)
                        .notifying(EmbeddedCdcApp::printEvent)
                        .build();

        AtomicBoolean closed = new AtomicBoolean(false);
        Runnable closeEngine = () -> {
            if (closed.compareAndSet(false, true)) {
                try {
                    engine.close();
                } catch (IOException e) {
                    System.err.println("Error while closing the engine: " + e.getMessage());
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(closeEngine, "cdc-shutdown"));
        try {
            engine.run();
        } finally {
            closeEngine.run();
        }
    }

    private static void printEvent(ChangeEvent<String, String> event) {
        JsonNode payload = JsonSupport.payload(event.value());

        System.out.println();
        System.out.println("============================================================");
        System.out.println(JsonSupport.shortSummary(payload));
        System.out.println("topic=" + event.destination()
                + " partition=" + event.partition()
                + " key=" + event.key());
        System.out.println("============================================================");
        System.out.println(JsonSupport.pretty(event.value()));
    }
}
