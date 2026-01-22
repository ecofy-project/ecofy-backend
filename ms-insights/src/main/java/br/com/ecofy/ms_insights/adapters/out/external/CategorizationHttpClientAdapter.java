package br.com.ecofy.ms_insights.adapters.out.external;

import br.com.ecofy.ms_insights.config.ExternalClientsProperties;
import br.com.ecofy.ms_insights.config.HttpClientConfig;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.port.out.CategorizedTxView;
import br.com.ecofy.ms_insights.core.port.out.LoadCategorizedTransactionsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class CategorizationHttpClientAdapter implements LoadCategorizedTransactionsPort {

    private final ExternalClientsProperties props;
    private final WebClient webClient;

    public CategorizationHttpClientAdapter(ExternalClientsProperties props, WebClient.Builder builder) {
        this.props = props;
        var c = props.categorization();
        this.webClient = HttpClientConfig.build(builder, c.connectTimeoutMs(), c.readTimeoutMs());
    }

    @Override
    public List<CategorizedTxView> loadForUserAndPeriod(UUID userId, Period period, int limit) {
        var c = props.categorization();
        if (!c.enabled()) {
            log.debug("[CategorizationHttpClientAdapter] disabled -> returning empty list userId={}", userId);
            return List.of();
        }

        try {
            // Endpoint “placeholder”: ajuste quando seu ms-categorization tiver endpoint real
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
                    .onErrorResume(ex -> {
                        log.warn("[CategorizationHttpClientAdapter] call failed -> fallback empty userId={} reason={}",
                                userId, ex.toString());
                        return Mono.just(new CategorizedTxPageResponse(List.of()));
                    })
                    .block();

            if (response == null || response.items() == null) return List.of();

            return response.items().stream()
                    .map(CategorizationHttpClientAdapter::toView)
                    .toList();

        } catch (Exception ex) {
            log.warn("[CategorizationHttpClientAdapter] unexpected -> fallback empty userId={} reason={}",
                    userId, ex.toString());
            return List.of();
        }
    }

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

    private static void applyCommonHeaders(HttpHeaders headers) {
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        // Hooks para integração real: Trace/Correlation/JWT
        String traceId = currentThreadValue("X-Trace-Id");
        if (StringUtils.hasText(traceId)) headers.set("X-Trace-Id", traceId);

        String correlationId = currentThreadValue("X-Correlation-Id");
        if (StringUtils.hasText(correlationId)) headers.set("X-Correlation-Id", correlationId);

        String bearer = currentThreadValue("Authorization");
        if (StringUtils.hasText(bearer)) headers.set(HttpHeaders.AUTHORIZATION, bearer);
    }

    private static String currentThreadValue(String key) {
        // Placeholder: se você já usa MDC, troque para MDC.get("traceId") etc.
        return null;
    }

    // DTOs internos (não vazam para o core)
    public record CategorizedTxPageResponse(List<CategorizedTxItem> items) {}

    public record CategorizedTxItem(
            UUID transactionId,
            UUID userId,
            UUID categoryId,
            long amountCents,
            String currency,
            LocalDate bookingDate,
            boolean income
    ) { }

}
