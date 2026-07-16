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
public class BudgetAlertConsumer {

    private final InsightsProperties properties;
    private final ObjectMapper objectMapper;
    private final InsightEventIngestionService ingestionService;

    // Injeta configurações, ObjectMapper e o serviço de ingestão garantindo que dependências essenciais não sejam nulas.
    public BudgetAlertConsumer(
            InsightsProperties properties,
            ObjectMapper objectMapper,
            InsightEventIngestionService ingestionService
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.ingestionService = Objects.requireNonNull(ingestionService, "ingestionService must not be null");
    }

    /**
     * Preferir placeholder de property ao invés de SpEL por nome de bean.
     * Isso evita o erro: "bean named 'insightsProperties' could not be found".
     *
     * Requer no application.yml:
     * ecofy.insights.topics.budget-alert-topic: eco.budget.alert
     */
    @KafkaListener(
            topics = "${ecofy.insights.topics.budget-alert-topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    // Consome eventos de alerta de budget do Kafka, valida payload, faz parsing do JSON e dispara a geração de insights para o usuário.
    public void consume(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("[BudgetAlertConsumer] - [consume] -> empty payload received; skipping");
            return;
        }

        // Correção Dia 8 (item #6): diferencia payload irrecuperável (poison) de falha transitória.
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
            // Payload malformado/invalido: reprocessar não resolve. Loga em WARN e ACK (evita loop infinito
            // sem DLT). Publicação em Dead Letter Topic fica como próximo passo documentado.
            log.warn(
                    "[BudgetAlertConsumer] - [consume] -> POISON payload skipped (no retry) payloadSize={} error={}",
                    payload.length(), poison.getMessage()
            );
            return;
        }

        try {
            ingestionService.onSignalGenerate(userId);
        } catch (Exception transient_) {
            // Falha transitória (ex.: downstream/DB): NÃO engole; relança para o DefaultErrorHandler (retry).
            log.error(
                    "[BudgetAlertConsumer] - [consume] -> TRANSIENT failure -> rethrow for retry userId={} error={}",
                    userId, transient_.getMessage()
            );
            throw new RuntimeException("Transient failure processing budget-alert event", transient_);
        }
    }

    // Faz parse e valida um UUID obrigatório do JSON, lançando IllegalArgumentException quando ausente/vazio/inválido.
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

    // Faz parse de um UUID opcional do JSON, retornando null quando ausente/vazio e lançando erro apenas se o formato for inválido.
    private static UUID parseOptionalUuid(JsonNode root, String field) {
        if (root == null || root.get(field) == null) return null;
        String v = root.get(field).asText(null);
        if (v == null || v.isBlank()) return null;
        return UUID.fromString(v.trim());
    }

    // Faz parse de um campo texto opcional do JSON, normalizando whitespace e retornando null quando ausente/vazio.
    private static String parseOptionalText(JsonNode root, String field) {
        if (root == null || root.get(field) == null) return null;
        String v = root.get(field).asText(null);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

}
