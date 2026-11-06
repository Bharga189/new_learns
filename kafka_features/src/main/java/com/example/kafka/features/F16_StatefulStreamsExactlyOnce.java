package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Feature 16: Stateful Kafka Streams with exactly-once processing guarantees (real-time aggregation).
 *
 * <p><b>Deep dive: stateful stream processing.</b> Unlike stateless filter/map, stateful operations
 * (aggregations, joins, windowing) maintain local state. Kafka Streams stores this state in local
 * state stores and backs it with internal <b>changelog topics</b> for fault tolerance. If an
 * instance fails, another instance can restore state from the changelog.</p>
 *
 * <ul>
 *   <li><b>KStream vs KTable:</b> a KStream is an event stream; a KTable represents the latest
 *       value per key (changelog/table semantics). Aggregations often produce table-like results.</li>
 *   <li><b>Processing guarantees:</b> {@code processing.guarantee} controls semantics.
 *       {@code at_least_once} is the default; {@code exactly_once_v2} (recommended on modern
 *       brokers/clients when supported) uses transactions to provide stronger end-to-end guarantees
 *       within the Kafka Streams topology.</li>
 *   <li><b>Realtime + correctness trade-off:</b> exactly-once can add overhead (transaction commits,
 *       buffering). Choose based on correctness requirements and SLOs.</li>
 *   <li><b>Operations:</b> state stores require disk; repartitioning may occur for key-based
 *       aggregations; monitor lag, commit latency, rebalance/state restoration time.</li>
 * </ul>
 *
 * <p><b>What this demo shows.</b> Input events like {@code userId:amount} are aggregated into a
 * running total per user key. Results are written to an output topic and also printed live.</p>
 *
 * <p><b>Local Podman test:</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F16_StatefulStreamsExactlyOnce
 * podman compose down -v
 * }</pre>
 */
public class F16_StatefulStreamsExactlyOnce {
    private static final Logger log = LoggerFactory.getLogger(F16_StatefulStreamsExactlyOnce.class);
    private static final String INPUT = "demo.streams.eos.input";
    private static final String OUTPUT = "demo.streams.eos.output";
    private static final String APP_ID = "f16-stateful-eos-" + UUID.randomUUID().toString().substring(0, 8);

    public static void main(String[] args) throws Exception {
        log.info("=== F16 Stateful Streams + exactly_once_v2 (bootstrap={}) ===", KafkaConfig.bootstrapServers());

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, APP_ID);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.bootstrapServers());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        // Stronger processing guarantee (Kafka Streams uses transactions under the hood).
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        // Fast feedback for local demo.
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> input = builder.stream(INPUT, Consumed.with(Serdes.String(), Serdes.String()));

        input
                .filter((k, v) -> k != null && v != null && v.contains(":"))
                .map((k, v) -> {
                    String[] parts = v.split(":", 2);
                    String user = parts[0].trim();
                    String amountStr = parts[1].trim();
                    long amount;
                    try {
                        amount = Long.parseLong(amountStr);
                    } catch (NumberFormatException e) {
                        amount = 0L;
                    }
                    log.info("[STREAM-IN] key={} value={} -> user={} amount={}", k, v, user, amount);
                    return KeyValue.pair(user, amount);
                })
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
                .aggregate(
                        () -> 0L,
                        (user, amount, total) -> total + amount,
                        Materialized.with(Serdes.String(), Serdes.Long())
                )
                .toStream()
                .mapValues(total -> String.valueOf(total))
                .peek((user, total) -> log.info("[STREAM-AGG] user={} runningTotal={}", user, total))
                .to(OUTPUT, Produced.with(Serdes.String(), Serdes.String()));

        try (KafkaStreams streams = new KafkaStreams(builder.build(), props)) {
            streams.start();
            produceInputEvents();
            consumeOutputSample();
            streams.close(Duration.ofSeconds(5));
        }

        log.info("Done. Stateful aggregation used local state + changelog semantics; exactly_once_v2 requested for stronger guarantees.");
    }

    private static void produceInputEvents() throws Exception {
        Properties p = KafkaConfig.baseProducerProps("f16-eos-producer");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(p)) {
            String[] events = {
                    "evt1:u1:10", "evt2:u2:5", "evt3:u1:7", "evt4:u2:3", "evt5:u1:2", "evt6:u3:9"
            };
            for (String e : events) {
                producer.send(new ProducerRecord<>(INPUT, e.split(":")[1], e)).get();
                log.info("[PRODUCED] {}", e);
                Thread.sleep(150);
            }
            producer.flush();
        }
    }

    private static void consumeOutputSample() {
        Properties c = KafkaConfig.baseConsumerProps("f16-eos-out-" + UUID.randomUUID(), "f16-eos-consumer");
        c.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(c)) {
            consumer.subscribe(List.of(OUTPUT));
            long deadline = System.currentTimeMillis() + 15_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : records) {
                    log.info("[OUTPUT] partition={} offset={} key={} value={} ts={}",
                            r.partition(), r.offset(), r.key(), r.value(), Instant.ofEpochMilli(r.timestamp()));
                }
            }
        }
    }
}
