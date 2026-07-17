package br.com.ecofy.gateway.api_gateway.support;

/**
 * Nomes de headers e chaves de atributo usados de forma transversal pelo gateway.
 * Centralizado para evitar strings mágicas duplicadas entre filtros, handler de
 * erro e fallback (nomes de header HTTP são case-insensitive por definição).
 */
public final class GatewayHeaders {

    /** Header oficial de correlação (ECO-05). */
    public static final String CORRELATION_ID = "X-Correlation-Id";

    /** Atributo do ServerWebExchange onde o correlation ID resolvido é armazenado. */
    public static final String CORRELATION_ID_ATTR = "ecofy.gateway.correlationId";

    /** Chave do Reactor Context / MDC usada para propagar o correlation ID nos logs. */
    public static final String CORRELATION_ID_CONTEXT_KEY = "correlationId";

    private GatewayHeaders() {
    }
}
