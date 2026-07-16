package br.com.ecofy.ms_insights.adapters.in.kafka;

import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.application.service.InsightEventIngestionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class CategorizedTransactionConsumer {

    private final InsightsProperties properties;
    private final ObjectMapper objectMapper;
    private final InsightEventIngestionService ingestionService;

    // Injeta as dependências do consumer (properties, parser JSON e serviço de ingestão) garantindo que não sejam nulas.
    public CategorizedTransactionConsumer(
            InsightsProperties properties,
            ObjectMapper objectMapper,
            InsightEventIngestionService ingestionService
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.ingestionService = Objects.requireNonNull(ingestionService, "ingestionService must not be null");
    }

    // Consome eventos de transação categorizada do Kafka, valida o payload, extrai campos principais e sinaliza geração de insights por usuário.
    @KafkaListener(
            topics = "${ecofy.insights.topics.categorized-transaction-topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("[CategorizedTransactionConsumer] - [consume] -> empty payload received; skipping");
            return;
        }

        // Correção Dia 8 (item #6): diferencia payload irrecuperável (poison) de falha transitória.
        final UUID userId;
        try {
            JsonNode root = objectMapper.readTree(payload);
            userId = parseRequiredUuid(root, "userId");
            UUID transactionId = parseOptionalUuid(root, "transactionId");
            UUID categoryId = parseOptionalUuid(root, "categoryId");

            log.info(
                    "[CategorizedTransactionConsumer] - [consume] -> topic={} userId={} transactionId={} categoryId={}",
                    properties.topics().categorizedTransactionTopic(),
                    userId, transactionId, categoryId
            );
        } catch (Exception poison) {
            // Payload malformado/invalido: reprocessar não resolve. Loga em WARN e ACK (evita loop infinito
            // sem DLT). Publicação em Dead Letter Topic fica como próximo passo documentado.
            log.warn(
                    "[CategorizedTransactionConsumer] - [consume] -> POISON payload skipped (no retry) topic={} payloadSize={} error={}",
                    properties.topics().categorizedTransactionTopic(), payload.length(), poison.getMessage()
            );
            return;
        }

        try {
            ingestionService.onSignalGenerate(userId);
        } catch (Exception transient_) {
            // Falha transitória (ex.: downstream/DB): NÃO engole; relança para o DefaultErrorHandler (retry).
            log.error(
                    "[CategorizedTransactionConsumer] - [consume] -> TRANSIENT failure -> rethrow for retry userId={} error={}",
                    userId, transient_.getMessage()
            );
            throw new RuntimeException("Transient failure processing categorized-transaction event", transient_);
        }
    }

    // Faz parse e valida um UUID obrigatório do JSON, lançando IllegalArgumentException quando ausente/vazio/inválido.
    private static UUID parseRequiredUuid(JsonNode root, String field) {
        if (root == null || root.get(field) == null || root.get(field).asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return UUID.fromString(root.get(field).asText().trim());
    }

    // Faz parse de um UUID opcional do JSON, retornando null quando ausente/vazio e lançando erro apenas se o formato for inválido.
    private static UUID parseOptionalUuid(JsonNode root, String field) {
        if (root == null || root.get(field) == null) return null;
        String v = root.get(field).asText();
        if (v == null || v.isBlank()) return null;
        return UUID.fromString(v.trim());
    }

}
