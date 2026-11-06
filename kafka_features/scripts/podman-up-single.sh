#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../podman/single-broker-kraft"
podman compose up -d
podman compose ps
echo "Kafka bootstrap: localhost:9092"
