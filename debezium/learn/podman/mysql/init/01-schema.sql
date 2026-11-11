-- ============================================================================
-- Demo schema for the Debezium MySQL examples.
-- This file runs once, on the first boot of an empty MySQL data volume.
-- To re-run it:  podman compose -f podman/compose.yaml down -v
-- ============================================================================

CREATE DATABASE IF NOT EXISTS test_db;
USE test_db;

-- The main business table. updated_at changes on every UPDATE, which makes
-- update events easy to spot in the Kafka topic.
CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_name VARCHAR(100) NOT NULL,
    amount        DECIMAL(10,2) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                  ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Used by the transform demo. email and customer_name are masked by the
-- custom SMT before the event is written to Kafka.
CREATE TABLE IF NOT EXISTS customers (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_name VARCHAR(100) NOT NULL,
    email         VARCHAR(200) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- The signal table is a normal InnoDB table that Debezium watches. A row is a
-- command such as execute-snapshot. Debezium does not emit data events for it.
CREATE TABLE IF NOT EXISTS debezium_signal (
    id   VARCHAR(42)  NOT NULL PRIMARY KEY,
    type VARCHAR(32)  NOT NULL,
    data VARCHAR(2048) NULL
) ENGINE=InnoDB;

-- Transactional outbox. The application writes the order and this row in the
-- same transaction; Debezium routes the row to outbox.<aggregate_type>.
CREATE TABLE IF NOT EXISTS outbox (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    type           VARCHAR(255) NOT NULL,
    payload        JSON NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Debezium's MySQL user needs more than ordinary SELECT:
--   REPLICATION SLAVE / REPLICATION CLIENT -> read the binlog
--   RELOAD                                 -> coordinate consistent snapshots
--   SHOW DATABASES                         -> discover schemas
-- ---------------------------------------------------------------------------
ALTER USER 'cdc_user'@'%' IDENTIFIED WITH mysql_native_password BY 'cdc_pass';
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT
    ON *.* TO 'cdc_user'@'%';
FLUSH PRIVILEGES;

-- Seed data so the initial snapshot has something to publish.
INSERT INTO orders (id, customer_name, amount, status) VALUES
    (1, 'Alice', 120.50, 'PENDING'),
    (2, 'Bob',   75.00, 'SHIPPED');

INSERT INTO customers (id, customer_name, email) VALUES
    (1, 'Alice', 'alice@example.com'),
    (2, 'Bob',   'bob@example.com');
