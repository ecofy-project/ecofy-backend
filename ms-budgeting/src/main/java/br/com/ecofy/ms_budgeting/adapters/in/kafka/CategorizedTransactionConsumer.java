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
public class CategorizedTransactionConsumer {

    private final InboundEventMapper mapper;
    // Correção Dia 6 (item #9): delega ao serviço de ingestão, que centraliza MDC,
    // validação de campos obrigatórios, logs padronizados START/DONE/FAIL e o wrapping
    // de exceções (BudgetEventIngestionFailedException). Antes o consumer chamava o use case
    // diretamente, duplicando MDC e ignorando esse tratamento padronizado.
    private final BudgetEventIngestionService ingestionService;

    @KafkaListener(
            topics = "${ecofy.budgeting.topics.categorized-transaction}",
            groupId = "${spring.application.name}",
            containerFactory = "budgetingKafkaListenerContainerFactory"
    )
    // Consome mensagens Kafka de transações categorizadas e delega ao serviço de ingestão.
    public void onMessage(ConsumerRecord<String, CategorizedTransactionMessage> record) {
        UUID runId = UUID.randomUUID();
        CategorizedTransactionMessage msg = record.value();

        // headers úteis (opcional): correlationId / eventId (propagados p/ MDC no serviço de ingestão)
        String correlationId = headerAsString(record, "correlationId");
        String eventId = headerAsString(record, "eventId");

        if (msg == null) {
            // payload nulo geralmente é descartável (tombstone/erro de serialização)
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

        // Se falhar, o serviço de ingestão trata/loga e relança para o ErrorHandler (retry / DLT).
        ProcessTransactionCommand command = mapper.toCommand(msg, runId, eventId, correlationId, record);
        ingestionService.ingest(command);
    }

    // Extrai um header do Kafka como String (UTF-8) a partir da chave informada.
    private static String headerAsString(ConsumerRecord<?, ?> record, String key) {
        Header h = record.headers().lastHeader(key);
        if (h == null || h.value() == null) return null;
        return new String(h.value(), StandardCharsets.UTF_8);
    }

}
