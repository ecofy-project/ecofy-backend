package br.com.ecofy.gateway.api_gateway.error;

/**
 * Detalhe controlado de erro (ECO-04). Campos genéricos e seguros para o
 * consumidor: nunca carrega stack trace, nome de classe interna, host/porta
 * de microsserviço ou valores sensíveis.
 */
public record ApiErrorDetail(
        String field,
        String code,
        String message
) {
}
