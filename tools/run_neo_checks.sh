#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v kotlinc >/dev/null || { echo "BLOCKED: Kotlin compiler (kotlinc) is unavailable" >&2; exit 127; }
check_tmp_dir="$(mktemp -d)"
trap 'rm -rf "$check_tmp_dir"' EXIT
neo_sources="app/src/main/java/com/todoshift/app/v11"
kotlinc "$neo_sources/LocalizedNumbers.kt" "$neo_sources/V11Models.kt" "$neo_sources/V11RateEngine.kt" "$neo_sources/V11FinanceEngine.kt" \
  "$neo_sources/V11ShiftConflictEngine.kt" "$neo_sources/NeoAppearance.kt" "$neo_sources/NeoPresentation.kt" \
  "$neo_sources/Neo4ShiftPaint.kt" app/src/main/java/com/todoshift/app/cloud/CloudContracts.kt \
  tools/NeoPresentationChecks.kt -include-runtime -d "$check_tmp_dir/neo-checks.jar"
java -jar "$check_tmp_dir/neo-checks.jar"
