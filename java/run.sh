#!/usr/bin/env bash

# Re-run with bash if the user invoked via sh/other shells so that we can use bash
# features consistently (pipefail, nounset, etc.).
if [ -z "${BASH_VERSION:-}" ]; then
  exec bash "$0" "$@"
fi

set -eu
if set -o pipefail >/dev/null 2>&1; then
  set -o pipefail
fi

cd "$(dirname "$0")"
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
exec java -cp out rcs.RcsApp
