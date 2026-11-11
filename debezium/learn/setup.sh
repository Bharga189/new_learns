#!/usr/bin/env bash
# ============================================================================
# One entry point for the whole Debezium + MySQL demo.
#
#   ./setup.sh build        compile and install the Maven modules
#   ./setup.sh up           start MySQL, Kafka and Connect with Podman
#   ./setup.sh deploy all   register every example connector
#   ./setup.sh basic        run the embedded engine (no Kafka needed)
#   ./setup.sh consume      read mysql-cdc.test_db.orders
#   ./setup.sh status       show containers, connectors and topics
#   ./setup.sh down         stop the stack, keep the MySQL volume
#   ./setup.sh clean        stop and delete containers, volumes and build output
#
# The script uses `podman compose` when available, otherwise `podman-compose`.
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT="${ROOT}/debezium-mysql-parent"
COMPOSE_FILE="${ROOT}/podman/compose.yaml"
PLUGIN_DIR="${ROOT}/podman/plugins"

MVN="mvn"
if ! command -v "${MVN}" >/dev/null 2>&1; then
  if [[ -f "${PROJECT}/mvnw" ]]; then
    MVN="bash ${PROJECT}/mvnw"
  else
    echo "ERROR: Maven is not installed and ${PROJECT}/mvnw is missing." >&2
    exit 1
  fi
fi

usage() {
  cat <<'EOF'
Usage: ./setup.sh <command> [argument]

Environment:
  MYSQL_HOST        default localhost
  MYSQL_PORT        default 3306
  MYSQL_USER        default cdc_user
  MYSQL_PASSWORD    default cdc_pass
  KAFKA_BOOTSTRAP   default localhost:9092
  CONNECT_URL       default http://localhost:8083

Commands:
  build                 mvn clean install, then copy the custom SMT into the Connect plugin dir
  up                    start mysql + kafka + connect and wait for readiness
  down                  stop the stack (keeps the MySQL volume)
  clean                 stop the stack, delete volumes, data and build output
  reset                 clean volumes, start again, deploy the basic connector
  status                show containers, deployed connectors and Kafka topics
  logs [service]        follow logs (mysql | kafka | connect), default connect
  topics                list Kafka topics

  deploy <feature>      orders | transforms | schema | snapshot | outbox | all
  delete <name>         delete a connector by name
  basic                 module 01: embedded engine, no Kafka required
  schema                module 05: apply the schema-evolution DDL to orders
  consume [args]        module 03: consume events (default mysql-cdc.test_db.orders)
  signal [args]          module 06: send execute-snapshot | stop | log
  outbox [args]         module 07: write an order + outbox row atomically

  sql <file>            run a .sql file against test_db
  all                   build + up + deploy orders + status
EOF
}

require_podman() {
  if ! command -v podman >/dev/null 2>&1; then
    echo "ERROR: podman is not installed or not on PATH." >&2
    exit 1
  fi
  if ! podman info >/dev/null 2>&1; then
    echo "ERROR: Podman is installed but not running." >&2
    echo "       Start it first, for example:  podman machine start" >&2
    exit 1
  fi
}

compose() {
  require_podman
  if podman compose version >/dev/null 2>&1; then
    podman compose -f "${COMPOSE_FILE}" "$@"
  elif command -v podman-compose >/dev/null 2>&1; then
    podman-compose -f "${COMPOSE_FILE}" "$@"
  else
    echo "ERROR: no Podman Compose provider found." >&2
    echo "       Install podman-compose, or use a Podman version with 'podman compose'." >&2
    exit 1
  fi
}

run_module() {
  local module="$1" main="$2" args="${3:-}"
  ( cd "${PROJECT}/${module}" && ${MVN} -q exec:java \
      -Dexec.mainClass="${main}" \
      -Dexec.args="${args}" )
}

wait_for_mysql() {
  echo -n "Waiting for MySQL"
  for _ in $(seq 1 60); do
    if podman exec cdc-mysql mysqladmin ping -h 127.0.0.1 \
        -u"${MYSQL_USER:-cdc_user}" -p"${MYSQL_PASSWORD:-cdc_pass}" --silent >/dev/null 2>&1; then
      echo " ... ready"
      return 0
    fi
    echo -n "."
    sleep 2
  done
  echo " ... timed out"
  podman logs --tail 60 cdc-mysql || true
  return 1
}

wait_for_kafka() {
  echo -n "Waiting for Kafka"
  for _ in $(seq 1 60); do
    if podman exec cdc-kafka /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
      echo " ... ready"
      return 0
    fi
    echo -n "."
    sleep 2
  done
  echo " ... timed out"
  podman logs --tail 60 cdc-kafka || true
  return 1
}

wait_for_connect() {
  echo -n "Waiting for Kafka Connect"
  for _ in $(seq 1 90); do
    if curl -fsS "${CONNECT_URL:-http://localhost:8083}/connectors" >/dev/null 2>&1; then
      echo " ... ready"
      return 0
    fi
    echo -n "."
    sleep 2
  done
  echo " ... timed out"
  podman logs --tail 80 cdc-connect || true
  return 1
}

