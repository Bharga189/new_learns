#!/usr/bin/env bash
# Build, start the Podman stack, and deploy the basic connector.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${ROOT}/learn/setup.sh" "${1:-all}"
