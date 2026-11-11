#!/usr/bin/env bash
# Compile (if needed) and run the Game of Life. All arguments are passed through.
set -euo pipefail
cd "$(dirname "$0")"
if [ ! -d out ] || [ -n "$(find src -name '*.java' -newer out 2>/dev/null)" ]; then
  rm -rf out
  javac -d out $(find src -name '*.java')
  touch out
fi
exec java -cp out gameoflife.GameOfLife "$@"
