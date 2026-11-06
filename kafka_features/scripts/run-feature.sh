#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/run-feature.sh <FeatureClassSimpleName|FQCN> [extra mvn exec args...]
# Examples:
#   ./scripts/run-feature.sh F01_ProducerBasics
#   ./scripts/run-feature.sh F03_ConsumerGroupsRebalancing -Dbootstrap.servers=localhost:19092,localhost:29092,localhost:39092 -Dclient.id=member-1

FEATURE="${1:-}"
if [[ -z "$FEATURE" ]]; then
  echo "Usage: $0 <F01_ProducerBasics|com.example.kafka.features.F01_ProducerBasics> [extra args]" >&2
  exit 2
fi
shift || true

if [[ "$FEATURE" == *.* ]]; then
  MAIN="$FEATURE"
else
  MAIN="com.example.kafka.features.${FEATURE}"
fi

mvn -q -DskipTests compile exec:java -Dexec.mainClass="$MAIN" "$@"
