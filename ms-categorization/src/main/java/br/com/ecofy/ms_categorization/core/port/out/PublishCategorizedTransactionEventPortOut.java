package br.com.ecofy.ms_categorization.core.port.out;

import br.com.ecofy.ms_categorization.core.domain.event.CategorizationAppliedDomainEvent;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizedTransactionDomainEvent;

/**
 * Porta de saída para publicação de eventos de categorização.
 *
 * Usa EVENTOS DE DOMÍNIO (core), não DTOs Kafka — o adapter Kafka converte para o DTO
 * de saída. Assim o core não depende de adapters/messaging/Jackson.
 */
public interface PublishCategorizedTransactionEventPortOut {

    void publish(CategorizedTransactionDomainEvent event);

    void publish(CategorizationAppliedDomainEvent event);

}
