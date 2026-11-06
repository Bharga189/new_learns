# Serialization / Deserialization (SerDe) Variations

Kafka brokers store records as **bytes**. SerDe is the application contract that turns domain data into bytes (serialize) and back (deserialize). Correct SerDe choice affects interoperability, evolution, performance, and operability.

## 1. Built-in primitives

| SerDe | Use case | Caveats |
|---|---|---|
| `StringSerializer/Deserializer` | Text payloads, simple messages, keys | Agree on encoding (UTF-8) and format; no schema |
| `ByteArraySerializer/Deserializer` | Full control (binary formats, encryption, custom codecs) | You own framing, versioning, and compatibility |
| Numeric SerDes (`Integer`, `Long`, etc.) | Counters, IDs, compact numeric values | Simple; define semantics clearly |

**Demo:** `A03_SerDeBytesAndStringVariations`.

## 2. JSON (Jackson) variations

### 2.1 Typed JSON (fixed POJO)
- **How:** `JacksonJsonSerializer<T>` + `JacksonJsonDeserializer<T>(MyType.class)`.
- **Pros:** explicit, easy to debug, good for one-type-per-topic.
- **Cons:** schema evolution requires discipline; breaking changes can fail consumers.
- **Demo:** `A02` (typed), `A01`, `A09`.

### 2.2 Envelope JSON (metadata + payload)
- **How:** `EventEnvelope` with `eventType`, `schemaVersion`, `eventId`, timestamps, and a `JsonNode`/object payload.
- **Pros:** supports routing, versioning, correlation, tolerant payload reading.
- **Cons:** slightly larger payloads; still needs governance.
- **Demo:** `A02` (envelope), `A04`.

### 2.3 Header-based polymorphic JSON
- **How:** producer sets headers (`event-type`, `schema-version`); deserializer resolves a registered target or falls back to `JsonNode`.
- **Pros:** explicit routing without parsing the whole payload first (depending on design).
- **Cons:** requires header/registry agreement; unknown types must be handled.
- **Demo:** `A02` (polymorphic), `MultiTypeJsonDeserializer`.

### 2.4 Schema evolution (additive + tolerant reads)
- **Guidelines:** prefer additive optional fields; keep `@JsonIgnoreProperties(ignoreUnknown=true)` / tolerant readers where appropriate; supply defaults for missing fields; version events.
- **Breaking changes:** renames, removals, type changes — use a new version/topic and migration plan.
- **Demo:** `A04`, `SchemaEvolutionSupport`.

## 3. Binary / schema-based formats (production guidance)

Kafka is format-agnostic. For multi-team production systems, strongly consider schema-governed formats:
- **Avro / Protobuf / JSON Schema + Schema Registry:** explicit contracts, compatibility checks (backward/forward/full), evolution tooling.
- **Trade-offs:** operational dependency (registry), tooling/learning curve; pays off at scale and across teams.
- **In this project:** JSON demos illustrate SerDe mechanics and evolution patterns; the same contract/versioning principles apply to Avro/Protobuf.

## 4. Error-handling SerDe (poison pills)

A bad payload can crash deserialization and stall the consumer.
- **Pattern:** wrap deserializer (`SafeDeserializer`) to catch errors, return `null`/error marker, log/metric, and route raw context to a **DLQ** with headers (source topic/partition/offset, error, key).
- **Best practices:** preserve forensics (respect PII/secrets policy), alert on DLQ volume, keep processing idempotent for redrives.
- **Demo:** `A05`.

## 5. Cross-cutting best practices

- **Contract:** make format + encoding + version explicit (payload, headers, or registry).
- **Compatibility tests:** validate producer/consumer compatibility in CI.
- **Compression:** use `compression.type` (lz4/zstd/snappy) independently of SerDe for larger payloads.
- **Security:** encrypt sensitive data at the right layer; never log secrets/raw sensitive payloads.
- **Keys:** serialize keys consistently; key choice affects partitioning/ordering.
- **Performance:** reuse `ObjectMapper`/serializers; avoid unnecessary allocations; measure payload sizes.
