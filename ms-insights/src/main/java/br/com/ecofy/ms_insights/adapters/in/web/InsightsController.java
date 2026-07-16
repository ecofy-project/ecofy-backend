package br.com.ecofy.ms_insights.adapters.in.web;

import br.com.ecofy.ms_insights.adapters.in.web.dto.request.GenerateInsightsRequest;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.InsightsBundleResponse;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.InsightResponse;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.MetricSnapshotResponse;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.GoalResponse;
import br.com.ecofy.ms_insights.core.application.command.GenerateInsightsCommand;
import br.com.ecofy.ms_insights.core.application.result.InsightsBundleResult;
import br.com.ecofy.ms_insights.core.port.in.GenerateInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.in.GetDashboardInsightsUseCase;
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
                "[InsightsController] - [generate] -> Generating insights userId={} start={} end={} granularity={} hasIdempotencyKey={}",
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

        // Correção Dia 8 (item #3): mapeamento direto List<MetricSnapshotResult> -> List<MetricSnapshotResponse>,
        // sem aninhamento artificial (antes era lista de lista de lista via singletonList duplo).
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
