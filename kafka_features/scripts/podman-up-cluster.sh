#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../podman/cluster-3-broker"
podman compose up -d
podman compose ps
echo "Kafka bootstrap: localhost:19092,localhost:29092,localhost:39092"
