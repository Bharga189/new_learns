package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Feature 01: Producer basics — real-time send patterns, callbacks, batching, compression.
 *
 * <p><b>Deep dive: what a producer does.</b> A Kafka producer publishes records to topics.
 * Each record has an optional key, a value, and metadata (topic, partition, timestamp,
 * headers). The producer does far more than a simple network write:</p>
 * <ol>
 *   <li><b>Serialization</b>: key/value objects become bytes.</li>
 *   <li><b>Partitioning</b>: if no partition is specified, the partitioner chooses one.
 *       Default behavior: records with a key are hashed to a stable partition; records with
 *       no key use a sticky strategy to batch efficiently.</li>
 *   <li><b>Batching</b>: records for the same partition are accumulated in memory batches
 *       ({@code batch.size}, {@code linger.ms}) to improve throughput.</li>
 *   <li><b>Compression</b>: batches can be compressed ({@code compression.type}=gzip,
 *       snappy, lz4, zstd) to reduce network and storage cost.</li>
 *   <li><b>Acknowledgment</b>: {@code acks} controls durability. {@code acks=0} is
 *       fire-and-forget; {@code acks=1} waits for the partition leader; {@code acks=all}
 *       waits for all in-sync replicas (strongest durability when combined with
 *       {@code min.insync.replicas}).</li>
 * </ol>
 *
 * <p><b>Sync vs async.</b> {@code send()} is asynchronous: it enqueues the record and
 * returns a {@link Future}. Calling {@code get()} makes it synchronous (blocks until ack
 * or error). Production code usually prefers async sends with callbacks to keep throughput
 * high while still handling per-record results.</p>
 *
 * <p><b>Local test (Podman):</b></p>
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F01_ProducerBasics
 * # observe sync and async send acknowledgments with partition/offset metadata
 * }</pre>
 */
public class F01_ProducerBasics {
    private static final Logger log = LoggerFactory.getLogger(F01_ProducerBasics.class);
    private static final String TOPIC = "demo.producer-basics";

    public static void main(String[] args) throws Exception {
        Properties props = KafkaConfig.baseProducerProps("f01-producer");
        // Reliability/durability choices (deep dive): wait for all ISR replicas.
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // Batching: wait up to 20ms to accumulate records into a batch for throughput.
        props.put(ProducerConfig.LINGER_MS_CONFIG, 20);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32 * 1024);
        // Compression trades CPU for network/storage efficiency.
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            log.info("=== F01 Producer Basics (bootstrap={}) ===", KafkaConfig.bootstrapServers());

            // 1) Synchronous send: simple but blocks the calling thread per record.
            for (int i = 1; i <= 3; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        TOPIC, "sync-key", "sync-message-" + i + " @ " + Instant.now());
                RecordMetadata md = producer.send(record).get();
                log.info("[SYNC] topic={} partition={} offset={} timestamp={}",
                        md.topic(), md.partition(), md.offset(), md.timestamp());
            }

            // 2) Asynchronous send with callback: non-blocking, result handled later.
            for (int i = 1; i <= 5; i++) {
                final int n = i;
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        TOPIC, "async-key-" + (i % 2), "async-message-" + i);
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        log.error("[ASYNC] failed to send message {}", n, exception);
                    } else {
                        log.info("[ASYNC] message {} -> partition={} offset={}",
                                n, metadata.partition(), metadata.offset());
                    }
                });
            }

            // flush() ensures buffered async records are sent before close/exit.
            producer.flush();
            log.info("Done. Records were serialized, partitioned, batched, compressed, and acknowledged (acks=all).");
        }
    }
}
