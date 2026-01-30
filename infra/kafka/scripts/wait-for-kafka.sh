#!/usr/bin/env bash

set -euo pipefail

BOOTSTRAP_SERVERS="${1:-kafka:9092}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-120}"
SLEEP_SECONDS="${SLEEP_SECONDS:-3}"

echo "[wait-for-kafka] Waiting for Kafka at ${BOOTSTRAP_SERVERS} (timeout: ${TIMEOUT_SECONDS}s)..."

start_ts="$(date +%s)"

while true; do
  if /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server "${BOOTSTRAP_SERVERS}" >/dev/null 2>&1; then
    echo "[wait-for-kafka] Kafka is up: ${BOOTSTRAP_SERVERS}"
    exit 0
  fi

  now_ts="$(date +%s)"
  elapsed="$((now_ts - start_ts))"

  if [ "${elapsed}" -ge "${TIMEOUT_SECONDS}" ]; then
    echo "[wait-for-kafka] ERROR: Timeout after ${TIMEOUT_SECONDS}s waiting for Kafka at ${BOOTSTRAP_SERVERS}"
    exit 1
  fi

  echo "[wait-for-kafka] Still waiting... (${elapsed}s)"
  sleep "${SLEEP_SECONDS}"
done
