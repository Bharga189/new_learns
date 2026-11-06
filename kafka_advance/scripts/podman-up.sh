#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../podman/single-broker-kraft"
echo "Starting kafka_advance single-broker (KRaft) on localhost:9092 ..."
podman compose up -d
podman compose ps
echo "Ready when broker logs show started. Use: podman compose logs -f kafka"
