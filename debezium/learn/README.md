# Debezium + MySQL: hands-on CDC from basic to advanced

A single Maven multi-module project with **seven feature modules** (basic → advanced)
plus a **Podman local stack** so you can run every example on your machine with no
Docker, no cloud account and no manual database administration.

- **Basic**: run Debezium's embedded engine inside a plain Java program.
- **Advanced**: deploy connectors through Kafka Connect's REST API.
- Each production/observability feature is its own module, so `read the code of
  module N` never requires reading module N+1.

---

## What you get

| Module | Feature | Key idea |
|---|---|---|
| [01-basic-embedded](learn/debezium-mysql-parent/01-basic-embedded) | Embedded engine (no Kafka needed) | Debezium runs as a library inside your app |
| [02-connect-deploy](learn/debezium-mysql-parent/02-connect-deploy) | Connector admin REST CLI | Deploy/verify/remove connectors with `curl` |
| [03-consumer](learn/debezium-mysql-parent/03-consumer) | JSON consumer | Read the raw CDC envelope from Kafka |
| [04-transforms](learn/debezium-mysql-parent/04-transforms) | Custom Single Message Transform (SMT) | Mask sensitive fields before they reach Kafka |
| [05-schema-evolution](learn/debezium-mysql-parent/05-schema-evolution) | Schema-change capture | Turning a new column into a CDC event |
| [06-incremental-snapshot](learn/debezium-mysql-parent/06-incremental-snapshot) | Incremental snapshot + signal | Restartable snapshots driven by the `debezium_signal` table |
| [07-outbox](learn/debezium-mysql-parent/07-outbox) | Outbox pattern | Reliable messaging without a dual-write |

---

## Quick start

```bash
cd learn
./setup.sh build      # compile the jars (needed once)
./setup.sh up         # start MySQL + Kafka + Connect
./setup.sh deploy all # register the five demo connectors
./setup.sh status     # health check
# now start a console consumer and watch events appear:
./setup.sh consume
```

Everything is local and disposable:

```bash
./setup.sh down -v    # stop + remove volumes (data lost)
./setup.sh clean      # nuke data, jars, offsets and connector state
./setup.sh reset      # rebuild everything from scratch
```

`run.sh` at the repository root is a shortcut for `./setup.sh all`
(build + start + deploy).

### The Podman stack

| Container | Image | Role | Ports |
|---|---|---|---|
| `cdc-mysql` | `mysql:8.0` | The database whose binlog is streamed (seed schema + demo DB) | `3306` |
| `cdc-kafka` | `apache/kafka:3.7.0` | Message broker, KRaft mode (no ZooKeeper) | `9092` |
| `cdc-connect` | `quay.io/debezium/connect:2.6` | Kafka Connect with the Debezium MySQL + outbox + scripting plugins | `8083` |

### The local MySQL database

`podman/mysql/init/01-schema.sql` creates `test_db` with four tables:

- `orders` — the main demo table (customer name / amount / status / loyalty tier).
- `customers` — the PII demo subject (name, email).
- `debezium_signal` — the Signal table that makes snapshots restartable.
- `outbox` — the outbox queue for the transactional-outbox demo.

A `cdc_user` is created with the exact privileges a connector needs
(`SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT`).

---

## The CDC story behind every module

### 1. What the binlog actually is

MySQL keeps all writes in a **binary log** (`binlog`), one file per rotation
(`mysql-bin.000003`, …), each entry carrying the raw SQL plus the row images
(`BINLOG_FORMAT=ROW`). Debezium reads that stream as a replica. It opens its own
connection with a **unique `server-id`** (never collide with any app database),
asks for `binlog-format=ROW`, `binlog-row-image=FULL` and `binlog-row-image`
metadata, then reads:

- `COMMIT` events that group changes into transactions.
- `TABLE_MAP` / `ROWS_QUERY` events describing how to decode the rows.
- The **`before` / `after` / `source` / `op` / `ts_ms`** envelope that each row change carries.

For the connector to see changes, MySQL **must** be configured:
`server-id`, `log_bin`, `binlog_format=ROW`, `binlog_row_image=FULL` (and
`binlog_expire_logs_seconds` to avoid unbounded growth).

### 2. Two ways to run Debezium, one library

- **Embedded engine** (module `01`): `CdcConfig` builds an
  `Engine` with `file` offset/schema-history storage and the MySQL connector
  jar as a `ServiceLoader` dependency. No Kafka, no Connect. Best for
  CLI tools, ETL jobs and testing.
