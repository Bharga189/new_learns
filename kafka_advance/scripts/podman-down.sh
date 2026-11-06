#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../podman/single-broker-kraft"
echo "Stopping kafka_advance single-broker and removing volumes ..."
podman compose down -v
echo "Done."
