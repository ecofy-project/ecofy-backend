package br.com.ecofy.ms_budgeting.adapters.in.kafka;

import br.com.ecofy.ms_budgeting.adapters.in.kafka.dto.CategorizedTransactionMessage;
import br.com.ecofy.ms_budgeting.adapters.in.kafka.mapper.InboundEventMapper;
import br.com.ecofy.ms_budgeting.core.application.command.ProcessTransactionCommand;
import br.com.ecofy.ms_budgeting.core.application.service.BudgetEventIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
// Centraliza o consumo de eventos Kafka de transações categorizadas.
public class CategorizedTransactionConsumer {

    private final InboundEventMapper mapper;
    private final BudgetEventIngestionService ingestionService;

    @KafkaListener(
            topics = "${ecofy.budgeting.topics.categorized-transaction}",
            groupId = "${spring.application.name}",
            containerFactory = "budgetingKafkaListenerContainerFactory"
    )
    // Converte a mensagem recebida e delega seu processamento ao serviço de ingestão.
    public void onMessage(ConsumerRecord<String, CategorizedTransactionMessage> record) {
        UUID runId = UUID.randomUUID();
        CategorizedTransactionMessage msg = record.value();

        String correlationId = headerAsString(record, "correlationId");
        String eventId = headerAsString(record, "eventId");

        if (msg == null) {
            log.warn("[CategorizedTransactionConsumer] IGNORE null payload runId={} topic={} partition={} offset={}",
                    runId, record.topic(), record.partition(), record.offset());
            return;
        }

        log.info(
                "[CategorizedTransactionConsumer] RECEIVED runId={} eventId={} correlationId={} topic={} partition={} offset={} key={} txId={} userId={} categoryId={}",
                runId, eventId, correlationId,
                record.topic(), record.partition(), record.offset(), record.key(),
                msg.transactionId(), msg.userId(), msg.categoryId()
        );

        ProcessTransactionCommand command = mapper.toCommand(
                msg,
                runId,
                eventId,
                correlationId,
                record
        );

        ingestionService.ingest(command);
    }

    // Converte o último cabeçalho Kafka encontrado em texto UTF-8.
    private static String headerAsString(ConsumerRecord<?, ?> record, String key) {
        Header h = record.headers().lastHeader(key);

        if (h == null || h.value() == null) {
            return null;
        }

        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
