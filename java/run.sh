#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
exec java -cp out rcs.RcsApp
