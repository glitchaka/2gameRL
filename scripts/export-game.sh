#!/usr/bin/env bash
set -euo pipefail
PROJECT="${1:?Falta archivo .2grl}"
OUT="${2:?Falta carpeta de destino}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/build/game-export"
rm -rf "$BUILD"; mkdir -p "$BUILD/classes" "$BUILD/embed" "$BUILD/input" "$OUT"
find "$ROOT/src/main/java" -name '*.java' > "$BUILD/sources.txt"
javac --release 21 -encoding UTF-8 -d "$BUILD/classes" @"$BUILD/sources.txt"
cp "$PROJECT" "$BUILD/embed/game-project.2grl"
jar --create --file "$BUILD/input/2gameRL-game.jar" --main-class com.buttclapdev.twogamerl.runtime.GameLauncher -C "$BUILD/classes" .
jar --update --file "$BUILD/input/2gameRL-game.jar" -C "$BUILD/embed" game-project.2grl
jpackage --type app-image --name "2gameRL" --input "$BUILD/input" --main-jar "2gameRL-game.jar" --main-class com.buttclapdev.twogamerl.runtime.GameLauncher --dest "$OUT"
echo "Listo: $OUT/2gameRL"
