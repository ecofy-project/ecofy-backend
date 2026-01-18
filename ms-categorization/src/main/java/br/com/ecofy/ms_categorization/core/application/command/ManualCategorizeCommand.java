package br.com.ecofy.ms_categorization.core.application.command;

import java.util.Objects;
import java.util.UUID;

public record ManualCategorizeCommand(

        UUID transactionId,

        UUID categoryId,

        String rationale

) {

    // Valida os dados de entrada para categorização manual de uma transação (transação, categoria e justificativa opcional).
    public ManualCategorizeCommand {

        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        rationale = (rationale == null ? "" : rationale);

    }

}
