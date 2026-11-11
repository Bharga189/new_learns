# Debezium + MySQL: embeddable CDC engine

Minimal, no-broker example. The whole Debezium engine runs inside this Java
program — no Kafka, no ZooKeeper, no Connect. It only needs a MySQL 8 instance
with binary logging enabled (see `../podman/`).

## Run it

```bash
cd learn/debezium-mysql-parent

# 1) MySQL only (Kafka is NOT used by this module)
podman compose -f ../podman/compose.yaml up -d mysql

# 2) run the demo
mvn -q -pl 01-basic-embedded exec:java
#  or:
./setup.sh basic
```

## What to observe

- The app prints a **snapshot** of `orders`/`customers` first (`op: r`), then
  every `c` (create), `u` (update), `d` (delete) change as a JSON envelope:
  `before`, `after`, `source`, `op`, `ts_ms`.
- To see it in action, in another terminal:

```bash
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "INSERT INTO orders(customer_name, amount, status) VALUES ('Carol', 42.00, 'PENDING');"
```

- Stop the app (Ctrl+C), start it again, and you return to the exact binlog
  position — no duplicate events, because **file offset storage** remembers
  where you stopped (`data/basic-embedded/offset`, `schema`).

## Key settings in `CdcConfig`

| Property | Value | Why |
|---|---|---|
| `database.include.list` | `test_db` | Only watch this schema |
| `table.include.list` | `orders,customers` | Finer-grained inclusion |
| `snapshot.mode` | `initial` | Full read on first start |
| `tombstones.on.delete` | `false` | Demo-friendly; no tombstones in output |
| `offset.storage` / `schema.history.internal` | `file` | Embedded engine needs a local store |

## Notes

- The MySQL connector jar is loaded via `ServiceLoader` (the `CdcConfig` adds
  `debezium-connector-mysql`, `debezium-storage-file`, `debezium-embedded`).
  That is why `01` works on a plain classpath while the later modules rely on
  the fat plugin jars Kafka Connect ships with.
