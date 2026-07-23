package br.com.ecofy.ms_ingestion.core.application.exception;

import org.springframework.http.HttpStatus;

// Centraliza os códigos de erro do serviço e o status HTTP correspondente, mantendo corpo e resposta coerentes.
public enum IngestionErrorCode {

    // ---- Upload: presença e tamanho ----
    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "FILE_REQUIRED"),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "FILE_EMPTY"),
    FILE_SIZE_LIMIT_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_SIZE_LIMIT_EXCEEDED"),

    // ---- Upload: tipo ----
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE"),

    // ---- Conteúdo do arquivo (estrutural -> falha global) ----
    EMPTY_FILE(HttpStatus.UNPROCESSABLE_ENTITY, "EMPTY_FILE"),
    INVALID_FILE_CONTENT(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_FILE_CONTENT"),
    INVALID_FILE_ENCODING(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_FILE_ENCODING"),
    INVALID_FILE_HEADER(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_FILE_HEADER"),
    FILE_LINE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "FILE_LINE_LIMIT_EXCEEDED"),
    FILE_LINE_TOO_LONG(HttpStatus.UNPROCESSABLE_ENTITY, "FILE_LINE_TOO_LONG"),
    FILE_COLUMN_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "FILE_COLUMN_LIMIT_EXCEEDED"),

    // ---- Erro por linha (não derruba o arquivo) ----
    INVALID_FILE_ROW(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_FILE_ROW"),
    FILE_FIELD_TOO_LONG(HttpStatus.UNPROCESSABLE_ENTITY, "FILE_FIELD_TOO_LONG"),

    // ---- Idempotência ----
    IMPORT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "IMPORT_ALREADY_PROCESSED"),
    IDEMPOTENCY_KEY_PAYLOAD_MISMATCH(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_PAYLOAD_MISMATCH"),

    // ---- Job ----
    IMPORT_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "IMPORT_JOB_NOT_FOUND"),
    IMPORT_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "IMPORT_ACCESS_FORBIDDEN"),
    IMPORT_PROCESSING_TIMEOUT(HttpStatus.UNPROCESSABLE_ENTITY, "IMPORT_PROCESSING_TIMEOUT"),

    // ---- Paginação ----
    PAGINATION_PARAMETER_INVALID(HttpStatus.BAD_REQUEST, "PAGINATION_PARAMETER_INVALID"),

    // ---- Kafka ----
    KAFKA_PUBLICATION_FAILED(HttpStatus.BAD_GATEWAY, "KAFKA_PUBLICATION_FAILED"),
    INVALID_KAFKA_MESSAGE(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_KAFKA_MESSAGE"),
    UNSUPPORTED_EVENT_VERSION(HttpStatus.INTERNAL_SERVER_ERROR, "UNSUPPORTED_EVENT_VERSION"),
    EMPTY_TRANSACTIONS_PAYLOAD(HttpStatus.BAD_REQUEST, "EMPTY_TRANSACTIONS_PAYLOAD"),

    // ---- Internos (nunca detalhados ao cliente) ----
    INVALID_COMMAND(HttpStatus.BAD_REQUEST, "INVALID_COMMAND"),
    IMPORT_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMPORT_FILE_NOT_FOUND"),
    IMPORT_FILE_STORED_PATH_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "IMPORT_FILE_STORED_PATH_MISSING"),
    STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR"),
    PERSISTENCE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PERSISTENCE_ERROR"),
    INTERNAL_INGESTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_INGESTION_ERROR");

    private final HttpStatus httpStatus;
    private final String code;

    IngestionErrorCode(HttpStatus httpStatus, String code) {
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }
}
