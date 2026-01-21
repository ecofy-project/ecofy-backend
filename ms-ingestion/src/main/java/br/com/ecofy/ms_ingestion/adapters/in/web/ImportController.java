package br.com.ecofy.ms_ingestion.adapters.in.web;

import br.com.ecofy.ms_ingestion.adapters.in.web.dto.response.ImportJobResponse;
import br.com.ecofy.ms_ingestion.adapters.in.web.dto.response.ImportJobStatusResponse;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;
import br.com.ecofy.ms_ingestion.core.port.in.GetImportJobStatusUseCase;
import br.com.ecofy.ms_ingestion.core.port.in.StartImportJobUseCase;
import br.com.ecofy.ms_ingestion.core.port.in.UploadFileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/import", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Ingestion", description = "Upload de arquivos (CSV/OFX) e acompanhamento do processamento de importações")
@Slf4j
@RequiredArgsConstructor
public class ImportController {

    private final UploadFileUseCase uploadFileUseCase;
    private final StartImportJobUseCase startImportJobUseCase;
    private final GetImportJobStatusUseCase getImportJobStatusUseCase;

    @Operation(
            summary = "Upload de arquivo e início de importação",
            description = """
                    Recebe um arquivo CSV/OFX, persiste os metadados, armazena o conteúdo e dispara a criação de um ImportJob.
                    
                    Comportamento:
                    - Se `type` não for informado, o serviço tenta inferir pelo sufixo do nome do arquivo (.csv / .ofx).
                    - A resposta é 202 (Accepted) pois o processamento ocorre de forma assíncrona (job-based).
                    
                    Use o endpoint de status para acompanhar a execução do job.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Arquivo aceito e job iniciado com sucesso",
                    content = @Content(schema = @Schema(implementation = ImportJobResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido ou tipo não suportado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar upload/início do job")
    })
    @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> uploadAndStart(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam(value = "type", required = false) ImportFileType type
    ) throws IOException {

        log.info(
                "[ImportController] - [uploadAndStart] -> Recebendo upload fileName={} size={}",
                file.getOriginalFilename(), file.getSize()
        );

        ImportFileType resolvedType = type != null ? type : guessType(file.getOriginalFilename());
        byte[] bytes = file.getBytes();

        ImportFile importFile = uploadFileUseCase.upload(
                new UploadFileUseCase.UploadFileCommand(
                        file.getOriginalFilename(),
                        resolvedType,
                        file.getSize(),
                        bytes
                )
        );

        var job = startImportJobUseCase.start(
                new StartImportJobUseCase.StartImportJobCommand(importFile.id())
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .replacePath("/api/import/jobs/{id}")
                .buildAndExpand(job.id())
                .toUri();

        return ResponseEntity
                .accepted()
                .location(location)
                .body(ImportJobResponse.fromDomain(job));
    }

    @Operation(
            summary = "Consulta o status de um job de importação",
            description = """
                    Retorna o status atual do ImportJob, contadores e (quando aplicável) erros associados.
                    
                    Use este endpoint para polling após o upload (202 Accepted).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = ImportJobStatusResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Job não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao consultar status")
    })
    @GetMapping("/jobs/{id}")
    public ResponseEntity<ImportJobStatusResponse> getJob(@PathVariable("id") UUID id) {

        log.debug("[ImportController] - [getJob] -> Consultando job id={}", id);

        var view = getImportJobStatusUseCase.getById(id);
        return ResponseEntity.ok(ImportJobStatusResponse.fromView(view));
    }

    private ImportFileType guessType(String filename) {
        String lower = filename != null ? filename.toLowerCase() : "";
        if (lower.endsWith(".csv")) {
            return ImportFileType.CSV;
        }
        if (lower.endsWith(".ofx")) {
            return ImportFileType.OFX;
        }
        throw new IllegalArgumentException("Could not infer file type from name: " + filename);
    }

}
