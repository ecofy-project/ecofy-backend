package br.com.ecofy.ms_insights.core.domain.exception;

import java.util.UUID;

// Sinaliza que a execução de rebuild informada não existe.
public class RebuildRunNotFoundException extends RuntimeException {
    public RebuildRunNotFoundException(UUID runId) {
        super("Insight rebuild not found for id: " + runId);
    }
}