cmd_build() {
  echo "==> Building ${PROJECT}"
  ( cd "${PROJECT}" && ${MVN} -q clean install -DskipTests )
  mkdir -p "${PLUGIN_DIR}"
  rm -f "${PLUGIN_DIR}"/*.jar
  local jar
  jar="$(find "${PROJECT}/04-transforms/target" -maxdepth 1 \
        -name 'cdc-04-transforms-*.jar' ! -name '*sources*' ! -name '*javadoc*' | head -1)"
  if [[ -z "${jar}" ]]; then
    echo "ERROR: custom SMT jar was not produced." >&2
    exit 1
  fi
  cp "${jar}" "${PLUGIN_DIR}/"
  echo "==> Custom SMT: ${PLUGIN_DIR}/$(basename "${jar}")"

  # A connector plugin is loaded at Connect start-up: restart so the freshly
  # copied jar is picked up when you build after the stack is already running.
  if podman ps --filter name=cdc-connect --format '{{.Names}}' | grep -q cdc-connect; then
    podman restart cdc-connect
  fi
}

cmd_up() {
  command -v curl >/dev/null 2>&1 || {
    echo "ERROR: curl is required to wait for Kafka Connect." >&2
    exit 1
  }
  mkdir -p "${PLUGIN_DIR}"
  echo "==> Starting MySQL, Kafka and Debezium Connect"
  compose up -d
  wait_for_mysql
  wait_for_kafka
  wait_for_connect
}

cmd_down() {
  echo "==> Stopping the stack"
  compose down
}

cmd_clean() {
  echo "==> Removing containers and volumes"
  compose down -v || true
  rm -rf "${PLUGIN_DIR}"/*.jar
  find "${PROJECT}" -maxdepth 2 -type d -name data -prune -exec rm -rf {} +
  ( cd "${PROJECT}" && ${MVN} -q clean || true )
  echo "==> Clean"
}

connector_config() {
  case "$1" in
    orders)     echo "${PROJECT}/02-connect-deploy/src/main/resources/connectors/orders.json" ;;
    transforms) echo "${PROJECT}/04-transforms/src/main/resources/connectors/transforms.json" ;;
    schema)     echo "${PROJECT}/05-schema-evolution/src/main/resources/connectors/schema-evolution.json" ;;
    snapshot)   echo "${PROJECT}/06-incremental-snapshot/src/main/resources/connectors/incremental-snapshot.json" ;;
    outbox)     echo "${PROJECT}/07-outbox/src/main/resources/connectors/outbox.json" ;;
    *) return 1 ;;
  esac
}

cmd_deploy() {
  local feature="${1:-orders}"
  if [[ "${feature}" == "all" ]]; then
    for item in orders transforms schema snapshot outbox; do
      cmd_deploy "${item}"
    done
    return
  fi

  local config
  if ! config="$(connector_config "${feature}")"; then
    echo "ERROR: unknown feature '${feature}'." >&2
    echo "       Choose: orders | transforms | schema | snapshot | outbox | all" >&2
    exit 2
  fi
  if [[ ! -f "${config}" ]]; then
    echo "ERROR: connector config not found: ${config}" >&2
    exit 1
  fi

  echo "==> Deploying ${feature}: $(basename "${config}")"
  run_module 02-connect-deploy io.buffy.cdc.connect.ConnectAdmin "register ${config}"
}

cmd_delete() {
  local name="${1:-}"
  if [[ -z "${name}" ]]; then
    echo "Usage: ./setup.sh delete <connector-name>" >&2
    exit 2
  fi
  run_module 02-connect-deploy io.buffy.cdc.connect.ConnectAdmin "delete ${name}"
}

cmd_topics() {
  podman exec cdc-kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 --list | sort
}

cmd_status() {
  require_podman
  echo "==> Containers"
  podman ps --filter name=cdc- --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || true

  echo
  echo "==> Deployed connectors"
  curl -s "${CONNECT_URL:-http://localhost:8083}/connectors" || echo "(Connect is not reachable)"
  echo

  echo
  echo "==> Topics"
  cmd_topics || true
}

cmd_logs() {
  local service="${1:-connect}"
  podman logs -f "cdc-${service}"
}

cmd_sql() {
  local file="${1:-}"
  if [[ -z "${file}" || ! -f "${file}" ]]; then
    echo "Usage: ./setup.sh sql <file.sql>" >&2
    exit 2
  fi
  podman exec -i cdc-mysql mysql -ucdc_user -pcdc_pass test_db < "${file}"
}

case "${1:-help}" in
  build)    cmd_build ;;
  up|start) cmd_up ;;
  down|stop) cmd_down ;;
  clean)    cmd_clean ;;
  reset)    cmd_clean; cmd_build; cmd_up; cmd_deploy orders ;;
  status)   cmd_status ;;
  logs)     shift || true; cmd_logs "${1:-connect}" ;;
  topics)   cmd_topics ;;
  deploy)   shift || true; cmd_deploy "${1:-orders}" ;;
  delete)   shift || true; cmd_delete "${1:-}" ;;
  "basic")    run_module 01-basic-embedded io.buffy.cdc.basic.EmbeddedCdcApp "" ;;
  "schema") shift || true; run_module 05-schema-evolution io.buffy.cdc.schemaevo.SchemaEvolutionApp "${*:---apply-ddl}" ;;

  consume)  shift || true; run_module 03-consumer io.buffy.cdc.consumer.CdcConsumerApp "$*" ;;
  signal)   shift || true; run_module 06-incremental-snapshot io.buffy.cdc.snapshot.SignalSender "${*:-incremental test_db.orders}" ;;
  outbox)   shift || true; run_module 07-outbox io.buffy.cdc.outbox.OutboxWriter "${*:-Dana 99.50}" ;;
  sql)      shift || true; cmd_sql "${1:-}" ;;
  all)      cmd_build; cmd_up; cmd_deploy orders; cmd_status ;;
  help|-h|--help) usage ;;
  *)        usage; exit 2 ;;
esac
