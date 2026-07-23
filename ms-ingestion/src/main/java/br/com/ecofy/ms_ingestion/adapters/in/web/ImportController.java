package br.com.ecofy.ms_ingestion.adapters.in.web;

import br.com.ecofy.ms_ingestion.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.ms_ingestion.adapters.in.web.dto.response.ImportJobResponse;
import br.com.ecofy.ms_ingestion.adapters.in.web.dto.response.ImportJobStatusResponse;
import br.com.ecofy.ms_ingestion.adapters.in.web.dto.response.PageResponse;
import br.com.ecofy.ms_ingestion.adapters.in.web.security.AuthenticatedUser;
import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.ErrorDetail;
import br.com.ecofy.ms_ingestion.core.application.exception.FileRequiredException;
import br.com.ecofy.ms_ingestion.core.application.exception.PaginationParameterInvalidException;
import br.com.ecofy.ms_ingestion.core.application.exception.StorageException;
import br.com.ecofy.ms_ingestion.core.application.exception.UnsupportedFileTypeException;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import br.com.ecofy.ms_ingestion.core.port.in.GetImportJobStatusUseCase;
import br.com.ecofy.ms_ingestion.core.port.in.ListImportJobsUseCase;
import br.com.ecofy.ms_ingestion.core.port.in.StartImportJobUseCase;
import br.com.ecofy.ms_ingestion.core.port.in.UploadFileUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

