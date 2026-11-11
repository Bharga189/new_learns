# Debezium + MySQL — module 05: Schema evolution

When a table changes (`ALTER TABLE ... ADD/DROP/MODIFY COLUMN`), Debezium
publishes a **schema-change event** to the topic `<prefix>` while the normal
`c/u/d` events on the data topic carry the new column. This module shows how a
consumer turns those DDL events into something useful: apply the change to its
own schema, log the new column, and keep running.

## Run it

```bash
cd learn/debezium-mysql-parent
./setup.sh up
./setup.sh deploy schema
./setup.sh schema --apply-ddl
# or:   mvn -q -pl 05-schema-evolution exec:java -- --apply-ddl
```

If the demo connector is already running, deploy it **before** the DDL runs so
the schema-change topic exists:

```bash
./setup.sh deploy schema
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "ALTER TABLE orders ADD COLUMN loyalty_tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE';"
```

## What you observe

1. The consumer prints the schema-change topic `mysql-cdc-schema`:

```
schema-change topic: mysql-cdc-schema
schema-name:        test_db
table-name:         orders
dml:                ALTER TABLE `test_db`.`orders` ADD COLUMN `loyalty_tier` varchar(20) NOT NULL DEFAULT 'BRONZE'
```

2. A plain consumer on `mysql-cdc.test_db.orders` now sees the column in
   every `after` envelope (module `03`):

```jsonc
{ "before": { "id": 1, "customer_name": "Alice", "amount": 10.00, "status": "PENDING" },
  "after": { "id": 1, "customer_name": "Alice", "amount": 10.00, "status": "PENDING",
             "loyalty_tier": "BRONZE" }, ... }
```

## Connector config highlights

`src/main/resources/connectors/schema-evolution.json`:

```jsonc
{
  "name": "mysql-schema",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "cdc-mysql", "database.port": "3306",
    "database.user": "cdc_user", "database.password": "cdc_pass",
    "database.server.id": "184056",
    "database.include.list": "test_db",
    "table.include.list": "test_db.orders",
    "topic.prefix": "mysql-cdc-schema",
    "include.schema.changes": "true",          // default in Debezium 2.x
    "schema.history.internal.kafka.bootstrap.servers": "cdc-kafka:9092",
    "snapshot.mode": "initial",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false"
  }
}
```

## Why this is useful

- **DDL is data** — you can gate releases on schema changes, backfill data only
  for the columns that changed, or keep an audit trail of schema evolution.
- **`schema.history.internal`** records which schema version produced each
  event, so a schema change is understood only by consumers that are running
  with the same history (in Kafka Connect, `__schemas` is Kafka's schema store
  for this; in the embedded engine, a file).
- The consumer here is intentionally simple: it dumps the DDL and the new
  columns. In production you would write a small adapter that maps the
  `tableChanges` payload to `ALTER TABLE`, `CREATE INDEX`, etc.

## The schema-change event payload

For the DDL above the connector publishes to `mysql-cdc-schema`:

```jsonc
{
  "schema": { ... },
  "payload": {
    "databaseName": "test_db",
    "schemaName": "orders",
    "tableChanges": [ {
      "table": "orders",
      "ddl": "ALTER TABLE `test_db`.`orders` ADD COLUMN `loyalty_tier` varchar(20) NOT NULL DEFAULT 'BRONZE'",
      "columns": [ { "name": "loyalty_tier", "jdbcType": 12, "nativeType": "varchar",
                     "typeName": "varchar", "configuration": { "nullable": false,
                     "defaultValue": "BRONZE", "columnSize": 20, "decimalDigits": 0 } } ]
    } ]
  }
}
```

A production schema-drift monitor would parse `tableChanges[].ddl` and send it
to your migration tool. That is exactly what module `05` prints and logs.
