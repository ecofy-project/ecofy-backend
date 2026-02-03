#!/usr/bin/env bash
# infra/kafka/scripts/create-topics.sh
set -euo pipefail

BOOTSTRAP_SERVERS="${BOOTSTRAP_SERVERS:-kafka:9092}"

# ATENÇÃO: seu compose monta ../kafka/scripts em /scripts
# então o default correto aqui é /scripts/../topics.yml
TOPICS_FILE="${TOPICS_FILE:-/scripts/../topics.yml}"

echo "[create-topics] Using bootstrap: ${BOOTSTRAP_SERVERS}"
echo "[create-topics] Using topics file: ${TOPICS_FILE}"

if [[ ! -f "${TOPICS_FILE}" ]]; then
  echo "[create-topics] ERROR: topics file not found: ${TOPICS_FILE}"
  exit 1
fi

# Se existir wait-for-kafka no mesmo mount, usa.
if [[ -x "/scripts/wait-for-kafka.sh" ]]; then
  /scripts/wait-for-kafka.sh "${BOOTSTRAP_SERVERS}"
else
  echo "[create-topics] wait-for-kafka.sh not found; doing simple readiness check..."
  /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server "${BOOTSTRAP_SERVERS}" >/dev/null
fi

# Parser simples para o formato atual do topics.yml:
# - cada tópico começa com "- name:"
# - partitions/replicationFactor são escalares
# - config é um map identado, pares "k: v"
#
# Observação: o parser ignora comentários e linhas em branco.
parse_topics() {
  awk '
    function trim(s) { gsub(/^[ \t]+|[ \t]+$/, "", s); return s }
    function strip_quotes(s) { gsub(/^"|"$/, "", s); gsub(/^'\''|'\''$/, "", s); return s }
    function flush() {
      if (name != "") {
        if (partitions == "") partitions = "1"
        if (rf == "") rf = "1"
        # imprime: name|partitions|rf|k=v,k2=v2
        print name "|" partitions "|" rf "|" cfg
      }
      name=""; partitions=""; rf=""; cfg=""
    }
    /^[ \t]*#/ { next }
    /^[ \t]*$/ { next }

    /^[ \t]*-[ \t]*name:/ {
      flush()
      line=$0
      sub(/^[ \t]*-[ \t]*name:[ \t]*/, "", line)
      name=strip_quotes(trim(line))
      next
    }

    /^[ \t]*partitions:/ {
      line=$0
      sub(/^[ \t]*partitions:[ \t]*/, "", line)
      partitions=strip_quotes(trim(line))
      next
    }

    /^[ \t]*replicationFactor:/ {
      line=$0
      sub(/^[ \t]*replicationFactor:[ \t]*/, "", line)
      rf=strip_quotes(trim(line))
      next
    }

    # Linha de config (dentro de "config:")
    # Ex: "      retention.ms: "604800000"   # 7 dias"
    /^[ \t]+[A-Za-z0-9_.-]+:[ \t]*/ {
      # só consideramos config se já tem name aberto e se estamos dentro do bloco do tópico
      if (name == "") next

      # remove comentário inline
      line=$0
      sub(/[ \t]*#.*/, "", line)

      # separa chave:valor
      key=line
      sub(/:.*/, "", key)
      key=trim(key)

      val=line
      sub(/^[^:]*:[ \t]*/, "", val)
      val=trim(val)
      val=strip_quotes(val)

      # se for linha do próprio "config:" (sem valor), ignora
      if (key == "config" && val == "") next

      # adiciona ao cfg "k=v,k2=v2"
      if (key != "" && val != "") {
        if (cfg == "") cfg = key "=" val
        else cfg = cfg "," key "=" val
      }
      next
    }

    END { flush() }
  ' "${TOPICS_FILE}"
}

# Processa cada tópico
while IFS='|' read -r name partitions rf cfg; do
  if [[ -z "${name}" ]]; then
    echo "[create-topics] Skipping: empty name"
    continue
  fi

  echo "[create-topics] Ensuring topic: ${name} (partitions=${partitions}, rf=${rf})"

  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP_SERVERS}" \
    --create --if-not-exists \
    --topic "${name}" \
    --partitions "${partitions}" \
    --replication-factor "${rf}" >/dev/null

  if [[ -n "${cfg}" ]]; then
    echo "[create-topics] Applying configs to ${name}: ${cfg}"
    /opt/kafka/bin/kafka-configs.sh \
      --bootstrap-server "${BOOTSTRAP_SERVERS}" \
      --entity-type topics \
      --entity-name "${name}" \
      --alter \
      --add-config "${cfg}" >/dev/null || true
  fi
done < <(parse_topics)

echo "[create-topics] Done."
