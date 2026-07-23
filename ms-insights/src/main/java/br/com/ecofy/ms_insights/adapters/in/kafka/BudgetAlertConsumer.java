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

// Consome alertas de orçamento e aciona a geração de insights.
@Slf4j
@Component
public class BudgetAlertConsumer {

    private final InsightsProperties properties;
    private final ObjectMapper objectMapper;
    private final InsightEventIngestionService ingestionService;

    public BudgetAlertConsumer(
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
            topics = "${ecofy.insights.topics.budget-alert-topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("[BudgetAlertConsumer] - [consume] -> Payload vazio recebido; ignorando");
            return;
        }

        final UUID userId;
        try {
            JsonNode root = objectMapper.readTree(payload);
            userId = parseRequiredUuid(root, "userId");
            UUID budgetId = parseOptionalUuid(root, "budgetId");
            String severity = parseOptionalText(root, "severity");
            String status = parseOptionalText(root, "status");

            log.info(
                    "[BudgetAlertConsumer] - [consume] -> userId={} budgetId={} severity={} status={}",
                    userId, budgetId, severity, status
            );
        } catch (Exception poison) {
            log.warn(
                    "[BudgetAlertConsumer] - [consume] -> Payload inválido (poison) descartado sem retentativa payloadSize={} error={}",
                    payload.length(), poison.getMessage()
            );
            return;
        }

        try {
            ingestionService.onSignalGenerate(userId);
        } catch (Exception transient_) {
            log.error(
                    "[BudgetAlertConsumer] - [consume] -> Falha transitória; relançando para nova tentativa userId={} error={}",
                    userId, transient_.getMessage()
            );
            throw new RuntimeException("Transient failure processing budget-alert event", transient_);
        }
    }

    // Valida e converte um identificador obrigatório do evento.
    private static UUID parseRequiredUuid(JsonNode root, String field) {
        if (root == null || root.get(field) == null) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        String v = root.get(field).asText(null);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return UUID.fromString(v.trim());
    }

    // Converte um identificador opcional do evento.
    private static UUID parseOptionalUuid(JsonNode root, String field) {
        if (root == null || root.get(field) == null) return null;
        String v = root.get(field).asText(null);
        if (v == null || v.isBlank()) return null;
        return UUID.fromString(v.trim());
    }

    // Normaliza um campo textual opcional do evento.
    private static String parseOptionalText(JsonNode root, String field) {
        if (root == null || root.get(field) == null) return null;
        String v = root.get(field).asText(null);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

}
