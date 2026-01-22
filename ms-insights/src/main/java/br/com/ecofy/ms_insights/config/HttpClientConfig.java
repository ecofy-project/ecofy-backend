package br.com.ecofy.ms_insights.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class HttpClientConfig {

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 2_000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 5_000;
    private static final int DEFAULT_MAX_IN_MEMORY_MB = 2;

    // Disponibiliza um WebClient.Builder padrão com limite de memória para codecs, prevenindo uso excessivo de heap em respostas grandes.
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Limite de memória para evitar payloads gigantes estourarem heap (ajuste se necessário).
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(DEFAULT_MAX_IN_MEMORY_MB * 1024 * 1024))
                .build();

        return WebClient.builder()
                .exchangeStrategies(strategies);
    }

    // Constrói um WebClient com timeouts configuráveis (connect/read), aplicando defaults e registrando parâmetros para observabilidade.
    public static WebClient build(WebClient.Builder builder, Integer connectTimeoutMs, Integer readTimeoutMs) {
        Assert.notNull(builder, "builder must not be null");

        int ct = normalize(connectTimeoutMs, DEFAULT_CONNECT_TIMEOUT_MS, "connectTimeoutMs");
        int rt = normalize(readTimeoutMs, DEFAULT_READ_TIMEOUT_MS, "readTimeoutMs");

        log.debug("[HttpClientConfig] - [build] -> Creating WebClient connectTimeoutMs={} readTimeoutMs={}", ct, rt);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ct)
                .responseTimeout(Duration.ofMillis(rt))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(rt, TimeUnit.MILLISECONDS)));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    // Normaliza valores opcionais de timeout aplicando fallback quando nulo e validando > 0 quando informado.
    private static int normalize(Integer value, int fallback, String field) {
        if (value == null) return fallback;
        if (value <= 0) throw new IllegalArgumentException(field + " must be > 0");
        return value;
    }

}
