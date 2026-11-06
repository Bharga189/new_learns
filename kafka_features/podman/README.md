# Podman Local Kafka Setup

All feature tests in this project use **Podman** to run Kafka locally. The default setups use **KRaft** (Kafka Raft metadata mode, no ZooKeeper).

## Prerequisites

- Podman 4+ (`podman --version`)
- `podman compose` (Podman 4.7+) or `podman-compose`
- Java 17+ and Maven (for running examples)

## Available Environments

| Environment | Path | Bootstrap servers (host) | Use for |
|---|---|---|---|
| Single broker (KRaft) | `single-broker-kraft/` | `localhost:9092` | Most feature demos (F01–F02, F04–F11, F13–F17, F12 basic) |
| 3-broker cluster (KRaft) | `cluster-3-broker/` | `localhost:19092,localhost:29092,localhost:39092` | Replication/ISR/leader placement, rebalancing/failover, F03, F12 RF=3 |
| Security overlay (template) | `security-tls-sasl/README.md` | depends on overlay | TLS/SASL/ACL local testing guidance |

## Per-Feature Test Steps

**Every feature has explicit local Podman setup and test commands** in [`../docs/PODMAN_TEST_GUIDE.md`](../docs/PODMAN_TEST_GUIDE.md). Use that guide as the source of truth for “how to test feature X”.

## Quick Start (single broker)

```bash
cd podman/single-broker-kraft
podman compose up -d
podman compose ps
# Kafka: localhost:9092
```

Stop and clean up:

```bash
podman compose down -v
```

## 3-Broker Cluster

```bash
cd podman/cluster-3-broker
podman compose up -d
# Bootstrap: localhost:19092,localhost:29092,localhost:39092
podman compose down -v
```

## Troubleshooting

- **Port in use**: stop existing Kafka or change published ports in the compose file.
- **Not ready**: wait a few seconds and retry; check `podman compose ps` / `podman compose logs`.
- **SELinux / volume issues (Linux)**: add `:Z` to volume mounts if needed.
- **Windows**: run in a bash shell (Git Bash/MSYS). Podman Machine works with `podman compose`.
- **Reset state**: `podman compose down -v` removes volumes/topics between test runs.
