#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR_PATH="$ROOT_DIR/dist/xenoverse-portable.jar"

"$ROOT_DIR/scripts/compile.sh"

if ! command -v java >/dev/null 2>&1; then
  echo "Java 21 or newer is required but was not found on PATH." >&2
  exit 1
fi

cd "$ROOT_DIR"
exec java -jar "$JAR_PATH" "$@"
