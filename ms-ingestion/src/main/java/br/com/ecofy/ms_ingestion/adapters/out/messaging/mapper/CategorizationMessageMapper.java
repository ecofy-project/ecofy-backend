package br.com.ecofy.ms_ingestion.adapters.out.messaging.mapper;

import br.com.ecofy.ms_ingestion.adapters.out.messaging.dto.CategorizationRequestMessage;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;

public final class CategorizationMessageMapper {

    private CategorizationMessageMapper() {}

    // Mapeia um RawTransaction para a mensagem de request de categorização (payload enviado ao Kafka).
    public static CategorizationRequestMessage from(RawTransaction tx) {
        return new CategorizationRequestMessage(
                tx.id(),
                tx.importJobId(),
                tx.description(),
                tx.amount().amount(),
                tx.amount().currency(),
                tx.date().value(),
                tx.sourceType().name()
        );
    }

}
