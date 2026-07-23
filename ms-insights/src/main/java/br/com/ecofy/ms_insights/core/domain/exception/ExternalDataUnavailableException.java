package br.com.ecofy.ms_insights.core.domain.exception;

// Sinaliza falha de uma integração externa habilitada, distinguindo erro real de ausência legítima de dados.
public class ExternalDataUnavailableException extends RuntimeException {

    private final String source;

    public ExternalDataUnavailableException(String source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
