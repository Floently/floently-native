#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$ROOT/shared/read-core-native-ffi/Cargo.toml"
OUTPUT="$ROOT/apps/android/FloentlyRead/app/src/main/jniLibs"
CARGO_TARGET_DIR="$ROOT/.build/read-core-native/android"

if ! command -v cargo-ndk >/dev/null 2>&1; then
  echo "cargo-ndk is required. Install it with: cargo install cargo-ndk --locked --version 3.5.4" >&2
  exit 1
fi

export CARGO_TARGET_DIR

rustup target add \
  armv7-linux-androideabi \
  aarch64-linux-android \
  i686-linux-android \
  x86_64-linux-android

rm -rf "$OUTPUT"
mkdir -p "$OUTPUT"

pushd "$(dirname "$MANIFEST")" >/dev/null
cargo ndk \
  -p 26 \
  -t armeabi-v7a \
  -t arm64-v8a \
  -t x86 \
  -t x86_64 \
  -o "$OUTPUT" \
  build --release
popd >/dev/null

for abi in armeabi-v7a arm64-v8a x86 x86_64; do
  test -f "$OUTPUT/$abi/libfloently_read_core_native.so"
done

echo "Built Android Read Core JNI libraries under $OUTPUT"
