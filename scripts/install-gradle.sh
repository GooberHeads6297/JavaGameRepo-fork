#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID:-$(id -u)}" -eq 0 ]]; then
  echo "Do not run this installer with sudo or as root." >&2
  echo "Run it as your normal user so Gradle is installed under your home directory." >&2
  exit 1
fi

GRADLE_VERSION="${GRADLE_VERSION:-9.2.0}"
GRADLE_MIN_MAJOR="${GRADLE_MIN_MAJOR:-8}"
INSTALL_ROOT="${GRADLE_INSTALL_ROOT:-$HOME/.local/opt}"
BIN_DIR="${GRADLE_BIN_DIR:-$HOME/.local/bin}"
PROFILE_FILE="${GRADLE_PROFILE_FILE:-$HOME/.profile}"
ARCHIVE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
INSTALL_DIR="$INSTALL_ROOT/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$INSTALL_DIR/bin/gradle"

gradle_major_version() {
  local gradle_path="$1"
  "$gradle_path" -v 2>/dev/null | sed -n 's/^Gradle \([0-9][0-9]*\)\..*/\1/p' | head -n 1
}

gradle_is_sufficient() {
  local gradle_path major
  gradle_path="$(command -v gradle 2>/dev/null || true)"
  if [[ -z "$gradle_path" ]]; then
    return 1
  fi

  major="$(gradle_major_version "$gradle_path" || true)"
  [[ "${major:-0}" -ge "$GRADLE_MIN_MAJOR" ]]
}

if gradle_is_sufficient; then
  echo "Gradle is already available at: $(command -v gradle)"
  gradle -v
  exit 0
fi

tmp_dir="$(mktemp -d)"
archive_path="$tmp_dir/gradle-${GRADLE_VERSION}-bin.zip"
extract_dir="$tmp_dir/extract"

mkdir -p "$extract_dir" "$INSTALL_ROOT" "$BIN_DIR"

if command -v curl >/dev/null 2>&1; then
  curl -fsSL "$ARCHIVE_URL" -o "$archive_path"
elif command -v wget >/dev/null 2>&1; then
  wget -qO "$archive_path" "$ARCHIVE_URL"
else
  echo "curl or wget is required to download Gradle." >&2
  exit 1
fi

(cd "$extract_dir" && jar xf "$archive_path")
rm -rf "$INSTALL_DIR"
mv "$extract_dir/gradle-${GRADLE_VERSION}" "$INSTALL_DIR"
rm -rf "$tmp_dir"

if [[ -f "$GRADLE_BIN" ]]; then
  chmod +x "$GRADLE_BIN"
fi
if [[ -f "$INSTALL_DIR/bin/gradle.bat" ]]; then
  chmod +x "$INSTALL_DIR/bin/gradle.bat"
fi

ln -sfn "$GRADLE_BIN" "$BIN_DIR/gradle"

path_entry="export PATH=\"$BIN_DIR:\$PATH\""
if [[ -f "$PROFILE_FILE" ]] && ! grep -Fqx "$path_entry" "$PROFILE_FILE"; then
  printf '\n%s\n' "$path_entry" >> "$PROFILE_FILE"
elif [[ ! -f "$PROFILE_FILE" ]]; then
  printf '%s\n' "$path_entry" > "$PROFILE_FILE"
fi

export PATH="$BIN_DIR:$PATH"

echo "Gradle installed to: $INSTALL_DIR"
echo "Gradle command linked at: $BIN_DIR/gradle"
echo "Current shell PATH updated. Open a new terminal or source $PROFILE_FILE to persist it in login shells."
"$GRADLE_BIN" -v
