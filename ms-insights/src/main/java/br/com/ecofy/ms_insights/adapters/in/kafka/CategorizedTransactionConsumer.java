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

// Consome transações categorizadas e aciona a geração de insights.
@Slf4j
@Component
public class CategorizedTransactionConsumer {

    private final InsightsProperties properties;
    private final ObjectMapper objectMapper;
    private final InsightEventIngestionService ingestionService;

    public CategorizedTransactionConsumer(
            InsightsProperties properties,
            ObjectMapper objectMapper,
            InsightEventIngestionService ingestionService
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.ingestionService = Objects.requireNonNull(ingestionService, "ingestionService must not be null");
    }

    // Valida o evento recebido e aciona o processamento para o usuário.
    @KafkaListener(
            topics = "${ecofy.insights.topics.categorized-transaction-topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("[CategorizedTransactionConsumer] - [consume] -> Payload vazio recebido; ignorando");
            return;
        }

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
            log.warn(
                    "[CategorizedTransactionConsumer] - [consume] -> Payload inválido (poison) descartado sem retentativa topic={} payloadSize={} error={}",
                    properties.topics().categorizedTransactionTopic(), payload.length(), poison.getMessage()
            );
            return;
        }

        try {
            ingestionService.onSignalGenerate(userId);
        } catch (Exception transient_) {
            log.error(
                    "[CategorizedTransactionConsumer] - [consume] -> Falha transitória; relançando para nova tentativa userId={} error={}",
                    userId, transient_.getMessage()
            );
            throw new RuntimeException("Transient failure processing categorized-transaction event", transient_);
        }
    }

    // Valida e converte um identificador obrigatório do evento.
    private static UUID parseRequiredUuid(JsonNode root, String field) {
        if (root == null || root.get(field) == null || root.get(field).asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return UUID.fromString(root.get(field).asText().trim());
    }

    // Converte um identificador opcional do evento.
    private static UUID parseOptionalUuid(JsonNode root, String field) {
        if (root == null || root.get(field) == null) return null;
        String v = root.get(field).asText();
        if (v == null || v.isBlank()) return null;
        return UUID.fromString(v.trim());
    }

}
