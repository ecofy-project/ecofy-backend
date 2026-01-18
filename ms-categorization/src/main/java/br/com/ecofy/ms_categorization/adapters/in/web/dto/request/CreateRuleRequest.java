package br.com.ecofy.ms_categorization.adapters.in.web.dto.request;

import br.com.ecofy.ms_categorization.core.domain.enums.RuleStatus;
import br.com.ecofy.ms_categorization.core.domain.valueobject.RuleCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateRuleRequest(

        @NotNull
        UUID categoryId,
        @NotBlank
        String name,
        @NotNull
        RuleStatus status,
        int priority,
        @NotEmpty
        List<@NotNull RuleCondition> conditions

) { }