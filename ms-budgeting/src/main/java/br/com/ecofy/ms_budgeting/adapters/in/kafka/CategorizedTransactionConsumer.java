package br.com.ecofy.ms_budgeting.adapters.in.kafka;

import br.com.ecofy.ms_budgeting.adapters.in.kafka.dto.CategorizedTransactionMessage;
import br.com.ecofy.ms_budgeting.adapters.in.kafka.mapper.InboundEventMapper;
import br.com.ecofy.ms_budgeting.core.port.in.ProcessTransactionForBudgetUseCase;
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
public class CategorizedTransactionConsumer {

    private final InboundEventMapper mapper;
    private final ProcessTransactionForBudgetUseCase useCase;

    @KafkaListener(
            topics = "${ecofy.budgeting.topics.categorized-transaction}",
            groupId = "${spring.application.name}",
            containerFactory = "budgetingKafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, CategorizedTransactionMessage> record) {
        UUID runId = UUID.randomUUID();
        CategorizedTransactionMessage msg = record.value();

        // headers úteis (opcional): traceId / correlationId / eventId
        String correlationId = headerAsString(record, "correlationId");
        String eventId = headerAsString(record, "eventId");

        if (msg == null) {
            log.warn("[CategorizedTransactionConsumer] IGNORE null payload runId={} topic={} partition={} offset={}",
                    runId, record.topic(), record.partition(), record.offset());
            return; // payload nulo geralmente é descartável
        }

        validate(msg);

        log.info(
                "[CategorizedTransactionConsumer] RECEIVED runId={} eventId={} correlationId={} topic={} partition={} offset={} key={} txId={} userId={} categoryId={}",
                runId, eventId, correlationId,
                record.topic(), record.partition(), record.offset(), record.key(),
                msg.transactionId(), msg.userId(), msg.categoryId()
        );

        // Se falhar, lança exception e o ErrorHandler toma conta (retry / DLT)
        useCase.process(mapper.toCommand(msg, runId, eventId, correlationId, record));
    }

    private static void validate(CategorizedTransactionMessage msg) {
        if (isBlank(String.valueOf(msg.transactionId()))) throw new IllegalArgumentException("transactionId is required");
        if (isBlank(String.valueOf(msg.userId()))) throw new IllegalArgumentException("userId is required");
        // categoryId pode ser opcional dependendo do seu domínio; se for obrigatória, valide aqui:
        // if (isBlank(msg.categoryId())) throw new IllegalArgumentException("categoryId is required");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String headerAsString(ConsumerRecord<?, ?> record, String key) {
        Header h = record.headers().lastHeader(key);
        if (h == null || h.value() == null) return null;
        return new String(h.value(), StandardCharsets.UTF_8);
    }

}
