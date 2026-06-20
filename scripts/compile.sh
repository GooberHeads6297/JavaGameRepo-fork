#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID:-$(id -u)}" -eq 0 ]]; then
  echo "Do not run compile.sh with sudo or as root." >&2
  echo "Run it as your normal user so Gradle caches stay writable." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GRADLE_VERSION="${GRADLE_VERSION:-9.2.0}"
GRADLE_MIN_MAJOR="${GRADLE_MIN_MAJOR:-8}"
INSTALL_SCRIPT="$ROOT_DIR/scripts/install-gradle.sh"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-${XDG_CACHE_HOME:-$HOME/.cache}/xenoverse/gradle}"
PROJECT_CACHE_DIR="${PROJECT_CACHE_DIR:-$GRADLE_USER_HOME/project-cache}"
OUTPUT_BUILD_DIR="${OUTPUT_BUILD_DIR:-$GRADLE_USER_HOME/build}"
DIST_DIR="$ROOT_DIR/dist"
JAR_NAME="xenoverse-portable.jar"
export GRADLE_USER_HOME

gradle_major_version() {
  local gradle_path="$1"
  "$gradle_path" -v 2>/dev/null | sed -n 's/^Gradle \([0-9][0-9]*\)\..*/\1/p' | head -n 1
}

use_system_gradle() {
  local gradle_path major
  gradle_path="$(command -v gradle 2>/dev/null || true)"
  if [[ -z "$gradle_path" ]]; then
    return 1
  fi

  major="$(gradle_major_version "$gradle_path" || true)"
  [[ "${major:-0}" -ge "$GRADLE_MIN_MAJOR" ]]
}

if use_system_gradle; then
  GRADLE_CMD="$(command -v gradle)"
else
  if [[ ! -x "$INSTALL_SCRIPT" ]]; then
    echo "Gradle is required but was not found on PATH." >&2
    exit 1
  fi

  GRADLE_VERSION="$GRADLE_VERSION" GRADLE_MIN_MAJOR="$GRADLE_MIN_MAJOR" "$INSTALL_SCRIPT"
  GRADLE_CMD="$HOME/.local/opt/gradle-$GRADLE_VERSION/bin/gradle"
fi

if [[ ! -x "$GRADLE_CMD" && -x "$HOME/.local/bin/gradle" ]]; then
  GRADLE_CMD="$HOME/.local/bin/gradle"
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java 21 or newer is required but was not found on PATH." >&2
  exit 1
fi

mkdir -p "$GRADLE_USER_HOME" "$PROJECT_CACHE_DIR" "$OUTPUT_BUILD_DIR" "$DIST_DIR"

"$GRADLE_CMD" \
  --gradle-user-home "$GRADLE_USER_HOME" \
  --project-cache-dir "$PROJECT_CACHE_DIR" \
  -PxenoverseBuildDir="$OUTPUT_BUILD_DIR" \
  portableJar

SOURCE_JAR="$OUTPUT_BUILD_DIR/libs/$JAR_NAME"
if [[ ! -f "$SOURCE_JAR" ]]; then
  echo "Build completed, but the portable JAR was not created at: $SOURCE_JAR" >&2
  exit 1
fi

install -m 0644 "$SOURCE_JAR" "$DIST_DIR/$JAR_NAME"
echo "Portable game created: $DIST_DIR/$JAR_NAME"
