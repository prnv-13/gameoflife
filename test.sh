#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out-test
javac -d out-test $(find src test -name '*.java')
java -cp out-test gameoflife.GridTest
