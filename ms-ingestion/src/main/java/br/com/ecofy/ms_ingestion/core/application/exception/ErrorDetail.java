package br.com.ecofy.ms_ingestion.core.application.exception;

// Descreve um detalhe de erro no core, indicando linha, campo e código sem depender de HTTP.
public record ErrorDetail(Integer row, String field, String code, String message) {

    public static ErrorDetail ofField(String field, String code, String message) {
        return new ErrorDetail(null, field, code, message);
    }

    public static ErrorDetail ofRow(int row, String field, String code, String message) {
        return new ErrorDetail(row, field, code, message);
    }
}
