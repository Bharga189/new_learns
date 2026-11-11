# Debezium + MySQL — module 04: Custom Single Message Transform (SMT)

This module shows how to write a **pluggable message transform**: a compiled
class that Kafka Connect loads at runtime and applies to every record passing
through a connector. The built-in SMTs (`unwrap`, `MaskField`) are useful, but
business-specific masking rules (PII, internal IDs, tenant isolation) are the
classic case for your own transform.

## Components

- `MaskPiiTransform` — the transform itself (`io.buffy.cdc.transforms`).
- `src/main/resources/connectors/transforms.json` — the connector config that
  uses it.
- `setup.sh build` — compiles the project, copies the JAR into
  `podman/plugins/`, and restarts Connect (otherwise the new plugin is invisible).

## Run it

```bash
cd learn/debezium-mysql-parent
./setup.sh build          # compiles + copies the jar + restarts cdc-connect if running
./setup.sh deploy transforms
./setup.sh consume --topic mysql-cdc-transforms.test_db.customers
```

At the same time produce (module `03` console shows the *unmasked* value):

```bash
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "INSERT INTO customers(id, customer_name, email, phone) VALUES (1, 'Alice', 'alice@example.com', '+1-555-0100');"
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "UPDATE customers SET email = 'alice.new@example.com' WHERE id = 1;"
```

## What you see

With the transform, the email is **redacted**:

```jsonc
{ "__op": "c", "__source_ts_ms": 1730000000000,
  "customer_name": "Alice",
  "email": "***",        // <-- masked
  "phone": "+1-555-0100" }
```

Without the transform (raw `mysql-cdc-transforms.test_db.customers` left as a
console topic) the same record keeps the full email. The transform never
touches the key or the envelope, and it never changes schemas — it only
replaces values in an existing value tree.

## `transforms.json` — the connector config

```jsonc
{
  "name": "mysql-customers-masked",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "cdc-mysql", "database.port": "3306",
    "database.user": "cdc_user", "database.password": "cdc_pass",
    "database.server.id": "184055",
    "database.include.list": "test_db",
    "table.include.list": "test_db.customers",
    "topic.prefix": "mysql-cdc-transforms",
    "snapshot.mode": "initial",
    "transforms": "unwrap,mask",
    "transforms.unwrap.type": "org.apache.kafka.connect.transforms.ExtractNewRecordState",
    "transforms.unwrap.delete.handling.mode": "rewrite",
    "transforms.unwrap.add.fields": "op,source.ts_ms",
    "transforms.mask.type": "io.buffy.cdc.transforms.MaskPiiTransform",
    "transforms.mask.mask.fields": "email,phone",
    "transforms.mask.mask.replacement": "***",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false"
  }
}
```

Notes:

- `transforms.unwrap` (ExtractNewRecordState) flattens the envelope into the
  record value so the mask operates on `customer_name`/`email`/`phone` at the
  top level.
- `transforms.unwrap.add.fields` re-adds the `op`/`ts_ms` bookkeeping fields so
  consumers still see what happened.
- `mask.fields` is a comma-separated dotted path list; `mask.replacement` is
  the replacement string.

## `MaskPiiTransform` — implementation notes

Kafka Connect discovers the class through the standard contract:

```java
public class MaskPiiTransform implements Transformation<R extends ConnectRecord<R>>, Configurable
```

- `CONFIG_DEF` (a `ConfigDef`) declares every parameter with type, default
  value and semantic name — Connect validates config before the task starts.
- `configure(Map<String,String>)` stores the user config.
- `apply(R record)` walks the value tree (`JsonNode` map → replace matching
  leaf values with `***`) and returns the record.

Contract rules for custom SMTs:

1. **Never change the schema.** The runtime generics (`Transformation<R>`)
   make this explicit; changing schemas breaks the Connect contract tests and
   the JSON/Avro serializers.
2. **Treat nulls and missing keys gracefully** — the connector may send
   `before: null` for tombstones.
3. **Make it re-entrant and stateless** between records (Connect may
   parallelise tasks).
4. Put the compiled JAR into `podman/plugins/<name>.jar` and restart Connect
   after building (`setup.sh build` does this).

## The built-in SMTs you should know

| SMT | Purpose | Typical combine |
|---|---|---|
| `ExtractNewRecordState` (`unwrap`) | Pull `after` out of the envelope, drop tombstones, add bookkeeping fields | always first |
| `MaskField` | Built-in static masking, no code | before/after unwrap |
| `ColumnRename` / `Cast` | Schema shape only | inside `unwrap` chain |
| `EventRouter` | Outbox pattern (module `07`) | always last |

## Why SMTs are a good fit here

- **Zero app changes**: the mask is a transformer of the stream, so every
  consumer (and every future connector) gets the same treatment.
- **Testable in isolation**: `MaskPiiTransformTest` covers masking paths,
  missing fields and nested JSON.
- **Unlimited consumption**: you can have one masked connector and one raw
  connector pointing at the same table without duplicating code.
