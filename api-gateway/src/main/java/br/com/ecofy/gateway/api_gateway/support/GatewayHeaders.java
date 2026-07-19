package br.com.ecofy.gateway.api_gateway.support;

// Centraliza os headers e as chaves de contexto utilizados pelo Gateway.
public final class GatewayHeaders {

    // Define o header utilizado para propagar o correlation ID.
    public static final String CORRELATION_ID = "X-Correlation-Id";

    // Define o atributo que armazena o correlation ID no exchange.
    public static final String CORRELATION_ID_ATTR = "ecofy.gateway.correlationId";

    // Define a chave utilizada para propagar o correlation ID no contexto reativo.
    public static final String CORRELATION_ID_CONTEXT_KEY = "correlationId";

    private GatewayHeaders() {
    }
}
