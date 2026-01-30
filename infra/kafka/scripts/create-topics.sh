#!/usr/bin/env bash
# infra/kafka/scripts/create-topics.sh
set -euo pipefail

BOOTSTRAP_SERVERS="${BOOTSTRAP_SERVERS:-kafka:9092}"
TOPICS_FILE="${TOPICS_FILE:-/infra/kafka/topics.yml}"

# Requer yq no container/ambiente onde rodar o script.
command -v yq >/dev/null 2>&1 || {
  echo "[create-topics] ERROR: 'yq' not found. Install yq or run this script in an image that contains yq."
  exit 1
}

echo "[create-topics] Using bootstrap: ${BOOTSTRAP_SERVERS}"
echo "[create-topics] Using topics file: ${TOPICS_FILE}"

# garante que Kafka está pronto (se o script wait-for-kafka estiver disponível)
if [ -x "/infra/kafka/scripts/wait-for-kafka.sh" ]; then
  /infra/kafka/scripts/wait-for-kafka.sh "${BOOTSTRAP_SERVERS}"
fi

count="$(yq '.topics | length' "${TOPICS_FILE}")"

if [ "${count}" -eq 0 ]; then
  echo "[create-topics] No topics found in ${TOPICS_FILE}"
  exit 0
fi

for i in $(seq 0 $((count - 1))); do
  name="$(yq -r ".topics[${i}].name" "${TOPICS_FILE}")"
  partitions="$(yq -r ".topics[${i}].partitions // 1" "${TOPICS_FILE}")"
  rf="$(yq -r ".topics[${i}].replicationFactor // 1" "${TOPICS_FILE}")"

  if [ -z "${name}" ] || [ "${name}" = "null" ]; then
    echo "[create-topics] Skipping topic index ${i}: invalid name"
    continue
  fi

  echo "[create-topics] Creating (or ensuring) topic: ${name} (partitions=${partitions}, rf=${rf})"

  # cria se não existir
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP_SERVERS}" \
    --create --if-not-exists \
    --topic "${name}" \
    --partitions "${partitions}" \
    --replication-factor "${rf}" >/dev/null

  # aplica configs (se existirem)
  # transforma o map "config" em argumentos --config k=v
  cfg_len="$(yq ".topics[${i}].config | length" "${TOPICS_FILE}" 2>/dev/null || echo 0)"
  if [ "${cfg_len}" != "0" ] && [ "${cfg_len}" != "null" ]; then

    # extrai as configs como "k=v" e aplica via kafka-configs.sh
    mapfile -t configs < <(yq -r ".topics[${i}].config | to_entries | .[] | \"\(.key)=\(.value)\"" "${TOPICS_FILE}")

    if [ "${#configs[@]}" -gt 0 ]; then
      echo "[create-topics] Applying configs to ${name}: ${configs[*]}"
      /opt/kafka/bin/kafka-configs.sh \
        --bootstrap-server "${BOOTSTRAP_SERVERS}" \
        --entity-type topics \
        --entity-name "${name}" \
        --alter \
        $(printf -- '--add-config %s ' "$(IFS=,; echo "${configs[*]}")") >/dev/null || true
    fi
  fi
done

echo "[create-topics] Done."
