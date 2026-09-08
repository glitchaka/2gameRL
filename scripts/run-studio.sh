#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/build-editor.sh
java -cp "target/app-input/*" com.buttclapdev.twogamerl.studio.DesktopLauncher
