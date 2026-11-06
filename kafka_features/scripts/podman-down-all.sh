#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
(cd "$ROOT/podman/single-broker-kraft" && podman compose down -v) || true
(cd "$ROOT/podman/cluster-3-broker" && podman compose down -v) || true
echo "Stopped Podman Kafka environments."
