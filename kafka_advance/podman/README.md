# Podman Local Environments (kafka_advance)

Local Kafka test environments for the advanced examples (KRaft, no ZooKeeper).

| Environment | Path | Bootstrap servers (host) | Best for |
|---|---|---|---|
| Single broker (KRaft) | `single-broker-kraft/docker-compose.yml` | `localhost:9092` | Real-time pipeline, SerDe variations, best-practice demos, retry/DLQ, dedup, EOS enhancements |

## Commands

```bash
# Start
cd podman/single-broker-kraft
podman compose up -d

# Status / logs
podman compose ps
podman compose logs -f kafka

# Stop and remove data (clean slate)
podman compose down -v
```

> Use `podman compose down -v` between runs when you want a clean topic/state reset.
> If port `9092` conflicts, change the published port and pass `-Dbootstrap.servers=localhost:<port>` when running demos.