- **Kafka Connect** (modules `02`-`07`): the same connector is shipped as a
  connector plugin in the Debezium Connect image; you POST JSON configs to
  `http://localhost:8083/connectors`. Connect handles lifecycle, offsets
  (Kafka offset topic), schema history (Kafka offset topic), REST, scaling.

### 3. Events arrive on `<topic-prefix>.<database>.<table>`

Config `topic.prefix=mysql-cdc` turns a write to
`test_db.orders` into the Kafka topic `mysql-cdc.test_db.orders`.

Envelope shape (most recent versions of the connector):

```jsonc
{
  "before": { "id": 7, "customer_name": "Eve", "amount": 12.50, "status": "PENDING" },
  "after":  { "id": 7, "customer_name": "Eve", "amount": 12.50, "status": "SHIPPED" },
  "source": {
    "version": "2.6.0.Final", "connector": "mysql", "name": "mysql-cdc",
    "ts_ms": 1730000000000, "snapshot": false, "db": "test_db", "table": "orders",
    "server_id": 184054, "file": "mysql-bin.000003", "pos": 2048, "row": 0
  },
  "op": "u",                     // c = create, u = update, d = delete, r = snapshot row
  "ts_ms": 1730000000000,
  "transaction": null
}
```

- `op: r` = row read during the snapshot, `op: c/u/d` = live change.
- When Debezium writes with `schema.history.internal`, the next consumer sees
  an extra `schema` JSON field; with a JSON converter + `schemas.enable=false`
  (as in this demo), the payload stays a plain value.
- Deleted rows become **tombstones** (`before` present, `after: null`) so Kafka
  compacted topics can drop the last value — Connect uses that for cleanup.

### 4. Offsets and schema history are NOT optional bureaucracy

The embedded engine and Connect both keep their position in MySQL via
**offset storage**; the connector would resume at the exact same binlog position
after a restart. Without it, every restart re-runs the whole history and you get
duplicate events.

- Embedded: `io.debezium.storage.file.history.FileSchemaHistory` backed by the
  module-local `data/<demo>/schema` + `data/<demo>/offset` files.
- Connect: Kafka topics `__schemas` and `__debezium_offsets`, which the
  connectors never need you to create.

### 5. Transforms let you change the data shape between the source and Kafka

**Built-in** SMTs:

- `org.apache.kafka.connect.transforms.ExtractNewRecordState` (`unwrap`) — pull
  the `after` struct out of the envelope, optionally renaming/dropping fields
  (`delete.handling.mode=rewrite` drops the `before` key entirely).
- `org.apache.kafka.connect.transforms.MaskField` — static masking.

**Custom SMT** (module `04`): `MaskPiiTransform` uses
`MapStruct`/`JsonNode` scanning to hide any configured field
(value replaced by `***`). It is compiled against Kafka Connect's API, dropped
into `podman/plugins/*.jar` and, once Connect restarts, it is picked up by the
connector plugin loader.

### 6. Schema evolution is a first-class CDC feature

When an app adds a column (`ALTER TABLE orders ADD loyalty_tier VARCHAR(20)`),
Debezium appends a **schema-change event** to the topic `<prefix>` and the
column appears in `before`/`after` of subsequent events. Module `05` consumes
that topic, replays the DDL against its own copy of the schema and prints what
it learned. Requires the connector's `include.schema.changes=true` (the default
in Debezium 2.x) and a connector deployed **before** the DDL runs, so it can be
present in the first place.

### 7. Incremental snapshots — restartable, not full scans

A **blocking initial snapshot** (`snapshot.mode=initial`, default) reads the
whole table, then switches to streaming. Re-running it twice means a second full
scan. An **incremental snapshot** (`snapshot.mode=incremental`) instead:

1. reads a bounded "chunk" of rows per pass,
2. records its progress in the `debezium_signal` table,
3. can be **paused, resumed or stopped** by writing to that table.

The connector partitions a table by the primary key column (in this demo,
`id`), reads chunks of 100 rows, persists `/debezium_signal/<schema>/<table>`
rows, and resumes from the highest `id` it saw. While it runs you can still
write new rows; they are caught by the binlog tailer and appended to the same
topics.

### 8. The outbox pattern: one writer, no dual commit

