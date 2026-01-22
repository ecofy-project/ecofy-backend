package br.com.ecofy.ms_insights.adapters.out.external;

import br.com.ecofy.ms_insights.config.ExternalClientsProperties;
import br.com.ecofy.ms_insights.config.HttpClientConfig;
import br.com.ecofy.ms_insights.core.port.out.LoadBudgetsForUserPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class BudgetingHttpClientAdapter implements LoadBudgetsForUserPort {

    private final ExternalClientsProperties props;
    private final WebClient webClient;

    // Inicializa o adapter com propriedades do client externo e constrói um WebClient configurado com timeouts.
    public BudgetingHttpClientAdapter(ExternalClientsProperties props, WebClient.Builder builder) {
        this.props = props;
        var b = props.budgeting();
        this.webClient = HttpClientConfig.build(builder, b.connectTimeoutMs(), b.readTimeoutMs());
    }

    // Carrega budgets do serviço externo de budgeting para um usuário, com feature-flag (enabled) e fallback resiliente para lista vazia.
    @Override
    public List<BudgetView> loadBudgets(UUID userId) {
        var b = props.budgeting();
        if (!b.enabled()) {
            log.debug("[BudgetingHttpClientAdapter] disabled -> returning empty list userId={}", userId);
            return List.of();
        }

        try {
            // Endpoint “placeholder”: ajuste para o seu BudgetController real
            String url = b.baseUrl() + "/api/budgeting/v1/budgets/user/" + userId;

            var response = webClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(BudgetingHttpClientAdapter::applyCommonHeaders)
                    .retrieve()
                    .bodyToMono(BudgetListResponse.class)
                    .timeout(java.time.Duration.ofMillis(b.readTimeoutMs()))
                    .onErrorResume(ex -> {
                        log.warn("[BudgetingHttpClientAdapter] call failed -> fallback empty userId={} reason={}",
                                userId, ex.toString());
                        return Mono.just(new BudgetListResponse(List.of()));
                    })
                    .block();

            if (response == null || response.items() == null) return List.of();

            return response.items().stream()
                    .map(i -> new BudgetView(i.budgetId(), i.categoryId(), i.limitCents(), i.currency(), i.status()))
                    .toList();

        } catch (Exception ex) {
            log.warn("[BudgetingHttpClientAdapter] unexpected -> fallback empty userId={} reason={}",
                    userId, ex.toString());
            return List.of();
        }
    }

    // Aplica headers comuns (Accept, trace/correlation e Authorization) propagando contexto do thread quando disponível.
    private static void applyCommonHeaders(HttpHeaders headers) {
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        String traceId = currentThreadValue("X-Trace-Id");
        if (StringUtils.hasText(traceId)) headers.set("X-Trace-Id", traceId);

        String correlationId = currentThreadValue("X-Correlation-Id");
        if (StringUtils.hasText(correlationId)) headers.set("X-Correlation-Id", correlationId);

        String bearer = currentThreadValue("Authorization");
        if (StringUtils.hasText(bearer)) headers.set(HttpHeaders.AUTHORIZATION, bearer);
    }

    // Recupera um valor de contexto associado à thread atual (ex.: MDC/Reactor Context) para propagação de headers.
    private static String currentThreadValue(String key) {
        return null;
    }

    // DTOs internos (não vazam para o core)
    public record BudgetListResponse(List<BudgetItem> items) {}

    // Representa um item retornado pelo serviço externo de budgeting para mapeamento em BudgetView.
    public record BudgetItem(
            UUID budgetId,
            UUID categoryId,
            long limitCents,
            String currency,
            String status
    ) { }

}
