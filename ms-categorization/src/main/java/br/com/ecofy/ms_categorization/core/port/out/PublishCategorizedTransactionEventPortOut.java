package br.com.ecofy.ms_categorization.core.port.out;

import br.com.ecofy.ms_categorization.core.domain.event.CategorizationAppliedDomainEvent;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizedTransactionDomainEvent;

public interface PublishCategorizedTransactionEventPortOut {

    void publish(CategorizedTransactionDomainEvent event);

    void publish(CategorizationAppliedDomainEvent event);

}