Databases don't support "commit a row AND write to Kafka atomically". The
outbox pattern solves this by **storing events in a normal table** (`outbox`),
then a relay (here: Debezium's `io.debezium.transforms.outbox.EventRouter` SMT)
drains that table via the binlog in the *same transaction*:

- Transactional outbox table + connector = exactly-once against the outbox,
  at-least-once against Kafka (anackered/de-duplicated reads needed on the
  consumer for true exactly-once).
- `EventRouter` routes to `outbox.Order` based on the `aggregate_type` field,
  and expands the JSON payload so the value is the event itself.

Key settings:

```jsonc
"transforms": "route",
"transforms.route.type": "io.debezium.transforms.outbox.EventRouter",
"transforms.route.table.field.event.id": "id",
"transforms.route.table.field.event.key": "aggregate_id",
"transforms.route.table.field.event.payload": "payload",
"transforms.route.table.field.event.timestamp": "created_at",
"transforms.route.route.by.field": "aggregate_type",
"transforms.route.table.headers": "aggregate_type",
"transforms.route.table.expand.json.payload": true
```

---

## The five connectors (what `deploy all` registers)

| Connector name | Prefix | What we write in the demo |
|---|---|---|
| `mysql-orders` | `mysql-cdc` | every `op` `c/u/d/r` on `orders` |
| `mysql-customers-masked` | `mysql-cdc-transforms` | same, through the SMT |
| `mysql-schema` | `mysql-cdc-schema` | events of `orders` + the schema-change topic |
| `mysql-snapshot` | `mysql-cdc-snapshot` | `orders`, restartable snapshots |
| `outbox` | `outbox` | `outbox` rows → `outbox.Order` (subject key `aggregate_id`) |

Every `server.id` (184054–184058) is different so all five connectors coexist
on the same binlog; all of them use the same `topic.prefix` and database
so they read the same stream at their own pace.

---

## Module walkthroughs

### 01 — Embedded engine: CDC without a broker

**Goal.** See the change stream with zero infrastructure besides MySQL.

**Run it.**
```bash
cd learn/debezium-mysql-parent
./setup.sh up                                   # only MySQL is needed for this module
mvn -q -pl 01-basic-embedded exec:java          # from the parent directory
```
(The same thing `./setup.sh basic` does.)

**Observe.** The program prints `snapshot` rows (`op: r`) first, then every
`c/u/d` event as a JSON envelope. On exit (Ctrl+C) the offset files
`01-basic-embedded/data/basic-embedded/offset` + `schema` survive; point the
config at the same files again and you resume.

**Key `io.buffy.cdc.basic.EmbeddedCdcApp`** properties:

```java
database.include.list = test_db          // only this schema is watched
table.include.list  = orders,customers   // finer-grained inclusion
snapshot.mode       = initial            // full read on first start
tombstones.on.delete= false              // demo-friendly: no tombstones in output
offset.storage      = file                 // embedded offset store
schema.history.internal = file           // embedded schema store
```

The connector jar is loaded through the ServiceLoader (`io.buffy.cdc.common.CdcConfig`
adds `debezium-connector-mysql` + `debezium-storage-file` + `debezium-embedded` to the
engine), which is how `01` runs on a bare classpath while `02`-`07` rely on the
fat plugin jars shipped in the Connect image.

### 02 — Connector admin REST CLI

**Goal.** Work with Kafka Connect as a REST API: create, verify and remove
connectors.

**Run it.**
```bash
./setup.sh up                 # starts the stack
./setup.sh deploy orders      # POST connector config
curl -s http://localhost:8083/connectors/mysql-orders/status | jq .state
./setup.sh consume
```

**Observe.** `connect/monitor.py`-style `ConnectAdmin` prints the REST responses.
`orders.json` is the connector config:

```jsonc
{
  "name": "mysql-orders",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "cdc-mysql", "database.port": "3306",
    "database.user": "cdc_user", "database.password": "cdc_pass",
    "database.server.id": "184054",
    "database.include.list": "test_db",
    "table.include.list": "test_db.orders",
    "topic.prefix": "mysql-cdc",
    "schema.history.internal.kafka.bootstrap.servers": "cdc-kafka:9092",
    "snapshot.mode": "initial",
    "tombstones.on.delete": false,
    "transforms": "unwrap",
    "transforms.unwrap.type": "org.apache.kafka.connect.transforms.ExtractNewRecordState",
    "transforms.unwrap.drop.tombstones": "false",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false"
  }
}
```

Why the JSON converters with `schemas.enable=false`? Without them, every message
has a schema id + the record value as a nested JSON object; with them, the value
is the raw event object, which the demo consumers just print. For production,
`avro`/JSON with schema registry is the norm — this demo keeps the "raw envelope
as produced by the connector" visible.

### 03 — Consume the envelope

**Goal.** Read the raw CDC stream from Kafka in a plain Java program.

**Run it.**
```bash
./setup.sh up
./setup.sh deploy all
./setup.sh consume                        # default topic: mysql-cdc.test_db.orders
./setup.sh consume --topic mysql-cdc.test_db.orders --max 5
./setup.sh consume --group my-group        # one consumer per group, same topic -> fan-out
```

**Observe.** Each line is one envelope from the table above: `before` (null on
create), `after`, `source`, `op`, `ts_ms`. Insert/update/delete from a MySQL
client and they appear in seconds:

```bash
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "INSERT INTO orders(customer_name, amount, status) VALUES ('Carol', 42.00, 'PENDING');"
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "UPDATE orders SET amount = amount * 1.1 WHERE customer_name = 'Carol';"
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "DELETE FROM orders WHERE customer_name = 'Carol';"
```

Expect `op: r` (snapshot), `c`, `u`, `d` for the same row, in that order.
`--max N` caps lines; `--group` separates consumer groups.

### 04 — Custom SMT: mask PII before Kafka

**Goal.** Transform the payload of every message with a pluggable, compiled-in
transform, not a config string.

**Run it.**
```bash
./setup.sh build          # compiles + copies the jar into podman/plugins/
podman restart cdc-connect   # only if Connect was already running
./setup.sh deploy transforms
./setup.sh consume --topic mysql-cdc-transforms.test_db.customers
```

**Observe.** The customer table contains email addresses; the connector's JSON
payload shows the field **redacted to `***`**, while the (`__op`,
`__source_ts_ms`, …) envelope fields the connector adds are untouched. The same
topic without the transform (module `02` connector) keeps the emails.

**Why `build` then `restart`?** Kafka Connect loads connector plugins from the
`plugins` directory at startup; a brand-new jar is invisible to a running
Connect, hence the restart. `podman/restart-connect.sh` is a convenience wrapper
if you prefer; `setup.sh build` does it for you.

**How it works.** `MaskPiiTransform` implements Kafka Connect's
`Transformation<R extends ConnectRecord<R>>` + `Configurable`:

```java
CONFIG_DEF = new ConfigDef()
  .define("mask.fields", ... )        // comma-separated dotted field paths
  .define("mask.replacement", "..."); // default "***"

public void configure(Map<String, String> configs) { ... }
public R apply(R record) { ... }      // walks the value tree, replaces matches
```

Conventions: keep the `Transformation` interface, use `ConfigDef` for every
parameter, and never touch the key schema/value schema — a transform that
changes schemas breaks Connect's Contract tests and the runtime serializer.

### 05 — Schema evolution: DDL is data

**Goal.** Turn `ALTER TABLE` into an event and let a consumer apply it.

**Run it.**
```bash
./setup.sh up
./setup.sh deploy schema
./setup.sh schema --apply-ddl     # or: mvn -q -pl 05-schema-evolution exec:java -- --apply-ddl
```

**Observe.** `orders` gains a `loyalty_tier` column. Module `05` prints:

```
schema-change topic: mysql-cdc-schema
schema-name:        test_db
table-name:         orders
dml:                ALTER TABLE `test_db`.`orders` ADD COLUMN `loyalty_tier` varchar(20) NOT NULL DEFAULT 'BRONZE'
new column:         loyalty_tier = BRONZE
```

After that, a plain consumer on `mysql-cdc.test_db.orders` sees
`loyalty_tier: BRONZE` in every `after` object.

**Connector config highlights** (`schema-evolution.json`):

```jsonc
"topic.prefix": "mysql-cdc-schema",
"database.include.list": "test_db",
"table.include.list": "test_db.orders",
"include.schema.changes": "true",          // the old and new default
"schema.history.internal.kafka.bootstrap.servers": "cdc-kafka:9092",
"snapshot.mode": "initial"
```

**Why the consumer must be running when DDL happens?** The schema-change event
is published to `mysql-cdc-schema` as soon as the DDL commits. If no
consumer/connector is listening, the event sits in Kafka (or is discarded by
retention). In a real system you would have exactly one consumer group for
schema changes; this demo runs one.

### 06 — Incremental snapshots + the signal table

**Goal.** Snapshot a large table in bounded chunks, then pause/resume/stop the
snapshot via the `debezium_signal` table — no restart needed.

**Run it.**
```bash
./setup.sh up
./setup.sh deploy snapshot
./setup.sh signal incremental test_db.orders

# while the snapshot is running, insert rows — they will be caught by the binlog tailer
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "INSERT INTO orders(customer_name, amount, status) VALUES ('Dana', 8.50, 'PENDING');"

./setup.sh signal stop test_db.orders   # stop cleanly
./setup.sh signal log hello             # the connector persists progress here
./setup.sh signal status test_db.orders
```

**Observe.** Connector logs show chunk reads, e.g.
`Reading chunk ...` / `Processed 100 rows`; `debezium_signal` holds one row per
table with `value` = highest processed key + `op=` snapshot markers. On restart
the connector **continues from the last watermark**, not from the beginning.

**What `signal incremental <db>.<table>` sends** to the `debezium_signal`
table:

```sql
INSERT INTO debezium_signal (signal_id, type, data) VALUES ('sync', 'execute-snapshot', '{"data-collections":["test_db.orders"]}');
```

- `execute-snapshot` → start an incremental snapshot.
- `pause-snapshot` / `resume-snapshot` / `stop-snapshot` → pause/resume/stop.
- `log <message>` → append a line to the connector's `log` file.

**Key config** (`incremental-snapshot.json`):

```jsonc
"snapshot.mode": "incremental",
"incremental.snapshot.chunk.size": "100",
"signal.data.collection": "test_db.debezium_signal",
"table.include.list": "test_db.orders"
```

### 07 — Transactional outbox

**Goal.** Publish events from the same transaction that owns the domain data,
avoiding the dual-write problem.

**Run it.**
```bash
./setup.sh up
./setup.sh deploy outbox
./setup.sh outbox Dana 99.50     # inserts a row into outbox
./setup.sh consume --topic outbox.Order
```

**Observe.** The outbox row is written inside the same DB transaction as the
`orders` row; Debezium's `EventRouter` then routes it to Kafka topic
`outbox.Order` with key `Dana` and a JSON value built from the `payload` column
(JSON expanded, `aggregate_type` in headers). Consumer output looks like:

```jsonc
{ "type": "ORDER_PLACED", "amount": 99.50, "customer": "Dana", "id": 103 }
```

**Why not connect `outbox` to the same `mysql-cdc` prefix?** Module `07` uses
its own prefix (`outbox`) so it is a separate topic namespace, and the
`EventRouter` config replaces the destination completely. Two connectors on the
same topic prefix would both stream the outbox table, producing duplicate
events — the outbox topic is deliberately hidden from the CDC connector.

---

## Connector configuration that every module shares

All module connectors live in JSON under the module's `src/main/resources/connectors/`.
The fields that matter most:

| Property | Meaning |
|---|---|
| `database.hostname` / `port` / `user` / `password` | MySQL connection |
| `database.include.list` | schemas watched (replication user needs `SELECT` + `SHOW DATABASES`) |
| `table.include.list` | tables watched (prefer this to blacklisting) |
| `topic.prefix` | Kafka namespace of every event |
| `database.server.id` | unique replica id, never collide with app servers |
| `database.server.name` | connector logical name used for `source.name` |
| `schema.history.internal.kafka.bootstrap.servers` | where offsets + schema history live (Connect) |
| `snapshot.mode` | `initial` | `incremental` |
| `include.schema.changes` | publish DDL as events (2.x default on) |
| `signal.data.collection` | restartable-snapshot control table |
| `incremental.snapshot.chunk.size` | rows per incremental chunk |
| `transforms` / `transforms.*.type` | SMT chain (order matters, run before converters) |
| `key.converter` / `value.converter` | JSON (demo) / Avro (production) |

---

## Producing demo data

```bash
# 01-basic-embedded / 03-consumer / 05-schema-evolution / 06-incremental-snapshot
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db <<'SQL'
INSERT INTO orders(customer_name, amount, status) VALUES ('Carol', 42.00, 'PENDING');
UPDATE orders SET amount = 46.20 WHERE customer_name = 'Carol';
DELETE FROM orders WHERE customer_name = 'Carol';
SQL

# 04-transforms (PII masking, module 04)
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db <<'SQL'
INSERT INTO customers(id, customer_name, email, phone) VALUES
  (1, 'Alice', 'alice@example.com', '+1-555-0100'),
  (2, 'Bob',   'bob@example.com',   '+1-555-0200');
UPDATE customers SET email = 'alice.new@example.com' WHERE id = 1;
SQL

# 07-outbox (transactional outbox)
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db <<'SQL'
INSERT INTO outbox(execute_at, aggregate_type, aggregate_id, payload, created_at)
VALUES (NOW(), 'ORDER_PLACED', 'Dana', '{"type":"ORDER_PLACED","amount":99.50,"customer":"Dana"}', NOW());
SQL
```

Put these in `podman/mysql/init/02-demo.sql` later if you want a repeatable
scenario; for quick experiments a heredoc is enough.

---

## Useful Podman commands

```bash
podman ps -a                                  # all containers
podman compose -f podman/compose.yaml logs -f # live logs (MySQL/Kafka/Connect)
podman compose -f podman/compose.yaml logs -f cdc-connect
podman exec -it cdc-mysql mysql -ucdc_user -pcdc_pass test_db
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass -e "SHOW DATABASES; USE test_db; SHOW TABLES;"
podman compose -f podman/compose.yaml down -v   # tear down (volumes = data)
podman compose -f podman/compose.yaml up -d     # restart everything without rebuilding
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `podman: command not found` | Podman not installed or `podman` on PATH in the shell you opened | `podman machine init` + `podman machine start` on macOS/WSL; verify `podman system connection list` |
| `connect` exits immediately | Java 17+ runtime mismatch in the container | Use the provided `quay.io/debezium/connect:2.6` image; do not point it at a JDK 21+ host |
| Connector `state=Failed` | Missing privileges, port, server.id clash or `database.include.list` misspelled | `podman logs cdc-connect`; test with `mysql -h 127.0.0.1 -P 3306 -uccdc_user -pcdc_pass -e "SHOW DATABASES"` |
| Events stop after restart | Offset files/schema history lost | `podman compose down` without `-v` keeps volumes; `clean` wipes offsets |
| `topic not found` | Connector deployed **after** the data arrived | Deploy connectors **before** producing demo data |
| MQTT/`schema.history` fields in output | Connector produced with `schemas.enable=true` | Set `key.converter.schemas.enable=false` for raw-envelope demos |
| `unknown column loyalty_tier` | Connector deployed before `05-schema-evolution` applied DDL | Deploy connectors in order (`deploy all` already does it) |

---

## Production checklist (what the demo does NOT do)

- **TLS / hostname verification** for MySQL and Kafka.
- **Schema registry + Avro/Protobuf** converters instead of raw JSON.
- **Offset migration** (`FileSchemaHistory` → `KafkaOffsetBackingStore`) before big clusters.
- **Monitoring**: Debezium JMX metrics (`debezium.<connector>.records.lag`) + Kafka.offset tracking.
- **`debezium_signal` + `outbox` production hardening**: synthetic keys, partition keys by aggregate,
  idempotent consumers, exactly-once via transactions (Kafka + connector transactions are experimental).
- Never run MySQL with `binlog_row_image=MINIMAL` and never reuse a Jinja/Confd-managed
  `server-id` across containers.
- Use a dedicated replication user; never grant `FILE`, `PROCESS`, `SUPER` or
  `SHUTDOWN` to it.

---

## Project layout

```
learn/
├── README.md                 # this file
├── run.sh                    # shortcuts: all | build | up | deploy ...
├── setup.sh                  # full local-stack CLI
└── podman/
    ├── compose.yaml          # mysql + kafka + connect
    ├── mysql/
    │   ├── my.cnf            # binlog settings
    │   └── init/01-schema.sql  # the demo database
    └── plugins/.gitkeep      # custom SMT jar dropped in by scripts
└── debezium-mysql-parent/
    ├── pom.xml               # one reactor, one Debezium BOM, exec plugin
    ├── common/               # CdcConfig (engine wiring), JDBC helpers, JSON support
    ├── 01-basic-embedded/    # embedded engine demo
    ├── 02-connect-deploy/    # Connect REST CLI
    ├── 03-consumer/          # Kafka consumer
    ├── 04-transforms/        # custom SMT + demo
    ├── 05-schema-evolution/  # schema-change consumer
    ├── 06-incremental-snapshot/ # signal-driven restartable snapshot
    └── 07-outbox/            # transactional outbox demo
```

---

## Further reading

- [Debezium documentation](https://debezium.io/documentation/) — connectors, SMTs, transforms.
- [MySQL: binary log concepts](https://dev.mysql.com/doc/refman/8.0/en/binary-log.html).
- [Debezium Kafka Connect docs](https://debezium.io/documentation/reference/stable/connect.html).

