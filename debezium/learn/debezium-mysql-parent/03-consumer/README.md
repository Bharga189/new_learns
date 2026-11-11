# Debezium + MySQL — module 03: Consume the CDC envelope

Read raw Debezium events from Kafka with a plain Java Kafka consumer. This is
the reference consumer for every other module: the same topic names, group
ids and envelope shapes you see here are what production consumers use.

## Run it

```bash
cd learn/debezium-mysql-parent
./setup.sh up
./setup.sh deploy all          # must be before you insert demo data
./setup.sh consume
# or target another topic:
./setup.sh consume --topic mysql-cdc.test_db.orders --max 10
./setup.sh consume --group my-group --topic mysql-cdc.test_db.customers
```

## Expected output

With the default topic `mysql-cdc.test_db.orders` you should see, for each row
of the initial snapshot, an `r` row *first*, then the live changes as they come:

```jsonc
{ "op": "r", "table": "orders", "id": 1, "customer_name": "Alice", "amount": 10.00, "status": "PENDING" }
{ "op": "c", "table": "orders", "id": 2, "customer_name": "Bob", "amount": 20.00, "status": "PENDING" }
{ "op": "u", "table": "orders", "id": 1, "amount": 11.00, "status": "SHIPPED" }
{ "op": "d", "table": "orders", "id": 2, "amount": 22.00 }
```

`op` values: `r` = snapshot row, `c` = create, `u` = update, `d` = delete
(tombstone, `after` is null).

## Envelope for module 03

The consumer prints the full event with `source` metadata — the same object you
saw in the module `02` connector JSON:

```jsonc
{
  "before": null,                     // null on create
  "after":  { "id": 1, "customer_name": "Alice", "amount": 11.00, "status": "SHIPPED" },
  "source": {
    "version": "2.6.0.Final", "connector": "mysql", "name": "mysql-cdc",
    "ts_ms": 1730000000000, "snapshot": false,
    "db": "test_db", "table": "orders", "server_id": 184054,
    "file": "mysql-bin.000003", "pos": 2048, "row": 0
  },
  "op": "u",
  "ts_ms": 1730000000000,
  "transaction": null
}
```

Useful fields in `source`:

- `snapshot` — `false` for live change, `true` for the initial snapshot.
  Incremental snapshots use `snapshot: "incremental"` instead.
- `file` / `pos` — binlog position, used for exact recovery.
- `server_id`, `thread`, `query` — internal details of the replication stream.

## Insert/update/delete from the host

```bash
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "INSERT INTO orders(customer_name, amount, status) VALUES ('Carol', 42.00, 'PENDING');"
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "UPDATE orders SET amount = amount * 1.1 WHERE customer_name = 'Carol';"
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "DELETE FROM orders WHERE customer_name = 'Carol';"
```

## Notes

- `--max N` caps printed lines; `--group <id>` gives you true fan-out:
  multiple groups read the same topic independently.
- The consumer ignores schemas (`key/value.converter.schemas.enable=false`),
  like many production consumers that only need the `after` payload.
