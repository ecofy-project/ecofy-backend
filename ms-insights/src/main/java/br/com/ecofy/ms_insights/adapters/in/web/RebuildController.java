package br.com.ecofy.ms_insights.adapters.in.web;

import br.com.ecofy.ms_insights.adapters.in.web.dto.request.RebuildInsightsRequest;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.PageResponse;
import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.application.command.RebuildInsightsCommand;
import br.com.ecofy.ms_insights.core.application.result.RebuildRunResult;
import br.com.ecofy.ms_insights.core.port.in.RebuildInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.in.RebuildRunQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Expõe a inicialização e a consulta de rebuilds de insights.
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Insights Rebuild",
        description = "Reconstrução de insights com controle de escopo, status e histórico"
)
@RequestMapping(
        path = "/api/insights/v1/rebuild",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class RebuildController {

    private final RebuildInsightsUseCase rebuildInsightsUseCase;
    private final RebuildRunQueryUseCase rebuildRunQueryUseCase;
    private final InsightsProperties properties;

    @Operation(
            summary = "Inicia um rebuild de insights",
            description = """
                    Inicia uma execução assíncrona de rebuild de insights conforme:
                    - usuário
                    - período
                    - granularidade
                    - tipo de insight
                    - modo de execução

                    Resposta:
                    - 202 (Accepted) com os dados da execução iniciada.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Rebuild aceito para processamento",
                    content = @Content(schema = @Schema(implementation = RebuildRunResult.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido ou regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao iniciar o rebuild")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RebuildRunResult> start(
            @Valid @RequestBody RebuildInsightsRequest request
    ) {
        log.info(
                "[RebuildController] - [start] -> Iniciando rebuild userId={} start={} end={} granularity={} type={} mode={}",
                request.userId(),
                request.periodStart(),
                request.periodEnd(),
                request.granularity(),
                request.insightType(),
                request.mode()
        );

        RebuildRunResult result = rebuildInsightsUseCase.rebuild(
                new RebuildInsightsCommand(
                        request.userId(),
                        request.periodStart(),
                        request.periodEnd(),
                        request.granularity(),
                        request.insightType(),
                        request.mode()
                )
        );

        return ResponseEntity.accepted().body(result);
    }

    @Operation(
            summary = "Consulta o status de um rebuild",
            description = """
                    Retorna o estado atual de uma execução de rebuild identificada pelo runId.

                    Resposta:
                    - 200 (OK) com os dados atualizados da execução.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = RebuildRunResult.class))
            ),
            @ApiResponse(responseCode = "400", description = "Identificador da execução inválido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Execução de rebuild não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao consultar o rebuild")
    })
    @GetMapping("/{runId}")
    public ResponseEntity<RebuildRunResult> status(
            @PathVariable("runId") @NotNull UUID runId
    ) {
        log.debug(
                "[RebuildController] - [status] -> Consultando status do rebuild runId={}",
                runId
        );

        return ResponseEntity.ok(rebuildRunQueryUseCase.getStatus(runId));
    }

    @Operation(
            summary = "Histórico paginado de rebuilds do usuário",
            description = """
                    Lista as execuções de rebuild associadas ao usuário informado.

                    A paginação utiliza os limites configurados pela aplicação.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de execuções retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao consultar o histórico")
    })
    @GetMapping
    public ResponseEntity<PageResponse<RebuildRunResult>> list(
            @RequestParam("userId") @NotNull UUID userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        int resolvedPage = page == null || page < 0 ? 0 : page;
        int resolvedSize = resolveSize(size);

        log.debug(
                "[RebuildController] - [list] -> Consultando histórico userId={} page={} size={}",
                userId,
                resolvedPage,
                resolvedSize
        );

        var pageResult = rebuildRunQueryUseCase.listByUser(
                userId,
                resolvedPage,
                resolvedSize
        );

        return ResponseEntity.ok(PageResponse.from(pageResult, result -> result));
    }

    // Limita o tamanho solicitado aos valores configurados para paginação.
    private int resolveSize(Integer size) {
        int defaultSize = properties.pagination() != null
                ? properties.pagination().defaultSize()
                : 20;

        int maxSize = properties.pagination() != null
                ? properties.pagination().maxSize()
                : 100;

        if (size == null) {
            return defaultSize;
        }

        if (size < 1) {
            return 1;
        }

        return Math.min(size, maxSize);
    }
}
