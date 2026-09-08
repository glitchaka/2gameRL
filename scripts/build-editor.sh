#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v java >/dev/null || { echo "ERROR: Java no esta en PATH"; exit 1; }
command -v mvn >/dev/null || { echo "ERROR: Maven no esta en PATH"; exit 1; }
mvn -DskipTests package
echo "OK: target/app-input/2gameRL-Studio.jar"
