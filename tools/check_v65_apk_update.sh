#!/usr/bin/env bash
set -euo pipefail
assets="https://github.com/${GITHUB_REPOSITORY}/releases/download"
baseline="$RUNNER_TEMP/v64.apk"
candidate="$RUNNER_TEMP/v65.apk"
curl -fsSL --retry 2 "$assets/v64-notes-rtl-fe47b4f409ed/ToDoShift-v64-Notes-RTL-Navigation.apk" -o "$baseline"
echo "5bd72d1eec704e5b8b90393cf7ca6fe1f82d222c4de088bb432d96ec2880cf7c  $baseline" | sha256sum -c -
release="$assets/v65-nile-jade-${GITHUB_SHA:0:12}"
# The signing job publishes the candidate from this exact commit in parallel.
curl -fsSL --retry 24 --retry-delay 10 --retry-all-errors "$release/ToDoShift-v65-Nile-Jade.apk" -o "$candidate"
curl -fsSL --retry 2 "$release/SHA256SUMS" -o "$RUNNER_TEMP/v65-checksums"
digest=$(awk '$2 == "ToDoShift-v65-Nile-Jade.apk" {print $1}' "$RUNNER_TEMP/v65-checksums")
echo "$digest  $candidate" | sha256sum -c -
python3 tools/verify_v65_update.py "$baseline" "$candidate"
