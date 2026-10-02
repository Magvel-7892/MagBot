#!/usr/bin/env bash
# Laptop only. Fetches the official Gradle wrapper from its pinned upstream version.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM='https://raw.githubusercontent.com/gradle/gradle/v8.9.0'
mkdir -p "$ROOT/gradle/wrapper"
for file in gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar; do
    curl --fail --location --retry 3 "$UPSTREAM/$file" --output "$ROOT/$file"
done
EXPECTED="$(curl --fail --silent --show-error --location https://services.gradle.org/distributions/gradle-8.9-wrapper.jar.sha256)"
if command -v sha256sum >/dev/null; then
    ACTUAL="$(sha256sum "$ROOT/gradle/wrapper/gradle-wrapper.jar" | awk '{print $1}')"
else
    ACTUAL="$(shasum -a 256 "$ROOT/gradle/wrapper/gradle-wrapper.jar" | awk '{print $1}')"
fi
[ "$EXPECTED" = "$ACTUAL" ] || { echo 'Wrapper checksum mismatch; do not run Gradle.' >&2; exit 1; }
chmod +x "$ROOT/gradlew"
printf 'Official Gradle 8.9 wrapper installed and checksum checked. Open the project in Android Studio.\n'
