package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Feature 13: Kafka Streams — real-time stream processing.
 *
 * <p><b>Deep dive: Kafka Streams.</b> Kafka Streams is a client library for building
 * stream-processing applications that read from Kafka topics, transform/aggregate data, and
 * write results back to Kafka (or expose state). Unlike a separate cluster system, it runs in
 * your application JVM(s) and scales by running more instances with the same
 * {@code application.id} (which acts like a consumer group for partition assignment).</p>
 *
 * <p><b>Core concepts:</b></p>
 * <ul>
 *   <li><b>Topology</b>: a directed graph of sources, processors, and sinks built via the DSL
 *       ({@link StreamsBuilder}) or Processor API.</li>
 *   <li><b>KStream</b>: an unbounded stream of records (event-by-event). <b>KTable</b> (not
 *       shown in this minimal demo) models a changelog/table view (latest value per key).</li>
 *   <li><b>SerDes</b>: serializers/deserializers for keys/values at every edge; mismatches are a
 *       common source of errors.</li>
 *   <li><b>State stores & fault tolerance</b>: stateful operations (aggregations, joins) use
 *       local state stores backed by changelog topics for recovery.</li>
 *   <li><b>Processing guarantees</b>: configurable via {@code processing.guarantee}
 *       ({@code at_least_once} or {@code exactly_once_v2} when supported by brokers/clients).</li>
 * </ul>
 *
 * <p><b>This real-time demo:</b> reads text events, filters/normalizes them, and writes
 * enriched output to another topic continuously until stopped.</p>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F13_KafkaStreamsRealtime
 * # in another terminal, produce input and/or consume output:
 * # consume output: run a simple consumer on demo.streams.output, or inspect logs
 * }</pre>
 */
public class F13_KafkaStreamsRealtime {
    private static final Logger log = LoggerFactory.getLogger(F13_KafkaStreamsRealtime.class);
    private static final String INPUT = "demo.streams.input";
    private static final String OUTPUT = "demo.streams.output";

    public static void main(String[] args) throws Exception {
        seedInput();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "f13-realtime-uppercase-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.bootstrapServers());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        // Read from earliest for deterministic demo behavior on a fresh local run.
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> source = builder.stream(INPUT, Consumed.with(Serdes.String(), Serdes.String()));
        source
                .peek((k, v) -> log.info("[STREAM-IN]  key={} value={}", k, v), Named.as("peek-in"))
                .filter((k, v) -> v != null && !v.isBlank(), Named.as("filter-nonblank"))
                .mapValues(v -> "ENRICHED::" + v.trim().toUpperCase(), Named.as("map-uppercase"))
                .peek((k, v) -> log.info("[STREAM-OUT] key={} value={}", k, v), Named.as("peek-out"))
                .to(OUTPUT, Produced.with(Serdes.String(), Serdes.String()));

        Topology topology = builder.build();
        log.info("=== F13 Kafka Streams topology ===\n{}", topology.describe());

        try (KafkaStreams streams = new KafkaStreams(topology, props)) {
            CountDownLatch latch = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown requested; closing Kafka Streams.");
                streams.close(Duration.ofSeconds(5));
                latch.countDown();
            }));

            streams.start();
            log.info("Kafka Streams started (application.id={}). It will process input and exit in ~15s for demo purposes, or on Ctrl+C.",
                    props.get(StreamsConfig.APPLICATION_ID_CONFIG));
            // Demo: run briefly so `mvn exec:java` returns in automated/local checks.
            Thread.sleep(15_000);
            streams.close(Duration.ofSeconds(5));
            log.info("Kafka Streams stopped. Output written to topic '{}'.", OUTPUT);
        }
    }

    private static void seedInput() throws Exception {
        Properties p = KafkaConfig.baseProducerProps("f13-seeder");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(p)) {
            String[] events = {"  hello kafka streams  ", "real-time event", "  ", "feature demo"};
            for (int i = 0; i < events.length; i++) {
                producer.send(new ProducerRecord<>(INPUT, "e" + i, events[i])).get();
            }
            producer.flush();
            log.info("Seeded {} events to {}", events.length, INPUT);
        }
    }
}
