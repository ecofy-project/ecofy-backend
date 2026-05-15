package br.com.ecofy.ms_ingestion.adapters.in.web.dto.response;

import br.com.ecofy.ms_ingestion.core.port.in.GetImportJobStatusUseCase;

import java.util.List;

public record ImportJobStatusResponse(

        ImportJobResponse job,
        List<ImportErrorResponse> errors

) {

    // Converte a view do use case (job + erros) para o DTO de resposta da API.
    public static ImportJobStatusResponse fromView(GetImportJobStatusUseCase.ImportJobStatusView view) {
        return new ImportJobStatusResponse(
                ImportJobResponse.fromDomain(view.job()),
                view.errors().stream().map(ImportErrorResponse::fromDomain).toList()
        );
    }

}
