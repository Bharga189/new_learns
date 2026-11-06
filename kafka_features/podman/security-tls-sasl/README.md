# Optional Podman Security Overlay (TLS/SASL/ACL) — Template

The default local demos in this project use `PLAINTEXT` for simplicity. This folder is a **template/guide** for adding a secured local setup to test Kafka security features.

## What to configure (production-style concepts)

| Area | Broker/listener concepts | Client concepts |
|---|---|---|
| **TLS/SSL** | `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP`, `KAFKA_SSL_*` (keystore/truststore, passwords) | `security.protocol=SSL`, `ssl.truststore.*`, `ssl.keystore.*` (for mTLS) |
| **SASL** | `KAFKA_SASL_*`, JAAS config, enabled mechanisms (SCRAM/PLAIN/OAUTHBEARER) | `security.protocol=SASL_SSL`, `sasl.mechanism`, `sasl.jaas.config` |
| **ACLs** | Authorizer class, `super.users`, ACL storage | Principals must have permissions for topics/groups/cluster operations |

## Local testing approach (Podman)

1. Generate test certificates (CA, broker keystore/truststore, optional client certs) and store them **outside git** or use ignored paths. Never commit private keys/passwords.
2. Create a `docker-compose.yml` overlay that:
   - mounts cert/JAAS files into containers,
   - sets KRaft + listener security protocol maps (e.g., `SASL_SSL:...`),
   - configures `KAFKA_SSL_*` and `KAFKA_SASL_*` env vars for the chosen image.
3. Update client properties (or pass `-D` system properties / env-based config in `KafkaConfig`) with `security.protocol`, SASL mechanism, JAAS, and SSL truststore settings.
4. Start: `podman compose up -d` from this directory (once implemented). Test with `F17_SecurityConcepts` checklist and a real secured client config.

## Notes

- Use **SCRAM** or **mTLS** for realistic auth testing; avoid committing `PLAIN` credentials.
- The exact env var names depend on the Kafka container image (`apache/kafka`, `confluentinc/cp-kafka`, etc.). Check the image documentation.
- ACL testing requires an authorizer enabled on the broker and corresponding `kafka-acls`/Admin API operations.

This template keeps security testing possible locally while preserving the repo’s safe default (no secrets, plaintext local demos).
