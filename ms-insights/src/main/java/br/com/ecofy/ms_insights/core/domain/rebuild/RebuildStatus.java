package br.com.ecofy.ms_insights.core.domain.rebuild;

// Define os estados do ciclo de vida de uma execução de rebuild, com transições guardadas pelo domínio.
public enum RebuildStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    COMPLETED_WITH_ERRORS,
    FAILED,
    CANCELLED
}
