#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ $# -ge 2 ]] || { echo "Uso: export-game.sh proyecto.2grl carpeta-salida"; exit 2; }
bash scripts/build-editor.sh
java -cp "target/app-input/*" com.buttclapdev.twogamerl.export.ExportCli "$1" "$2"
