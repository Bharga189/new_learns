# Debezium + MySQL — module 07: Transactional outbox

Databases can commit rows atomically, but they **cannot** commit a row and
publish to Kafka in the same transaction. The **outbox pattern** solves this:

1. Your application writes domain data **and** a copy of the event into an
   `outbox` table — in one `INSERT … , INSERT …` statement, inside the same
   local transaction.
2. Debezium's `EventRouter` SMT streams the outbox table through the binlog
   and turns each row into a Kafka message — no second system, no dual-write.

## Run it

```bash
cd learn/debezium-mysql-parent
./setup.sh up
./setup.sh deploy outbox

# 1) a "domain" write coupled to an outbox row (the pattern your app follows):
./setup.sh outbox Dana 99.50

# 2) read the routed event
./setup.sh consume --topic outbox.Order
```

## What you observe

The outbox row is written in the same transaction as the `orders` row; the
`EventRouter` then emits:

```jsonc
{
  "type": "ORDER_PLACED",
  "amount": 99.50,
  "customer": "Dana",
  "id": 103
}
```

with message **key** `Dana` (from `aggregate_id`) and the `aggregate_type`
header. The connector config that does this:

```jsonc
{
  "name": "outbox",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "cdc-mysql", "database.port": "3306",
    "database.user": "cdc_user", "database.password": "cdc_pass",
    "database.server.id": "184058",
    "database.include.list": "test_db",
    "table.include.list": "test_db.outbox",
    "topic.prefix": "outbox",
    "snapshot.mode": "initial",
    "transforms": "route",
    "transforms.route.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.route.table.field.event.id": "id",
    "transforms.route.table.field.event.key": "aggregate_id",
    "transforms.route.table.field.event.payload": "payload",
    "transforms.route.table.field.event.timestamp": "created_at",
    "transforms.route.route.by.field": "aggregate_type",
    "transforms.route.table.headers": "aggregate_type",
    "transforms.route.table.expand.json.payload": true,
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false"
  }
}
```

## Why this beats dual-write

| Approach | Atomicity | Complexity |
|---|---|---|
| Two independent writes (orders + Kafka producer) | Crash between them → data without event, or event without data | Manual compensating logic |
| Outbox table + EventRouter | One DB transaction owns both | Router/connect delivers events in the same stream |
| App-side poll loop on a queue table | Polling lag, no transactional semantics | Very manual |
| Kafka Connect outbox SMT | Outbox committed in-app transaction; events follow the binlog | Same reliability as the connector's normal guarantees |

## Notes

- `transforms.route.table.expand.json.payload = true` turns the JSON string in
  `payload` into a proper JSON object value — exactly the shape above.
- `route.by.field` picks the destination topic: the `ORDER_PLACED` event goes
  to `outbox.Order`; other `aggregate_type` values would route elsewhere
  (e.g. `CUSTOMER_CREATED` → `outbox.Customer`).
- The key is the aggregate id so that all events of one aggregate land on the
  same partition (ordered delivery per aggregate).
- Consume with a group that tolerates at-least-once semantics
  (`enable.auto.commit=false`, idempotent sink), because Kafka Connect
  delivers outbox events at-least-once (a restarted relay re-sends them).

## Putting the demo together

`setup.sh` registers the connectors in the order `orders`, `transforms`,
`schema`, `snapshot`, `outbox`; the outbox connector is the last one. Keep
that order: the outbox table is the last thing that changes, and every demo
data insert happens *after* the whole stack is up.
