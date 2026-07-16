package br.com.ecofy.ms_insights.adapters.in.web.dto.request;

import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Correção Dia 8 (item #2): campos opcionais, mas validados quando presentes
 * (targetCents positivo; currency ISO 3 letras; name com tamanho máximo).
 * A regra "targetCents e currency devem vir juntos" permanece no GoalService.
 */
public record UpdateGoalRequest(

        @Size(max = 120, message = "name must be at most 120 chars")
        String name,

        @Positive(message = "targetCents must be > 0")
        Long targetCents,

        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
        String currency,

        GoalStatus status

) { }
