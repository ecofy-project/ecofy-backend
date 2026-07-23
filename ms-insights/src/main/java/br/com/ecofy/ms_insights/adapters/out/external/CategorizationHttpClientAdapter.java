package br.com.ecofy.ms_insights.adapters.out.external;

import br.com.ecofy.ms_insights.config.ExternalClientsProperties;
import br.com.ecofy.ms_insights.config.HttpClientConfig;
import br.com.ecofy.ms_insights.core.domain.exception.ExternalDataUnavailableException;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.port.out.CategorizedTxView;
import br.com.ecofy.ms_insights.core.port.out.LoadCategorizedTransactionsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Integra a consulta de transações categorizadas com o serviço de categorization.
@Slf4j
@Component
public class CategorizationHttpClientAdapter implements LoadCategorizedTransactionsPort {

    private final ExternalClientsProperties props;
    private final WebClient webClient;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    public CategorizationHttpClientAdapter(
            ExternalClientsProperties props,
            WebClient.Builder builder,
            io.micrometer.core.instrument.MeterRegistry meterRegistry
    ) {
        this.props = props;
        this.meterRegistry = meterRegistry;
        var c = props.categorization();
        this.webClient = HttpClientConfig.build(builder, c.connectTimeoutMs(), c.readTimeoutMs());
    }

    // Carrega as transações categorizadas do usuário no período informado.
    @Override
    public List<CategorizedTxView> loadForUserAndPeriod(UUID userId, Period period, int limit) {
        var c = props.categorization();

        if (!c.enabled()) {
            meterRegistry.counter(
                    "ecofy.insights.fallback.total",
                    "provider",
                    "categorization",
                    "reason",
                    "disabled"
            ).increment();

            log.debug(
                    "[CategorizationHttpClientAdapter] disabled -> returning empty list (legitimate) userId={}",
                    userId
            );

            return List.of();
        }

        try {
            String url = c.baseUrl() + "/api/categorization/v1/transactions/categorized";

            var response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(url)
                            .queryParam("userId", userId)
                            .queryParam("start", period.start())
                            .queryParam("end", period.end())
                            .queryParam("limit", limit)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(h -> applyCommonHeaders(h))
                    .retrieve()
                    .bodyToMono(CategorizedTxPageResponse.class)
                    .timeout(java.time.Duration.ofMillis(c.readTimeoutMs()))
                    .block();

            if (response == null || response.items() == null) {
                return List.of();
            }

            return response.items().stream()
                    .map(CategorizationHttpClientAdapter::toView)
                    .toList();

        } catch (Exception ex) {
            log.error(
                    "[CategorizationHttpClientAdapter] call FAILED (enabled) userId={} reason={}",
                    userId,
                    ex.toString()
            );

            throw new ExternalDataUnavailableException(
                    "categorization",
                    "Failed to load categorized transactions from ms-categorization",
                    ex
            );
        }
    }

    // Converte o item externo para a representação utilizada pelo núcleo da aplicação.
    private static CategorizedTxView toView(CategorizedTxItem i) {
        return new CategorizedTxView(
                i.transactionId(),
                i.userId(),
                i.categoryId(),
                i.amountCents(),
                i.currency(),
                i.bookingDate(),
                i.income()
        );
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

    // Representa a resposta paginada recebida do serviço de categorization.
    public record CategorizedTxPageResponse(List<CategorizedTxItem> items) {
    }

    // Representa uma transação categorizada recebida da integração externa.
    public record CategorizedTxItem(
            UUID transactionId,
            UUID userId,
            UUID categoryId,
            long amountCents,
            String currency,
            LocalDate bookingDate,
            boolean income
    ) {
    }
}
