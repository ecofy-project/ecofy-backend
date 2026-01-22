package br.com.ecofy.ms_notification.adapters.in.web.advice;

import java.time.Instant;

public record ApiErrorResponse(

        String errorCode,

        String message,

        Instant timestamp,

        String path

) {

    public static ApiErrorResponse of(String errorCode, String message, String path) {
        return new ApiErrorResponse(errorCode, message, Instant.now(), path);
    }

}
