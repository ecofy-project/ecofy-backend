package br.com.ecofy.ms_budgeting.adapters.out.messaging;

import br.com.ecofy.ms_budgeting.adapters.out.messaging.dto.BudgetAlertEvent;
import br.com.ecofy.ms_budgeting.adapters.out.messaging.mapper.EventMapper;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Correção Dia 6 (item #10 / compatibilidade ms-notification):
 * garante que o JSON publicado em {@code eco.budget.alert} contém EXATAMENTE os campos que o
 * consumidor {@code BudgetAlertEventMessage} do ms-notification exige — especialmente
 * {@code userId} (o mapper de entrada do ms-notification lança NPE se vier nulo).
 */
class BudgetAlertEventCompatibilityTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private BudgetAlert enrichedAlert() {
        return new BudgetAlert(
                UUID.randomUUID(),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                AlertSeverity.WARNING,
                "Budget WARNING: 80.00% consumed",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                Instant.parse("2026-01-15T10:00:00Z"),
                UUID.fromString("11111111-1111-1111-1111-111111111111"), // userId
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), // categoryId
                new BigDecimal("1000.00"),                                // limitAmount
                new BigDecimal("800.00"),                                 // consumedAmount
                80,                                                       // consumedPct
                "BRL"
        );
    }

    @Test
    void publishedJsonShouldContainAllFieldsRequiredByMsNotification() throws Exception {
        BudgetAlertEvent event = EventMapper.toEvent(enrichedAlert());

        JsonNode json = mapper.readTree(mapper.writeValueAsString(event));

        // userId é OBRIGATÓRIO no ms-notification (InboundEventMapper.requireUserId lança NPE se nulo)
        assertTrue(json.hasNonNull("userId"), "userId ausente/nulo quebraria o ms-notification");
        assertEquals("11111111-1111-1111-1111-111111111111", json.get("userId").asText());

        // Campos lidos pelo ms-notification (putIfNotNull): budgetId, categoryId, limit, consumed, pct, severity
        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", json.get("budgetId").asText());
        assertEquals("cccccccc-cccc-cccc-cccc-cccccccccccc", json.get("categoryId").asText());
        assertEquals(0, new BigDecimal(json.get("limitAmount").asText()).compareTo(new BigDecimal("1000.00")));
        assertEquals(0, new BigDecimal(json.get("consumedAmount").asText()).compareTo(new BigDecimal("800.00")));
        assertEquals(80, json.get("consumedPct").asInt());
        assertEquals("WARNING", json.get("severity").asText());

        // metadata.eventId alimenta a chave de idempotência do ms-notification
        assertTrue(json.has("metadata"));
        assertTrue(json.get("metadata").hasNonNull("eventId"));
        assertEquals("ms-budgeting", json.get("metadata").get("source").asText());
    }

    @Test
    void severityShouldBeSerializedAsStringName() throws Exception {
        BudgetAlertEvent event = EventMapper.toEvent(enrichedAlert());
        JsonNode json = mapper.readTree(mapper.writeValueAsString(event));

        assertTrue(json.get("severity").isTextual());
        assertEquals("WARNING", json.get("severity").asText());
    }
}
