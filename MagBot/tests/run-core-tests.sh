#!/usr/bin/env bash
# Requires JDK 17+. Does not require Android SDK or a local model.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
if command -v javac >/dev/null; then
    COMPILER=(javac)
else
    COMPILER=(java com.sun.tools.javac.Main)
fi
"${COMPILER[@]}" -source 17 -target 17 -Xlint:-options -d "$OUT" \
    "$ROOT/app/src/main/java/com/magbot/localagent/ActionValidator.java" \
    "$ROOT/app/src/main/java/com/magbot/localagent/AgentProtocol.java" \
    "$ROOT/tests/ActionValidatorTest.java"
java -cp "$OUT" com.magbot.localagent.ActionValidatorTest