// Centraliza os endpoints de upload e acompanhamento das importações.
@RestController
@RequestMapping(path = "/api/import", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Ingestion", description = "Upload de arquivos (CSV/OFX) e acompanhamento do processamento de importações")
@Slf4j
public class ImportController {

    private final UploadFileUseCase uploadFileUseCase;
    private final StartImportJobUseCase startImportJobUseCase;
    private final GetImportJobStatusUseCase getImportJobStatusUseCase;
    private final ListImportJobsUseCase listImportJobsUseCase;
    private final IngestionProperties properties;

    public ImportController(UploadFileUseCase uploadFileUseCase,
                            StartImportJobUseCase startImportJobUseCase,
                            GetImportJobStatusUseCase getImportJobStatusUseCase,
                            ListImportJobsUseCase listImportJobsUseCase,
                            IngestionProperties properties) {
        this.uploadFileUseCase = Objects.requireNonNull(uploadFileUseCase);
        this.startImportJobUseCase = Objects.requireNonNull(startImportJobUseCase);
        this.getImportJobStatusUseCase = Objects.requireNonNull(getImportJobStatusUseCase);
        this.listImportJobsUseCase = Objects.requireNonNull(listImportJobsUseCase);
        this.properties = Objects.requireNonNull(properties);
    }

    // Processa o upload autenticado e retorna o resultado final da importação.
    @Operation(
            summary = "Upload de arquivo e processamento da importação",
            description = """
                    Recebe um arquivo CSV/OFX, valida, armazena em streaming, cria e PROCESSA o ImportJob.

                    Comportamento:
                    - Requer JWT. O dono do arquivo vem da claim `sub`; não há upload anônimo.
                    - Se `type` não for informado, o serviço infere pelo sufixo do nome do arquivo (.csv / .ofx).
                    - O processamento é SÍNCRONO: a resposta 200 já traz o job com o STATUS FINAL e os contadores.
                    - Idempotência: reenviar o MESMO conteúdo (mesmo dono) devolve 409 com o header
                      `Location` apontando o job existente — replay não duplica transações.
                    - `Idempotency-Key` é opcional; reusá-la com conteúdo diferente devolve 409
                      IDEMPOTENCY_KEY_PAYLOAD_MISMATCH.
                    - Uma linha inválida NÃO invalida o arquivo: o job termina COMPLETED_WITH_ERRORS
                      e os contadores discriminam válidas/inválidas/duplicadas.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo processado; job com status final e contadores",
                    content = @Content(schema = @Schema(implementation = ImportJobResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente ou requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "409", description = "Arquivo já importado, ou Idempotency-Key reutilizada com outro conteúdo"),
            @ApiResponse(responseCode = "413", description = "Arquivo excede o tamanho máximo permitido"),
            @ApiResponse(responseCode = "415", description = "Extensão/MIME/conteúdo não suportado"),
            @ApiResponse(responseCode = "422", description = "Cabeçalho, encoding ou estrutura do arquivo inválidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar upload/início do job")
    })
    @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> uploadAndStart(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "type", required = false) ImportFileType type,
            @Parameter(description = "Chave de idempotência opcional; escopo por usuário")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        UUID ownerId = AuthenticatedUser.requireOwnerId();
        String correlationId = CorrelationId.currentOrGenerate();

        if (file == null || file.isEmpty()) {
            throw new FileRequiredException();
        }

        ImportFileType resolvedType = type != null ? type : guessType(file.getOriginalFilename());

        log.info("[ImportController] - [uploadAndStart] -> Recebendo upload type={} sizeBytes={}",
                resolvedType, file.getSize());

        ImportFile importFile;

        try (InputStream content = file.getInputStream()) {
            importFile = uploadFileUseCase.upload(new UploadFileUseCase.UploadFileCommand(
                    ownerId,
                    file.getOriginalFilename(),
                    resolvedType,
                    file.getContentType(),
                    file.getSize(),
                    content,
                    idempotencyKey,
                    correlationId
            ));
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file", e);
        }

        ImportJob job = startImportJobUseCase.start(
                new StartImportJobUseCase.StartImportJobCommand(importFile.id(), ownerId, correlationId));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .replacePath("/api/import/jobs/{id}")
                .buildAndExpand(job.id())
                .toUri();

        return ResponseEntity.ok().location(location).body(ImportJobResponse.fromDomain(job));
    }

    // Consulta o status da importação pertencente ao usuário autenticado.
    @Operation(
            summary = "Consulta o status de um job de importação",
            description = """
                    Retorna o status atual do ImportJob, contadores e os erros por linha registrados.

                    Só devolve jobs do usuário autenticado. Um job de outro usuário responde 403.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = ImportJobStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "403", description = "O job pertence a outro usuário"),
            @ApiResponse(responseCode = "404", description = "Job não encontrado")
    })
    @GetMapping("/jobs/{id}")
    public ResponseEntity<ImportJobStatusResponse> getJob(@PathVariable("id") UUID id) {
        UUID ownerId = AuthenticatedUser.requireOwnerId();

        log.debug("[ImportController] - [getJob] -> Consultando job id={}", id);

        var view = getImportJobStatusUseCase.getById(id, ownerId);
        return ResponseEntity.ok(ImportJobStatusResponse.fromView(view));
    }

    // Consulta o histórico paginado do usuário autenticado.
    @Operation(
            summary = "Histórico paginado de importações do usuário",
            description = """
                    Lista os ImportJobs do usuário autenticado, do mais recente para o mais antigo.

                    - `size` é limitado por `ecofy.ingestion.pagination.max-size`; acima disso responde 400.
                    - `sort` aceita apenas: createdAt, finishedAt, status, fileName — outro valor responde 400.
                    - Não existe parâmetro de usuário: o escopo vem sempre do JWT.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetro de paginação/ordenação inválido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)")
    })
    @GetMapping("/jobs")
    public ResponseEntity<PageResponse<ImportJobResponse>> listJobs(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "Campo e direção, ex.: createdAt,desc")
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "status", required = false) String status
    ) {
        UUID ownerId = AuthenticatedUser.requireOwnerId();

        IngestionProperties.Pagination limits = properties.getPagination();

        int resolvedPage = resolvePage(page);
        int resolvedSize = resolveSize(size, limits);
        SortSpec sortSpec = resolveSort(sort, limits);
        ImportJobStatus statusFilter = resolveStatus(status);

        PageResult<ImportJob> result = listImportJobsUseCase.list(new ListImportJobsUseCase.ListImportJobsQuery(
                ownerId, statusFilter, resolvedPage, resolvedSize, sortSpec.field(), sortSpec.ascending()));

        return ResponseEntity.ok(PageResponse.from(result, ImportJobResponse::fromDomain));
    }

    private static int resolvePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw new PaginationParameterInvalidException(List.of(
                    ErrorDetail.ofField("page", "OUT_OF_RANGE", "page deve ser >= 0.")));
        }
        return page;
    }

    // Valida o tamanho solicitado conforme os limites configurados.
    private static int resolveSize(Integer size, IngestionProperties.Pagination limits) {
        if (size == null) {
            return limits.getDefaultSize();
        }
        if (size < 1 || size > limits.getMaxSize()) {
            throw new PaginationParameterInvalidException(List.of(
                    ErrorDetail.ofField("size", "OUT_OF_RANGE",
                            "size deve estar entre 1 e " + limits.getMaxSize() + ".")));
        }
        return size;
    }

    // Valida o campo e a direção usados na ordenação.
    private static SortSpec resolveSort(String sort, IngestionProperties.Pagination limits) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("createdAt", false);
        }

        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();

        if (limits.getSortableFields().stream().noneMatch(f -> f.equals(field))) {
            throw new PaginationParameterInvalidException(List.of(
                    ErrorDetail.ofField("sort", "NOT_ALLOWED",
                            "Ordenação permitida apenas por: " + limits.getSortableFields() + ".")));
        }

        boolean ascending = true;
        if (parts.length == 2) {
            String direction = parts[1].trim().toLowerCase(Locale.ROOT);
            if (direction.equals("desc")) {
                ascending = false;
            } else if (!direction.equals("asc")) {
                throw new PaginationParameterInvalidException(List.of(
                        ErrorDetail.ofField("sort", "INVALID_DIRECTION", "Direção deve ser asc ou desc.")));
            }
        }
        return new SortSpec(field, ascending);
    }

    private static ImportJobStatus resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ImportJobStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PaginationParameterInvalidException(List.of(
                    ErrorDetail.ofField("status", "INVALID_VALUE",
                            "Status inválido. Valores: " + List.of(ImportJobStatus.values()) + ".")));
        }
    }

    // Resolve o tipo do arquivo pela extensão informada.
    private static ImportFileType guessType(String filename) {
        String lower = filename != null ? filename.toLowerCase(Locale.ROOT) : "";
        if (lower.endsWith(".csv")) {
            return ImportFileType.CSV;
        }
        if (lower.endsWith(".ofx")) {
            return ImportFileType.OFX;
        }
        throw new UnsupportedFileTypeException(
                "Could not infer the file type from the file name",
                "reason=unknownExtension");
    }

    private record SortSpec(String field, boolean ascending) {
    }
}
