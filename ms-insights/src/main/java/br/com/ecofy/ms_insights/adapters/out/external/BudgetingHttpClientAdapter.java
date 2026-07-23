package br.com.ecofy.ms_insights.adapters.out.external;

import br.com.ecofy.ms_insights.config.ExternalClientsProperties;
import br.com.ecofy.ms_insights.config.HttpClientConfig;
import br.com.ecofy.ms_insights.core.domain.exception.ExternalDataUnavailableException;
import br.com.ecofy.ms_insights.core.port.out.LoadBudgetsForUserPort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

// Integra a consulta de orçamentos do usuário com o serviço de budgeting.
@Slf4j
@Component
public class BudgetingHttpClientAdapter implements LoadBudgetsForUserPort {

    private final ExternalClientsProperties props;
    private final WebClient webClient;
    private final MeterRegistry meterRegistry;

    public BudgetingHttpClientAdapter(
            ExternalClientsProperties props,
            WebClient.Builder builder,
            MeterRegistry meterRegistry
    ) {
        this.props = props;
        this.meterRegistry = meterRegistry;
        var b = props.budgeting();
        this.webClient = HttpClientConfig.build(builder, b.connectTimeoutMs(), b.readTimeoutMs());
    }

    // Carrega os orçamentos do usuário e propaga falhas externas de forma controlada.
    @Override
    public List<BudgetView> loadBudgets(UUID userId) {
        var b = props.budgeting();

        if (!b.enabled()) {
            meterRegistry.counter(
                    "ecofy.insights.fallback.total",
                    "provider",
                    "budgeting",
                    "reason",
                    "disabled"
            ).increment();

            log.debug(
                    "[BudgetingHttpClientAdapter] disabled -> returning empty list (legitimate) userId={}",
                    userId
            );

            return List.of();
        }

        try {
            String url = b.baseUrl() + "/api/budgeting/v1/budgets/user/" + userId;

            var response = webClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(BudgetingHttpClientAdapter::applyCommonHeaders)
                    .retrieve()
                    .bodyToMono(BudgetListResponse.class)
                    .timeout(java.time.Duration.ofMillis(b.readTimeoutMs()))
                    .block();

            if (response == null || response.items() == null) {
                return List.of();
            }

            return response.items().stream()
                    .map(item -> new BudgetView(
                            item.budgetId(),
                            item.categoryId(),
                            item.limitCents(),
                            item.currency(),
                            item.status()
                    ))
                    .toList();
        } catch (Exception ex) {
            log.error(
                    "[BudgetingHttpClientAdapter] call FAILED (enabled) userId={} reason={}",
                    userId,
                    ex.toString()
            );

            throw new ExternalDataUnavailableException(
                    "budgeting",
                    "Failed to load budgets from ms-budgeting",
                    ex
            );
        }
    }

    // Propaga os cabeçalhos de rastreamento e autenticação disponíveis no contexto atual.
    private static void applyCommonHeaders(HttpHeaders headers) {
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        String traceId = currentThreadValue("X-Trace-Id");
        if (StringUtils.hasText(traceId)) {
            headers.set("X-Trace-Id", traceId);
        }

        String correlationId = currentThreadValue("X-Correlation-Id");
        if (StringUtils.hasText(correlationId)) {
            headers.set("X-Correlation-Id", correlationId);
        }

        String bearer = currentThreadValue("Authorization");
        if (StringUtils.hasText(bearer)) {
            headers.set(HttpHeaders.AUTHORIZATION, bearer);
        }
    }

    // Resolve um valor associado ao contexto da thread atual.
    private static String currentThreadValue(String key) {
        return null;
    }

    // Representa a resposta interna recebida do serviço de budgeting.
    public record BudgetListResponse(List<BudgetItem> items) {
    }

    // Representa um orçamento retornado pela integração externa.
    public record BudgetItem(
            UUID budgetId,
            UUID categoryId,
            long limitCents,
            String currency,
            String status
    ) {
    }
}
