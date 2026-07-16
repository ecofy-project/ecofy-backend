package br.com.ecofy.ms_ingestion.adapters.out.messaging;

import br.com.ecofy.ms_ingestion.adapters.out.messaging.dto.CategorizationRequestMessage;
import br.com.ecofy.ms_ingestion.config.KafkaConfig;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategorizationRequestKafkaAdapterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final KafkaConfig.IngestionTopics topics = new KafkaConfig.IngestionTopics();

    private RawTransaction tx() {
        return RawTransaction.create(UUID.randomUUID(), "ext", "Supermarket",
                new TransactionDate(LocalDate.of(2026, 1, 15)),
                new Money(new BigDecimal("42.90"), "BRL"), TransactionSourceType.FILE_CSV);
    }

    @Test
    void publish_shouldSendToConfiguredCategorizationTopic_withSufficientPayload() {
        var adapter = new CategorizationRequestKafkaAdapter(kafkaTemplate, topics);
        RawTransaction tx = tx();

        adapter.publish(List.of(tx));

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        // Tópico preservado (compatível com ms-categorization).
        assertEquals("eco.categorization.request", topicCaptor.getValue());
        assertEquals(tx.id().toString(), keyCaptor.getValue());

        // Payload contém dados suficientes para categorização.
        assertInstanceOf(CategorizationRequestMessage.class, valueCaptor.getValue());
        CategorizationRequestMessage msg = (CategorizationRequestMessage) valueCaptor.getValue();
        assertEquals(tx.id(), msg.transactionId());
        assertEquals("Supermarket", msg.description());
        assertEquals(new BigDecimal("42.90"), msg.amount());
        assertEquals("BRL", msg.currency());
        assertEquals(LocalDate.of(2026, 1, 15), msg.transactionDate());
        assertEquals("FILE_CSV", msg.sourceType());
        assertNotNull(msg.importJobId());
    }

    @Test
    void publish_emptyList_shouldNotSend() {
        var adapter = new CategorizationRequestKafkaAdapter(kafkaTemplate, topics);

        adapter.publish(List.of());

        verifyNoInteractions(kafkaTemplate);
    }
}
