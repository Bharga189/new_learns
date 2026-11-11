# Debezium + MySQL — module 02: Connector admin REST CLI

Work with Debezium through **Kafka Connect's REST API** instead of embedding
the engine: create, inspect and remove connectors. This is how production
deployments run, and it is the gateway for all modules 03–07.

## Run it

```bash
cd learn/debezium-mysql-parent
./setup.sh up                     # MySQL + Kafka + Connect up
./setup.sh deploy orders          # register the connector
curl -s http://localhost:8083/connectors/mysql-orders/status | jq .state
./setup.sh consume                # watch events while you insert rows

# teardown (from another service): use ConnectAdmin or kubectl
./setup.sh down
```

## How it works

`ConnectAdmin` is a small REST client around `http://localhost:8083/connectors`:

```java
GET    /connectors                 // list connectors
POST   /connectors                 // register a connector ({ "name": ..., "config": { ... } })
GET    /connectors/{name}/status   // state, connector/type/task state
GET    /connector-plugins          // list installed connector plugins
DELETE /connectors/{name}          // delete a connector
PUT    /connectors/{name}/restart // restart a connector (used by the "restart" command)
```

The connector definition lives in `src/main/resources/connectors/orders.json`.

### `orders.json` — the Debezium MySQL connector

```jsonc
{
  "name": "mysql-orders",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "cdc-mysql",
    "database.port": "3306",
    "database.user": "cdc_user",
    "database.password": "cdc_pass",
    "database.server.id": "184054",
    "database.include.list": "test_db",
    "table.include.list": "test_db.orders",
    "topic.prefix": "mysql-cdc",
    "schema.history.internal.kafka.bootstrap.servers": "cdc-kafka:9092",

    // lifecycle
    "snapshot.mode": "initial",
    "tombstones.on.delete": false,

    // plain JSON envelopes (no schema registry) so the raw CDC payload is visible
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false"
  }
}
```

Why `schemas.enable=false`? The default producer schema would wrap every value
in a `schema` + `payload` JSON object. Setting it to `false` gives you the
**raw envelope** Debezium writes — exactly what module `03` consumes:

```jsonc
{
  "before": { "id": 7, "customer_name": "Eve", "amount": 12.50, "status": "PENDING" },
  "after": { "id": 7, "customer_name": "Eve", "amount": 12.50, "status": "SHIPPED" },
  "source": { "version": "2.6.0.Final", "connector": "mysql", "name": "mysql-cdc",
              "ts_ms": 1730000000000, "snapshot": false, "db": "test_db",
              "table": "orders", "server_id": 184054, "file": "mysql-bin.000003",
              "pos": 2048, "row": 0 },
  "op": "u",
  "ts_ms": 1730000000000,
  "transaction": null
}
```

## Try it

```bash
# from the host:
./setup.sh deploy orders
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "UPDATE orders SET amount = amount * 1.1 WHERE customer_name = 'Eve';"
podman exec cdc-mysql mysql -ucdc_user -pcdc_pass test_db \
  -e "DELETE FROM orders WHERE customer_name = 'Eve';"

# verify the connector is healthy
curl -s http://localhost:8083/connectors/mysql-orders/status | jq .connector.state
```

## Connect commands in `ConnectAdmin`

```
status <name>    # state of one connector
list             # list all connectors
config <name>    # full config
restart <name>   # polite restart (does not destroy offset state)
delete <name>    # remove a connector
```

## Notes

- `database.server.id` **must** be unique per connector; the five connectors of
  this project use 184054–184058 so they never fight over the binlog position.
- Deploy order matters (see the `deploy all` command in `setup.sh`): the
  connectors are registered in this order — `orders`, `transforms`, `schema`,
  `snapshot`, `outbox`. If a consumer sees `topic not found`, all connectors must
  have been deployed **before** you insert demo data.
