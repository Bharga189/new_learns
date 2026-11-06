package com.example.kafka.features;

import com.example.kafka.common.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Feature 17: Kafka security concepts — TLS/SSL, SASL authentication, and ACL authorization.
 *
 * <p><b>Deep dive: securing Kafka.</b> Production Kafka deployments typically layer encryption,
 * authentication, and authorization. Local demos in this project default to {@code PLAINTEXT} for
 * simplicity, but every team should understand the security model before production rollout.</p>
 *
 * <h2>1) Encryption in transit (TLS/SSL)</h2>
 * <ul>
 *   <li><b>Purpose:</b> protect data and credentials on the network; optionally authenticate
 *       endpoints via certificates.</li>
 *   <li><b>Broker/listeners:</b> configure security protocol map (e.g., {@code SASL_SSL}), keystore,
 *       truststore, and enabled protocols/ciphers per policy.</li>
 *   <li><b>Clients:</b> {@code security.protocol=SSL} (or {@code SASL_SSL}), truststore to verify
 *       brokers; keystore for mutual TLS (mTLS) when required.</li>
 * </ul>
 *
 * <h2>2) Authentication (SASL / mTLS identity)</h2>
 * <ul>
 *   <li><b>SASL mechanisms:</b> SCRAM-SHA-* (common), PLAIN (only over TLS), GSSAPI/Kerberos,
 *       OAUTHBEARER (token-based). Choice depends on platform and identity provider.</li>
 *   <li><b>Clients:</b> {@code sasl.mechanism} and {@code sasl.jaas.config} (or external JAAS).
 *       Never commit credentials; use env vars/secret stores.</li>
 *   <li><b>mTLS:</b> client certificates can provide strong identity; integrate with authorization
 *       principal mapping.</li>
 * </ul>
 *
 * <h2>3) Authorization (ACLs)</h2>
 * <ul>
 *   <li><b>Purpose:</b> control which principals can read/write/create/describe/admin topics,
 *       consumer groups, cluster resources, and transactional IDs.</li>
 *   <li><b>Operations:</b> least privilege; separate producer/consumer/admin identities; audit ACL
 *       changes. Use Admin API or {@code kafka-acls} tooling according to your ops process.</li>
 *   <li><b>Quotas:</b> often paired with security for multi-tenancy (producer/consumer byte rates,
 *       request rates) to protect cluster stability.</li>
 * </ul>
 *
 * <h2>4) Additional production concerns</h2>
 * <ul>
 *   <li><b>Encryption at rest:</b> disk/filesystem or broker/tiered-storage features depending on
 *       platform; protect keys.</li>
 *   <li><b>Network controls:</b> private listeners, firewalls, service mesh policies.</li>
 *   <li><b>Secrets/config hygiene:</b> rotate credentials/certs; avoid plaintext secrets in repo
 *       (this project ignores {@code .env*} except templates).</li>
 * </ul>
 *
 * <p><b>Local Podman test:</b> this class runs a safe plaintext connectivity check (if the local
 * broker is up) and prints a configuration checklist. For a real TLS/SASL/ACL test, implement the
 * overlay template in {@code podman/security-tls-sasl/README.md} and point clients at the secured
 * listener with appropriate {@code security.*}/{@code sasl.*}/{@code ssl.*} properties.</p>
 *
 * <pre>{@code
 * cd podman/single-broker-kraft && podman compose up -d
 * mvn -q compile exec:java -Dexec.mainClass=com.example.kafka.features.F17_SecurityConcepts
 * podman compose down -v
 * }</pre>
 */
public class F17_SecurityConcepts {
    private static final Logger log = LoggerFactory.getLogger(F17_SecurityConcepts.class);

    public static void main(String[] args) {
        log.info("=== F17 Kafka Security Concepts (bootstrap={}) ===", KafkaConfig.bootstrapServers());
        printChecklist();
        tryConnectivity();
    }

    private static void printChecklist() {
        log.info("--- Security checklist (configure per environment) ---");
        log.info("[TLS] security.protocol=SSL|SASL_SSL; ssl.truststore.location/type/password; verify broker certs; consider mTLS keystore.");
        log.info("[SASL] sasl.mechanism=SCRAM-SHA-512 (example); sasl.jaas.config via secret store; never hardcode credentials.");
        log.info("[ACLs] enable authorizer on brokers; define least-privilege ACLs for topics/groups/transactional IDs; audit changes.");
        log.info("[OPS] use private networks, rotate certs/credentials, monitor auth failures, and protect .env/secret files.");
        log.info("[LOCAL] see podman/security-tls-sasl/README.md for a Podman overlay template.");
    }

    private static void tryConnectivity() {
        Properties props = KafkaConfig.baseAdminProps();
        // If you enable security locally, add here (example placeholders — do NOT commit real secrets):
        // props.put("security.protocol", "SASL_SSL");
        // props.put("sasl.mechanism", "SCRAM-SHA-512");
        // props.put("sasl.jaas.config", "... from env/secret ...");
        // props.put("ssl.truststore.location", "/path/to/truststore.jks");
        try (AdminClient admin = AdminClient.create(props)) {
            String clusterId = admin.describeCluster().clusterId().get(5, TimeUnit.SECONDS);
            log.info("[CONNECTIVITY] Connected to clusterId={} using current client config. For secured clusters, replace properties with TLS/SASL settings.", clusterId);
        } catch (Exception e) {
            log.warn("[CONNECTIVITY] Could not connect with current config (bootstrap={}). Start Podman Kafka for the plaintext check, or configure security properties for a secured cluster. Details: {}",
                    KafkaConfig.bootstrapServers(), e.toString());
        }
    }
}
