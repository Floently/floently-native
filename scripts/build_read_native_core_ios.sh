#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$ROOT/shared/read-core-native-ffi/Cargo.toml"
INCLUDE_DIR="$ROOT/shared/read-core-native-ffi/include"
CARGO_TARGET_DIR="$ROOT/.build/read-core-native/ios"
GENERATED_DIR="$ROOT/apps/ios/FloentlyRead/Generated"
XCFRAMEWORK="$GENERATED_DIR/FloentlyReadCore.xcframework"
SIMULATOR_LIB="$CARGO_TARGET_DIR/libfloently_read_core_native-simulator.a"

export CARGO_TARGET_DIR

rustup target add \
  aarch64-apple-ios \
  aarch64-apple-ios-sim \
  x86_64-apple-ios

cargo build --release --manifest-path "$MANIFEST" --target aarch64-apple-ios
cargo build --release --manifest-path "$MANIFEST" --target aarch64-apple-ios-sim
cargo build --release --manifest-path "$MANIFEST" --target x86_64-apple-ios

mkdir -p "$GENERATED_DIR"
rm -rf "$XCFRAMEWORK"

lipo -create \
  "$CARGO_TARGET_DIR/aarch64-apple-ios-sim/release/libfloently_read_core_native.a" \
  "$CARGO_TARGET_DIR/x86_64-apple-ios/release/libfloently_read_core_native.a" \
  -output "$SIMULATOR_LIB"

xcodebuild -create-xcframework \
  -library "$CARGO_TARGET_DIR/aarch64-apple-ios/release/libfloently_read_core_native.a" \
  -headers "$INCLUDE_DIR" \
  -library "$SIMULATOR_LIB" \
  -headers "$INCLUDE_DIR" \
  -output "$XCFRAMEWORK"

echo "Built $XCFRAMEWORK"
