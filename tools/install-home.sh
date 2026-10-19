#!/usr/bin/env bash
# Keep the existing launcher installer as the single implementation.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
exec bash "$ROOT/launcher/build-and-install.sh" "$@"
