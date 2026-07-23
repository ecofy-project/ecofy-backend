package br.com.ecofy.ms_ingestion.adapters.in.web.dto.response;

import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportErrorType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

// Expõe o erro de uma linha ao cliente sem devolver o conteúdo bruto do registro.
@Schema(description = "Erro de uma linha específica do arquivo")
public record ImportErrorResponse(

        @Schema(description = "Linha do arquivo (1-based)")
        Integer row,
        ImportErrorType errorType,
        String message,
        Instant createdAt
) {

    // Converte um ImportError de domínio para o DTO de resposta da API.
    public static ImportErrorResponse fromDomain(ImportError error) {
        return new ImportErrorResponse(
                error.lineNumber(),
                error.errorType(),
                error.errorMessage(),
                error.createdAt()
        );
    }
}
