#!/usr/bin/env bash
set -euo pipefail
if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <A01_RealTimeEventPipeline|A02_...|...> [extra -D props]" >&2
  exit 1
fi
DEMO="$1"; shift
cd "$(dirname "$0")/.."
mvn -q -DskipTests compile exec:java -Dexec.mainClass="com.example.kafka.advance.features.${DEMO}" "$@"
