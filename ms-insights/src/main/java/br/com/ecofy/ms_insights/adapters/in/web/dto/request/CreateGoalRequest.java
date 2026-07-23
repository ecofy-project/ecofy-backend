package br.com.ecofy.ms_insights.adapters.in.web.dto.request;

import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// Transporta os dados de criação de uma meta, validados na borda da API.
public record CreateGoalRequest(

        @NotNull(message = "userId is required")
        UUID userId,

        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 chars")
        String name,

        @Positive(message = "targetCents must be > 0")
        long targetCents,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
        String currency,

        GoalStatus status

) { }
