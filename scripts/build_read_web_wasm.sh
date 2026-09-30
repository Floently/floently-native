#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
WASM_CRATE="$ROOT_DIR/shared/read-core-wasm"
WEB_APP="$ROOT_DIR/apps/web/FloentlyReadWeb"
GENERATED_DIR="$WEB_APP/src/generated/read-core-wasm"

command -v cargo >/dev/null 2>&1
command -v rustup >/dev/null 2>&1
command -v wasm-pack >/dev/null 2>&1
command -v npm >/dev/null 2>&1

rustup target add wasm32-unknown-unknown >/dev/null

rm -rf "$GENERATED_DIR"
mkdir -p "$GENERATED_DIR"

(
  cd "$WASM_CRATE"
  wasm-pack build \
    --target web \
    --release \
    --out-dir "$GENERATED_DIR" \
    --out-name floently_read_core_wasm
)

(
  cd "$WEB_APP"
  npm install --no-audit --no-fund
  npm run build
)

echo "Floently Read Web production bundle built successfully."
