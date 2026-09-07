#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/build"
rm -rf "$BUILD/classes"; mkdir -p "$BUILD/classes"
find "$ROOT/src/main/java" -name '*.java' > "$BUILD/sources.txt"
javac --release 21 -encoding UTF-8 -d "$BUILD/classes" @"$BUILD/sources.txt"
jar --create --file "$BUILD/2gameRL-Studio.jar" --main-class com.buttclapdev.twogamerl.App -C "$BUILD/classes" .
echo "Creado: $BUILD/2gameRL-Studio.jar"
