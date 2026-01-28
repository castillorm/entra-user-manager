#!/usr/bin/env bash
set -euo pipefail

# Always run from project root
cd "$(dirname "$0")"

# ---- Java / Maven setup (Arch-friendly) ----

# Auto-detect JAVA_HOME from javac (robust on Arch)
export JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"

# Maven from PATH (pacman install)
export PATH="$JAVA_HOME/bin:$PATH"

# ---- Config ----

CONFIG_PATH="${1:-config.ini}"

if [[ ! -f "$CONFIG_PATH" ]]; then
  echo "Config file not found: $CONFIG_PATH" >&2
  exit 1
fi

# ---- Extract logmode from config.ini ----
# Looks for:
# [operation]
# logMode=INFO

LOGMODE="$(awk -F= '
  $0 ~ /^\[operation\]/ { in_op=1; next }
  /^\[/ { in_op=0 }
  in_op && $1=="logMode" { print $2 }
' "$CONFIG_PATH" | tr -d "[:space:]")"

# Default + normalize
LOGMODE="${LOGMODE:-WARN}"
LOGMODE="$(echo "$LOGMODE" | tr '[:lower:]' '[:upper:]')"

# Validate
case "$LOGMODE" in
  TRACE|DEBUG|INFO|WARN|ERROR|OFF) ;;
  *)
    echo "Invalid logmode '$LOGMODE' in config.ini, defaulting to WARN" >&2
    LOGMODE=WARN
    ;;
esac

# ---- Build + run ----

mvn -q -DskipTests clean package


mvn -q \
  -Dconsole.level=$LOGMODE \
  exec:java \
  -Dexec.args="config.ini"
