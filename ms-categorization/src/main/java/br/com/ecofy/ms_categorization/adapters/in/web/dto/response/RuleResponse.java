package br.com.ecofy.ms_categorization.adapters.in.web.dto.response;

import br.com.ecofy.ms_categorization.core.domain.enums.RuleStatus;
import br.com.ecofy.ms_categorization.core.domain.valueobject.RuleCondition;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleResponse(

        UUID id,
        UUID categoryId,
        String name,
        RuleStatus status,
        int priority,
        List<RuleCondition> conditions,
        Instant createdAt,
        Instant updatedAt

) { }
