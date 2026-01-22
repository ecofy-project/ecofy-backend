package br.com.ecofy.ms_insights.adapters.in.kafka.mapper;

import br.com.ecofy.ms_insights.adapters.in.kafka.dto.BudgetAlertMessage;
import br.com.ecofy.ms_insights.adapters.in.kafka.dto.CategorizedTransactionMessage;
import br.com.ecofy.ms_insights.core.port.out.CategorizedTxView;

public final class InboundEventMapper {

    // Impede instanciação e reforça o uso estático (classe utilitária de mapeamento inbound).
    private InboundEventMapper() {}

    // Converte a mensagem de transação categorizada recebida do Kafka em uma view interna (DTO de leitura) para uso no core.
    public static CategorizedTxView toView(CategorizedTransactionMessage m) {
        return new CategorizedTxView(
                m.transactionId(),
                m.userId(),
                m.categoryId(),
                m.amountCents(),
                m.currency(),
                m.bookingDate(),
                m.income()
        );
    }

    // Extrai o userId da mensagem de alerta de budget para facilitar roteamento/lookup no processamento do evento.
    public static java.util.UUID userIdFromAlert(BudgetAlertMessage m) {
        return m.userId();
    }

}
