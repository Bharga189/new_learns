# Debezium + MySQL — module 06: Incremental snapshots + signal

A default snapshot (`snapshot.mode=initial`) reads the whole table once and
then streams. Do it again and again: another full scan. **Incremental
snapshoting** reads the table in **bounded chunks**, records its progress in
the `debezium_signal` table, and can be **stopped, paused or resumed** — even
while the binlog tailer keeps capturing new rows.

## Run it

```bash
cd learn/debezium-mysql-parent
./setup.sh up
./setup.sh deploy snapshot

# 1) start an incremental snapshot
./setup.sh signal incremental test_db.orders

# 2) while the snapshot runs, add rows — the tailer catches them
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "INSERT INTO orders(customer_name, amount, status) VALUES ('Dana', 8.50, 'PENDING');"

# 3) stop the snapshot cleanly (it resumes on next start from the same position)
./setup.sh signal stop test_db.orders
./setup.sh signal status test_db.orders
```

## What you observe

The connector logs chunk progress, e.g.:

```
Reading chunk ... partition=0 offset=0
Reading chunk ... partition=0 offset=100
Processed 200 rows from 2 chunks
```

`debezium_signal` (created by `podman/mysql/init/01-schema.sql`) keeps one row
per snapshotted table:

```sql
mysql> SELECT table_name, last_value, op FROM debezium_signal WHERE signal_id = 'sync';
+------------+-----------+------------------+
| table_name | last_value| op               |
+------------+-----------+------------------+
| test_db.orders | 7      | snapshot         |
+------------+-----------+------------------+
```

On the next start, the connector **continues from `last_value`** instead of
re-scanning rows below it — this is the "incremental" guarantee.

You can also send meta-commands through the signal table:

| SQL sent | Effect |
|---|---|
| `{"data-collections":["test_db.orders"]}` | start `execute-snapshot` |
| `{"data-collections":["test_db.orders"], "snapshot.mode":"incremental"}` | start resumable snapshot |
| `{"data-collections":["test_db.orders"], "snapshot.mode":"blocking"}` | full initial snapshot |
| `{"data-collections":["test_db.orders"], "snapshot.mode":"incremental"` | resume |
| `{"data-collections":["test_db.orders"], "snapshot.mode":"stop"}` | graceful stop |
| `{"type":"log","data":"hello"}` | append to the connector's log |

## Connector config

`src/main/resources/connectors/incremental-snapshot.json`:

```jsonc
{
  "name": "mysql-snapshot",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "cdc-mysql", "database.port": "3306",
    "database.user": "cdc_user", "database.password": "cdc_pass",
    "database.server.id": "184057",
    "database.include.list": "test_db",
    "table.include.list": "test_db.orders",
    "topic.prefix": "mysql-cdc-snapshot",
    "snapshot.mode": "incremental",
    "incremental.snapshot.chunk.size": "100",
    "signal.data.collection": "test_db.debezium_signal",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false"
  }
}
```

Key properties:

| Property | Meaning |
|---|---|
| `snapshot.mode: incremental` | bounded chunks, progress in signal table |
| `incremental.snapshot.chunk.size` | rows per chunk (100 here) |
| `signal.data.collection` | the `debezium_signal` table |

## Partition by primary key

The connector splits a table into chunks by its primary key column (here `id`),
reads `chunk.size` rows per pass, and resumes from the highest `id` it saw.
Because each chunk is a bounded read, a long-running snapshot never blocks:

- **New rows** written by the app are captured by the binlog tailer and emitted
  on the same topic (`mysql-cdc-snapshot.test_db.orders`).
- **The snapshot itself** streams chunks in sequence, so a consumer sees a
  natural transition from `op: r` rows to `op: c/u/d` rows.

## Notes

- `snapshot.mode=incremental` requires a **primary key** on the table (the demo
  `orders.id` qualifies).
- Stop gracefully (`stop-snapshot`) so the connector records its final offset;
  force-stop mid-chunk leaves the signal table stale and the next run re-reads
  that chunk.
- The `SignalSender` class (`06/src/main/java/io/buffy/cdc/snapshot/SignalSender`)
  is a reusable helper: give it a table name and it writes the right payload
  into `debezium_signal`, hiding the JSON construction.
