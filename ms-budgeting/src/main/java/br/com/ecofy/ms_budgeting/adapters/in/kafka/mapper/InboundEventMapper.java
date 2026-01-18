package br.com.ecofy.ms_budgeting.adapters.in.kafka.mapper;

import br.com.ecofy.ms_budgeting.adapters.in.kafka.dto.CategorizedTransactionMessage;
import br.com.ecofy.ms_budgeting.core.application.command.ProcessTransactionCommand;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class InboundEventMapper {

    private static final String HDR_EVENT_ID = "event_id";
    private static final String HDR_CORRELATION_ID = "correlation_id";

    // Overload recomendado: o consumer passa apenas (msg, record).
    // Gera runId e tenta extrair eventId/correlationId dos headers do Kafka.
    public ProcessTransactionCommand toCommand(
            CategorizedTransactionMessage msg,
            ConsumerRecord<String, CategorizedTransactionMessage> record
    ) {
        UUID runId = UUID.randomUUID();
        String eventId = firstHeaderAsString(record, HDR_EVENT_ID);
        String correlationId = firstHeaderAsString(record, HDR_CORRELATION_ID);

        return toCommand(msg, runId, eventId, correlationId, record);
    }

    // Overload completo: permite o consumer passar runId/eventId/correlationId explicitamente.
    public ProcessTransactionCommand toCommand(
            CategorizedTransactionMessage msg,
            UUID runId,
            String eventId,
            String correlationId,
            ConsumerRecord<String, CategorizedTransactionMessage> record
    ) {
        if (msg == null) throw new IllegalArgumentException("msg must not be null");
        if (runId == null) throw new IllegalArgumentException("runId must not be null");
        if (record == null) throw new IllegalArgumentException("record must not be null");

        // Observação: ajuste os getters abaixo conforme o seu DTO real.
        return new ProcessTransactionCommand(
                runId,
                msg.transactionId(),
                msg.userId(),
                msg.categoryId(),
                msg.amount(),
                msg.currency(),
                msg.transactionDate(),
                new ProcessTransactionCommand.EventMetadata(
                        blankToNull(eventId),
                        blankToNull(correlationId),
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key(),
                        Instant.ofEpochMilli(record.timestamp())
                )
        );
    }

    private static String firstHeaderAsString(ConsumerRecord<?, ?> record, String headerKey) {
        if (record == null || record.headers() == null) return null;

        Header header = record.headers().lastHeader(headerKey);
        if (header == null || header.value() == null) return null;

        String value = new String(header.value(), StandardCharsets.UTF_8);
        return value.isBlank() ? null : value;
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

}
