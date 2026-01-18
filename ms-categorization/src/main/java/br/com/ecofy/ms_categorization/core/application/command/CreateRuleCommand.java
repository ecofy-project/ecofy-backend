package br.com.ecofy.ms_categorization.core.application.command;

import br.com.ecofy.ms_categorization.core.domain.valueobject.RuleCondition;
import br.com.ecofy.ms_categorization.core.domain.enums.RuleStatus;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateRuleCommand(

        UUID categoryId,

        String name,

        RuleStatus status,

        int priority,

        List<RuleCondition> conditions

) {
    // Valida os dados de entrada para criação de uma regra de categorização (categoria, nome, status, prioridade e condições).
    public CreateRuleCommand {

        Objects.requireNonNull(categoryId, "categoryId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(conditions, "conditions must not be null");

        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");

        if (conditions.isEmpty()) throw new IllegalArgumentException("conditions must not be empty");

    }

}
