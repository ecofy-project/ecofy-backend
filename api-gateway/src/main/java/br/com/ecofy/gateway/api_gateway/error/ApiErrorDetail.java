package br.com.ecofy.gateway.api_gateway.error;

// Representa um detalhe seguro associado à resposta de erro.
public record ApiErrorDetail(
        String field,
        String code,
        String message
) { }
