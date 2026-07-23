package br.com.ecofy.ms_ingestion.adapters.in.web.dto.response;

import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

// Expõe o estado e os contadores do job, omitindo detalhes internos de storage e idempotência.
@Schema(description = "Estado e contadores de um job de importação")
public record ImportJobResponse(

        UUID id,
        UUID importFileId,
        ImportJobStatus status,

        @Schema(description = "Linhas de dados lidas (válidas + inválidas + duplicadas)")
        int totalRecords,
        int processedRecords,

        @Schema(description = "Linhas válidas persistidas")
        int successCount,

        @Schema(description = "Total de linhas inválidas — sempre exato, mesmo quando os detalhes são truncados")
        int errorCount,

        @Schema(description = "Linhas ignoradas por já existirem (idempotência por linha)")
        int duplicateRecords,

        @Schema(description = "Linhas confirmadas pelo broker Kafka")
        int publishedRecords,

        @Schema(description = "Quantos erros foram detalhados/persistidos (<= errorCount)")
        int recordedErrors,

        @Schema(description = "true quando errorCount > recordedErrors: existem mais erros do que os detalhados")
        boolean errorsTruncated,

        @Schema(description = "Código do motivo da falha global; null quando não houve")
        String failureCode,
        String failureReason,

        @Schema(description = "Correlation ID da requisição que originou o job; use-o no suporte")
        String correlationId,

        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt
) {

    // Converte um ImportJob de domínio para o DTO de resposta da API.
    public static ImportJobResponse fromDomain(ImportJob job) {
        return new ImportJobResponse(
                job.id(),
                job.importFileId(),
                job.status(),
                job.totalRecords(),
                job.processedRecords(),
                job.successCount(),
                job.errorCount(),
                job.duplicateRecords(),
                job.publishedRecords(),
                job.recordedErrors(),
                job.errorsTruncated(),
                job.failureCode(),
                job.failureReason(),
                job.correlationId(),
                job.startedAt(),
                job.finishedAt(),
                job.createdAt(),
                job.updatedAt()
        );
    }
}
