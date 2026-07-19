package br.com.ecofy.gateway.api_gateway.error;

import org.springframework.http.HttpStatus;

// Centraliza os códigos, status e mensagens seguras dos erros do Gateway.
public enum GatewayErrorCode {

    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT,
            "O serviço solicitado não respondeu dentro do tempo esperado."),

    DOWNSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
            "O serviço solicitado está temporariamente indisponível."),

    CIRCUIT_BREAKER_OPEN(HttpStatus.SERVICE_UNAVAILABLE,
            "O serviço solicitado está temporariamente indisponível."),

    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "O recurso solicitado não foi encontrado."),

    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED,
            "Método HTTP não suportado para este recurso."),

    INVALID_REQUEST(HttpStatus.BAD_REQUEST,
            "A requisição contém dados inválidos."),

    INTERNAL_GATEWAY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "Erro interno ao processar a requisição.");

    private final HttpStatus status;
    private final String defaultMessage;

    GatewayErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
