package br.com.ecofy.ms_categorization.adapters.in.kafka.mapper;

import br.com.ecofy.ms_categorization.adapters.in.kafka.dto.CategorizationRequestMessage;
import br.com.ecofy.ms_categorization.core.domain.Transaction;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Merchant;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

// Centraliza a conversão das mensagens recebidas para o domínio.
@Component
public class InboundMessageMapper {

    private final Clock clock;

    public InboundMessageMapper() {
        this(Clock.systemUTC());
    }

    public InboundMessageMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Converte a solicitação de categorização em uma transação.
    public Transaction toDomain(CategorizationRequestMessage msg) {
        Objects.requireNonNull(msg, "msg must not be null");

        Instant now = Instant.now(clock);

        String rawDesc = safe(msg.description());
        Merchant merchant = Merchant.of(rawDesc);
        Money money = new Money(msg.amount(), msg.currency());

        return new Transaction(
                msg.transactionId(),
                msg.importJobId(),
                msg.externalId(),
                rawDesc,
                merchant,
                msg.transactionDate(),
                money,
                msg.sourceType(),
                null,
                now,
                now
        );
    }

    // Normaliza descrições nulas para uma representação vazia.
    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
