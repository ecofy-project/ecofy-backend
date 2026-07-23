package br.com.ecofy.ms_insights.adapters.in.web;

import br.com.ecofy.ms_insights.adapters.in.web.dto.request.GenerateInsightsRequest;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.InsightsBundleResponse;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.InsightResponse;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.MetricSnapshotResponse;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.GoalResponse;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.PageResponse;
import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.application.command.GenerateInsightsCommand;
import br.com.ecofy.ms_insights.core.application.result.InsightsBundleResult;
import br.com.ecofy.ms_insights.core.port.in.GenerateInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.in.GetDashboardInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.in.ListInsightsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Expõe a geração e a consulta de insights do usuário.
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Insights", description = "Geração e consulta de insights do dashboard por usuário")
@RequestMapping(path = "/api/insights/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class InsightsController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final GenerateInsightsUseCase generateInsightsUseCase;
    private final GetDashboardInsightsUseCase getDashboardInsightsUseCase;
    private final ListInsightsUseCase listInsightsUseCase;
    private final InsightsProperties properties;

    @Operation(
            summary = "Dashboard de insights por usuário",
            description = """
                    Retorna o bundle consolidado do dashboard do usuário:
                    - insights (cards)
                    - métricas (snapshot)
                    - goals
                                        
                    Resposta:
                    - 200 (OK) com o bundle do dashboard.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dashboard retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = InsightsBundleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao buscar dashboard")
    })
    @GetMapping("/dashboard/{userId}")
    public ResponseEntity<InsightsBundleResponse> dashboard(@PathVariable("userId") @NotNull UUID userId) {
        log.debug("[InsightsController] - [dashboard] -> userId={}", userId);

        InsightsBundleResult bundle = getDashboardInsightsUseCase.getDashboard(userId);
        return ResponseEntity.ok(toResponse(bundle));
    }

    @Operation(
            summary = "Gera insights para um período",
            description = """
                    Gera (ou recalcula) insights para um usuário no período informado.
                                        
                    Idempotência:
                    - Envie o header `Idempotency-Key` para garantir operação segura em retries.
                                        
                    Resposta:
                    - 200 (OK) com o bundle gerado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Insights gerados com sucesso",
                    content = @Content(schema = @Schema(implementation = InsightsBundleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotência"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao gerar insights")
    })
    @PostMapping(path = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InsightsBundleResponse> generate(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars")
            String idempotencyKey,
            @Valid @RequestBody GenerateInsightsRequest request
    ) {
        log.info(
                "[InsightsController] - [generate] -> Gerando insights userId={} start={} end={} granularity={} hasIdempotencyKey={}",
                request.userId(), request.start(), request.end(), request.granularity(), idempotencyKey != null
        );

        InsightsBundleResult result = generateInsightsUseCase.generate(new GenerateInsightsCommand(
                request.userId(),
                request.start(),
                request.end(),
                request.granularity(),
                idempotencyKey
        ));

        return ResponseEntity.ok(toResponse(result));
    }

    @Operation(
            summary = "Histórico paginado de insights do usuário",
            description = "Lista insights por usuário (ECO-10). `userId` entra no predicado da query (não filtra em memória)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de insights"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/insights")
    public ResponseEntity<PageResponse<InsightResponse>> list(
            @RequestParam("userId") @NotNull UUID userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        int resolvedPage = (page == null || page < 0) ? 0 : page;
        int resolvedSize = resolveSize(size);

        var pageResult = listInsightsUseCase.listByUser(userId, resolvedPage, resolvedSize);
        var response = PageResponse.from(pageResult, i -> new InsightResponse(
                i.id(), i.userId(), i.type(), i.score(), i.title(), i.summary(), i.payload(), i.createdAt()));
        return ResponseEntity.ok(response);
    }

    // Limita o tamanho solicitado aos valores configurados para paginação.
    private int resolveSize(Integer size) {
        int def = properties.pagination() != null ? properties.pagination().defaultSize() : 20;
        int max = properties.pagination() != null ? properties.pagination().maxSize() : 100;
        if (size == null) return def;
        if (size < 1) return 1;
        return Math.min(size, max);
    }

    // Converte o resultado consolidado para o contrato da API.
    private static InsightsBundleResponse toResponse(InsightsBundleResult bundle) {
        List<InsightResponse> insights = bundle.insights().stream()
                .map(i -> new InsightResponse(
                        i.id(),
                        i.userId(),
                        i.type(),
                        i.score(),
                        i.title(),
                        i.summary(),
                        i.payload(),
                        i.createdAt()
                ))
                .toList();

        List<MetricSnapshotResponse> metrics = bundle.metrics().stream()
                .map(m -> new MetricSnapshotResponse(
                        m.id(),
                        m.userId(),
                        m.metricType(),
                        m.valueCents(),
                        m.currency(),
                        m.createdAt()
                ))
                .toList();

        List<GoalResponse> goals = bundle.goals().stream()
                .map(g -> new GoalResponse(
                        g.id(),
                        g.userId(),
                        g.name(),
                        g.targetCents(),
                        g.currency(),
                        g.status(),
                        g.createdAt(),
                        g.updatedAt()
                ))
                .toList();

        return new InsightsBundleResponse(insights, metrics, goals);
    }

}
