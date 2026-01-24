package br.com.ecofy.ms_users.adapters.in.web.advice;

import java.time.Instant;

public record ApiErrorResponse(

        String code,

        String message,

        Instant timestamp,

        String path

) {

    // Cria uma resposta de erro padronizada preenchendo code/message/path e gerando automaticamente o timestamp atual.
    public static ApiErrorResponse of(String code, String message, String path) {
        return new ApiErrorResponse(code, message, Instant.now(), path);
    }

}
