#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUN_LABEL="${1:-local-baseline}"
RUN_DIR="${ROOT_DIR}/perf/results/recommendation-feed/${RUN_LABEL}"
SCREENSHOT_DIR="${RUN_DIR}/screenshots"

required_files=(
  "code-state.txt"
  "row-counts-and-sizes.txt"
  "recommendation-feed-explain.txt"
  "recommendation-feed-explain.json"
  "k6-summary.txt"
  "k6-summary.json"
)

for file in "${required_files[@]}"; do
  if [[ ! -s "${RUN_DIR}/${file}" ]]; then
    echo "Missing required evidence file: ${RUN_DIR}/${file}" >&2
    exit 1
  fi
done

if [[ ! -d "${SCREENSHOT_DIR}" ]]; then
  echo "Missing screenshot directory: ${SCREENSHOT_DIR}" >&2
  exit 1
fi

screenshot_count="$(
  find "${SCREENSHOT_DIR}" -type f \( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' \) |
    wc -l |
    tr -d ' '
)"

if [[ "${screenshot_count}" -lt 3 ]]; then
  echo "Expected at least 3 screenshots in ${SCREENSHOT_DIR}; found ${screenshot_count}" >&2
  exit 1
fi

echo "Recommendation feed evidence is complete for ${RUN_LABEL}"
