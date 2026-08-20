#!/usr/bin/env bash
set -euo pipefail

case "${1:-}" in
    --release)
        (cd java && ./gradlew jar)
        (cd rust && cargo build --release)
        ;;
    --test)
        (cd java && ./gradlew test :patchbukkit-test-plugin:build)
        (cd rust && cargo test)
        ;;
    "")
        (cd java && ./gradlew jar)
        (cd rust && cargo build)
        ;;
    *)
        printf 'Usage: %s [--release|--test]\n' "$0" >&2
        exit 2
        ;;
esac
