package br.com.ecofy.ms_categorization.adapters.out.messaging;

import br.com.ecofy.ms_categorization.adapters.out.messaging.dto.CategorizationAppliedEvent;
import br.com.ecofy.ms_categorization.adapters.out.messaging.dto.CategorizedTransactionEvent;
import br.com.ecofy.ms_categorization.config.CategorizationProperties;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizationAppliedDomainEvent;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizedTransactionDomainEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategorizedTransactionKafkaAdapterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private CategorizationProperties props() {
        CategorizationProperties p = new CategorizationProperties();
        p.getTopics().setTransactionCategorized("eco.transaction.categorized");
        p.getTopics().setCategorizationApplied("eco.categorization.applied");
        return p;
    }

    @Test
    void publish_categorized_mapsDomainToKafkaDto_withBudgetingFields() {
        var adapter = new CategorizedTransactionKafkaAdapter(kafkaTemplate, props());
        // Future que nunca completa: evita executar o callback (foco é no que foi enviado).
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<SendResult<String, Object>>());

        UUID txId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        var domainEvent = new CategorizedTransactionDomainEvent(
                UUID.randomUUID(), txId, UUID.randomUUID(), "ext",
                LocalDate.of(2026, 1, 15), new BigDecimal("42.90"), Currency.getInstance("BRL"),
                categoryId, "AUTO", Instant.parse("2026-01-15T12:00:00Z"));

        adapter.publish(domainEvent);

        ArgumentCaptor<String> topicC = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyC = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueC = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(topicC.capture(), keyC.capture(), valueC.capture());

        assertEquals("eco.transaction.categorized", topicC.getValue());
        assertEquals(txId.toString(), keyC.getValue());

        assertInstanceOf(CategorizedTransactionEvent.class, valueC.getValue());
        CategorizedTransactionEvent dto = (CategorizedTransactionEvent) valueC.getValue();
        assertEquals(txId, dto.transactionId());
        assertEquals(categoryId, dto.categoryId());
        assertEquals(new BigDecimal("42.90"), dto.amount());
        assertEquals("BRL", dto.currency().getCurrencyCode());
        assertEquals(LocalDate.of(2026, 1, 15), dto.transactionDate());
    }

    @Test
    void publish_applied_mapsDomainToKafkaDto_onAppliedTopic() {
        var adapter = new CategorizedTransactionKafkaAdapter(kafkaTemplate, props());
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<SendResult<String, Object>>());

        UUID txId = UUID.randomUUID();
        var domainEvent = new CategorizationAppliedDomainEvent(
                UUID.randomUUID(), txId, UUID.randomUUID(), UUID.randomUUID(), "MANUAL", 100, UUID.randomUUID(),
                Instant.parse("2026-01-15T12:00:00Z"));

        adapter.publish(domainEvent);

        ArgumentCaptor<String> topicC = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueC = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(topicC.capture(), anyString(), valueC.capture());

        assertEquals("eco.categorization.applied", topicC.getValue());
        assertInstanceOf(CategorizationAppliedEvent.class, valueC.getValue());
        assertEquals("MANUAL", ((CategorizationAppliedEvent) valueC.getValue()).mode());
    }
}
