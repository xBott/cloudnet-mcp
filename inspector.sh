#!/usr/bin/env bash

set -euo pipefail
cd "$(dirname "$0")"

jar="$(ls -t build/libs/*-all.jar 2>/dev/null | head -n 1 || true)"
[[ -n "$jar" ]] || { echo "No jar in build/libs. Run: ./gradlew shadowJar" >&2; exit 1; }

echo "Using $jar" >&2
exec npx -y @modelcontextprotocol/inspector java -jar "$jar" "$@"
