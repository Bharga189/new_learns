#!/usr/bin/env bash
# Convenience wrapper. The real project and all commands live in learn/.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${ROOT}/learn/setup.sh" "${1:-help}"
